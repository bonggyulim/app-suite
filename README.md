# 📘 Notes API (Flask + NLP)

> 메모를 작성/관리하면서 **자동 요약**과 **감정 분석**까지 해주는 Flask 기반 REST API 프로젝트입니다.  
> HuggingFace Transformers 모델을 활용하여 텍스트를 처리하며, SQLite + SQLAlchemy로 데이터를 관리합니다.

---

## ✨ 주요 기능

- **메모 CRUD API** (생성, 조회, 수정, 삭제)
- **자동 요약 기능**: KoBART 기반 요약 모델 사용 (`EbanLee/kobart-summary-v3`)
- **감정 분석 기능**: 다국어 BERT 기반 감정 분류 (`nlptown/bert-base-multilingual-uncased-sentiment`)
- **비동기 처리**: 메모 작성 시 백그라운드 스레드에서 요약/감정 분석 실행
- **데이터베이스**: SQLite (기본), SQLAlchemy ORM
- **CORS 지원**: 프론트엔드와 연동 가능

---

## 📂 프로젝트 구조

├── app.py # Flask 앱, REST API 엔드포인트 

├── models.py # Note 모델 정의 (SQLAlchemy)

├── note_summarize_model.py # HuggingFace 기반 KoBART 요약기

├── sentiment_model.py # HuggingFace 기반 BERT 감정 분석기

---

## 🖼️ 캡처본 (샘플 UI)

|감정에 따른 배경색|클릭시 요약 내용 확인|
|---|---|
|<img src="https://github.com/user-attachments/assets/7637f897-b471-4854-8523-f6ef8421d44e" width="300" alt="메모 생성 API 응답 캡처">|<img src="https://github.com/user-attachments/assets/3eb37b33-5b5b-4b44-8a60-220f3fc54686" width="300" alt="메모 목록 조회 캡처">|

---

## 📜 License

MIT License

---
