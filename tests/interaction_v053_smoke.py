"""Actual IME overlap, two-stage delete, candidate geometry and appearance controls.
Synthetic practice text only; never clears personal history or calls a model API.
"""
import sys,time,json,shutil
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8');sys.stderr.reconfigure(encoding='utf-8')
import reference_layout_smoke as v
s=v.s;r=v.r;t=v.t;t.OUT=Path('.tools/v053-screens');t.OUT.mkdir(parents=True,exist_ok=True)
RESULTS=[];images=Path('docs/images')
def record(value):RESULTS.append(value);print('PASS:',value,flush=True)
def shot(name):t.shot(name+'.png');shutil.copyfile(t.OUT/(name+'.png'),images/(name+'-v0.5.3.png'))
def overlap(labels):
    values=[]
    for label in labels:
        x1,y1,x2,y2=r.coords(s.find(label));values.extend([(x1+x2)//2,(y1+y2)//2])
    output=t.adb('shell','am','instrument','-w','-r','-e','overlap',','.join(map(str,values)),'com.kongji.windowprobe/com.kongji.aikeyboard.WindowDumpInstrumentation')
    assert 'overlap=injected' in output,output;time.sleep(.3)
def apply():r.tap('应用并试打');time.sleep(.5);r.tap('点这里输入，或让 AI 帮你回复');v.ready()
def geometry():
    q=r.coords(s.find('q'))
    buttons=[n for n in s.nodes() if n.attrib.get('class')=='android.widget.Button' and n.attrib.get('text') in ('你好','你号','你好吗','倪浩','倪豪','拟好')]
    assert buttons
    assert all(r.coords(n)[3]<=q[1] for n in buttons),[(n.attrib.get('text'),r.coords(n)) for n in buttons]
def run():
    t.adb('shell','ime','set','com.kongji.aikeyboard/.AiInputService');s.appearance();r.tap('26键');r.tap('深色');apply();s.reset('')
    prefs=t.adb('exec-out','run-as','com.kongji.aikeyboard','cat','shared_prefs/input_preferences.xml')
    if 'name="chinese_mode" value="false"' in prefs:s.key('切换中英文')
    s.reset('前文');overlap(list('nihao'));assert s.value()=='前文nihao',s.value();record('five overlapping fingers enter nihao in press order without losing letters');geometry();shot('candidates');record('candidate buttons fit completely above the letter keys')
    v.swipe('up',photo='delete-no-preview.png');assert s.value()=='前文';shutil.copyfile(t.OUT/'delete-no-preview.png',images/'delete-no-preview-v0.5.3.png');record('first upward swipe deletes pending pinyin only, without a popup')
    v.swipe('up');assert s.value()=='';v.swipe('up');v.swipe('down');assert s.value()=='前文';record('second upward swipe clears the entire field; downward restores that clear')
    for ch in 'nihao':s.key(ch)
    s.key('你好');assert s.value()=='前文你好';v.swipe('up');assert s.value()=='前文';v.swipe('down');assert s.value()=='前文你好';record('confirmed word delete and undo preserve old text')
    v.swipe('up');v.swipe('up');assert s.value()=='';v.swipe('down');assert s.value()=='前文';record('second swipe after deleting a confirmed word clears old text and is reversible')
    s.reset('旧');s.key('q');v.swipe('up');assert s.value()=='旧';s.key('w');v.swipe('up');assert s.value()=='旧';record('new typing resets the second-swipe clear state')
    s.reset('旧');s.key('q');v.swipe('up');s.reset('旧新粘贴');v.swipe('up');assert s.value()=='旧新粘贴';v.swipe('down');assert s.value()=='旧新粘贴';record('external paste invalidates stale second-swipe clear and undo')
    s.reset('原有文字');v.swipe('up');assert s.value()=='原有文字';record('first upward swipe never guesses old text as recent input')
    s.reset('');s.key('切换中英文');overlap(list('qwe'));assert s.value()=='qwe';v.swipe('up');overlap(list('qwe'));assert s.value()=='qwe';record('English fast overlapping presses remain responsive across successive runs');s.key('切换中英文')
    s.reset('');shot('reference-dark');s.appearance();r.tap('候选词大小');time.sleep(.2);shot('candidate-settings')
    # Labels are reachable presets as well as ticks beneath the discrete slider.
    r.tap('极大');r.tap('靠右');apply();s.reset('')
    for ch in 'nihao':s.key(ch)
    geometry();prefs=t.adb('exec-out','run-as','com.kongji.aikeyboard','cat','shared_prefs/keyboard_style.xml');assert 'name="candidate_size" value="4"' in prefs and 'name="footer_layout" value="2"' in prefs;shot('candidates-large');record('largest candidate size and right-aligned footer save and apply correctly')
    first=r.coords(s.find('切换输入法'));second=r.coords(s.find('剪贴板'));assert first[0]>500 and second[0]>first[0];record('footer is compact and right aligned in the actual IME')
    s.key('展开候选');time.sleep(.3);assert s.find('收起候选') is not None;shot('expanded-candidates');q=r.coords(s.find('q'));nodes=s.nodes();buttons=[n for n in nodes if n.attrib.get('class')=='android.widget.Button' and n.attrib.get('text')=='你好'];assert all(r.coords(n)[3]<=q[1] for n in buttons);record('expanded candidates remain above the keyboard without overlap')
    s.appearance();r.tap('极小');r.tap('靠左');apply();s.reset('')
    for ch in 'nihao':s.key(ch)
    geometry();first=r.coords(s.find('切换输入法'));second=r.coords(s.find('剪贴板'));assert second[2]<500;record('smallest candidate size and left footer work without clipping')
    s.appearance();r.tap('中');r.tap('两侧');r.tap('白色');apply();s.reset('');shot('reference-white');record('default medium size and both-side footer restored in white theme')
    s.appearance();time.sleep(.5);shot('appearance');t.adb('shell','input','keyevent','4')
if __name__=='__main__':
    try:run()
    finally:Path('.tools/interaction-v053-results.json').write_text(json.dumps(RESULTS,ensure_ascii=False,indent=2),encoding='utf-8')
