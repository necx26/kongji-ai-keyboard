package com.kongji.aikeyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG=0xfffafaf8,INK=0xff202020,MUTED=0xff737373,ACCENT=0xff202021;
    static KeyboardStyle style(Context c){if(c instanceof LocalizedActivity a&&a.pageStyle!=null)return a.pageStyle;return KeyboardStyle.load(c);}
    static int bg(Context c){return style(c).dark()?0xff171717:BG;}
    static int panel(Context c){return style(c).dark()?0xff252525:0xfff0f0ed;}
    static int ink(Context c){return style(c).ink();}
    static int muted(Context c){return style(c).muted();}
    static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable rounded(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    static TextView text(Context c,String value,int size,int color){return rawText(c,Language.text(c,value),size,color);}
    static TextView rawText(Context c,String value,int size,int color){
        TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color==INK||color==ACCENT?ink(c):color==MUTED?muted(c):color);
        t.setLineSpacing(dp(c,2),1f);t.setPadding(dp(c,4),dp(c,4),dp(c,4),dp(c,4));return t;
    }
    static Button button(Context c,String value,boolean primary,View.OnClickListener action){
        Button b=Glass.button(c,style(c),Language.text(c,value),primary,false,action);b.setMaxLines(2);b.setAutoSizeTextTypeUniformWithConfiguration(10,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);return b;
    }
    static Button navigation(Context c,String value,View.OnClickListener action){
        Button b=button(c,value,false,action);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setPadding(dp(c,4),0,dp(c,4),0);
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(KeyboardStyle.alpha(ink(c),20)),null,null));
        b.setTextSize(15);
        android.graphics.drawable.Drawable arrow=new android.graphics.drawable.Drawable(){
            final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(android.graphics.Canvas canvas){android.graphics.Rect bounds=getBounds();paint.setColor(muted(c));paint.setStrokeWidth(dp(c,1.6f));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);paint.setStyle(android.graphics.Paint.Style.STROKE);
                float x=bounds.centerX(),y=bounds.centerY(),halfWidth=dp(c,3.5f),halfHeight=dp(c,7);android.graphics.Path path=new android.graphics.Path();path.moveTo(x-halfWidth,y-halfHeight);path.lineTo(x+halfWidth,y);path.lineTo(x-halfWidth,y+halfHeight);canvas.drawPath(path,paint);}
            @Override public void setAlpha(int a){}@Override public void setColorFilter(android.graphics.ColorFilter f){}@Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
        };arrow.setBounds(0,0,dp(c,20),dp(c,24));b.setCompoundDrawablesRelative(null,null,arrow,null);return b;
    }
    static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setBaselineAligned(false);return l;}
    static void addButton(LinearLayout l,Button b,float weight,int height){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(l.getContext(),height),weight);p.setMargins(dp(l.getContext(),2),0,dp(l.getContext(),2),0);l.addView(b,p);}
    static TextView heading(Context c,String value,int size){TextView t=text(c,value,Math.min(size,24),INK);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));t.setPadding(dp(c,4),dp(c,20),dp(c,4),dp(c,10));return t;}
    static void rule(LinearLayout parent){View line=new View(parent.getContext());line.setBackgroundColor(style(parent.getContext()).dark()?0xff383838:0xffdeded9);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(parent.getContext(),1));p.setMargins(0,dp(parent.getContext(),12),0,dp(parent.getContext(),12));parent.addView(line,p);}
    static LinearLayout disclosure(LinearLayout parent,String title,boolean open){
        Context c=parent.getContext();LinearLayout body=column(c);Button label=navigation(c,title,v->{body.setVisibility(body.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);});
        parent.addView(label,new LinearLayout.LayoutParams(-1,dp(c,52)));parent.addView(body);body.setVisibility(open?View.VISIBLE:View.GONE);return body;
    }
    static void activityInsets(View view,int padding){view.setOnApplyWindowInsetsListener((v,window)->{
        android.graphics.Insets i=window.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.ime());v.setPadding(padding+i.left,padding+i.top,padding+i.right,padding+i.bottom);return window;});view.requestApplyInsets();}
}
