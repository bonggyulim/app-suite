# note_summarize_model.py
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import torch, threading, os

_MODEL_ID = "EbanLee/kobart-summary-v3"

_TOK = None
_MOD = None
_READY = False
_LOCK = threading.Lock()

# (옵션) CPU에서 과도한 스레드 사용 방지
torch.set_num_threads(int(os.getenv("TORCH_NUM_THREADS", "1")))

def _ensure_loaded():
    global _TOK, _MOD, _READY
    if _MOD is not None:
        return
    with _LOCK:
        if _MOD is not None:
            return
        _TOK = AutoTokenizer.from_pretrained(_MODEL_ID)
        _MOD = AutoModelForSeq2SeqLM.from_pretrained(_MODEL_ID)
        _MOD.eval()
        _READY = True

def summarize_text(text: str, max_char: int = 300) -> str:
    if not text or not text.strip():
        return ""
    _ensure_loaded()

    inputs = _TOK([text], max_length=1024, truncation=True, return_tensors="pt")
    with torch.inference_mode():
        output = _MOD.generate(
            **inputs,
            num_beams=4,
            do_sample=False,
            min_length=0,
            max_length=160,
            length_penalty=1.0,
            no_repeat_ngram_size=3
        )
    decoded = _TOK.batch_decode(output, skip_special_tokens=True)[0].strip()
    return decoded[:max_char]

def is_summarizer_ready() -> bool:
    # 처음 호출 시 로딩을 트리거해도 되고, 빠른 체크만 원하면 _READY만 반환해도 됩니다.
    try:
        _ensure_loaded()
        return _READY
    except Exception:
        return False
