"""UI smoke checks on the dedicated emulator. Uses a local fixture, never a real API."""
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADB = r"C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe"
SERIAL = "emulator-5556"
OUT = ROOT / "交付" / os.environ.get("KONGJI_SCREENSHOT_DIR", "测试截图")
OUT.mkdir(parents=True, exist_ok=True)
RESULTS = []

def adb(*args, binary=False):
    p = subprocess.run([ADB, "-s", SERIAL, *args], capture_output=True, timeout=35)
    if p.returncode:
        raise RuntimeError(p.stderr.decode("utf-8", errors="replace"))
    return p.stdout if binary else p.stdout.decode("utf-8", errors="replace")

def nodes():
    output = adb("shell", "am", "instrument", "-w", "-r", "com.kongji.windowprobe/com.kongji.aikeyboard.WindowDumpInstrumentation")
    line = next((line for line in output.splitlines() if line.startswith("INSTRUMENTATION_RESULT: tree=")), None)
    if line is None: raise RuntimeError(output)
    return list(ET.fromstring(line.split("tree=",1)[1]).iter("node"))

def tap_node(n):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.attrib["bounds"]))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(.35)

def tap(text):
    for n in nodes():
        if n.attrib.get("text") == text or n.attrib.get("content-desc") == text:
            if text == "帮我回答":
                # Short-lived UiAutomation sessions can invalidate a service binding.
                # Reconnect only on the dedicated emulator, after inspecting the tap target.
                adb("shell", "settings", "put", "secure", "enabled_accessibility_services", "null")
                time.sleep(.15)
                adb("shell", "settings", "put", "secure", "enabled_accessibility_services", "com.kongji.aikeyboard/.ScreenReaderService")
                time.sleep(.6)
            tap_node(n)
            return
    raise AssertionError(f"UI item missing: {text}")

def scroll_tap(text):
    for _ in range(6):
        for n in nodes():
            if n.attrib.get("text") == text or n.attrib.get("content-desc") == text:
                tap_node(n)
                return
        adb("shell", "input", "swipe", "710", "1400", "710", "400", "450")
        time.sleep(.25)
    raise AssertionError(f"Scrolled UI item missing: {text}")

def wait_text(text, timeout=12):
    end = time.time()+timeout
    while time.time() < end:
        current = nodes()
        if any(text in n.attrib.get("text", "") for n in current):
            return current
        time.sleep(.3)
    raise AssertionError(f"UI text missing: {text}")

def wait_keyboard():
    end = time.time()+15
    while time.time() < end:
        current = nodes()
        if any(n.attrib.get("text") == "帮我回答" for n in current):
            if any("词库加载" in n.attrib.get("text", "") for n in current):
                time.sleep(.3)
                continue
            return
        time.sleep(.3)
    raise AssertionError("Keyboard toolbar missing")

def assert_text(text):
    wait_text(text)

def record(name):
    RESULTS.append(name)
    print("PASS:", name, flush=True)

def shot(name):
    (OUT / name).write_bytes(adb("exec-out", "screencap", "-p", binary=True))

def mode(value):
    (ROOT / ".tools" / "mock-mode.txt").write_text(value, encoding="utf-8")

def screen_generate(image=True):
    tap("帮我回答")
    wait_text("屏幕预览")
    if not image:
        tap("同时提交截图（需要视觉模型）")
    tap("生成回复")

def setup():
    adb("shell", "am", "start", "-f", "0x14000000", "-n", "com.kongji.aikeyboard/.SettingsActivity")
    time.sleep(.5)
    shot("01-安装引导.png")
    tap("https://你的服务商地址/v1")
    adb("shell", "input", "text", "http://10.0.2.2:8756/v1")
    adb("shell", "input", "keyevent", "4")
    adb("shell", "input", "swipe", "600", "1370", "600", "500", "450")
    tap("API Key 输入框")
    adb("shell", "input", "text", "test-fixture-secret")
    adb("shell", "input", "keyevent", "4")
    tap("填写服务商提供的模型 ID")
    adb("shell", "input", "text", "fixture-keyboard-model")
    adb("shell", "input", "keyevent", "4")
    tap("允许局域网 HTTP（本地模型，明文传输）")
    tap("保存 API 设置")
    prefs = adb("shell", "run-as", "com.kongji.aikeyboard", "cat", "shared_prefs/configuration.xml")
    assert "key_cipher" in prefs and "test-fixture-secret" not in prefs
    record("API 设置通过 UI 保存，测试密钥未以明文写入偏好文件")
    adb("shell", "ime", "enable", "com.kongji.aikeyboard/.AiInputService")
    adb("shell", "ime", "set", "com.kongji.aikeyboard/.AiInputService")
    adb("shell", "settings", "put", "secure", "show_ime_with_hard_keyboard", "1")
    adb("shell", "settings", "put", "secure", "enabled_accessibility_services", "com.kongji.aikeyboard/.ScreenReaderService")
    adb("shell", "settings", "put", "secure", "accessibility_enabled", "1")
    time.sleep(.8)
    adb("shell", "input", "swipe", "600", "1380", "600", "430", "450")
    scroll_tap("打开键盘练习页")
    tap("点这里输入，或让 AI 帮你回复")
    wait_keyboard()
    record("APK 安装、输入法启用、练习页调出键盘")

def checks():
    # Earlier settings tests may have used a password field, which puts the IME in English mode.
    if any(n.attrib.get("text")=="EN" for n in nodes()): tap("EN")
    # Tap actual keyboard keys rather than injecting Chinese text.
    for key in "nihao": tap(key)
    tap("你好")
    assert_text("你好")
    record("基础拼音 nihao 候选填入你好")
    shot("02-中文键盘.png")
    tap("删除"); tap("删除")
    tap("中")
    for key in "hi": tap(key)
    assert_text("hi")
    tap("删除"); tap("删除")
    tap("EN")
    record("英文键盘输入与删除")

    mode("ok")
    tap("帮我回答")
    wait_text("屏幕预览")
    shot("03-屏幕预览.png")
    tap("生成回复")
    wait_text("点选一条回复")
    shot("04-候选回复-本地测试服务.png")
    reply_nodes = nodes()
    candidate = next(n for n in reply_nodes if n.attrib.get("text", "").startswith("1  明天下午"))
    tap_node(candidate)
    assert_text("明天下午我再确认一下，确定后告诉你。")
    shot("05-回复填入-本地测试服务.png")
    record("读取屏幕和截图、兼容 API 请求、三条候选、点选填入")

    mode("plain")
    screen_generate(image=False)
    wait_text("点选一条回复")
    assert_text("好的，我确认时间后告诉你。")
    record("纯文本请求及非 JSON 回复兼容")

    mode("401")
    screen_generate()
    wait_text("API 鉴权失败")
    record("HTTP 401 鉴权失败提示")

    mode("empty")
    screen_generate()
    wait_text("模型返回为空")
    record("空模型回复错误提示")

    mode("delay")
    screen_generate()
    tap("取消请求")
    time.sleep(4.5)
    assert_text("已取消请求")
    assert not any("1  明天下午" in n.attrib.get("text", "") for n in nodes())
    record("取消后延迟返回不会覆盖键盘或插入回复")

    metadata = [json.loads(line) for line in (ROOT/".tools"/"testrequests.jsonl").read_text(encoding="utf-8").splitlines()]
    assert any(r["has_image"] and r["sample_context_found"] for r in metadata)
    assert any(not r["has_image"] and r["sample_context_found"] for r in metadata)
    assert all(r["path"] == "/v1/chat/completions" for r in metadata)
    record("请求路径正确，图片和文字分支均带有屏幕示例上下文")
    crashes = adb("logcat", "-d", "-b", "crash")
    assert "com.kongji.aikeyboard" not in crashes
    record("应用崩溃日志未发现本应用异常")

if __name__ == "__main__":
    try:
        if "--configured" in sys.argv:
            RESULTS.extend(json.loads((ROOT/".tools"/"smoke-results.json").read_text(encoding="utf-8")))
            adb("shell", "am", "start", "-f", "0x14000000", "-n", "com.kongji.aikeyboard/.SettingsActivity")
            time.sleep(.5)
            adb("shell", "settings", "put", "secure", "enabled_accessibility_services", "null")
            adb("shell", "settings", "put", "secure", "enabled_accessibility_services", "com.kongji.aikeyboard/.ScreenReaderService")
            adb("shell", "settings", "put", "secure", "accessibility_enabled", "1")
            adb("shell", "input", "swipe", "600", "1380", "600", "430", "450")
            scroll_tap("打开键盘练习页")
            tap("点这里输入，或让 AI 帮你回复")
            wait_keyboard()
            record("APK 安装、输入法启用、练习页调出键盘")
        else:
            setup()
        checks()
    finally:
        (ROOT/".tools"/"smoke-results.json").write_text(json.dumps(RESULTS, ensure_ascii=False, indent=2), encoding="utf-8")
        mode("ok")
