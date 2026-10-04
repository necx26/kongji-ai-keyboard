"""Real IME checks against the supplied layout and recent-input deletion semantics."""
import sys,time,json,shutil
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8');sys.stderr.reconfigure(encoding='utf-8')
import smartbar_smoke as s
r=s.r;t=s.t;t.OUT=Path('.tools/v052-screens');t.OUT.mkdir(parents=True,exist_ok=True)
RESULTS=[]
DELETE='删除，上滑删除最近输入，下滑撤回'
def record(message):RESULTS.append(message);print('PASS:',message,flush=True)
def swipe(direction,label=DELETE,photo=None,hold=70):
    x1,y1,x2,y2=r.coords(s.find(label));x=(x1+x2)//2;y=(y1+y2)//2
    r.gesture(x,y,x,y+(-90 if direction=='up' else 90),hold,photo);time.sleep(.3)
def ready():s.find('回车');time.sleep(.3)
def mode(label):s.setmode(label);ready()
def run():
    t.adb('shell','ime','enable','com.kongji.aikeyboard/.AiInputService');t.adb('shell','ime','set','com.kongji.aikeyboard/.AiInputService')
    s.appearance();r.tap('26键');r.tap('深色');r.tap('应用并试打');time.sleep(.7);r.tap('点这里输入，或让 AI 帮你回复');ready();s.reset('')
    prefs=t.adb('exec-out','run-as','com.kongji.aikeyboard','cat','shared_prefs/input_preferences.xml')
    if 'name="chinese_mode" value="false"' in prefs:s.key('切换中英文')
    assert s.has('帮我回答') and s.has('123') and s.find('切换中英文') is not None and s.find('剪贴板') is not None
    t.shot('reference-dark.png');record('reference four-row keyboard and bottom icons are shown in the actual IME')
    s.reset('更早的文字')
    for ch in 'nihao':s.key(ch)
    t.wait_text('你好');assert s.value()=='更早的文字nihao'
    swipe('up',photo='delete-preview.png');assert s.value()=='更早的文字' and s.has('帮我回答');record('up-swipe deletes only pending pinyin, preserving the old prefix')
    swipe('down');assert s.value()=='更早的文字nihao' and s.has('你好');s.key('m');assert s.value()=='更早的文字nihaom';record('down-swipe restores pinyin; further typing does not duplicate it')
    swipe('up');s.reset('旧文字')
    for ch in 'nihao':s.key(ch)
    s.key('你好');assert s.value()=='旧文字你好'
    swipe('up');assert s.value()=='旧文字';swipe('up');assert s.value()=='旧文字';swipe('down');assert s.value()=='旧文字你好';record('last confirmed word is deleted and restored; repeated swipe cannot delete older words')
    swipe('up');assert s.value()=='旧文字';s.reset('旧文字新粘贴');swipe('down');assert s.value()=='旧文字新粘贴';record('restored word can be deleted again; new pasted text is protected from undo')
    s.reset('先前文字');swipe('up');assert s.value()=='先前文字';record('opening or pasting pre-existing text does not invent a last-input range')
    swipe('up',label='回车');assert s.value().startswith('先前文字') and '\n' in s.value();record('swiping enter performs only ordinary enter, never recent deletion')
    s.reset('');q=r.coords(s.find('q'));w=r.coords(s.find('w'));mid=(q[2]+w[0])//2;y=(q[1]+q[3])//2
    t.adb('shell','input','tap',str(mid-2),str(y));time.sleep(.2);assert s.value()=='q';swipe('up')
    t.adb('shell','input','tap',str(mid+2),str(y));time.sleep(.2);assert s.value()=='w';swipe('up');record('both sides of the q/w gap choose the closer letter')
    a=r.coords(s.find('a'));t.adb('shell','input','tap','2',str((a[1]+a[3])//2));time.sleep(.2);assert s.value()=='a';swipe('up');record('outer gap beside the inset second row remains tappable')
    s.reset('旧');s.key('切换中英文')
    for ch in 'hel':s.key(ch)
    s.key('hello');assert s.value()=='旧hello ';swipe('up');assert s.value()=='旧';swipe('down');assert s.value()=='旧hello ';record('English confirmed word and its automatic space are deleted/restored together')
    s.key('切换中英文');mode('九宫格');s.reset('前文')
    for label in ('6 MNO','4 GHI','4 GHI','2 ABC','6 MNO'):s.key(label)
    swipe('up');assert s.value()=='前文';swipe('down');assert s.value()=='前文64426';record('nine-key delete gestures preserve earlier text and restore composition')
    mode('手写');s.reset('前文');time.sleep(2);r.k.draw_horizontal();t.wait_text('点选候选文字确认输入',timeout=25);s.key('一');assert s.value()=='前文一'
    swipe('up');assert s.value()=='前文';swipe('down');assert s.value()=='前文一';record('handwriting canvas keeps its touch input; deletion targets only the confirmed character')
    mode('26键');original='甲乙丙丁戊己庚辛壬癸'*5;s.reset(original);x1,y1,x2,y2=r.coords(s.find(DELETE));x=(x1+x2)//2;y=(y1+y2)//2
    # Keep the intended hold duration: screenshot compression in the preview probe
    # otherwise extends a stationary press by several seconds on the emulator.
    t.adb('shell','input','swipe',str(x),str(y),str(x),str(y),'650');time.sleep(.2);value=s.value();assert 0<len(value)<50 and original.startswith(value);time.sleep(.3);assert s.value()==value;record('holding delete removes individual characters and stops at release')
    s.reset('');s.appearance();r.tap('白色');r.tap('应用并试打');time.sleep(.6);r.tap('点这里输入，或让 AI 帮你回复');ready();t.shot('reference-white.png');record('white theme and 26-key mode restored after verification')
if __name__=='__main__':
    try:run()
    finally:Path('.tools/reference-results.json').write_text(json.dumps(RESULTS,ensure_ascii=False,indent=2),encoding='utf-8')
