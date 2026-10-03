package com.kongji.aikeyboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Lattice decoding: each edge is a word, each bounded beam is a sentence hypothesis. */
final class ChineseEngine {
    static final class Candidate {
        final String value,spelling;final int consumed,corrections;final double score;final boolean approximate;
        Candidate(String value,String spelling,int consumed,double score,boolean approximate){this(value,spelling,consumed,score,approximate,0);}
        Candidate(String value,String spelling,int consumed,double score,boolean approximate,int corrections){this.value=value;this.spelling=spelling;this.consumed=consumed;this.score=score;this.approximate=approximate;this.corrections=corrections;}
    }
    private static final int BEAM=8;
    private final Lexicon lexicon;
    private final Map<String,Double> memoryScores=new HashMap<>();
    private final TreeMap<String,List<Lexicon.Entry>> personal=new TreeMap<>();
    private final TreeMap<String,List<Lexicon.Entry>> personalNine=new TreeMap<>();
    private final Map<String,List<Lexicon.Entry>> personalShortcuts=new HashMap<>(),personalFuzzy=new HashMap<>();
    private final Map<String,Map<String,Double>> contextScores=new HashMap<>();
    // Cursor callbacks and backspace often repeat the same query. Never retain more than 24.
    private final Map<String,List<Candidate>> queryCache=new LinkedHashMap<>(32,.75f,true);
    private boolean useInitials=true,useFinals=true;
    private boolean correctTypos=true;
    private TypingCorrections correctionKeys=new TypingCorrections("QWERTY");
    private static final Comparator<Candidate> RANK=Comparator.comparingInt((Candidate c)->c.corrections).thenComparing(Comparator.comparingDouble((Candidate c)->c.score).reversed());
    ChineseEngine(Lexicon lexicon){this.lexicon=lexicon;}
    void fuzzy(boolean initials,boolean finals){useInitials=initials;useFinals=finals;queryCache.clear();}
    void correction(boolean enabled,String layout){correctTypos=enabled;correctionKeys=new TypingCorrections(layout);queryCache.clear();}
    void clearPersonal(){memoryScores.clear();personal.clear();personalNine.clear();personalShortcuts.clear();personalFuzzy.clear();contextScores.clear();queryCache.clear();}
    void personal(String spelling,String value,int count){personal(spelling,value,count,System.currentTimeMillis());}
    void personal(String spelling,String value,int count,long updated){Lexicon.Entry e=new Lexicon.Entry(value,spelling,3000);memoryScores.put(e.spelling+"\t"+value,4*LearningWeights.strength(count,updated,System.currentTimeMillis()));queryCache.clear();
        List<Lexicon.Entry> list=personal.computeIfAbsent(e.code,k->new ArrayList<>());list.removeIf(x->x.value.equals(value)&&x.spelling.equals(e.spelling));list.add(e);
        personalIndex(personalNine,Lexicon.nineCode(e.code),e);
        String[] syllables=e.spelling.split(" +");if(syllables.length>1){StringBuilder shortCode=new StringBuilder();for(String syllable:syllables)shortCode.append(syllable.charAt(0));personalIndex(personalShortcuts,shortCode.toString(),e);}
        for(int flags=1;flags<=3;flags++)personalIndex(personalFuzzy,flags+":"+Lexicon.fuzzyCode(e.code,(flags&1)!=0,(flags&2)!=0),e);
    }
    private static void personalIndex(Map<String,List<Lexicon.Entry>> index,String key,Lexicon.Entry e){List<Lexicon.Entry> entries=index.computeIfAbsent(key,k->new ArrayList<>());entries.removeIf(x->x.value.equals(e.value)&&x.spelling.equals(e.spelling));entries.add(e);}
    void rememberNext(String previous,String value,int hits,long updated){
        if(previous.isEmpty()||value.isEmpty())return;
        contextScores.computeIfAbsent(previous,k->new HashMap<>()).put(value,Math.min(8,2*LearningWeights.strength(hits,updated,System.currentTimeMillis())));
        queryCache.clear();
    }
    private double score(Lexicon.Entry e){return Math.log(e.frequency+1)-16+memoryScores.getOrDefault(e.spelling+"\t"+e.value,0.0);}
    private double contextBonus(String previous,String value){double bonus=0;for(var pair:contextScores.getOrDefault(previous,Map.of()).entrySet())if(value.startsWith(pair.getKey()))bonus=Math.max(bonus,pair.getValue());return bonus;}
    String spelling(String value){Lexicon.Entry entry=lexicon.byValue.get(value);if(entry!=null)return entry.spelling;for(var list:personal.values())for(Lexicon.Entry e:list)if(e.value.equals(value))return e.spelling;return "";}
    private List<Lexicon.Entry> personalComplete(String code,boolean nine){List<Lexicon.Entry> found=new ArrayList<>();for(var row:(nine?personalNine:personal).tailMap(code).entrySet()){if(!row.getKey().startsWith(code))break;found.addAll(row.getValue());}return found;}
    private static boolean boundary(String raw,Lexicon.Entry entry){if(raw.indexOf('\'')<0)return true;int offset=0;List<Integer> ends=new ArrayList<>();for(String s:entry.spelling.split(" +")){offset+=s.length();ends.add(offset);}int read=0;for(int i=0;i<raw.length();i++){if(raw.charAt(i)=='\''){if(!ends.contains(read))return false;}else read++;}return true;}
    private List<Candidate> edges(String input,int start,boolean nine){
        List<Candidate> result=new ArrayList<>();int flags=(useInitials?1:0)|(useFinals?2:0);
        for(int end=start+1;end<=Math.min(input.length(),start+48);end++){
            if(input.charAt(end-1)=='\'')continue;String raw=input.substring(start,end),code=Lexicon.normalize(raw);if(code.isEmpty())continue;
            Map<String,Candidate> word=new LinkedHashMap<>();
            List<Lexicon.Entry> exact=(nine?lexicon.nine:lexicon.exact).getOrDefault(code,List.of());
            for(Lexicon.Entry e:exact)if(boundary(raw,e))put(word,new Candidate(e.value,e.spelling,end,score(e),false));
            for(Lexicon.Entry e:(nine?personalNine:personal).getOrDefault(code,List.of()))if(boundary(raw,e))put(word,new Candidate(e.value,e.spelling,end,score(e),false));
            if(correctTypos&&input.indexOf('\'')<0)for(String variant:correctionKeys.variants(code)){
                for(Lexicon.Entry e:lexicon.exact.getOrDefault(variant,List.of()))put(word,new Candidate(e.value,e.spelling,end,score(e)-8,true,1));
                for(Lexicon.Entry e:personal.getOrDefault(variant,List.of()))put(word,new Candidate(e.value,e.spelling,end,score(e)-8,true,1));
            }
            if(!nine&&flags!=0&&raw.indexOf('\'')<0){String fuzzy=Lexicon.fuzzyCode(code,useInitials,useFinals);
                for(Lexicon.Entry e:lexicon.fuzzy.getOrDefault(flags+":"+fuzzy,List.of()))if(!e.code.equals(code))put(word,new Candidate(e.value,e.spelling,end,score(e)-4,true));
                for(Lexicon.Entry e:personalFuzzy.getOrDefault(flags+":"+fuzzy,List.of()))if(!e.code.equals(code))put(word,new Candidate(e.value,e.spelling,end,score(e)-4,true));
            }
            List<Candidate> choices=new ArrayList<>(word.values());limitGroups(choices,10,6);result.addAll(choices);
        }return result;
    }
    private static void put(Map<String,Candidate> map,Candidate c){String key=c.value+"\t"+c.consumed;Candidate old=map.get(key);if(old==null||RANK.compare(c,old)<0)map.put(key,c);}
    List<Candidate> candidates(String input){return candidates(input,"");}
    List<Candidate> candidates(String input,String previous){
        String key=previous+"\t"+input;List<Candidate> cached=queryCache.get(key);if(cached!=null)return cached;
        List<Candidate> result=List.copyOf(decode(input,previous));
        queryCache.put(key,result);if(queryCache.size()>24)queryCache.remove(queryCache.keySet().iterator().next());return result;
    }
    private List<Candidate> decode(String input,String previous){
        if(input.isEmpty())return List.of();int n=input.length();boolean nine=input.matches("[2-9']+");
        List<List<Candidate>> beams=new ArrayList<>();for(int i=0;i<=n;i++)beams.add(new ArrayList<>());beams.get(0).add(new Candidate("","",0,0,false));
        List<Candidate> heads=edges(input,0,nine);
        for(int i=0;i<n;i++){List<Candidate> states=beams.get(i);if(states.isEmpty())continue;limitGroups(states,BEAM,BEAM);
            if(input.charAt(i)=='\''){for(Candidate state:states)addState(beams.get(i+1),new Candidate(state.value,state.spelling,i+1,state.score,state.approximate,state.corrections));continue;}
            List<Candidate> edges=i==0?heads:edges(input,i,nine);
            for(Candidate state:states)for(Candidate edge:edges){if(state.corrections+edge.corrections>1)continue;String spelling=state.spelling.isEmpty()?edge.spelling:state.spelling+" "+edge.spelling;
                addState(beams.get(edge.consumed),new Candidate(state.value+edge.value,spelling,edge.consumed,state.score+edge.score,state.approximate||edge.approximate,state.corrections+edge.corrections));
            }
        }
        Map<String,Candidate> complete=new LinkedHashMap<>();for(Candidate c:beams.get(n))if(!c.value.isEmpty())put(complete,c);
        String code=Lexicon.normalize(input);
        // Abbreviations have their own index and never consume a longer, untyped pinyin suffix.
        if(input.indexOf('\'')<0&&code.length()>=2){for(Lexicon.Entry e:lexicon.shortcuts.getOrDefault(code,List.of()))put(complete,new Candidate(e.value,e.spelling,n,score(e)-2,true));for(Lexicon.Entry e:personalShortcuts.getOrDefault(code,List.of()))put(complete,new Candidate(e.value,e.spelling,n,score(e)-2,true));}
        for(Lexicon.Entry e:(nine?lexicon.completeNine(code):lexicon.complete(code)))if(e.code.length()>code.length()&&boundary(input,e))put(complete,new Candidate(e.value,e.spelling,n,score(e)-5,true));
        for(Lexicon.Entry e:personalComplete(code,nine))if(e.code.length()>code.length()&&boundary(input,e))put(complete,new Candidate(e.value,e.spelling,n,score(e)-5,true));
        if(correctTypos&&input.indexOf('\'')<0)for(String variant:correctionKeys.variants(code)){
            for(Lexicon.Entry e:lexicon.complete(variant))if(e.code.length()>variant.length())put(complete,new Candidate(e.value,e.spelling,n,score(e)-13,true,1));
            for(Lexicon.Entry e:personalComplete(variant,false))if(e.code.length()>variant.length())put(complete,new Candidate(e.value,e.spelling,n,score(e)-13,true,1));
        }
        if(!previous.isEmpty())complete.replaceAll((key,c)->new Candidate(c.value,c.spelling,c.consumed,c.score+contextBonus(previous,c.value),c.approximate,c.corrections));
        List<Candidate> result=new ArrayList<>(complete.values());limitGroups(result,36,12);
        if(!previous.isEmpty())heads.replaceAll(c->new Candidate(c.value,c.spelling,c.consumed,c.score+contextBonus(previous,c.value),c.approximate,c.corrections));
        heads.sort(Comparator.comparingInt((Candidate c)->c.consumed).reversed().thenComparing(RANK));
        Map<String,Candidate> dedup=new LinkedHashMap<>();for(Candidate c:result)put(dedup,c);for(Candidate c:heads)if(c.consumed<n)put(dedup,c);
        List<Candidate> all=new ArrayList<>(dedup.values());return all.subList(0,Math.min(60,all.size()));
    }
    private static void addState(List<Candidate> states,Candidate next){for(int i=0;i<states.size();i++){Candidate old=states.get(i);if(old.value.equals(next.value)){if(RANK.compare(next,old)<0)states.set(i,next);return;}}
        states.add(next);if(states.size()>BEAM*4)limitGroups(states,BEAM,BEAM);
    }
    private static void limitGroups(List<Candidate> candidates,int normalLimit,int correctionLimit){candidates.sort(RANK);int normal=0,corrected=0;for(var it=candidates.iterator();it.hasNext();){Candidate c=it.next();if(c.corrections==0?++normal>normalLimit:++corrected>correctionLimit)it.remove();}}
    List<Candidate> continuations(String previous){List<Candidate> result=new ArrayList<>();for(Lexicon.Entry e:lexicon.following.getOrDefault(previous,List.of())){
        if(e.value.length()>previous.length()&&e.value.length()-previous.length()<=6){String[] tokens=e.spelling.split(" +");if(tokens.length!=e.value.length())continue;String spelling=String.join(" ",java.util.Arrays.copyOfRange(tokens,previous.length(),tokens.length));result.add(new Candidate(e.value.substring(previous.length()),spelling,0,score(e),false));}
    }return result;}
}
