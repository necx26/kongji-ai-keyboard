"""Check manually added Chinese/English words via their real settings form, then the live IME."""
import base64
import json
import adb_smoke as t
import typing_helpers as h

def field(description,value):
    encoded=base64.b64encode(value.encode('utf-8')).decode('ascii')
    output=t.adb('shell','am','instrument','-w','-r','-e','field',description,'-e','value64',encoded,'com.kongji.windowprobe/com.kongji.aikeyboard.WindowDumpInstrumentation')
    assert 'INSTRUMENTATION_RESULT: set_text=true' in output

try:
    h.input_settings();t.scroll_tap('添加自定义词语');t.wait_text('添加自定义词语')
    field('自定义词语输入框','控机大师');field('自定义拼音输入框','kong ji da shi');t.tap('添加');t.wait_text('本机用户词：1')
    h.practice();h.chinese();h.keys('kjds');h.candidate('控机大师');assert h.text()=='控机大师';t.shot('07-自定义中文词简拼.png');t.record('手动词条表单添加控机大师，实际键盘用 kjds 简拼选出并上屏')
    h.input_settings();t.scroll_tap('添加自定义词语');t.wait_text('添加自定义词语')
    field('自定义词语输入框','zorbixx');t.tap('添加');assert any(r[2]=='zorbixx' for r in h.snapshot())
    h.practice();h.english();h.keys('zor');h.candidate('zorbixx');assert h.text()=='zorbixx ';t.record('手动英文用户词添加与真实补全上屏')
    h.input_settings();t.scroll_tap('清空用户词库和学习记录');t.tap('清空');t.wait_text('本机用户词：0');assert h.snapshot()==[]
finally:
    (t.ROOT/'.tools'/'custom-word-results.json').write_text(json.dumps(t.RESULTS,ensure_ascii=False,indent=2),encoding='utf-8')
