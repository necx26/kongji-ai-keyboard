"""Check custom color validation and application through the real editor UI."""
import json
import time
import adb_smoke as t

t.adb('shell','am','start','-f','0x14000000','-n','com.kongji.aikeyboard/.SettingsActivity')
time.sleep(.5)
t.shot('07-新版设置.png')
t.tap('定制键盘外观');t.wait_text('外观工作室')
t.shot('08-外观工作室.png')
t.scroll_tap('自定义主题色  #735EBE');t.tap('自定义颜色 HEX')
t.adb('shell','input','keyevent','123')
t.adb('shell','input','keyevent',*['67']*7)
t.adb('shell','input','text','invalid');t.tap('确定')
assert any(n.attrib.get('content-desc')=='自定义颜色 HEX' for n in t.nodes())
t.adb('shell','input','keyevent','123')
t.adb('shell','input','keyevent',*['67']*7)
t.adb('shell','input','text','FFFFFF');t.tap('确定')
t.wait_text('自定义主题色  #FFFFFF')
t.tap('应用并试打');t.wait_text('键盘练习')
t.tap('点这里输入，或让 AI 帮你回复');t.wait_keyboard()
p=t.adb('shell','run-as','com.kongji.aikeyboard','cat','shared_prefs/keyboard_style.xml')
assert '<int name="accent" value="-1"' in p
t.shot('13-浅色自定义点缀.png')
t.record('无效 HEX 被拒绝；自定义白色保存并在实际键盘生效')
t.adb('shell','am','start','-f','0x14000000','-n','com.kongji.aikeyboard/.SettingsActivity')
time.sleep(.5)
t.tap('定制键盘外观');t.wait_text('外观工作室')
t.scroll_tap('恢复默认外观');t.tap('恢复');t.wait_text('外观工作室');t.tap('应用到键盘')
(t.ROOT/'.tools'/'color-results.json').write_text(json.dumps(t.RESULTS,ensure_ascii=False,indent=2),encoding='utf-8')
