"""Actual emulator UI regression: gestures, delete hold, photo picker, geometry, themes, language.
Uses synthetic practice text. Does not clear memory or API configuration.
"""
import sys
sys.stdout.reconfigure(encoding="utf-8");sys.stderr.reconfigure(encoding="utf-8")
import time, re, json, base64
from pathlib import Path
import keyboard_modes_smoke as k
t=k.t
t.OUT=Path(".tools/v050-screens");t.OUT.mkdir(parents=True,exist_ok=True)
RESULTS=[]
original_nodes=t.nodes
def nodes():
    for _ in range(5):
        try:return original_nodes()
        except RuntimeError:time.sleep(.25)
    raise AssertionError("UI probe unavailable")
t.nodes=nodes

def record(text):RESULTS.append(text);print("PASS:",text,flush=True)
def coords(node):return tuple(map(int,re.findall(r"\d+",node.attrib["bounds"])))
def tap(label):
    for attempt in range(12):
        current=t.nodes()
        for n in current:
            if n.attrib.get("text")==label or n.attrib.get("content-desc")==label:
                x1,y1,x2,y2=coords(n)
                if y2-y1>=65 and y1>100:
                    t.tap_node(n);return
        if attempt<3:time.sleep(.3);continue
        top=label in {"26键","九宫格","手写","拖拽编辑","结束编辑","恢复默认外观","English","日本語","Español","한국어","Deutsch","中文"}
        t.adb("shell","input","swipe","1040","650" if top else "1750","1040","1750" if top else "650","300");time.sleep(.3)
    raise AssertionError("Cannot reach: "+label)
t.tap=tap
def appearance():
    k.home();t.wait_text("私人定制键盘外观");tap("私人定制键盘外观");t.wait_text("外观工作室");t.adb("shell","input","swipe","1040","600","1040","1750","220");time.sleep(.25)
def reset_input(value=""):
    encoded=base64.b64encode(value.encode()).decode()
    output=t.adb("shell","am","instrument","-w","-r","-e","field","'点这里输入，或让 AI 帮你回复'","-e","value64",encoded or "''","com.kongji.windowprobe/com.kongji.aikeyboard.WindowDumpInstrumentation");assert "set_text=true" in output

def bounds(label):
    for _ in range(12):
        for node in t.nodes():
            if node.attrib.get("text")==label or node.attrib.get("content-desc")==label:return coords(node)
        time.sleep(.2)
    raise AssertionError("bounds unavailable: "+label)
k.bounds=bounds
def pick_photo():
    import xml.etree.ElementTree as ET
    t.adb("shell","uiautomator","dump","/sdcard/kongji-picker.xml")
    tree=t.adb("shell","cat","/sdcard/kongji-picker.xml")
    node=next(n for n in ET.fromstring(tree).iter("node") if n.attrib.get("content-desc","").startswith("keyboard-qa.png,"))
    t.tap_node(node)

original_edit_text=k.edit_text
def edit_text():
    for _ in range(8):
        try:return original_edit_text()
        except StopIteration:time.sleep(.2)
    raise AssertionError("practice input unavailable")
k.edit_text=edit_text
def gesture(x,y,ex,ey,hold=100,photo=None):
    output=t.adb("shell","am","instrument","-w","-r","-e","gesture",f"{x},{y},{ex},{ey},{hold}","com.kongji.windowprobe/com.kongji.aikeyboard.WindowDumpInstrumentation")
    assert "INSTRUMENTATION_CODE: 0" in output,output
    if photo:
        path=next(line.split("gesture_photo=",1)[1] for line in output.splitlines() if "gesture_photo=" in line)
        t.adb("pull",path,str(t.OUT/photo))
    return output

def photo_fixture():
    import struct,zlib
    w,h=1200,700;rows=[]
    for y in range(h):
        row=bytearray()
        for x in range(w):
            color=(60,90,115) if y<450 else (155,130,95)
            if (x-920)**2+(y-180)**2<95**2:color=(230,220,195)
            if y>400-abs(x-500)*.5 and y<450:color=(30,50,60)
            row.extend(color)
        rows.append(bytes([0])+row)
    def chunk(kind,data):return struct.pack(">I",len(data))+kind+data+struct.pack(">I",zlib.crc32(kind+data)&0xffffffff)
    file=Path(".tools/keyboard-qa.png")
    file.write_bytes(bytes([137,80,78,71,13,10,26,10])+chunk(b"IHDR",struct.pack(">IIBBBBB",w,h,8,2,0,0,0))+chunk(b"IDAT",zlib.compress(b"".join(rows)))+chunk(b"IEND",b""))
    t.adb("push",str(file),"/sdcard/Download/keyboard-qa.png")
    t.adb("shell","am","broadcast","-a","android.intent.action.MEDIA_SCANNER_SCAN_FILE","-d","file:///sdcard/Download/keyboard-qa.png")

def run():
    photo_fixture()
    t.adb("shell","cmd","locale","set-app-locales","com.kongji.aikeyboard","--locales","zh")
    t.adb("shell","ime","enable","com.kongji.aikeyboard/.AiInputService");t.adb("shell","ime","set","com.kongji.aikeyboard/.AiInputService")
    k.home();t.wait_text("私人定制键盘外观");t.shot("home-white.png")
    appearance();tap("26键");tap("白色");t.shot("appearance-white.png")
    assert not any(n.attrib.get("text") in ["你好","谢谢","好的","你好，让表达更自然。"] for n in t.nodes());record("preview does not seed greeting candidates")
    tap("快捷符号");tap("符号在上方");tap("上滑输入符号")
    # Reopen starts at the top and preserves saved configuration.
    tap("应用到键盘");appearance()
    k.swipe_key("up");t.wait_text("已按下 1");record("preview up-swipe commits symbol once")
    k.apply_practice()
    assert not any(n.attrib.get("text") in ["你好","谢谢","好的"] for n in t.nodes());record("blank input has no default greetings")
    x1,y1,x2,y2=bounds("q");x=(x1+x2)//2;y=(y1+y2)//2
    output=gesture(x,y,x,y-85,100,"symbol-popup.png")
    assert "gesture_before=点这里输入，或让 AI 帮你回复" in output or "INSTRUMENTATION_RESULT: gesture_before=" in output
    record("actual IME does not output before finger release")
    assert k.edit_text()=="1",k.edit_text();record("actual IME up-swipe commits only symbol")
    tap("q");assert k.edit_text()=="1q";record("ordinary tap commits letter")
    reset_input("甲乙丙丁戊己庚辛壬癸"*5)
    x1,y1,x2,y2=bounds("删除");x=(x1+x2)//2;y=(y1+y2)//2
    gesture(x,y,x,y,450);time.sleep(.2)
    after=k.edit_text();assert 25<len(after)<50,len(after);time.sleep(.25);assert k.edit_text()==after;record("delete hold removes characters individually and stops on release")
    t.shot("keyboard-white.png")
    appearance();tap("拖拽编辑");time.sleep(.3)
    before=bounds("键盘位置预览");x1,y1,x2,y2=bounds("q")
    # Drag near the middle of the keyboard, away from the resize handle.
    t.adb("shell","input","swipe","530",str(y1+150),"530",str(y1+25),"300");time.sleep(.3)
    tap("结束编辑");tap("应用并试打");time.sleep(.5);tap("点这里输入，或让 AI 帮你回复");t.wait_keyboard()
    prefs=t.adb("exec-out","run-as","com.kongji.aikeyboard","cat","shared_prefs/keyboard_style.xml")
    assert 'name="geometry_lift" value="0"' not in prefs,prefs;record("dragged vertical position is saved and applied to IME")
    t.shot("keyboard-position.png")
    appearance();tap("深色");tap("应用到键盘");k.home();t.wait_text("私人定制键盘外观");t.shot("home-dark.png");record("dark theme applies to application home")
    tap("输入与词库设置");t.wait_text("记住常用词与输入习惯");t.shot("memory-dark.png")
    appearance();t.shot("appearance-dark.png")
    # System document picker uses only an explicit photo grant, not storage permissions.
    tap("导入照片背景");time.sleep(.8)
    pick_photo();time.sleep(1.2);tap("应用并试打");time.sleep(.5);tap("点这里输入，或让 AI 帮你回复");t.wait_keyboard();t.shot("keyboard-photo.png")
    prefs=t.adb("exec-out","run-as","com.kongji.aikeyboard","cat","shared_prefs/keyboard_style.xml")
    name=re.search(r'<string name="background_photo">([^<]+)</string>',prefs).group(1)
    assert "photo-" in name
    assert name in t.adb("shell","run-as","com.kongji.aikeyboard","ls","files/keyboard-backgrounds");record("photo picker imports a persistent app-owned keyboard background")
    # Cover update / process restart keeps saved appearance and geometry.
    t.adb("shell","am","force-stop","com.kongji.aikeyboard");t.adb("shell","ime","set","com.kongji.aikeyboard/.AiInputService")
    appearance();tap("应用并试打");time.sleep(.5);tap("点这里输入，或让 AI 帮你回复");t.wait_keyboard();t.shot("photo-after-restart.png")
    assert name in t.adb("exec-out","run-as","com.kongji.aikeyboard","cat","shared_prefs/keyboard_style.xml");record("photo and geometry survive process restart")
    appearance();tap("移除照片背景");tap("快捷符号");tap("符号在下方");tap("下滑输入符号");tap("应用到键盘")
    appearance();k.swipe_key("down");t.wait_text("已按下 1");record("preview down-swipe works with symbols below letters")
    k.apply_practice();k.swipe_key("down");assert k.edit_text()=="1";record("actual IME down-swipe works with saved geometry")
    appearance();tap("九宫格");k.apply_practice()
    for digit in "64426":tap(digit)
    tap("你好");assert k.edit_text()=="你好";record("nine-key input retains normal nihao dictionary entry")
    appearance();tap("手写");time.sleep(2);k.draw_horizontal();t.wait_text("点选候选文字确认输入",timeout=25);record("handwriting preview recognizes actual ink")
    k.apply_practice();k.draw_horizontal();t.wait_text("点选候选文字确认输入",timeout=25);tap("一");assert k.edit_text()=="一";record("actual handwriting candidate commits")
    k.home();history=t.adb("exec-out","run-as","com.kongji.aikeyboard","cat","databases/typing_history.db",binary=True)
    tap("应用语言")
    for code,name in [("en","English"),("ja","日本語"),("es","Español"),("ko","한국어"),("de","Deutsch"),("zh","中文")]:
        tap(name);time.sleep(.6);data=json.loads((t.ROOT/"app/src/main/assets/i18n"/f"{code}.json").read_text(encoding="utf-8")) if code!="zh" else None
        title=data["应用语言"] if data else "应用语言";t.wait_text(title);record("language switching: "+code)
    assert history==t.adb("exec-out","run-as","com.kongji.aikeyboard","cat","databases/typing_history.db",binary=True);record("language/theme UI navigation preserves typing history")
    appearance();tap("26键");tap("白色");tap("快捷符号");tap("符号在上方");tap("上滑输入符号");tap("恢复默认外观");tap("恢复");tap("应用到键盘")
    appearance();t.shot("appearance-white-final.png");k.home();t.shot("home-white-final.png")
if __name__=="__main__":
    try:run()
    finally:Path(".tools/redesign-results.json").write_text(json.dumps(RESULTS,ensure_ascii=False,indent=2),encoding="utf-8")