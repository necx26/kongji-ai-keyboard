"""Check sensitive-field blocking and that the IME reads a different app."""
import json
import time
import adb_smoke as t

t.RESULTS.extend(json.loads((t.ROOT/".tools"/"smoke-results.json").read_text(encoding="utf-8")))
t.adb("shell", "am", "start", "-f", "0x14000000", "-n", "com.kongji.aikeyboard/.SettingsActivity")
time.sleep(.5)
t.scroll_tap("API Key 输入框")
t.wait_keyboard()
t.assert_text("密码输入框：屏幕读取已禁用")
before=(t.ROOT/".tools"/"testrequests.jsonl").read_text(encoding="utf-8")
t.tap("帮我回答")
t.assert_text("密码输入框：屏幕读取已禁用")
assert before==(t.ROOT/".tools"/"testrequests.jsonl").read_text(encoding="utf-8")
t.record("密码输入框屏幕读取禁用，点击帮助不会发起请求")
t.adb("shell", "input", "keyevent", "4")
t.adb("shell", "am", "start", "-a", "android.settings.SETTINGS")
time.sleep(.6)
current=t.nodes()
print("SYSTEM SETTINGS:",[(n.attrib.get("text"),n.attrib.get("content-desc")) for n in current if n.attrib.get("text") or n.attrib.get("content-desc")],flush=True)
for node in current:
    if "search settings" in node.attrib.get("text", "").lower() or "search" in node.attrib.get("content-desc", "").lower():
        t.tap_node(node)
        break
else:
    raise AssertionError("System settings search entry missing")
time.sleep(.4)
edit=next(n for n in t.nodes() if n.attrib.get("class")=="android.widget.EditText")
t.tap_node(edit)
t.wait_keyboard()
t.tap("帮我回答")
t.wait_text("屏幕预览")
t.shot("06-系统设置中的键盘与读屏.png")
t.tap("取消")
t.record("在 Android 系统设置搜索框调出键盘，并读取另一应用的屏幕")
(t.ROOT/".tools"/"smoke-results.json").write_text(json.dumps(t.RESULTS,ensure_ascii=False,indent=2),encoding="utf-8")
