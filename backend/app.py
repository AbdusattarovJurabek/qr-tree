import os
import sqlite3
import time
from contextlib import contextmanager
from io import BytesIO
from pathlib import Path
from typing import List, Optional

from fastapi import FastAPI, Header, HTTPException, Query
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

DB_PATH = os.environ.get("DB_PATH", "/app/data/kochatzor.db")
SYNC_KEY = os.environ.get("SYNC_KEY")
if not SYNC_KEY:
    raise RuntimeError("SYNC_KEY majburiy (.env faylga qo'shing)")

app = FastAPI(title="Ko'chatzor Sync API")


@contextmanager
def get_conn():
    Path(DB_PATH).parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()


def init_db():
    with get_conn() as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS surveys (
                id TEXT PRIMARY KEY,
                regionId INTEGER, districtId INTEGER, mahallaId INTEGER,
                region TEXT, district TEXT, mahalla TEXT,
                fio TEXT, phone TEXT, area REAL, tree TEXT, variety TEXT,
                count INTEGER, planting TEXT, source TEXT, payvandtag TEXT,
                createdAt INTEGER, updatedAt INTEGER, isDeleted INTEGER,
                deviceId TEXT, syncedAt INTEGER
            )
            """
        )


init_db()


def check_key(x_sync_key: Optional[str]):
    if x_sync_key != SYNC_KEY:
        raise HTTPException(status_code=401, detail="Noto'g'ri sync kaliti")


class SurveyIn(BaseModel):
    id: str
    regionId: int
    districtId: int
    mahallaId: int
    region: str
    district: str
    mahalla: str
    fio: str
    phone: str
    area: float
    tree: str
    variety: str
    count: int
    planting: str
    source: str
    payvandtag: str = ""
    createdAt: int
    updatedAt: int
    isDeleted: bool = False


class SyncRequest(BaseModel):
    deviceId: Optional[str] = None
    records: List[SurveyIn]


@app.get("/api/health")
def health():
    return {"status": "ok"}


@app.post("/api/sync")
def sync(payload: SyncRequest, x_sync_key: Optional[str] = Header(default=None)):
    check_key(x_sync_key)
    now = int(time.time() * 1000)
    results = []
    with get_conn() as conn:
        for r in payload.records:
            conn.execute(
                """
                INSERT INTO surveys (id, regionId, districtId, mahallaId, region, district,
                    mahalla, fio, phone, area, tree, variety, count, planting, source,
                    payvandtag, createdAt, updatedAt, isDeleted, deviceId, syncedAt)
                VALUES (:id, :regionId, :districtId, :mahallaId, :region, :district,
                    :mahalla, :fio, :phone, :area, :tree, :variety, :count, :planting, :source,
                    :payvandtag, :createdAt, :updatedAt, :isDeleted, :deviceId, :syncedAt)
                ON CONFLICT(id) DO UPDATE SET
                    regionId=excluded.regionId, districtId=excluded.districtId,
                    mahallaId=excluded.mahallaId, region=excluded.region,
                    district=excluded.district, mahalla=excluded.mahalla, fio=excluded.fio,
                    phone=excluded.phone, area=excluded.area, tree=excluded.tree,
                    variety=excluded.variety, count=excluded.count, planting=excluded.planting,
                    source=excluded.source, payvandtag=excluded.payvandtag,
                    createdAt=excluded.createdAt, updatedAt=excluded.updatedAt,
                    isDeleted=excluded.isDeleted, deviceId=excluded.deviceId,
                    syncedAt=excluded.syncedAt
                """,
                {
                    **r.model_dump(),
                    "isDeleted": int(r.isDeleted),
                    "deviceId": payload.deviceId,
                    "syncedAt": now,
                },
            )
            results.append({"id": r.id, "updatedAt": r.updatedAt})
    return {"results": results}


@app.get("/api/records")
def list_records(
    x_sync_key: Optional[str] = Header(default=None),
    region: Optional[str] = None,
    limit: int = Query(default=200, le=1000),
    offset: int = 0,
):
    check_key(x_sync_key)
    query = "SELECT * FROM surveys WHERE isDeleted=0"
    params: list = []
    if region:
        query += " AND region = ?"
        params.append(region)
    query += " ORDER BY updatedAt DESC LIMIT ? OFFSET ?"
    params += [limit, offset]
    with get_conn() as conn:
        rows = conn.execute(query, params).fetchall()
    return [dict(row) for row in rows]


@app.get("/api/export.xlsx")
def export_xlsx(key: Optional[str] = None):
    check_key(key)
    import openpyxl

    with get_conn() as conn:
        rows = conn.execute(
            "SELECT * FROM surveys WHERE isDeleted=0 ORDER BY region, district, mahalla, createdAt"
        ).fetchall()

    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Ko'chatzor"
    headers = [
        "Viloyat", "Tuman", "MFY", "FIO", "Telefon", "Maydon (ga)", "Ko'chat turi",
        "Nav", "Soni", "Ekilgan sana", "Manba", "Payvandtag", "Qurilma", "Kiritilgan vaqti",
    ]
    ws.append(headers)
    for r in rows:
        ws.append([
            r["region"], r["district"], r["mahalla"], r["fio"], r["phone"], r["area"],
            r["tree"], r["variety"], r["count"], r["planting"], r["source"], r["payvandtag"],
            r["deviceId"] or "", r["createdAt"],
        ])

    buf = BytesIO()
    wb.save(buf)
    buf.seek(0)
    return StreamingResponse(
        buf,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": "attachment; filename=kochatzor.xlsx"},
    )
