"""The actual symbol keyboard's bottom buttons must share one rectangle height/top."""
import re
import time
import adb_smoke as t

t.adb('shell','am','start','-f','0x14000000','-n','com.kongji.aikeyboard/.SettingsActivity')
time.sleep(.5)
t.adb('shell','ime','enable','com.kongji.aikeyboard/.AiInputService')
t.adb('shell','ime','set','com.kongji.aikeyboard/.AiInputService')
t.scroll_tap('打开键盘练习页');t.tap('点这里输入，或让 AI 帮你回复');t.wait_keyboard()
if not any(n.attrib.get('text')=='ABC' for n in t.nodes()):t.tap('123')
time.sleep(.6)
items={n.attrib['text']:tuple(map(int,re.findall(r'\d+',n.attrib['bounds']))) for n in t.nodes() if n.attrib.get('text') in ['ABC','中','EN','切换','空格','。','.','回车']}
print('BOTTOM ROW:',items,flush=True)
assert 'ABC' in items and len(items)==6
assert len({(v[1],v[3]) for v in items.values()})==1, 'ABC / bottom row shifted vertically'
t.shot('01-ABC对齐.png')
print('PASS: ABC 与底栏各键顶部和底部完全对齐',flush=True)
