import os
import sqlite3
import time
from contextlib import contextmanager
from io import BytesIO
from pathlib import Path
from typing import List, Optional

import bcrypt
import jwt
from fastapi import Depends, FastAPI, Header, HTTPException, Query
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

DB_PATH = os.environ.get("DB_PATH", "/app/data/kochatzor.db")
JWT_SECRET = os.environ.get("JWT_SECRET")
if not JWT_SECRET:
    raise RuntimeError("JWT_SECRET majburiy (.env fayliga qo'shing)")

TOKEN_TTL_SECONDS = 180 * 24 * 3600  # 180 kun

app = FastAPI(title="Ko'chatzor API")


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
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS surveys (
                id TEXT PRIMARY KEY,
                regionId INTEGER, districtId INTEGER, mahallaId INTEGER,
                region TEXT, district TEXT, mahalla TEXT,
                fio TEXT, phone TEXT, area REAL, tree TEXT, variety TEXT,
                count INTEGER, planting TEXT, source TEXT, payvandtag TEXT,
                createdAt INTEGER, updatedAt INTEGER, isDeleted INTEGER,
                submittedBy TEXT, syncedAt INTEGER
            )
            """
        )


init_db()


# ---- auth helpers ----
def make_token(user: sqlite3.Row) -> str:
    payload = {
        "sub": user["username"],
        "role": user["role"],
        "regionId": user["region_id"],
        "districtId": user["district_id"],
        "region": user["region_name"],
        "district": user["district_name"],
        "exp": int(time.time()) + TOKEN_TTL_SECONDS,
    }
    return jwt.encode(payload, JWT_SECRET, algorithm="HS256")


def current_user(authorization: Optional[str] = Header(default=None)) -> dict:
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=401, detail="Avtorizatsiya talab qilinadi")
    token = authorization.split(" ", 1)[1]
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=["HS256"])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Token yaroqsiz yoki muddati tugagan")
    return payload


def require_admin(user: dict = Depends(current_user)) -> dict:
    if user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Faqat admin uchun")
    return user


class LoginRequest(BaseModel):
    username: str
    password: str


@app.post("/api/auth/login")
def login(payload: LoginRequest):
    with get_conn() as conn:
        row = conn.execute(
            "SELECT * FROM users WHERE username = ?", (payload.username.strip().lower(),)
        ).fetchone()
    if not row or not bcrypt.checkpw(payload.password.encode(), row["password_hash"].encode()):
        raise HTTPException(status_code=401, detail="Login yoki parol noto'g'ri")
    return {
        "token": make_token(row),
        "role": row["role"],
        "regionId": row["region_id"],
        "districtId": row["district_id"],
        "region": row["region_name"],
        "district": row["district_name"],
    }


@app.get("/api/health")
def health():
    return {"status": "ok"}


# ---- survey sync ----
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
    records: List[SurveyIn]


@app.post("/api/sync")
def sync(payload: SyncRequest, user: dict = Depends(current_user)):
    now = int(time.time() * 1000)
    is_admin = user["role"] == "admin"
    results = []
    with get_conn() as conn:
        for r in payload.records:
            # Dala xodimi faqat o'ziga biriktirilgan tumanga tegishli yozuv yubora oladi.
            region_id = r.regionId
            district_id = r.districtId
            region_name = r.region
            district_name = r.district
            if not is_admin:
                region_id = user["regionId"]
                district_id = user["districtId"]
                region_name = user["region"]
                district_name = user["district"]

            conn.execute(
                """
                INSERT INTO surveys (id, regionId, districtId, mahallaId, region, district,
                    mahalla, fio, phone, area, tree, variety, count, planting, source,
                    payvandtag, createdAt, updatedAt, isDeleted, submittedBy, syncedAt)
                VALUES (:id, :regionId, :districtId, :mahallaId, :region, :district,
                    :mahalla, :fio, :phone, :area, :tree, :variety, :count, :planting, :source,
                    :payvandtag, :createdAt, :updatedAt, :isDeleted, :submittedBy, :syncedAt)
                ON CONFLICT(id) DO UPDATE SET
                    regionId=excluded.regionId, districtId=excluded.districtId,
                    mahallaId=excluded.mahallaId, region=excluded.region,
                    district=excluded.district, mahalla=excluded.mahalla, fio=excluded.fio,
                    phone=excluded.phone, area=excluded.area, tree=excluded.tree,
                    variety=excluded.variety, count=excluded.count, planting=excluded.planting,
                    source=excluded.source, payvandtag=excluded.payvandtag,
                    createdAt=excluded.createdAt, updatedAt=excluded.updatedAt,
                    isDeleted=excluded.isDeleted, submittedBy=excluded.submittedBy,
                    syncedAt=excluded.syncedAt
                """,
                {
                    "id": r.id, "regionId": region_id, "districtId": district_id,
                    "mahallaId": r.mahallaId, "region": region_name, "district": district_name,
                    "mahalla": r.mahalla, "fio": r.fio, "phone": r.phone, "area": r.area,
                    "tree": r.tree, "variety": r.variety, "count": r.count,
                    "planting": r.planting, "source": r.source, "payvandtag": r.payvandtag,
                    "createdAt": r.createdAt, "updatedAt": r.updatedAt,
                    "isDeleted": int(r.isDeleted), "submittedBy": user["sub"], "syncedAt": now,
                },
            )
            results.append({"id": r.id, "updatedAt": r.updatedAt})
    return {"results": results}


@app.get("/api/records")
def list_records(
    user: dict = Depends(current_user),
    q: Optional[str] = None,
    tree: Optional[str] = None,
    limit: int = Query(default=200, le=1000),
    offset: int = 0,
):
    where = "WHERE isDeleted=0"
    params: list = []
    if user["role"] != "admin":
        where += " AND districtId = ?"
        params.append(user["districtId"])
    if tree:
        where += " AND tree = ?"
        params.append(tree)
    if q:
        where += " AND (fio LIKE ? OR phone LIKE ? OR mahalla LIKE ?)"
        params += [f"%{q}%", f"%{q}%", f"%{q}%"]
    with get_conn() as conn:
        rows = conn.execute(
            f"SELECT * FROM surveys {where} ORDER BY updatedAt DESC LIMIT ? OFFSET ?",
            params + [limit, offset],
        ).fetchall()
        total = conn.execute(f"SELECT COUNT(*) c FROM surveys {where}", params).fetchone()["c"]
    return {"total": total, "rows": [dict(row) for row in rows]}


class SurveyUpdate(BaseModel):
    fio: Optional[str] = None
    phone: Optional[str] = None
    area: Optional[float] = None
    tree: Optional[str] = None
    variety: Optional[str] = None
    count: Optional[int] = None
    planting: Optional[str] = None
    payvandtag: Optional[str] = None


@app.put("/api/records/{record_id}")
def update_record(record_id: str, payload: SurveyUpdate, user: dict = Depends(require_admin)):
    fields = {k: v for k, v in payload.model_dump().items() if v is not None}
    if not fields:
        raise HTTPException(status_code=400, detail="Yangilanadigan maydon yo'q")
    fields["updatedAt"] = int(time.time() * 1000)
    set_clause = ", ".join(f"{k} = :{k}" for k in fields)
    with get_conn() as conn:
        cur = conn.execute(
            f"UPDATE surveys SET {set_clause} WHERE id = :id",
            {**fields, "id": record_id},
        )
        if cur.rowcount == 0:
            raise HTTPException(status_code=404, detail="Yozuv topilmadi")
    return {"ok": True}


@app.delete("/api/records/{record_id}")
def delete_record(record_id: str, user: dict = Depends(require_admin)):
    with get_conn() as conn:
        cur = conn.execute(
            "UPDATE surveys SET isDeleted=1, updatedAt=? WHERE id=?",
            (int(time.time() * 1000), record_id),
        )
        if cur.rowcount == 0:
            raise HTTPException(status_code=404, detail="Yozuv topilmadi")
    return {"ok": True}


@app.get("/api/export.xlsx")
def export_xlsx(user: dict = Depends(current_user)):
    import openpyxl

    query = "SELECT * FROM surveys WHERE isDeleted=0"
    params: list = []
    if user["role"] != "admin":
        query += " AND districtId = ?"
        params.append(user["districtId"])
    query += " ORDER BY region, district, mahalla, createdAt"
    with get_conn() as conn:
        rows = conn.execute(query, params).fetchall()

    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Ko'chatzor"
    headers = [
        "Viloyat", "Tuman", "MFY", "FIO", "Telefon", "Maydon (ga)", "Ko'chat turi",
        "Nav", "Soni", "Ekilgan sana", "Manba", "Payvandtag", "Kiritgan", "Kiritilgan vaqti",
    ]
    ws.append(headers)
    for r in rows:
        ws.append([
            r["region"], r["district"], r["mahalla"], r["fio"], r["phone"], r["area"],
            r["tree"], r["variety"], r["count"], r["planting"], r["source"], r["payvandtag"],
            r["submittedBy"] or "", r["createdAt"],
        ])

    buf = BytesIO()
    wb.save(buf)
    buf.seek(0)
    return StreamingResponse(
        buf,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": "attachment; filename=kochatzor.xlsx"},
    )
