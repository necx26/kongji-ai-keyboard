package com.kongji.aikeyboard;

import android.content.Context;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;

/** Shared layout for the live input method and the appearance preview. */
final class KeyboardLayout {
    interface Actions {void key(String value);void shift();void delete();void clear();void symbols();void language();void switchIme(boolean picker);void space();void punctuation();void enter();}
    static void build(LinearLayout keys,KeyboardStyle s,boolean chinese,boolean shift,boolean symbols,Actions action){
        Context c=keys.getContext();keys.removeAllViews();keys.setClipChildren(false);keys.setClipToPadding(false);
        if(s.quickSymbols)addQuickSymbols(keys,s,chinese,action::key);
        if(s.mode==1&&chinese&&!symbols){buildNine(keys,s,action);return;}
        if(s.numberRow&&!symbols)addNumbers(keys,s,action);
        String[] rows=symbols?new String[]{"1234567890","@#￥%&*()-",chinese?"，。？！：；/'":",.?!:;/'"}:switch(s.layout){
            case 1->new String[]{"qwertzuiop","asdfghjkl","yxcvbnm"};
            case 2->new String[]{"azertyuiop","qsdfghjklm","wxcvbn"};
            default->new String[]{"qwertyuiop","asdfghjkl","zxcvbnm"};};
        for(int r=0;r<rows.length;r++){
            LinearLayout row=Ui.row(c);row.setGravity(Gravity.CENTER);row.setClipChildren(false);row.setBaselineAligned(false);
            if(r==1&&!symbols)row.setPadding(Ui.dp(c,16),0,Ui.dp(c,16),0);
            if(r==2&&!symbols){Button shiftKey=Glass.button(c,s,"⇧",shift,true,v->action.shift());shiftKey.setTextSize(22);shiftKey.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));shiftKey.setContentDescription(Language.text(c,"切换大小写"));add(row,s,shiftKey,1.3f,s.height);}
            for(char ch:rows[r].toCharArray()){
                String label=String.valueOf(ch);if(!symbols&&shift)label=label.toUpperCase(java.util.Locale.ROOT);final String value=label;
                Button b=Glass.button(c,s,label,false,false,v->action.key(value));b.setTextSize(s.font);b.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));
                b.setAutoSizeTextTypeUniformWithConfiguration(12,s.font,1,android.util.TypedValue.COMPLEX_UNIT_SP);
                if(!symbols){String shortcut=shortcut(ch);Glass.shortcut(b,shortcut,()->action.key(shortcut));b.setContentDescription(value+", "+Language.text(c,s.swipeDirection==0?"上滑输入符号":"下滑输入符号")+" "+shortcut);}
                add(row,s,b,1,s.height);
            }
            if(r==2){Button b=Glass.button(c,s,"⌫",false,true,v->action.delete());b.setTextSize(22);b.setContentDescription(Language.text(c,"删除"));b.setOnLongClickListener(v->{action.clear();return true;});add(row,s,b,1.35f,s.height);}
            keys.addView(row);
        }
        LinearLayout bottom=Ui.row(c);bottom.setClipChildren(false);bottom.setBaselineAligned(false);
        add(bottom,s,Glass.button(c,s,symbols?"ABC":"123",false,true,v->action.symbols()),1.05f,s.height);
        add(bottom,s,Glass.button(c,s,chinese?"中":"EN",false,true,v->action.language()),.85f,s.height);
        Button switcher=Glass.button(c,s,Language.text(c,"切换"),false,true,v->action.switchIme(false));switcher.setTextSize(11);switcher.setOnLongClickListener(v->{action.switchIme(true);return true;});add(bottom,s,switcher,.9f,s.height);
        Button space=Glass.button(c,s,Language.text(c,"空格"),false,false,v->action.space());space.setTextSize(13);add(bottom,s,space,3.05f,s.height);
        Button punctuation=Glass.button(c,s,chinese?"。":".",false,false,v->action.punctuation());punctuation.setOnLongClickListener(v->{action.key("'");return true;});punctuation.setContentDescription(Language.text(c,"句号，长按输入撇号"));add(bottom,s,punctuation,.7f,s.height);
        add(bottom,s,Glass.button(c,s,Language.text(c,"回车"),true,true,v->action.enter()),1.35f,s.height);keys.addView(bottom);
    }
    private static String shortcut(char key){String letters="qwertyuiopasdfghjklzxcvbnm";String symbols="1234567890@#$%&*();!?:/_-+";int index=letters.indexOf(Character.toLowerCase(key));return index<0?"":String.valueOf(symbols.charAt(index));}
    static void addQuickSymbols(LinearLayout keys,KeyboardStyle s,boolean chinese,java.util.function.Consumer<String> action){LinearLayout row=Ui.row(keys.getContext());row.setBaselineAligned(false);for(char symbol:(chinese?"，。？！@#-_":",.?!@#-_").toCharArray()){String value=String.valueOf(symbol);Button b=Glass.button(keys.getContext(),s,value,false,true,v->action.accept(value));b.setTextSize(14);add(row,s,b,1,32);}keys.addView(row);}
    private static void buildNine(LinearLayout keys,KeyboardStyle s,Actions action){Context c=keys.getContext();String[] labels={"1","2","3","4","5","6","7","8","9"},letters={"符号","ABC","DEF","GHI","JKL","MNO","PQRS","TUV","WXYZ"};
        for(int r=0;r<3;r++){LinearLayout row=Ui.row(c);row.setBaselineAligned(false);for(int j=0;j<3;j++){int index=r*3+j;String value=labels[index];Button b=Glass.button(c,s,value,false,false,v->{if(index==0)action.symbols();else action.key(value);});b.setTextSize(21);Glass.hint(b,Language.text(c,letters[index]));b.setContentDescription(value+" "+Language.text(c,letters[index]));add(row,s,b,1,Math.max(48,s.height));}keys.addView(row);}
        LinearLayout functions=Ui.row(c);functions.setBaselineAligned(false);add(functions,s,Glass.button(c,s,"ABC",false,true,v->action.language()),1,s.height);add(functions,s,Glass.button(c,s,Language.text(c,"空格"),false,false,v->action.space()),2,s.height);add(functions,s,Glass.button(c,s,"⌫",false,true,v->action.delete()),1,s.height);add(functions,s,Glass.button(c,s,Language.text(c,"回车"),true,true,v->action.enter()),1,s.height);keys.addView(functions);
    }
    private static void addNumbers(LinearLayout keys,KeyboardStyle s,Actions actions){
        LinearLayout row=Ui.row(keys.getContext());row.setBaselineAligned(false);for(char ch:"1234567890".toCharArray()){String v=String.valueOf(ch);Button b=Glass.button(keys.getContext(),s,v,false,false,a->actions.key(v));b.setTextSize(16);add(row,s,b,1,Math.max(34,s.height-8));}keys.addView(row);
    }
    static void add(LinearLayout row,KeyboardStyle s,Button button,float weight,int height){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(row.getContext(),height),weight);
        int half=Ui.dp(row.getContext(),s.gap/2f);lp.setMargins(half,Ui.dp(row.getContext(),2),half,Ui.dp(row.getContext(),3));row.addView(button,lp);
    }
}
