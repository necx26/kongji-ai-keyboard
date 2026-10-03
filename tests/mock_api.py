"""Local test fixture, not a real model. Never used in the delivered APK."""
import json
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / ".tools"
ROOT.mkdir(exist_ok=True)

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
        user = body["messages"][-1]["content"]
        text = user if isinstance(user, str) else next(v["text"] for v in user if v["type"] == "text")
        image = isinstance(user, list) and any(v.get("type") == "image_url" for v in user)
        metadata = {"path": self.path, "model": body["model"], "has_image": image,
                    "sample_context_found": "明天下午" in text, "messages": len(body["messages"])}
        with (ROOT / "testrequests.jsonl").open("a", encoding="utf-8") as f:
            f.write(json.dumps(metadata, ensure_ascii=False) + "\n")
        mode_file = ROOT / "mock-mode.txt"
        mode = mode_file.read_text(encoding="utf-8").strip() if mode_file.exists() else "ok"
        if mode == "delay": time.sleep(4)
        if mode == "401":
            self.send_response(401)
            payload = {"error": {"message": "test authentication error"}}
        else:
            self.send_response(200)
            if mode == "empty": content = ""
            elif mode == "plain": content = "好的，我确认时间后告诉你。"
            else: content = json.dumps({"replies": ["明天下午我再确认一下，确定后告诉你。", "你想约几点？我看一下安排。", "谢谢邀请！我确认好时间再回复你。"]}, ensure_ascii=False)
            payload = {"choices": [{"message": {"role": "assistant", "content": content}}]}
        data = json.dumps(payload, ensure_ascii=False).encode()
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        try: self.wfile.write(data)
        except (BrokenPipeError, ConnectionResetError): pass

    def log_message(self, *_): pass

if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", 8756), Handler).serve_forever()
