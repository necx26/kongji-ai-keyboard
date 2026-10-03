"""Run on a dedicated emulator with the app and ui-probe APKs installed.

Uses actual touches and synthetic practice text. Never clears typing history or API settings.
"""
import json
import os
import re
import sys
import time
from pathlib import Path
import adb_smoke as t

t.ADB = os.environ.get('ANDROID_ADB', r'D:\Android\Sdk\platform-tools\adb.exe')
t.SERIAL = os.environ.get('ANDROID_SERIAL', 'emulator-5554')
t.OUT = t.ROOT / '.tools' / 'v040-screens'
t.OUT.mkdir(parents=True, exist_ok=True)
RESULTS = []

def stable_tap(label):
    deadline = time.time() + 10
    while time.time() < deadline:
        for node in t.nodes():
            if node.attrib.get('text') == label or node.attrib.get('content-desc') == label:
                t.tap_node(node)
                return
        time.sleep(.3)
    raise AssertionError('UI item missing after transition: ' + label)

t.tap = stable_tap

def record(message):
    RESULTS.append(message)
    print('PASS:', message, flush=True)

def home():
    t.adb('shell', 'am', 'start', '-f', '0x14000000', '-n', 'com.kongji.aikeyboard/.SettingsActivity')
    time.sleep(.8)

def appearance():
    home(); t.wait_text('私人定制键盘外观'); t.tap('私人定制键盘外观'); t.wait_text('外观工作室')

def apply_practice():
    t.tap('应用并试打'); time.sleep(.8)
    t.wait_text('点这里输入，或让 AI 帮你回复')
    t.tap('点这里输入，或让 AI 帮你回复'); t.wait_keyboard(); time.sleep(.5)

def edit_text():
    node = next(n for n in t.nodes() if n.attrib.get('class') == 'android.widget.EditText')
    text = node.attrib.get('text', '')
    return '' if text == '点这里输入，或让 AI 帮你回复' else text

def bounds(description):
    node = next(n for n in t.nodes() if n.attrib.get('content-desc') == description or n.attrib.get('text') == description)
    return tuple(map(int, re.findall(r'\d+', node.attrib['bounds'])))

def swipe_key(direction):
    x1, y1, x2, y2 = bounds('q')
    x = str((x1 + x2) // 2)
    start, end = (y1 + (y2-y1)*.8, y1 + (y2-y1)*.2)
    if direction == 'down': start, end = end, start
    t.adb('shell', 'input', 'swipe', x, str(int(start)), x, str(int(end)), '200')
    time.sleep(.4)

def draw_horizontal():
    x1, y1, x2, y2 = bounds('手写区域'); y = str((y1 + y2) // 2)
    t.adb('shell', 'input', 'swipe', str(x1+(x2-x1)//4), y, str(x1+3*(x2-x1)//4), y, '450')
    time.sleep(2)

def run():
    t.adb('shell', 'cmd', 'locale', 'set-app-locales', 'com.kongji.aikeyboard', '--locales', 'zh')
    t.adb('shell', 'ime', 'enable', 'com.kongji.aikeyboard/.AiInputService')
    t.adb('shell', 'ime', 'set', 'com.kongji.aikeyboard/.AiInputService')
    t.adb('shell', 'settings', 'put', 'secure', 'show_ime_with_hard_keyboard', '1')
    for direction, position in [('up', '符号在上方'), ('down', '符号在下方')]:
        appearance(); t.tap('26键'); t.tap(position); t.tap('上滑输入符号' if direction == 'up' else '下滑输入符号')
        # Preview must intercept a symbol gesture without scrolling its containing page.
        swipe_key(direction); t.wait_text('已按下 1'); record(f'26键预览 {direction} 滑动输入符号')
        t.shot(f'26键-{direction}-预览.png'); apply_practice()
        swipe_key(direction); assert edit_text() == '1', edit_text()
        record(f'26键实际 {direction} 滑动只输入一个符号，不重复字母')
        t.tap('q'); assert edit_text() == '1q', edit_text()
        record(f'26键 {direction} 模式普通点按仍输入字母')
        t.shot(f'26键-{direction}-实际输入.png')
    appearance(); t.tap('九宫格'); t.shot('九宫格-预览.png'); apply_practice()
    for key in '64426': t.tap(key)
    t.tap('你好'); assert edit_text() == '你好', edit_text()
    record('九宫格实际输入64426，候选填入你好'); t.shot('九宫格-实际输入.png')
    appearance(); t.tap('手写'); time.sleep(.8)
    current = t.nodes()
    if any('首次使用请下载' in n.attrib.get('text', '') for n in current):
        t.tap('下载手写模型'); t.wait_text('模型已就绪', timeout=90)
    draw_horizontal(); t.wait_text('一 ·'); record('手写预览识别真实触摸笔画'); t.shot('手写-预览识别.png')
    apply_practice(); draw_horizontal(); t.tap('一'); assert edit_text() == '一', edit_text()
    record('手写实际候选填入一'); t.shot('手写-实际输入.png')
    home()
    history = t.adb('exec-out', 'run-as', 'com.kongji.aikeyboard', 'cat', 'databases/typing_history.db', binary=True)
    style = t.adb('exec-out', 'run-as', 'com.kongji.aikeyboard', 'cat', 'shared_prefs/keyboard_style.xml', binary=True)
    t.tap('应用语言')
    for code, name in [('en','English'), ('ja','日本語'), ('es','Español'), ('ko','한국어'), ('de','Deutsch'), ('zh','中文')]:
        t.tap(name); time.sleep(.8)
        title = '应用语言' if code == 'zh' else json.loads((t.ROOT / 'app/src/main/assets/i18n' / f'{code}.json').read_text(encoding='utf-8'))['应用语言']
        t.wait_text(title); t.shot(f'语言-{code}.png')
        selected = t.adb('shell', 'cmd', 'locale', 'get-app-locales', 'com.kongji.aikeyboard')
        assert '[' + code + ']' in selected, selected
        record(f'{name}通过设置切换，系统保存应用语言')
    assert history == t.adb('exec-out', 'run-as', 'com.kongji.aikeyboard', 'cat', 'databases/typing_history.db', binary=True)
    assert style == t.adb('exec-out', 'run-as', 'com.kongji.aikeyboard', 'cat', 'shared_prefs/keyboard_style.xml', binary=True)
    record('六种语言切换未改变输入记忆和键盘外观数据')
    appearance(); t.tap('26键'); t.tap('符号在上方'); t.tap('上滑输入符号'); t.tap('应用到键盘'); time.sleep(.6)
    appearance(); t.shot('新版-最终设置预览.png')
    (t.ROOT / '.tools/keyboard-mode-results.json').write_text(json.dumps(RESULTS, ensure_ascii=False, indent=2), encoding='utf-8')

if __name__ == '__main__':
    if hasattr(sys.stdout, 'reconfigure'): sys.stdout.reconfigure(encoding='utf-8')
    run()
