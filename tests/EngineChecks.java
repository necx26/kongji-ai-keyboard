package com.kongji.aikeyboard;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EngineChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS: "+message);}
    static boolean includes(List<ChineseEngine.Candidate> candidates,String value){return candidates.stream().anyMatch(c->c.value.equals(value));}
    public static void main(String[] args)throws Exception{
        Path assets=Path.of("app/src/main/assets");long began=System.nanoTime();Lexicon lexicon=new Lexicon();try(var r=Files.newBufferedReader(assets.resolve("pinyin_simp.dict.yaml"))){lexicon.load(r);}try(var r=Files.newBufferedReader(assets.resolve("chat_phrases.tsv"))){lexicon.load(r);}lexicon.index();ChineseEngine chinese=new ChineseEngine(lexicon);
        check(lexicon.size>65000,"完整词库加载");
        check(includes(chinese.candidates("ek"),"可是"),"错序前缀 ek 能推荐可是");
        check(chinese.candidates("ekshi").stream().anyMatch(c->c.value.equals("可是")&&c.consumed==5&&c.spelling.equals("ke shi")),"错序 ekshi 解码可是并消费实际输入长度");
        check(includes(chinese.candidates("kwshi"),"可是"),"邻键误触 kwshi 可推荐可是");
        check(includes(chinese.candidates("leshi"),"可是")&&chinese.candidates("leshi").get(0).corrections==0,"合法拼音的邻键误触保留纠错候选，正常词优先");
        check(chinese.candidates("keshi").get(0).corrections==0,"正常输入优先于纠错词");
        chinese.correction(false,"QWERTY");check(!includes(chinese.candidates("ek"),"可是")&&!includes(chinese.candidates("kwshi"),"可是"),"关闭纠错不再改配错序或邻键输入");chinese.correction(true,"QWERTY");
        check(includes(chinese.candidates("jintianekshi"),"今天可是"),"连续组词中一个错序音节仍可解码");
        for(String layout:List.of("QWERTY","QWERTZ","AZERTY"))check(new TypingCorrections(layout).variants("ek").contains("ke"),"三种布局均支持错序："+layout);
        check(new TypingCorrections("QWERTZ").variants("ya").contains("xa")&&!new TypingCorrections("QWERTY").variants("ya").contains("xa"),"邻键位置跟随 QWERTZ 与 QWERTY 的差异");
        for(String input:List.of("nihao","jintianxiawuwomenqubeijing","woxianghe咖啡".replace("咖啡","kafei"),"xian","xi'an","nh","zongguo","wojintianxiangquxuexiao")){List<ChineseEngine.Candidate> choices=chinese.candidates(input);System.out.println(input+" => "+choices.stream().limit(7).map(c->c.value+"/"+c.consumed).toList());}
        check(includes(chinese.candidates("jintianxiawuwomenqubeijing"),"今天下午我们去北京"),"连续多个词组成完整候选");
        check(includes(chinese.candidates("xi'an"),"西安")&&!includes(chinese.candidates("xi'an"),"先"),"隔音符限定音节边界");
        check(includes(chinese.candidates("nh"),"你好"),"拼音首字母简拼");
        check(Lexicon.nineCode("ni hao").equals("64426"),"九宫格按标准 ABC/DEF 键位映射拼音");
        check(chinese.candidates("64426").stream().anyMatch(c->c.value.equals("你好")&&c.consumed==5),"九宫格数字编码可以选择你好并正确消费数字");
        check(includes(chinese.candidates(Lexicon.nineCode("jin tian xia wu wo men qu bei jing")),"今天下午我们去北京"),"九宫格支持连续多词拼音候选");
        check(includes(chinese.candidates("zongguo"),"中国"),"模糊 zh/z");chinese.fuzzy(false,false);check(!includes(chinese.candidates("zongguo"),"中国"),"关闭模糊拼音后不再做声母匹配");chinese.fuzzy(true,true);
        check(chinese.candidates("nihaoshijie").stream().anyMatch(c->c.value.equals("你好")&&c.consumed==5),"分词候选只消费对应的拼音前缀");
        long now=System.currentTimeMillis();
        check(LearningWeights.strength(4,now,now)>LearningWeights.strength(10,now-180L*24*60*60*1000,now),"最近习惯可超过半年以前的使用偏好");
        check(LearningWeights.strength(10000,now,now)==LearningWeights.strength(64,now,now),"历史频次权重有上限，避免老词永久霸榜");
        check(LearningWeights.strength(1,now+100000,now)==LearningWeights.strength(1,now,now),"系统时间回拨不产生超额学习权重");
        Lexicon contextLexicon=new Lexicon();contextLexicon.load(new java.io.StringReader("知道\tzhi dao\t1000\n指导\tzhi dao\t1000\n"));contextLexicon.index();ChineseEngine contextual=new ChineseEngine(contextLexicon);
        contextual.personal("zhi dao","知道",3,now);contextual.personal("zhi dao","指导",3,now);
        List<ChineseEngine.Candidate> beforeContext=contextual.candidates("zhidao","老师");
        contextual.rememberNext("我","知道",5,now);contextual.rememberNext("老师","指导",5,now);
        check(contextual.candidates("zhidao","我").get(0).value.equals("知道"),"前词“我”帮助同音词选择“知道”");
        check(contextual.candidates("zhidao","老师").get(0).value.equals("指导"),"前词“老师”帮助同音词选择“指导”，上下文缓存相互独立");
        check(contextual.candidates(Lexicon.nineCode("zhi dao"),"老师").get(0).value.equals("指导"),"九宫格共享前词搭配记忆，老师后优先指导");
        check(contextual.candidates("zhidao","老师")!=beforeContext,"新学习使旧候选缓存失效");
        List<ChineseEngine.Candidate> repeated=contextual.candidates("zhidao","老师");boolean immutable=false;try{repeated.clear();}catch(UnsupportedOperationException expected){immutable=true;}
        check(immutable&&contextual.candidates("zhidao","老师").get(0).value.equals("指导"),"重复查询缓存不会被调用方修改");
        contextual.clearPersonal();check(contextual.candidates("zhidao","我").get(0).value.equals(contextual.candidates("zhidao","老师").get(0).value),"关闭或清空记忆后停用个人上下文排序");
        chinese.personal("ke yi","刻意",20);check(chinese.candidates("keyi").get(0).value.equals("刻意"),"学习权重提升用户常选同音词");chinese.clearPersonal();check(!chinese.candidates("keyi").get(0).value.equals("刻意"),"清除学习恢复系统排序");
        chinese.personal("kong ji da shi","控机大师",20);check(includes(chinese.candidates("kongjidashi"),"控机大师"),"自定义词语参与拼音解码");
        check(includes(chinese.candidates(Lexicon.nineCode("kong ji da shi")),"控机大师"),"26键学习的用户词同样能通过九宫格输入");
        check(includes(chinese.candidates("kongjodashi"),"控机大师"),"用户词条也支持邻键误触纠错");
        check(includes(chinese.candidates("kjds"),"控机大师"),"用户词库同样支持首字母简拼");chinese.clearPersonal();check(!includes(chinese.candidates("kjds"),"控机大师"),"清空用户词同时清空用户简拼索引");
        EnglishEngine english=new EnglishEngine();try(var r=Files.newBufferedReader(assets.resolve("english.tsv"))){english.load(r);}
        check(english.candidates("hel").contains("hello"),"英文补全");check(english.candidates("teh").contains("the"),"英文相邻字母错序纠错");check(english.candidates("receve").contains("receive"),"英文漏字纠错");check(english.candidates("Hel").contains("Hello"),"英文候选跟随大小写");
        english.personal("zorbix",20);check(english.candidates("zor").contains("zorbix"),"英文用户词记忆");english.clearPersonal();check(!english.candidates("zor").contains("zorbix"),"英文学习记录清除");
        for(boolean cached:List.of(false,true)){long[] times=new long[120];for(int i=0;i<times.length;i++){if(!cached)chinese.fuzzy(true,true);long start=System.nanoTime();chinese.candidates(i%2==0?"jintianxiawuwomenqubeijing":"zhonghuarenmingongheguo");times[i]=(System.nanoTime()-start)/1000;}java.util.Arrays.sort(times);System.out.println("HOST "+(cached?"cached":"uncached")+" query p50/p95 microseconds: "+times[60]+" / "+times[114]);}System.out.println("Total load + checks ms: "+(System.nanoTime()-began)/1_000_000);
    }
}
