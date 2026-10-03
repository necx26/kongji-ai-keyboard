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

final class EnglishEngine {
    final TreeMap<String,Integer> words=new TreeMap<>();final Map<String,Integer> personal=new HashMap<>();
    void load(Reader input)throws IOException{BufferedReader r=new BufferedReader(input);String line;while((line=r.readLine())!=null){String[] p=line.split("\t");if(p.length==2&&!line.startsWith("#"))words.put(p[0],Integer.parseInt(p[1]));}}
    void clearPersonal(){personal.clear();}void personal(String word,int count){personal.put(word.toLowerCase(Locale.ROOT),count);}
    private double score(String w){return Math.log(words.getOrDefault(w,3000)+1)+4*Math.log1p(personal.getOrDefault(w,0));}
    List<String> candidates(String typed){String query=typed.toLowerCase(Locale.ROOT);Map<String,Double> found=new HashMap<>();
        for(Map.Entry<String,Integer> e:words.tailMap(query).entrySet()){if(!e.getKey().startsWith(query))break;found.put(e.getKey(),score(e.getKey()));}
        for(String w:personal.keySet())if(w.startsWith(query))found.put(w,score(w));
        if(query.length()>=3){for(String w:words.keySet())if(!found.containsKey(w)&&oneEdit(query,w))found.put(w,score(w)-5);for(String w:personal.keySet())if(oneEdit(query,w))found.put(w,score(w)-5);}
        found.remove(query);List<String> result=new ArrayList<>(found.keySet());result.sort(Comparator.comparingDouble((String w)->found.get(w)).reversed().thenComparing(w->w));
        if(result.size()>24)result.subList(24,result.size()).clear();List<String> display=new ArrayList<>();for(String w:result)display.add(caseLike(w,typed));return display;
    }
    static String caseLike(String word,String typed){if(typed.length()>1&&typed.equals(typed.toUpperCase(Locale.ROOT)))return word.toUpperCase(Locale.ROOT);if(!typed.isEmpty()&&Character.isUpperCase(typed.charAt(0)))return Character.toUpperCase(word.charAt(0))+word.substring(1);return word.equals("i")?"I":word;}
    // One insertion, deletion, substitution, or adjacent transposition; no silent replacement on space.
    static boolean oneEdit(String a,String b){if(Math.abs(a.length()-b.length())>1||a.equals(b))return false;int i=0,j=0,edits=0;
        while(i<a.length()&&j<b.length()){if(a.charAt(i)==b.charAt(j)){i++;j++;continue;}if(++edits>1)return false;
            if(a.length()==b.length()){if(i+1<a.length()&&a.charAt(i)==b.charAt(j+1)&&a.charAt(i+1)==b.charAt(j)){i+=2;j+=2;}else{i++;j++;}}
            else if(a.length()>b.length())i++;else j++;
        }return edits+(a.length()-i)+(b.length()-j)<=1;
    }
}
