"""Bir martalik skript: reference.json asosida har bir tuman uchun login va
174 XLSX kredensial faylini yaratadi. Ishga tushirish:

    DB_PATH=... python3 seed_users.py path/to/reference.json output.xlsx
"""
import json
import re
import sqlite3
import sys
import time
from pathlib import Path

import bcrypt
import openpyxl


def base_slug(name: str) -> str:
    n = re.sub(r"\s+(tumani|shahri|shahar)$", "", name.strip(), flags=re.IGNORECASE)
    n = n.lower()
    for ch in ["ʻ", "ʼ", "'", "`", "'"]:
        n = n.replace(ch, "")
    return re.sub(r"[^a-z0-9]+", "_", n).strip("_")


def base_title(name: str) -> str:
    n = re.sub(r"\s+(tumani|shahri|shahar)$", "", name.strip(), flags=re.IGNORECASE)
    for ch in ["ʻ", "ʼ", "'", "`", "'"]:
        n = n.replace(ch, "")
    return re.sub(r"[^a-zA-Z0-9]+", "", n).capitalize()


def build_accounts(reference_path: str):
    data = json.loads(Path(reference_path).read_text(encoding="utf-8"))
    regions = {r["id"]: r["name"] for r in data["regions"]}
    districts = data["districts"]

    bases = [base_slug(d["name"]) for d in districts]
    from collections import Counter
    counts = Counter(bases)

    accounts = []
    for d in districts:
        b = base_slug(d["name"])
        t = base_title(d["name"])
        is_city = "shahri" in d["name"].lower()
        if counts[b] > 1:
            username = b + ("_sh" if is_city else "_t")
            password = t + ("Sh" if is_city else "") + "Kochat26!"
        else:
            username = b
            password = t + "Kochat26!"
        accounts.append({
            "username": username,
            "password": password,
            "role": "field",
            "region_id": d["regionId"],
            "region_name": regions[d["regionId"]],
            "district_id": d["id"],
            "district_name": d["name"],
        })
    return accounts


def main():
    reference_path = sys.argv[1]
    output_xlsx = sys.argv[2]
    admin_password = sys.argv[3] if len(sys.argv) > 3 else None

    accounts = build_accounts(reference_path)

    db_path = __import__("os").environ.get("DB_PATH", "/app/data/kochatzor.db")
    conn = sqlite3.connect(db_path)
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            role TEXT NOT NULL,
            region_id INTEGER, region_name TEXT,
            district_id INTEGER, district_name TEXT,
            created_at INTEGER
        )
        """
    )
    now = int(time.time() * 1000)

    if admin_password:
        accounts.append({
            "username": "admin",
            "password": admin_password,
            "role": "admin",
            "region_id": None, "region_name": None,
            "district_id": None, "district_name": None,
        })

    for a in accounts:
        pw_hash = bcrypt.hashpw(a["password"].encode(), bcrypt.gensalt()).decode()
        conn.execute(
            """
            INSERT INTO users (username, password_hash, role, region_id, region_name,
                district_id, district_name, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(username) DO UPDATE SET password_hash=excluded.password_hash
            """,
            (a["username"], pw_hash, a["role"], a["region_id"], a["region_name"],
             a["district_id"], a["district_name"], now),
        )
    conn.commit()
    conn.close()

    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Loginlar"
    ws.append(["Viloyat", "Tuman/Shahar", "Login", "Parol", "Rol"])
    for a in accounts:
        ws.append([a["region_name"] or "—", a["district_name"] or "—", a["username"], a["password"], a["role"]])
    wb.save(output_xlsx)
    print(f"{len(accounts)} ta hisob yaratildi. Kredensiallar: {output_xlsx}")


if __name__ == "__main__":
    main()
