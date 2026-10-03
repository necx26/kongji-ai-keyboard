package com.kongji.aikeyboard;

import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** One physical near-key replacement or adjacent transposition, never a silent edit. */
final class TypingCorrections {
    private final Map<Character,String> neighbors=new HashMap<>();
    TypingCorrections(String layout){
        String[] rows=switch(layout){case "QWERTZ" -> new String[]{"qwertzuiop","asdfghjkl","yxcvbnm"};case "AZERTY" -> new String[]{"azertyuiop","qsdfghjklm","wxcvbn"};default -> new String[]{"qwertyuiop","asdfghjkl","zxcvbnm"};};
        double[] offsets={0,.25,.75};
        for(int r=0;r<rows.length;r++)for(int c=0;c<rows[r].length();c++){
            StringBuilder near=new StringBuilder();
            for(int y=0;y<rows.length;y++)for(int x=0;x<rows[y].length();x++)if((r!=y||c!=x)&&Math.hypot(c+offsets[r]-x-offsets[y],r-y)<=1.3)near.append(rows[y].charAt(x));
            neighbors.put(rows[r].charAt(c),near.toString());
        }
    }
    Set<String> variants(String code){
        Set<String> result=new LinkedHashSet<>();if(code.length()<2||code.length()>12||!code.matches("[a-z]+"))return result;
        for(int i=0;i+1<code.length();i++)if(code.charAt(i)!=code.charAt(i+1)){char[] chars=code.toCharArray();char old=chars[i];chars[i]=chars[i+1];chars[i+1]=old;result.add(new String(chars));}
        for(int i=0;i<code.length();i++)for(char near:neighbors.getOrDefault(code.charAt(i),"").toCharArray()){char[] chars=code.toCharArray();chars[i]=near;result.add(new String(chars));}
        result.remove(code);return result;
    }
}
