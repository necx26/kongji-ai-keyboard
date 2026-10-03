package com.kongji.aikeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

final class KeyboardStyle {
    static final String[] NAMES={"雾紫","冰蓝","奶油","深空"};
    static final String[] LAYOUTS={"QWERTY","QWERTZ","AZERTY"};
    static final String[] MODES={"26键","九宫格","手写"};
    int mode=0,symbolPosition=0,swipeDirection=0;
    boolean quickSymbols=true;
    int palette=0,accent=Color.rgb(115,94,190),height=46,radius=12,gap=4,font=20,opacity=74,layout=0;
    boolean numberRow=false,haptic=true,motion=true;
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("keyboard_style",Context.MODE_PRIVATE);}
    static KeyboardStyle load(Context c){
        SharedPreferences p=prefs(c);KeyboardStyle s=new KeyboardStyle();
        s.palette=clamp(p.getInt("palette",0),0,3);s.accent=p.getInt("accent",s.accent)|0xff000000;
        s.height=clamp(p.getInt("height",46),40,58);s.radius=clamp(p.getInt("radius",12),4,22);
        s.gap=clamp(p.getInt("gap",4),2,7);s.font=clamp(p.getInt("font",20),16,24);
        s.opacity=clamp(p.getInt("opacity",74),40,96);s.layout=clamp(p.getInt("layout",0),0,2);
        s.numberRow=p.getBoolean("number_row",false);s.haptic=p.getBoolean("haptic",true);s.motion=p.getBoolean("motion",true);
        s.mode=clamp(p.getInt("mode",0),0,2);s.symbolPosition=clamp(p.getInt("symbol_position",0),0,1);s.swipeDirection=clamp(p.getInt("swipe_direction",0),0,1);s.quickSymbols=p.getBoolean("quick_symbols",true);return s;
    }
    boolean save(Context c){return prefs(c).edit().putInt("palette",palette).putInt("accent",accent)
        .putInt("height",height).putInt("radius",radius).putInt("gap",gap).putInt("font",font)
        .putInt("opacity",opacity).putInt("layout",layout).putBoolean("number_row",numberRow)
        .putBoolean("haptic",haptic).putBoolean("motion",motion).putInt("mode",mode).putInt("symbol_position",symbolPosition).putInt("swipe_direction",swipeDirection).putBoolean("quick_symbols",quickSymbols).commit();}
    boolean dark(){return palette==3;}
    int ink(){return dark()?Color.rgb(236,241,253):Color.rgb(44,46,64);}
    int muted(){return dark()?Color.rgb(170,184,212):Color.rgb(112,115,136);}
    int base(){return switch(palette){case 1->Color.rgb(228,240,248);case 2->Color.rgb(246,239,226);case 3->Color.rgb(25,34,53);default->Color.rgb(234,230,247);};}
    int glow(){return switch(palette){case 1->Color.rgb(161,213,229);case 2->Color.rgb(229,200,160);case 3->Color.rgb(71,91,142);default->Color.rgb(186,174,224);};}
    int keyColor(){return dark()?Color.rgb(63,78,110):Color.WHITE;}
    static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    static int alpha(int color,int alpha){return (color&0xffffff)|(clamp(alpha,0,255)<<24);}
    static int mix(int a,int b,float t){return Color.rgb(Math.round(Color.red(a)*(1-t)+Color.red(b)*t),Math.round(Color.green(a)*(1-t)+Color.green(b)*t),Math.round(Color.blue(a)*(1-t)+Color.blue(b)*t));}
}
