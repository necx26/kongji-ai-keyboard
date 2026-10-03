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

try:
    if '--resume-ranking' in sys.argv:t.RESULTS.extend(json.loads((t.ROOT/'.tools'/'typing-results.json').read_text(encoding='utf-8')))
    else:
        input_settings();t.scroll_tap('清空用户词库和学习记录');t.tap('清空');t.wait_text('本机用户词：0')
        practice();chinese();keys('jintianxiawuwomenqubeijing');candidate('今天下午我们去北京');assert text()=='今天下午我们去北京';t.shot('03-整句上屏.png');t.record('连续多个中文词解码并整句上屏')
        practice();chinese();keys('nihaoshijie');candidate('你好');t.wait_text('shijie');assert text()=='你好shijie';candidate('世界');assert text()=='你好世界';t.record('选择前词保留剩余拼音，继续选词后学习组合短语')
        rows=snapshot();assert any(r[2]=='你好世界' and r[1]=='ni hao shi jie' for r in rows)
    for _ in range(3):practice();chinese();keys('keyi');candidate('刻意')
    practice();chinese();keys('keyi')
    labels=[n.attrib.get('text') for n in t.nodes() if n.attrib.get('class')=='android.widget.Button']
    assert labels.index('刻意')<labels.index('可以');t.shot('04-学习后的候选排序.png');t.record('选中较少用同音词后，真实候选顺序提升')
    t.adb('shell','am','force-stop','com.kongji.aikeyboard');practice();chinese();keys('keyi');candidate('刻意');assert text()=='刻意';t.record('重新启动输入法后学习词继续存在并可上屏')
    practice();chinese();keys('nh');candidate('你好');assert text()=='你好';t.record('简拼 nh 候选选择')
    practice();chinese();keys('zongguo');candidate('中国');assert text()=='中国';t.record('模糊声母 z/zh 候选选择')
    practice();chinese();keys('xi');long_press('句号，长按输入撇号');keys('an');candidate('西安');assert text()=='西安';t.record('隔音符 xi\'an 选择西安')
    practice();english();keys('hel');candidate('hello');assert text()=='hello ';t.wait_text('world');t.shot('05-英文补全和下一词.png');t.record('英文补全 hello 与后续词 world 推荐')
    keys('teh');candidate('the');assert text()=='hello the ';t.record('英文错序拼写纠错候选可替换原词')
    practice();english();keys('qzxword');t.tap('空格');assert text()=='qzxword ';t.record('空格不会强行修改未知专有词')
    practice();english();t.tap('切换大小写');t.tap('H');keys('el');candidate('Hello');assert text()=='Hello ';t.tap('.');assert text()=='Hello.';t.record('Shift 一次首字母大写，标点去掉候选附加空格')
    before=snapshot();input_settings();t.scroll_tap('记住常用词与输入习惯');practice();english();keys('nolearnprobe');t.tap('空格');assert snapshot()==before;t.record('关闭记忆后普通打字不再写入学习数据库')
    input_settings();t.scroll_tap('记住常用词与输入习惯');start();t.scroll_tap('API Key 输入框');t.wait_keyboard();keys('passwordprobe');assert snapshot()==before;t.record('密码输入框不写入词库')
    input_settings();t.shot('06-输入与词库设置.png');t.scroll_tap('清空用户词库和学习记录');t.tap('清空');t.wait_text('本机用户词：0');assert snapshot()==[];t.record('清空记忆后词库记录确实删除')
finally:
    (t.ROOT/'.tools'/'typing-results.json').write_text(json.dumps(t.RESULTS,ensure_ascii=False,indent=2),encoding='utf-8')
