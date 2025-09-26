# app.py
import os
import threading
import functools
import uuid
from datetime import datetime

from flask import Flask, jsonify, request, abort, current_app, g
from flask_cors import CORS
from werkzeug.exceptions import HTTPException
from sqlalchemy.exc import IntegrityError
from sqlalchemy import or_, and_, text

from models import db, Note
from note_summarize_model import summarize_text
from sentiment_model import classify_sentiment

# ===== Firebase Admin (환경변수로 ON/OFF) =====
FIREBASE_CREDENTIALS_JSON = os.getenv("FIREBASE_CREDENTIALS_JSON")  # 예: C:/keys/serviceAccount.json
FIREBASE_ENABLED = bool(FIREBASE_CREDENTIALS_JSON)
STRICT_AUTH = os.getenv("STRICT_AUTH", "false").lower() == "true"    # 운영 권장

if FIREBASE_ENABLED:
    try:
        import firebase_admin
        from firebase_admin import credentials, auth as fb_auth
        if not firebase_admin._apps:
            cred = credentials.Certificate(FIREBASE_CREDENTIALS_JSON)
            firebase_admin.initialize_app(cred)
    except Exception as _e:
        print(f"[auth] Firebase init failed: {_e}")
        FIREBASE_ENABLED = False

def require_auth(fn):
    """Bearer <idToken> 검증 → g.user(uid,name). 비활성+STRICT면 차단, 개발모드면 기본 유저 부여."""
    @functools.wraps(fn)
    def wrapper(*args, **kwargs):
        if not FIREBASE_ENABLED:
            if STRICT_AUTH:
                return jsonify({"error": {"code": "auth_disabled", "message": "auth not configured"}}), 503
            # 개발 편의: 가짜 유저 주입(로컬에서 동작 보장)
            g.user = {"uid": "dev", "name": "Developer"}
            return fn(*args, **kwargs)

        authz = request.headers.get("Authorization", "")
        if not authz.startswith("Bearer "):
            return jsonify({"error": {"code": "unauthorized", "message": "missing bearer token"}}), 401

        token = authz.split(" ", 1)[1]
        try:
            decoded = fb_auth.verify_id_token(token, check_revoked=True)
            g.user = {
                "uid": decoded["uid"],
                "name": decoded.get("name") or "",
                "email": decoded.get("email"),
                "email_verified": decoded.get("email_verified", False),
            }
        except Exception as e:
            return jsonify({"error": {"code": "unauthorized", "message": f"invalid token: {e}"}}), 401

        return fn(*args, **kwargs)
    return wrapper


def create_app():
    app = Flask(__name__)

    # SQLite (개발용)
    db_path = os.environ.get("DB_PATH", "sqlite:///notes.db")
    app.config["SQLALCHEMY_DATABASE_URI"] = db_path
    app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

    db.init_app(app)
    CORS(app, resources={r"/notes*": {"origins": "*"}, r"/health*": {"origins": "*"}})

    with app.app_context():
        db.create_all()

    # 공통 DTO (createdAt은 서버 created_at만 사용)
    def to_dto(note: Note):
        created_at_iso = None
        if getattr(note, "created_at", None):
            try:
                created_at_iso = note.created_at.isoformat().replace("+00:00", "Z")
            except Exception:
                created_at_iso = None
        return {
            "id": note.id,
            "userId": note.user_id,
            "userName": note.user_name,
            "title": note.title,
            "content": note.content,
            "summarize": note.summarize,
            "sentiment": note.sentiment,
            "createdAt": created_at_iso
        }

    # [C] Create (인증 필요) — 서버가 userId/userName/created_at 책임
    @app.post("/notes")
    @require_auth
    def create_note():
        data = request.get_json(silent=True) or {}
        uid = (g.user or {}).get("uid") or "dev"
        uname = (g.user or {}).get("name") or ""

        note = Note(
            user_id=uid,
            user_name=uname,
            title=data.get("title", ""),
            content=data.get("content", ""),
            summarize=None,   # 처리 전 NULL
            sentiment=None,   # 처리 전 NULL
            # created_at은 DB server_default로 자동 세팅
        )
        db.session.add(note)
        try:
            db.session.commit()
            # 백그라운드 후처리
            app_obj = current_app._get_current_object()
            threading.Thread(
                target=process_models,
                args=(app_obj, note.id, note.content),
                daemon=True,
            ).start()
        except IntegrityError:
            db.session.rollback()
            return jsonify({"message": "Integrity error"}), 409
        return jsonify(to_dto(note)), 201

    # 백그라운드: AI 요약/감성 → 완료 시 컬럼 채움
    def process_models(app_ctx, note_id, content):
        with app_ctx.app_context():
            try:
                try:
                    summary = summarize_text(content or "")
                except Exception:
                    summary = None
                try:
                    sentiment = classify_sentiment(content or "")
                except Exception:
                    sentiment = None

                rows = (
                    db.session.query(Note)
                    .filter(Note.id == note_id)
                    .update(
                        {"summarize": summary, "sentiment": sentiment},
                        synchronize_session=False,
                    )
                )
                if rows == 0:
                    db.session.rollback()
                    print(f"[bg] note {note_id} not found (deleted?), skip.")
                    return
                db.session.commit()
            except Exception as e:
                db.session.rollback()
                print(f"[bg] process_models failed: {e}")

    # [R] List (AI 완료만, 최신순 Keyset)
    @app.get("/notes")
    def list_notes():
        try:
            limit = min(int(request.args.get("limit", 10)), 50)
            order = request.args.get("order", "desc").lower()
            cursor = request.args.get("cursor")  # "<ts>_<id>"

            q = db.session.query(Note).filter(
                Note.summarize.isnot(None),
                Note.sentiment.isnot(None),
            )

            if order == "asc":
                q = q.order_by(Note.created_at.asc(), Note.id.asc())
                if cursor:
                    try:
                        ts_part, id_part = cursor.rsplit("_", 1)
                        cur_ts = datetime.fromisoformat(ts_part.replace("Z", "+00:00"))
                        cur_id = int(id_part)
                    except Exception:
                        return jsonify({"error": {"code": "bad_cursor", "message": "invalid cursor"}}), 400
                    q = q.filter(
                        or_(Note.created_at > cur_ts,
                            and_(Note.created_at == cur_ts, Note.id > cur_id))
                    )
            else:
                q = q.order_by(Note.created_at.desc(), Note.id.desc())
                if cursor:
                    try:
                        ts_part, id_part = cursor.rsplit("_", 1)
                        cur_ts = datetime.fromisoformat(ts_part.replace("Z", "+00:00"))
                        cur_id = int(id_part)
                    except Exception:
                        return jsonify({"error": {"code": "bad_cursor", "message": "invalid cursor"}}), 400
                    q = q.filter(
                        or_(Note.created_at < cur_ts,
                            and_(Note.created_at == cur_ts, Note.id < cur_id))
                    )

            rows = q.limit(limit + 1).all()
            has_more = len(rows) > limit
            items = rows[:limit]
            next_cursor = None
            if has_more and items:
                last = items[-1]
                iso = None
                try:
                    iso = last.created_at.isoformat().replace("+00:00", "Z")
                except Exception:
                    pass
                next_cursor = f"{iso or ''}_{last.id}"
            return jsonify({
                "items": [to_dto(n) for n in items],
                "next_cursor": next_cursor,
                "has_more": has_more
            }), 200
        except Exception as e:
            return jsonify({"message": f"Database error: {e}"}), 500

    # [R] Read one
    @app.get("/notes/<int:note_id>")
    def get_note(note_id: int):
        note = db.session.get(Note, note_id)
        if not note:
            abort(404, description="Note not found")
        return jsonify(to_dto(note)), 200

    # [U] Update (인증 필요) — 작성자/생성일은 수정 불가
    @app.put("/notes/<int:note_id>")
    @require_auth
    def update_note(note_id: int):
        note = db.session.get(Note, note_id)
        if not note:
            abort(404, description="Note not found")

        data = request.get_json(silent=True) or {}
        if "title" in data:
            note.title = data["title"] or ""
        if "content" in data:
            note.content = data["content"] or ""
        # user_id/user_name/created_at은 서버 소유 → 수정 허용하지 않음

        db.session.commit()
        return jsonify(to_dto(note)), 200

    # [D] Delete (인증 필요)
    @app.delete("/notes/<int:note_id>")
    @require_auth
    def delete_note(note_id: int):
        note = db.session.get(Note, note_id)
        if not note:
            abort(404, description="Note not found")
        db.session.delete(note)
        db.session.commit()
        return "", 200

    # --- 단순 헬스 ---
    @app.get("/health")
    def health():
        try:
            db.session.execute(text("SELECT 1"))
            return jsonify({"status": "ok"}), 200
        except Exception:
            return jsonify({"status": "fail"}), 503

    # --- 심화 헬스(DB + AI) ---
    @app.get("/healthz")
    def healthz():
        db_ok = True
        try:
            db.session.execute(text("SELECT 1"))
        except Exception:
            db_ok = False

        from note_summarize_model import is_summarizer_ready
        from sentiment_model import is_sentiment_ready
        models = {
            "summary": bool(is_summarizer_ready()),
            "sentiment": bool(is_sentiment_ready()),
        }

        ok = db_ok and models["summary"] and models["sentiment"]
        status = 200 if ok else 503
        return jsonify({"ok": ok, "db": "ok" if db_ok else "fail", "models": models}), status

    # --- 표준 에러 JSON ---
    def _error_json(code: str, message: str, status: int = 400):
        payload = {"error": {"code": code, "message": message, "traceId": str(uuid.uuid4())}}
        return jsonify(payload), status

    @app.errorhandler(HTTPException)
    def handle_http_exc(e: HTTPException):
        code = getattr(e, "name", "HTTP_ERROR").upper().replace(" ", "_")
        return _error_json(code=code, message=str(e.description), status=e.code)

    @app.errorhandler(Exception)
    def handle_unexpected_exc(e: Exception):
        app.logger.exception("Unhandled exception")
        return _error_json(code="INTERNAL_SERVER_ERROR", message="unexpected error", status=500)

    return app


if __name__ == "__main__":
    from waitress import serve
    app = create_app()
    serve(app, host="0.0.0.0", port=8080, threads=1)
