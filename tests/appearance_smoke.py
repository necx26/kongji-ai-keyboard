"""Tap the appearance editor and verify the applied IME on the dedicated emulator."""
import json
import re
import time
import adb_smoke as t

def settings():
    t.adb('shell','am','start','-f','0x14000000','-n','com.kongji.aikeyboard/.SettingsActivity')
    time.sleep(.5)

def prefs():
    return t.adb('shell','run-as','com.kongji.aikeyboard','cat','shared_prefs/keyboard_style.xml')

def slider(name, fraction):
    for _ in range(8):
        found=next((n for n in t.nodes() if n.attrib.get('content-desc')==name and n.attrib.get('class')=='android.widget.SeekBar'),None)
        if found is not None:
            x1,y1,x2,y2=map(int,re.findall(r'\d+',found.attrib['bounds']))
            if y2-y1>25 and y2<1440:
                t.adb('shell','input','tap',str(round(x1+18+(x2-x1-36)*fraction)),str((y1+y2)//2));time.sleep(.4);return
        t.adb('shell','input','swipe','710','1330','710','700','400');time.sleep(.25)
    raise AssertionError('Slider missing '+name)

def editor():
    settings()
    t.adb('shell','ime','enable','com.kongji.aikeyboard/.AiInputService')
    t.adb('shell','ime','set','com.kongji.aikeyboard/.AiInputService')
    t.tap('定制键盘外观');t.wait_text('外观工作室')

try:
    editor()
    t.scroll_tap('恢复默认外观');t.tap('恢复')
    t.wait_text('外观工作室')
    t.shot('08-外观工作室.png')
    t.tap('深空')
    t.shot('09-深空主题预览.png')
    slider('按键透明度',.2)
    slider('按键圆角',.95)
    slider('按键间距',.9)
    slider('按键高度',.95)
    slider('字母字号',.95)
    t.scroll_tap('AZERTY');t.scroll_tap('独立数字行')
    t.scroll_tap('按键轻触反馈');t.scroll_tap('按压回弹动画')
    t.shot('10-布局和触感设置.png')
    t.tap('应用并试打');t.tap('点这里输入，或让 AI 帮你回复');t.wait_keyboard()
    t.shot('11-实际深空数字键盘.png')
    p=prefs()
    assert '<int name="palette" value="3"' in p and '<int name="layout" value="2"' in p
    assert '<boolean name="number_row" value="true"' in p
    assert '<boolean name="motion" value="false"' in p and '<boolean name="haptic" value="false"' in p
    assert int(re.search(r'name="height" value="(\d+)"',p)[1])>=56
    current=t.nodes();names=[n.attrib.get('text') for n in current]
    assert all(str(i) in names for i in range(10))
    letters=[n for n in current if n.attrib.get('text') in ['a','z','e','r','t','y']]
    assert len(letters)==6 and len({n.attrib['bounds'].split('][')[0].split(',')[1] for n in letters})==1
    t.tap('1');t.tap('2');t.assert_text('12')
    t.record('实际键盘应用深空主题、最大附近尺寸、AZERTY 和独立数字行；数字输入正常')
    t.adb('shell','am','force-stop','com.kongji.aikeyboard')
    settings();t.assert_text('深空 · AZERTY')
    # Android switches away from a forcibly stopped IME; select it again for later input checks.
    t.adb('shell','ime','set','com.kongji.aikeyboard/.AiInputService')
    t.tap('定制键盘外观');t.wait_text('外观工作室');t.tap('冰蓝');t.tap('返回设置')
    t.assert_text('深空 · AZERTY')
    assert prefs()==p
    t.record('外观重启后保留；未应用的预览不会覆盖已保存设置')
    t.tap('定制键盘外观');t.wait_text('外观工作室');t.scroll_tap('恢复默认外观');t.tap('恢复')
    t.tap('应用并试打');t.tap('点这里输入，或让 AI 帮你回复');t.wait_keyboard()
    t.shot('12-实际雾紫键盘.png')
    p=prefs();assert '<int name="palette" value="0"' in p and '<int name="height" value="46"' in p and '<boolean name="number_row" value="false"' in p
    t.record('恢复默认并应用，实际键盘恢复雾紫 QWERTY 标准尺寸')
finally:
    (t.ROOT/'.tools'/'appearance-results.json').write_text(json.dumps(t.RESULTS,ensure_ascii=False,indent=2),encoding='utf-8')
