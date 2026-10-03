package com.kongji.aikeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

final class KeyboardStyle {
    static final String[] NAMES={"白色","深色"},LAYOUTS={"QWERTY","QWERTZ","AZERTY"},MODES={"26键","九宫格","手写"};
    int mode=0,symbolPosition=0,swipeDirection=0,palette=0;
    boolean quickSymbols=true,haptic=true,numberRow=false,motion=false;
    int accent=0xff202020,height=46,radius=7,gap=4,font=20,opacity=94,layout=0;
    float width=1f,horizontal=.5f;int lift=0;String photo="";
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("keyboard_style",Context.MODE_PRIVATE);}
    static KeyboardStyle load(Context c){
        SharedPreferences p=prefs(c);KeyboardStyle s=new KeyboardStyle();
        // Preserve the old dark theme; old pastel choices migrate to white.
        s.palette=clamp(p.getInt("theme",p.getInt("palette",0)==3?1:0),0,1);s.accent=s.dark()?0xffeeeeee:0xff202020;
        s.height=clamp(p.getInt("geometry_height",46),40,64);s.width=limit(p.getFloat("geometry_width",1f),.82f,1f);
        s.horizontal=limit(p.getFloat("geometry_horizontal",.5f),0,1);s.lift=clamp(p.getInt("geometry_lift",0),0,88);s.photo=p.getString("background_photo","");
        s.layout=clamp(p.getInt("layout",0),0,2);s.haptic=p.getBoolean("haptic",true);s.mode=clamp(p.getInt("mode",0),0,2);
        s.symbolPosition=clamp(p.getInt("symbol_position",0),0,1);s.swipeDirection=clamp(p.getInt("swipe_direction",0),0,1);s.quickSymbols=p.getBoolean("quick_symbols",true);return s;
    }
    boolean save(Context c){return prefs(c).edit().putInt("theme",palette).putInt("geometry_height",height)
        .putFloat("geometry_width",limit(width,.82f,1f)).putFloat("geometry_horizontal",limit(horizontal,0,1))
        .putInt("geometry_lift",clamp(lift,0,88)).putString("background_photo",photo).putInt("layout",layout)
        .putBoolean("haptic",haptic).putInt("mode",mode).putInt("symbol_position",symbolPosition).putInt("swipe_direction",swipeDirection)
        .putBoolean("quick_symbols",quickSymbols).commit();}
    boolean dark(){return palette==1;}
    int ink(){return dark()?0xfff2f2f2:0xff202020;}int muted(){return dark()?0xffababab:0xff737373;}
    int base(){return dark()?0xff171717:0xfff4f4f2;}int glow(){return base();}
    int keyColor(){return dark()?0xff343434:Color.WHITE;}
    static float limit(float v,float min,float max){return Float.isFinite(v)?Math.max(min,Math.min(max,v)):min;}
    static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    static int alpha(int color,int alpha){return (color&0xffffff)|(clamp(alpha,0,255)<<24);}
    static int mix(int a,int b,float t){return Color.rgb(Math.round(Color.red(a)*(1-t)+Color.red(b)*t),Math.round(Color.green(a)*(1-t)+Color.green(b)*t),Math.round(Color.blue(a)*(1-t)+Color.blue(b)*t));}
}