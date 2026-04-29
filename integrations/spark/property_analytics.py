import argparse
from pathlib import Path

from pyspark.sql import SparkSession
from pyspark.sql import functions as F


def parse_args():
    parser = argparse.ArgumentParser(description="Generate housing analytics with Spark.")
    parser.add_argument("--jdbc-url", required=True, help="MySQL JDBC URL, e.g. jdbc:mysql://host:3306/housing_db")
    parser.add_argument("--db-user", required=True, help="MySQL username")
    parser.add_argument("--db-password", required=True, help="MySQL password")
    parser.add_argument("--output", default="frontend/public/spark-analytics", help="Output directory for JSON reports")
    return parser.parse_args()


def load_table(spark, jdbc_url, user, password, table):
    return (
        spark.read.format("jdbc")
        .option("url", jdbc_url)
        .option("dbtable", table)
        .option("user", user)
        .option("password", password)
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .load()
    )


def write_json(df, output_dir, name):
    target = Path(output_dir) / name
    (
        df.coalesce(1)
        .write.mode("overwrite")
        .json(str(target))
    )


def main():
    args = parse_args()
    spark = (
        SparkSession.builder
        .appName("housing-platform-analytics")
        .getOrCreate()
    )

    properties = load_table(spark, args.jdbc_url, args.db_user, args.db_password, "properties")
    tickets = load_table(spark, args.jdbc_url, args.db_user, args.db_password, "support_tickets")

    approved = properties.filter(F.col("status") == "APPROVED")

    price_by_type = (
        approved
        .groupBy(F.coalesce(F.col("type"), F.lit("UNKNOWN")).alias("type"))
        .agg(
            F.count("*").alias("propertyCount"),
            F.round(F.avg("price"), 2).alias("avgPrice"),
            F.round(F.avg("area"), 2).alias("avgArea"),
        )
        .orderBy(F.desc("propertyCount"))
    )

    ticket_pressure = (
        tickets
        .groupBy(
            F.coalesce(F.col("status"), F.lit("OPEN")).alias("status"),
            F.coalesce(F.col("priority"), F.lit("MEDIUM")).alias("priority"),
        )
        .agg(F.count("*").alias("ticketCount"))
        .orderBy(F.desc("ticketCount"))
    )

    hot_tags = (
        approved
        .select(F.explode(F.split(F.coalesce(F.col("tags"), F.lit("")), ",")).alias("tag"))
        .select(F.trim(F.col("tag")).alias("tag"))
        .filter(F.length("tag") > 0)
        .groupBy("tag")
        .agg(F.count("*").alias("tagCount"))
        .orderBy(F.desc("tagCount"))
        .limit(20)
    )

    write_json(price_by_type, args.output, "price_by_type")
    write_json(ticket_pressure, args.output, "ticket_pressure")
    write_json(hot_tags, args.output, "hot_tags")

    spark.stop()


if __name__ == "__main__":
    main()
