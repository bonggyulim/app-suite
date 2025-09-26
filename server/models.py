# models.py
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.sql import func
from sqlalchemy import Index

db = SQLAlchemy()

class Note(db.Model):
    __tablename__ = 'notes'

    id = db.Column(db.Integer, primary_key=True, autoincrement=True)  # 자동 증가
    # 작성자 정보 (Firebase)
    user_id = db.Column(db.String(128), nullable=False)               # Firebase uid
    user_name = db.Column(db.String(80), nullable=False, default="")  # 표시 닉네임

    title = db.Column(db.String(255), nullable=False, default="")
    content = db.Column(db.Text, nullable=False, default="")

    # AI 결과(처리 전엔 NULL)
    summarize = db.Column(db.Text, nullable=True)
    sentiment = db.Column(db.Float, nullable=True)

    # 서버 타임스탬프(커서 정렬 키)
    created_at = db.Column(db.DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = db.Column(db.DateTime(timezone=True), server_default=func.now(),
                           onupdate=func.now(), nullable=False)

# 사용자별 최신 조회에 유리한 인덱스(선택)
Index("idx_notes_user_created_desc", Note.user_id, Note.created_at.desc(), Note.id.desc())
