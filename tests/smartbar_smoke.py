"""Actual IME toolbar/candidate switching and full-draft clear/undo regression.
Uses only synthetic practice text; no user database clearing or API requests.
"""
import sys,time,json
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8');sys.stderr.reconfigure(encoding='utf-8')
import redesign_smoke as r
t=r.t;t.OUT=Path('.tools/v051-screens');t.OUT.mkdir(parents=True,exist_ok=True)
RESULTS=[]
def record(message):RESULTS.append(message);print('PASS:',message,flush=True)
def nodes():
 for _ in range(8):
  current=t.nodes()
  if any(n.attrib.get('text')=='↵' or n.attrib.get('class')=='android.widget.EditText' for n in current):return current
  time.sleep(.2)
 raise AssertionError('Transient UI tree never settled')
def find(label):
 for _ in range(12):
  for n in nodes():
   if n.attrib.get('class')=='android.widget.Button' and (n.attrib.get('text')==label or n.attrib.get('content-desc')==label):return n
  time.sleep(.2)
 raise AssertionError('Button missing: '+label)
def key(label):t.tap_node(find(label));time.sleep(.12)
def has(label):return any(n.attrib.get('text')==label for n in nodes())
def value():return r.k.edit_text()
def reset(text):
 if value()==text:return
 for attempt in range(5):
  try:r.reset_input(text);return
  except AssertionError:time.sleep(.3)
 raise AssertionError('Practice text replacement failed')
def swipe(direction,photo=None):
 x1,y1,x2,y2=r.coords(find('↵'));x=(x1+x2)//2;y=(y1+y2)//2
 r.gesture(x,y,x,y+(-90 if direction=='up' else 90),70,photo);time.sleep(.25)
def appearance():
 t.adb('shell','am','start','-W','-f','0x14000000','-n','com.kongji.aikeyboard/.SettingsActivity');time.sleep(.5);r.tap('外观');time.sleep(.5)
def practice():
 t.adb('shell','am','start','-W','-n','com.kongji.aikeyboard/.PracticeActivity');time.sleep(.5);r.tap('点这里输入，或让 AI 帮你回复');time.sleep(.8)
def setmode(label):
 appearance();r.tap(label);r.tap('应用并试打');time.sleep(.5);r.tap('点这里输入，或让 AI 帮你回复');time.sleep(.5)
def run():
 t.adb('shell','ime','enable','com.kongji.aikeyboard/.AiInputService');t.adb('shell','ime','set','com.kongji.aikeyboard/.AiInputService')
 setmode('26键');reset('');time.sleep(.3)
 if has('EN'):key('EN')
 assert has('帮我回答') and not has('中文 · 连续拼音与简拼')
 assert not has('点“帮我回答”，读取当前可见对话')
 baseline=r.coords(find('q'));t.shot('toolbar-idle.png');record('idle toolbar visible; both explanatory rows removed')
 for letter in 'nihao':key(letter)
 assert not has('帮我回答') and has('你好'),[(n.attrib.get('text')) for n in nodes()]
 assert r.coords(find('q'))==baseline
 t.shot('candidates-chinese.png');record('typing swaps tools for candidates without moving the keys')
 swipe('up','clear-preview.png');assert value()=='' and has('帮我回答');record('up-swipe clears entire composing draft and restores toolbar')
 swipe('down');assert value()=='nihao' and has('你好');record('down-swipe restores pinyin and candidates')
 key('m');assert value()=='nihaom',value();record('restored composing span accepts more letters without duplicate text')
 swipe('up');reset('新文字');time.sleep(.2);swipe('down');assert value()=='新文字';record('undo never overwrites text pasted after clear')
 original='第一行🙂\n第二行还有后缀';reset(original)
 t.adb('shell','input','keyevent','21');t.adb('shell','input','keyevent','21');time.sleep(.2)
 swipe('up');assert value()=='';swipe('up');swipe('down');assert value()==original,value();record('mid-cursor multiline Unicode clear/undo; repeated empty clear retains undo')
 key('↵');assert '\n' in value() and len(value())==len(original)+1;record('normal arrow tap still performs enter')
 swipe('up');key('q');swipe('down');assert value()=='q';record('typing after clear invalidates previous undo')
 swipe('up');t.adb('shell','input','keyevent','4');time.sleep(.4);r.tap('点这里输入，或让 AI 帮你回复');time.sleep(.5);swipe('down');assert value()=='';record('hiding input method drops pending restore')
 key('中')
 for letter in 'hel':key(letter)
 assert not has('帮我回答') and has('hello');t.shot('candidates-english.png');record('English typing uses the same candidate row')
 swipe('up');swipe('down');assert value()=='hel';record('English draft undo preserves composing text')
 setmode('九宫格')
 if has('ABC'):key('ABC')
 # ABC switches language, so switch back through EN if necessary.
 if has('EN'):key('EN')
 for label in ('6 MNO','4 GHI','4 GHI','2 ABC','6 MNO'):key(label)
 assert has('你好') and not has('帮我回答');t.shot('candidates-nine.png');record('nine-key typing swaps tools for Chinese candidates')
 swipe('up');swipe('down');assert value()=='64426';key('你好');assert value()=='你好';record('nine-key clear/undo restores remaining composition and candidate selection')
 setmode('手写');time.sleep(2);r.k.draw_horizontal();t.wait_text('点选候选文字确认输入',timeout=25)
 assert has('一') and not has('帮我回答');t.shot('candidates-handwriting.png');key('一');assert value()=='一';assert has('帮我回答');record('handwriting candidates share the top row; choosing returns to tools')
 swipe('up');assert value()=='';swipe('down');assert value()=='一';record('handwriting mode enter arrow clears and restores committed text')
 setmode('26键');record('26-key layout restored after checks')
if __name__=='__main__':
 try:run()
 finally:Path('.tools/smartbar-results.json').write_text(json.dumps(RESULTS,ensure_ascii=False,indent=2),encoding='utf-8')
