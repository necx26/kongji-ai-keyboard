"""Real IME checks for swapped letters, nearby keys, partial selection and the opt-out."""
import json
import adb_smoke as t
import typing_helpers as h

try:
    for raw in ('ek','ekshi','kwshi'):
        h.practice();h.chinese();h.keys(raw)
        assert h.text()==raw, 'Input should remain unchanged until selection'
        h.candidate('可是');assert h.text()=='可是'
        t.shot('纠错-'+raw+'.png');t.record(raw+' 候选选择可是，确认前保留原文')
    h.practice();h.chinese();h.keys('ekshishijie');h.candidate('可是');assert h.text()=='可是shijie';h.candidate('世界');assert h.text()=='可是世界'
    t.record('错序输入先选可是，剩余拼音保留并继续选词')
    h.input_settings();t.scroll_tap('中文打字纠错：字母错序与邻键误触')
    h.practice();h.chinese();h.keys('ek')
    assert not any(n.attrib.get('text')=='可是' for n in t.nodes())
    if any(n.attrib.get('content-desc')=='展开候选' and n.attrib.get('text')=='⌄' for n in t.nodes()):
        t.tap('展开候选');assert not any(n.attrib.get('text')=='可是' for n in t.nodes())
    t.record('关闭中文纠错后 ek 不再匹配可是')
    h.input_settings();t.scroll_tap('中文打字纠错：字母错序与邻键误触')
finally:
    (t.ROOT/'.tools'/'correction-ui-results.json').write_text(json.dumps(t.RESULTS,ensure_ascii=False,indent=2),encoding='utf-8')
