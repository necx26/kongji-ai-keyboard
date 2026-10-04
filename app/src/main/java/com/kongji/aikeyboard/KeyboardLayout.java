package com.kongji.aikeyboard;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/** One four-row geometry shared by the input method and its editable preview. */
final class KeyboardLayout {
    interface Actions {void key(String value);void shift();void delete();void deleteRecent();void undo();void symbols();void language();void switchIme(boolean picker);void space();void punctuation();void enter();default void clipboard(){}}
    static void build(LinearLayout keys,KeyboardStyle s,boolean chinese,boolean shift,boolean symbols,Actions action){
        Context c=keys.getContext();keys.removeAllViews();keys.setClipChildren(false);keys.setClipToPadding(false);
        if(s.mode==1&&chinese&&!symbols){buildNine(keys,s,action);return;}
        String[] rows=symbols?new String[]{"1234567890","@#￥%&*()-",chinese?"，。？！：；/'":",.?!:;/'"}:switch(s.layout){
            case 1->new String[]{"qwertzuiop","asdfghjkl","yxcvbnm"};
            case 2->new String[]{"azertyuiop","qsdfghjklm","wxcvbn"};
            default->new String[]{"qwertyuiop","asdfghjkl","zxcvbnm"};};
        for(int r=0;r<rows.length;r++){
            LinearLayout row=Ui.row(c);row.setGravity(Gravity.CENTER);row.setClipChildren(false);
            if(r==1&&!symbols)inset(row,s);
            if(r==2&&!symbols){Button shiftKey=key(c,s,"⇧",v->action.shift());Glass.icon(shiftKey,"shift");shiftKey.setContentDescription(Language.text(c,"切换大小写"));add(row,s,shiftKey,1.4f,s.height);spacer(row,.17f);}
            for(char ch:rows[r].toCharArray()){
                String label=String.valueOf(ch);if(!symbols&&shift)label=label.toUpperCase(java.util.Locale.ROOT);final String value=label;
                Button b=key(c,s,label,v->action.key(value));b.setTextSize(s.font);b.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));Glass.preview(b);
                if(!symbols){String shortcut=shortcut(ch);Glass.shortcut(b,shortcut,()->action.key(shortcut));b.setContentDescription(value+", "+Language.text(c,s.swipeDirection==0?"上滑输入符号":"下滑输入符号")+" "+shortcut);}
                add(row,s,b,1,s.height);
            }
            if(r==1&&!symbols)inset(row,s);
            if(r==2){if(!symbols)spacer(row,.17f);add(row,s,deleteKey(c,s,action::delete,action::deleteRecent,action::undo),symbols?1.45f:1.4f,s.height);}
            keys.addView(row);
        }
        LinearLayout bottom=Ui.row(c);bottom.setClipChildren(false);
        add(bottom,s,key(c,s,symbols?"ABC":"123",v->action.symbols()),2,s.height);
        Button comma=key(c,s,chinese?"，":",",v->action.key(chinese?"，":","));Glass.shortcut(comma,chinese?"。":".",()->action.key(chinese?"。":"."));comma.setContentDescription(Language.text(c,"逗号"));add(bottom,s,comma,1,s.height);
        Button space=key(c,s,Language.text(c,"空格"),v->action.space());Glass.icon(space,"wave");space.setContentDescription(Language.text(c,"空格"));add(bottom,s,space,3.8f,s.height);
        Button language=key(c,s,"中/英",v->action.language());Glass.icon(language,"language");language.setContentDescription(Language.text(c,"切换中英文"));add(bottom,s,language,1.15f,s.height);
        add(bottom,s,enterKey(c,s,action::enter),2.05f,s.height);keys.addView(bottom);
        addFooter(keys,s,action);
    }
    private static Button key(Context c,KeyboardStyle s,String title,View.OnClickListener action){Button b=Glass.button(c,s,title,false,false,action);b.setTextSize(20);return b;}
    private static void spacer(LinearLayout row,float weight){row.addView(new View(row.getContext()),new LinearLayout.LayoutParams(0,1,weight));}
    private static void inset(LinearLayout row,KeyboardStyle s){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,1,.5f);lp.rightMargin=Ui.dp(row.getContext(),s.gap/2f);row.addView(new View(row.getContext()),lp);}
    private static String shortcut(char key){String letters="qwertyuiopasdfghjklzxcvbnm";String[] values={"1","2","3","4","5","6","7","8","9","0","-","/",":",";","(",")","~","“","”","@",".","#","、","?","!","…"};int index=letters.indexOf(Character.toLowerCase(key));return index<0?"":values[index];}
    static void addFooter(LinearLayout keys,KeyboardStyle s,Actions action){Context c=keys.getContext();LinearLayout row=Ui.row(c);row.setTag("footer");row.setGravity(Gravity.BOTTOM);row.setPadding(Ui.dp(c,24),0,Ui.dp(c,24),0);
        if(s.footerLayout==2)spacer(row,1);
        Button switcher=footerKey(c,s,"切换输入法","keyboard",v->action.switchIme(true));row.addView(switcher,new LinearLayout.LayoutParams(Ui.dp(c,44),Ui.dp(c,36)));
        if(s.footerLayout==0)spacer(row,1);else spacer(row,0);
        row.addView(footerKey(c,s,"剪贴板","clipboard",v->action.clipboard()),new LinearLayout.LayoutParams(Ui.dp(c,44),Ui.dp(c,36)));if(s.footerLayout==1)spacer(row,1);keys.addView(row);
    }
    private static Button footerKey(Context c,KeyboardStyle s,String label,String icon,View.OnClickListener action){Button b=key(c,s,Language.text(c,label),action);b.setContentDescription(Language.text(c,label));Glass.icon(b,icon);if(b instanceof Glass.Key k)k.footer=true;Glass.release(b);b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(KeyboardStyle.alpha(s.ink(),25)),null,null));return b;}
    static void addQuickSymbols(LinearLayout keys,KeyboardStyle s,boolean chinese,java.util.function.Consumer<String> action){LinearLayout row=Ui.row(keys.getContext());for(char symbol:(chinese?"，。？！@#-_":",.?!@#-_").toCharArray()){String value=String.valueOf(symbol);Button b=key(keys.getContext(),s,value,v->action.accept(value));Glass.preview(b);b.setTextSize(14);add(row,s,b,1,32);}keys.addView(row);}
    private static void buildNine(LinearLayout keys,KeyboardStyle s,Actions action){Context c=keys.getContext();String[] letters={"符号","ABC","DEF","GHI","JKL","MNO","PQRS","TUV","WXYZ"};
        if(s.quickSymbols)addQuickSymbols(keys,s,true,action::key);
        for(int r=0;r<3;r++){LinearLayout row=Ui.row(c);for(int j=0;j<3;j++){int index=r*3+j;String value=Integer.toString(index+1);Button b=key(c,s,value,v->{if(index==0)action.symbols();else action.key(value);});b.setTextSize(21);Glass.preview(b);Glass.hint(b,Language.text(c,letters[index]));b.setContentDescription(value+" "+Language.text(c,letters[index]));add(row,s,b,1,Math.max(48,s.height));}keys.addView(row);}
        LinearLayout functions=Ui.row(c);add(functions,s,key(c,s,"ABC",v->action.language()),1,s.height);add(functions,s,key(c,s,Language.text(c,"空格"),v->action.space()),2,s.height);add(functions,s,deleteKey(c,s,action::delete,action::deleteRecent,action::undo),1,s.height);add(functions,s,enterKey(c,s,action::enter),1,s.height);keys.addView(functions);addFooter(keys,s,action);
    }
    static Button deleteKey(Context c,KeyboardStyle s,Runnable delete,Runnable recent,Runnable undo){Button b=key(c,s,"⌫",v->delete.run());b.setContentDescription(Language.text(c,"删除，上滑删除最近输入，下滑撤回"));Glass.icon(b,"delete");Glass.repeat(b,delete);Glass.verticalActions(b,Language.text(c,"删除"),recent,Language.text(c,"撤回"),undo);return b;}
    static Button enterKey(Context c,KeyboardStyle s,Runnable enter){Button b=key(c,s,Language.text(c,"换行"),v->enter.run());b.setContentDescription(Language.text(c,"回车"));Glass.release(b);return b;}
    static void add(LinearLayout row,KeyboardStyle s,Button button,float weight,int height){Glass.release(button);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(row.getContext(),height),weight);int half=Ui.dp(row.getContext(),s.gap/2f);lp.setMargins(half,Ui.dp(row.getContext(),5),half,Ui.dp(row.getContext(),5));row.addView(button,lp);}
}
