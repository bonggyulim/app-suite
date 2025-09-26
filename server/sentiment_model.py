# sentiment_model.py
from transformers import AutoTokenizer, AutoModelForSequenceClassification, AutoConfig
import torch, torch.nn.functional as F
import threading, traceback, os

MODEL_ID = "nlptown/bert-base-multilingual-uncased-sentiment"

_tok = None
_model = None
_cfg = None
_lock = threading.Lock()
_SENT_READY = False

# (옵션) CPU 스레드 제한
torch.set_num_threads(int(os.getenv("TORCH_NUM_THREADS", "1")))

def _ensure_loaded():
    global _tok, _model, _cfg, _SENT_READY
    if _model is not None:
        return
    with _lock:
        if _model is not None:
            return
        try:
            print("[sent] loading tokenizer…")
            _tok = AutoTokenizer.from_pretrained(MODEL_ID)

            print("[sent] loading config…")
            _cfg = AutoConfig.from_pretrained(MODEL_ID)
            _cfg.id2label = {0:"1 star", 1:"2 stars", 2:"3 stars", 3:"4 stars", 4:"5 stars"}
            _cfg.label2id = {v:k for k,v in _cfg.id2label.items()}
            _cfg.num_labels = 5

            print("[sent] loading model…")
            m = AutoModelForSequenceClassification.from_pretrained(MODEL_ID, config=_cfg)
            m.eval()
            _model = m
            _SENT_READY = True
            print("[sent] ready. num_labels:", _model.config.num_labels)
        except Exception as e:
            print("[sent] model load failed:", e)
            print(traceback.format_exc())
            _tok = None
            _model = None
            _cfg = None
            _SENT_READY = False  # 실패 시 false 유지

@torch.inference_mode()
def classify_sentiment(text: str) -> float:
    if not text or not text.strip():
        return 0.0
    _ensure_loaded()
    if _model is None or _tok is None:
        return 0.0  # 안전 폴백
    inputs = _tok([text], return_tensors="pt", truncation=True, max_length=256)
    logits = _model(**inputs).logits
    probs = F.softmax(logits, dim=-1)[0]  # 5차원
    pos = float(probs[3].item() + probs[4].item())  # ★4, ★5 합
    # 수치 안정성
    if pos < 0.0: pos = 0.0
    if pos > 1.0: pos = 1.0
    return pos

def is_sentiment_ready() -> bool:
    try:
        _ensure_loaded()
        return _SENT_READY
    except Exception:
        return False
