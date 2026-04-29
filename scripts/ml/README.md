# ML 推荐训练脚本

## 作用

- 从后端导出训练数据
- 训练第一版 LightGBM 排序模型
- 生成预测结果
- 回写到后端推荐表

## 准备

1. 启动后端服务
2. 准备一个管理员账号的 JWT
3. 安装依赖

```powershell
pip install -r scripts/ml/requirements.txt
```

## 运行

```powershell
$env:ML_API_BASE="http://localhost:8080"
$env:ML_API_TOKEN="你的管理员JWT"
$env:ML_MODEL_VERSION="lgbm_v1"
python scripts/ml/train_ranker.py
```

## 结果

- 数据集导出到 `scripts/ml/artifacts/dataset.json`
- 模型文件导出到 `scripts/ml/artifacts/lightgbm_ranker.txt`
- 预测结果导出到 `scripts/ml/artifacts/predictions.json`

如果本机没有安装 `lightgbm`，脚本会退回启发式排序，但仍然会生成并导入预测结果，方便你先把整条链路跑通。
