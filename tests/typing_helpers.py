"""Input-engine integration checks on the dedicated emulator, with synthetic test phrases only."""
import json
import re
import sqlite3
import sys
import time
from pathlib import Path
import adb_smoke as t

def start():
    t.adb('shell','am','start','-f','0x14000000','-n','com.kongji.aikeyboard/.SettingsActivity');time.sleep(.5)
    t.adb('shell','ime','enable','com.kongji.aikeyboard/.AiInputService');t.adb('shell','ime','set','com.kongji.aikeyboard/.AiInputService')
def practice():
    start();t.scroll_tap('打开键盘练习页');t.tap('点这里输入，或让 AI 帮你回复');t.wait_keyboard()
    end=time.time()+20
    while time.time()<end:
        if not any('词库加载' in n.attrib.get('text','') for n in t.nodes()):break
        time.sleep(.5)
    else:raise AssertionError('Lexicon did not load')
    if any(n.attrib.get('text')=='ABC' for n in t.nodes()):t.tap('ABC')
def text():
    edit=next(n for n in t.nodes() if n.attrib.get('class')=='android.widget.EditText')
    return edit.attrib.get('text','')
def chinese():
    if any(n.attrib.get('text')=='EN' for n in t.nodes()):t.tap('EN')
def english():
    if any(n.attrib.get('text')=='中' for n in t.nodes()):t.tap('中')
def keys(word):
    for key in word:t.tap(key)
def long_press(label):
    n=next(n for n in t.nodes() if n.attrib.get('text')==label or n.attrib.get('content-desc')==label)
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']));x=str((x1+x2)//2);y=str((y1+y2)//2)
    t.adb('shell','input','swipe',x,y,x,y,'700');time.sleep(.3)
def candidate(word):
    for n in t.nodes():
        if n.attrib.get('text')==word or n.attrib.get('content-desc')==word:t.tap_node(n);return
    t.tap('展开候选')
    for _ in range(14):
        for n in t.nodes():
            if n.attrib.get('text')==word or n.attrib.get('content-desc')==word:t.tap_node(n);return
        t.adb('shell','input','swipe','660','1070','660','890','350');time.sleep(.2)
    raise AssertionError('Candidate missing '+word)
def snapshot():
    # Copy only synthetic test data after the keyboard is idle. Include WAL if SQLite enabled it.
    time.sleep(.6);p=t.ROOT/'.tools'/'typing-ui-snapshot.db'
    p.write_bytes(t.adb('exec-out','run-as','com.kongji.aikeyboard','cat','databases/typing_history.db',binary=True))
    # Android normally uses a rollback journal here; inspect actual files rather than assuming WAL.
    files=t.adb('shell','run-as','com.kongji.aikeyboard','ls','databases')
    if 'typing_history.db-wal' in files:(Path(str(p)+'-wal')).write_bytes(t.adb('exec-out','run-as','com.kongji.aikeyboard','cat','databases/typing_history.db-wal',binary=True))
    connection=sqlite3.connect(str(p));rows=connection.execute('select language,spelling,value,hits from words order by language,spelling,value').fetchall();connection.close();return rows
def input_settings():
    start();t.tap('输入与词库设置');t.wait_text('输入与词库')
