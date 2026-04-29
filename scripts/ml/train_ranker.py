import json
import os
from collections import defaultdict
from pathlib import Path

import requests

try:
    import lightgbm as lgb
except ImportError:
    lgb = None


ROOT = Path(__file__).resolve().parents[2]
ARTIFACTS = ROOT / "scripts" / "ml" / "artifacts"
ARTIFACTS.mkdir(parents=True, exist_ok=True)


def build_features(dataset):
    properties = {item["id"]: item for item in dataset.get("properties", [])}
    requirements_by_user = defaultdict(list)
    for req in dataset.get("requirements", []):
        requirements_by_user[req["userId"]].append(req)

    rows = []
    for interaction in dataset.get("interactions", []):
        prop = properties.get(interaction["propertyId"])
        if not prop:
            continue

        reqs = requirements_by_user.get(interaction["userId"], [])
        req = reqs[0] if reqs else {}
        tags = str(prop.get("tags") or "").lower()
        preferred_area = str(req.get("preferredArea") or "").lower()
        layout_preference = str(req.get("layoutPreference") or "").lower()
        prop_type = str(prop.get("type") or "").lower()
        req_type = str(req.get("propertyType") or "").lower()

        rows.append({
            "userId": interaction["userId"],
            "propertyId": interaction["propertyId"],
            "label": float(interaction.get("weight") or 1.0),
            "price": float(prop.get("price") or 0.0),
            "area": float(prop.get("area") or 0.0),
            "hasPanorama": 1.0 if prop.get("hasPanorama") else 0.0,
            "hasModel3d": 1.0 if prop.get("hasModel3d") else 0.0,
            "hasFloorPlan": 1.0 if prop.get("hasFloorPlan") else 0.0,
            "areaMatch": 1.0 if preferred_area and preferred_area in str(prop.get("address") or "").lower() else 0.0,
            "layoutMatch": 1.0 if layout_preference and layout_preference in str(prop.get("layout") or "").lower() else 0.0,
            "typeMatch": 1.0 if req_type and req_type in prop_type else 0.0,
            "subwayTag": 1.0 if "subway" in tags or "地铁" in tags else 0.0,
        })
    return rows


def train_model(rows):
    if not rows:
        raise RuntimeError("No training rows were generated.")

    feature_names = [
        "price",
        "area",
        "hasPanorama",
        "hasModel3d",
        "hasFloorPlan",
        "areaMatch",
        "layoutMatch",
        "typeMatch",
        "subwayTag",
    ]

    if lgb is None:
        return None, feature_names

    grouped = defaultdict(list)
    for row in rows:
        grouped[row["userId"]].append(row)

    X, y, group = [], [], []
    for user_rows in grouped.values():
        group.append(len(user_rows))
        for row in user_rows:
            X.append([row[name] for name in feature_names])
            y.append(row["label"])

    dataset = lgb.Dataset(X, label=y, group=group, feature_name=feature_names)
    params = {
        "objective": "lambdarank",
        "metric": "ndcg",
        "learning_rate": 0.05,
        "num_leaves": 31,
        "min_data_in_leaf": 5,
        "verbosity": -1,
    }
    model = lgb.train(params, dataset, num_boost_round=80)
    model.save_model(str(ARTIFACTS / "lightgbm_ranker.txt"))
    return model, feature_names


def score_all(dataset, rows, model, feature_names):
    properties = dataset.get("properties", [])
    property_map = {item["id"]: item for item in properties}
    users = dataset.get("users", [])
    requirements_by_user = defaultdict(list)
    for req in dataset.get("requirements", []):
        requirements_by_user[req["userId"]].append(req)

    predictions = []
    for user in users:
        reqs = requirements_by_user.get(user["id"], [])
        req = reqs[0] if reqs else {}
        for prop in properties:
            tags = str(prop.get("tags") or "").lower()
            preferred_area = str(req.get("preferredArea") or "").lower()
            layout_preference = str(req.get("layoutPreference") or "").lower()
            prop_type = str(prop.get("type") or "").lower()
            req_type = str(req.get("propertyType") or "").lower()
            feature_row = {
                "price": float(prop.get("price") or 0.0),
                "area": float(prop.get("area") or 0.0),
                "hasPanorama": 1.0 if prop.get("hasPanorama") else 0.0,
                "hasModel3d": 1.0 if prop.get("hasModel3d") else 0.0,
                "hasFloorPlan": 1.0 if prop.get("hasFloorPlan") else 0.0,
                "areaMatch": 1.0 if preferred_area and preferred_area in str(prop.get("address") or "").lower() else 0.0,
                "layoutMatch": 1.0 if layout_preference and layout_preference in str(prop.get("layout") or "").lower() else 0.0,
                "typeMatch": 1.0 if req_type and req_type in prop_type else 0.0,
                "subwayTag": 1.0 if "subway" in tags or "地铁" in tags else 0.0,
            }
            if model is not None:
                score = float(model.predict([[feature_row[name] for name in feature_names]])[0])
            else:
                score = (
                    feature_row["areaMatch"] * 2.0
                    + feature_row["layoutMatch"] * 1.5
                    + feature_row["typeMatch"] * 1.5
                    + feature_row["hasPanorama"] * 0.8
                    + feature_row["hasModel3d"] * 1.2
                    + feature_row["subwayTag"] * 0.6
                )
            predictions.append({
                "userId": user["id"],
                "propertyId": prop["id"],
                "score": round(score, 6),
                "reason": "offline ranker score",
            })
    return predictions


def main():
    base_url = os.getenv("ML_API_BASE", "http://localhost:8080")
    token = os.getenv("ML_API_TOKEN", "")
    model_version = os.getenv("ML_MODEL_VERSION", "lgbm_v1")

    headers = {"Authorization": f"Bearer {token}"} if token else {}
    response = requests.get(f"{base_url}/ml/recommendations/dataset", headers=headers, timeout=60)
    response.raise_for_status()
    dataset = response.json()

    dataset_path = ARTIFACTS / "dataset.json"
    dataset_path.write_text(json.dumps(dataset, ensure_ascii=False, indent=2), encoding="utf-8")

    rows = build_features(dataset)
    model, feature_names = train_model(rows)
    predictions = score_all(dataset, rows, model, feature_names)

    output = {
        "modelVersion": model_version,
        "modelName": "lightgbm-ranker" if model is not None else "heuristic-ranker",
        "predictions": predictions,
    }
    predictions_path = ARTIFACTS / "predictions.json"
    predictions_path.write_text(json.dumps(output, ensure_ascii=False, indent=2), encoding="utf-8")

    import_resp = requests.post(
        f"{base_url}/ml/recommendations/predictions/import",
        headers={**headers, "Content-Type": "application/json"},
        json=output,
        timeout=60,
    )
    import_resp.raise_for_status()
    print(import_resp.text)


if __name__ == "__main__":
    main()
