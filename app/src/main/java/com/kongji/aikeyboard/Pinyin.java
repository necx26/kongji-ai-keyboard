package com.kongji.aikeyboard;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class Pinyin implements AutoCloseable {
    static final class Candidate {final String value,spelling;final int consumed;Candidate(String value,int consumed,String spelling){this.value=value;this.consumed=consumed;this.spelling=spelling;}}
    private final Context context;private final LearningStore store;private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());private volatile boolean ready,closed,failed;private int epoch=-1;
    private ChineseEngine chinese;private EnglishEngine english;private InputPreferences options;
    private final Map<String,LearningStore.Word> words=new LinkedHashMap<>();private List<LearningStore.Next> learnedNext=List.of();
    private final Map<String,List<LearningStore.Next>> defaults=new HashMap<>();private boolean privateField;
    Pinyin(Context c,Runnable onReady){context=c.getApplicationContext();store=new LearningStore(context);options=InputPreferences.load(context);
        io.execute(()->{try{Lexicon lexicon=new Lexicon();try(var r=reader("pinyin_simp.dict.yaml")){lexicon.load(r);}try(var r=reader("chat_phrases.tsv")){lexicon.load(r);}lexicon.index();
            ChineseEngine loadedChinese=new ChineseEngine(lexicon);EnglishEngine loadedEnglish=new EnglishEngine();try(var r=reader("english.tsv")){loadedEnglish.load(r);}
            Map<String,List<LearningStore.Next>> seeds=new HashMap<>();try(var r=reader("next_words.tsv")){String line;while((line=r.readLine())!=null){String[] p=line.split("\t");if(p.length==5&&!line.startsWith("#"))seeds.computeIfAbsent(p[0]+"\t"+p[1],k->new ArrayList<>()).add(new LearningStore.Next(p[0],p[1],p[2],p[3],Integer.parseInt(p[4])));}}
            final List<LearningStore.Word> saved;final List<LearningStore.Next> pairs;final int savedEpoch;
            synchronized(LearningStore.LOCK){savedEpoch=InputPreferences.epoch(context);saved=store.words();pairs=store.next();}
            main.post(()->{if(closed)return;chinese=loadedChinese;english=loadedEnglish;defaults.putAll(seeds);replaceWords(saved);learnedNext=pairs;epoch=savedEpoch;ready=true;configure(privateField);onReady.run();});
        }catch(Exception failure){failed=true;main.post(()->{if(!closed)onReady.run();});}});
    }
    private BufferedReader reader(String asset)throws java.io.IOException{return new BufferedReader(new InputStreamReader(context.getAssets().open(asset),StandardCharsets.UTF_8));}
    boolean ready(){return ready;}
    String loadingMessage(){return Language.text(context,failed?"词库加载失败，请重新打开输入法":"");}
    private void replaceWords(List<LearningStore.Word> saved){words.clear();for(int i=saved.size()-1;i>=0;i--){LearningStore.Word w=saved.get(i);words.put(wordKey(w.language,w.spelling,w.value),w);}}
    void configure(boolean privateField){this.privateField=privateField;options=InputPreferences.load(context);if(ready){int current=InputPreferences.epoch(context);if(epoch!=current){replaceWords(store.words());learnedNext=store.next();epoch=current;}applyOptions();}}
    private void applyOptions(){chinese.fuzzy(options.initials,options.finals);chinese.correction(options.typos,new String[]{"QWERTY","QWERTZ","AZERTY"}[KeyboardStyle.load(context).layout]);chinese.clearPersonal();english.clearPersonal();if(options.memory&&!privateField){for(LearningStore.Word w:words.values()){if(w.language.equals("zh"))chinese.personal(w.spelling,w.value,w.hits,w.updated);else english.personal(w.value,w.hits);}for(LearningStore.Next pair:learnedNext)if(pair.language.equals("zh"))chinese.rememberNext(pair.previous,pair.value,pair.hits,pair.updated);}}
    List<Candidate> candidates(String input){return candidates(input,"");}
    Candidate handwriting(String value){return new Candidate(value,0,ready?chinese.spelling(value):"");}
    List<Candidate> candidates(String input,String previous){if(!ready)return List.of();List<Candidate> result=new ArrayList<>();for(ChineseEngine.Candidate c:chinese.candidates(input,previous))result.add(new Candidate(c.value,c.consumed,c.spelling));return result;}
    List<Candidate> english(String input){if(!ready||!options.english)return List.of();List<Candidate> result=new ArrayList<>();for(String word:english.candidates(input))result.add(new Candidate(word,input.length(),word.toLowerCase(Locale.ROOT)));return result;}
    List<Candidate> predictions(String language,String previous){if(!ready)return List.of();if(language.equals("en")&&!options.english)return List.of();Map<String,Candidate> result=new LinkedHashMap<>();
        if(options.memory&&!privateField){long now=System.currentTimeMillis();List<LearningStore.Next> ranked=new ArrayList<>();for(LearningStore.Next item:learnedNext)if(item.language.equals(language)&&item.previous.equals(previous))ranked.add(item);ranked.sort(Comparator.comparingDouble((LearningStore.Next item)->LearningWeights.strength(item.hits,item.updated,now)).reversed().thenComparing(Comparator.comparingLong((LearningStore.Next item)->item.updated).reversed()));for(LearningStore.Next item:ranked)result.put(item.value,new Candidate(item.value,0,item.spelling));}
        for(LearningStore.Next item:defaults.getOrDefault(language+"\t"+previous,List.of()))result.putIfAbsent(item.value,new Candidate(item.value,0,item.spelling));
        if(language.equals("zh")&&!previous.isEmpty())for(ChineseEngine.Candidate c:chinese.continuations(previous))result.putIfAbsent(c.value,new Candidate(c.value,0,c.spelling));
        List<Candidate> list=new ArrayList<>(result.values());return list.subList(0,Math.min(24,list.size()));
    }
    String context(String language,String before,String previous){if(!previous.isEmpty()&&before.endsWith(previous+(language.equals("en")?" ":"")))return previous;
        if(language.equals("en")){java.util.regex.Matcher m=java.util.regex.Pattern.compile("([A-Za-z']+)\\s*$").matcher(before);return m.find()?m.group(1).toLowerCase(Locale.ROOT):"";}
        for(int n=Math.min(6,before.length());n>=1;n--){String end=before.substring(before.length()-n);if(defaults.containsKey("zh\t"+end))return end;for(LearningStore.Next pair:learnedNext)if(options.memory&&!privateField&&pair.language.equals("zh")&&pair.previous.equals(end))return end;}
        return "";
    }
    void learn(String language,String spelling,String value,String previous,boolean allowed){if(closed||!ready||!allowed||privateField||!options.memory)return;
        if(language.equals("en")){value=value.toLowerCase(Locale.ROOT);spelling=value;if(!value.matches("[a-z]+(?:'[a-z]+)?")||value.length()>32)return;}
        else if(!value.matches("\\p{IsHan}{1,32}")||!spelling.matches("[a-z]+(?: [a-z]+)*"))return;
        String key=wordKey(language,spelling,value);LearningStore.Word old=words.get(key);int hits=old==null?1:Math.min(old.hits+1,10000);LearningStore.Word next=new LearningStore.Word(language,spelling,value,hits);words.remove(key);words.put(key,next);
        if(language.equals("zh"))chinese.personal(spelling,value,hits,next.updated);else english.personal(value,hits);
        if(!previous.isEmpty()&&previous.length()<=32){List<LearningStore.Next> pairs=new ArrayList<>(learnedNext);int count=1;for(LearningStore.Next pair:pairs)if(pair.language.equals(language)&&pair.previous.equals(previous)&&pair.value.equals(value))count=Math.min(pair.hits+1,10000);pairs.removeIf(pair->pair.language.equals(language)&&pair.previous.equals(previous)&&pair.value.equals(next.value));LearningStore.Next pair=new LearningStore.Next(language,previous,value,spelling,count);pairs.add(0,pair);learnedNext=pairs;if(language.equals("zh"))chinese.rememberNext(previous,value,count,pair.updated);}
        final String code=spelling,text=value;int capturedEpoch=epoch;io.execute(()->store.learn(language,code,text,previous,capturedEpoch));
    }
    private static String wordKey(String language,String spelling,String value){return language+"\t"+spelling+"\t"+value;}
    @Override public void close(){if(closed)return;closed=true;io.execute(store::close);io.shutdown();main.removeCallbacksAndMessages(null);}
}
