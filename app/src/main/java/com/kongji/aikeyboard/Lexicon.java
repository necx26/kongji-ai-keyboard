package com.kongji.aikeyboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Immutable system lexicon after loading; indexes keep queries away from full dictionary scans. */
final class Lexicon {
    static final class Entry {
        final String value,spelling,code;final int frequency;
        Entry(String value,String spelling,int frequency){this.value=value;this.spelling=spelling.trim().replace('ü','v').replace("u:","v");this.code=normalize(this.spelling);this.frequency=frequency;}
    }
    final TreeMap<String,List<Entry>> exact=new TreeMap<>();
    final TreeMap<String,List<Entry>> nine=new TreeMap<>();
    final Map<String,List<Entry>> ninePrefixes=new HashMap<>();
    final Map<String,Entry> byValue=new HashMap<>();
    final Map<String,List<Entry>> fuzzy=new HashMap<>(),shortcuts=new HashMap<>(),prefixes=new HashMap<>(),following=new HashMap<>();
    int size;
    static final Comparator<Entry> FREQUENCY=Comparator.comparingInt((Entry e)->e.frequency).reversed().thenComparing(e->e.value);
    static String normalize(String input){return input.toLowerCase(Locale.ROOT).replace("ü","v").replace("u:","v").replace(" ","").replace("'","").replaceAll("([jqxy])v","$1u");}
    static String fuzzyCode(String input,boolean initials,boolean finals){String s=input;if(initials)s=s.replace("zh","z").replace("ch","c").replace("sh","s");if(finals)s=s.replace("ng","n");return s;}
    static String nineCode(String spelling){StringBuilder code=new StringBuilder();for(char letter:normalize(spelling).toCharArray()){int digit=letter<='c'?2:letter<='f'?3:letter<='i'?4:letter<='l'?5:letter<='o'?6:letter<='s'?7:letter<='v'?8:9;code.append(digit);}return code.toString();}
    void load(Reader input)throws IOException{
        BufferedReader reader=new BufferedReader(input);String line;
        while((line=reader.readLine())!=null){if(line.startsWith("#"))continue;String[] fields=line.split("\t");if(fields.length<3)continue;
            try{Entry e=new Entry(fields[0],fields[1],Math.max(1,Integer.parseInt(fields[2])));if(!e.code.matches("[a-z]+")||e.code.length()>64)continue;
                exact.computeIfAbsent(e.code,k->new ArrayList<>()).add(e);size++;
            }catch(NumberFormatException ignored){/* YAML metadata is not a lexicon entry. */}
        }
    }
    void index(){
        for(List<Entry> entries:exact.values()){entries.sort(FREQUENCY);for(Entry e:entries){
            String digits=nineCode(e.code);nine.computeIfAbsent(digits,k->new ArrayList<>()).add(e);for(int n=1;n<=Math.min(3,digits.length()-1);n++)top(ninePrefixes,digits.substring(0,n),e,40);
            Entry old=byValue.get(e.value);if(old==null||e.frequency>old.frequency)byValue.put(e.value,e);
            for(int flags=1;flags<=3;flags++){String key=flags+":"+fuzzyCode(e.code,(flags&1)!=0,(flags&2)!=0);top(fuzzy,key,e,14);}
            String[] syllables=e.spelling.split(" +");if(syllables.length>=2&&syllables.length<=8){StringBuilder code=new StringBuilder();for(String syllable:syllables)code.append(syllable.charAt(0));top(shortcuts,code.toString(),e,30);}
            for(int n=1;n<=Math.min(3,e.code.length()-1);n++)top(prefixes,e.code.substring(0,n),e,40);
            // Chinese continuation text is displayed as a suffix, never as the already committed prefix.
            for(int n=1;n<e.value.length()&&n<=6;n++)top(following,e.value.substring(0,n),e,16);
        }}
        for(List<Entry> entries:nine.values())entries.sort(FREQUENCY);
    }
    static void top(Map<String,List<Entry>> map,String key,Entry entry,int limit){List<Entry> list=map.computeIfAbsent(key,k->new ArrayList<>());list.add(entry);list.sort(FREQUENCY);if(list.size()>limit)list.remove(list.size()-1);}
    List<Entry> complete(String code){
        if(code.length()<=3)return prefixes.getOrDefault(code,List.of());
        List<Entry> found=new ArrayList<>();int keys=0;
        for(Map.Entry<String,List<Entry>> row:exact.tailMap(code).entrySet()){if(!row.getKey().startsWith(code)||++keys>500)break;found.addAll(row.getValue());}
        found.sort(FREQUENCY);return found.subList(0,Math.min(40,found.size()));
    }
    List<Entry> completeNine(String code){if(code.length()<=3)return ninePrefixes.getOrDefault(code,List.of());List<Entry> found=new ArrayList<>();int keys=0;for(var row:nine.tailMap(code).entrySet()){if(!row.getKey().startsWith(code)||++keys>500)break;found.addAll(row.getValue());}found.sort(FREQUENCY);return found.subList(0,Math.min(40,found.size()));}
}
