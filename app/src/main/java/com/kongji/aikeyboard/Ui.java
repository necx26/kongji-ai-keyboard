package com.kongji.aikeyboard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = Color.rgb(245,242,252), INK = Color.rgb(38,31,54);
    static final int MUTED = Color.rgb(109,100,127), ACCENT = Color.rgb(118,88,213);
    static int dp(Context c, float n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    static GradientDrawable rounded(Context c, int color, int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c,radius)); return d;
    }
    static TextView text(Context c,String value,int size,int color) {
        return rawText(c,Language.text(c,value),size,color);
    }
    static TextView rawText(Context c,String value,int size,int color) {
        TextView t=new TextView(c); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(dp(c,4),dp(c,4),dp(c,4),dp(c,4)); return t;
    }
    static Button button(Context c,String value,boolean primary,View.OnClickListener action) {
        Button button=Glass.button(c,new KeyboardStyle(),Language.text(c,value),primary,false,action);button.setMaxLines(2);button.setAutoSizeTextTypeUniformWithConfiguration(10,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);return button;
    }
    static LinearLayout column(Context c) { LinearLayout l=new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    static LinearLayout row(Context c) { LinearLayout l=new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    static void addButton(LinearLayout l,Button b,float weight,int height) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(l.getContext(),height),weight);
        p.setMargins(dp(l.getContext(),2),0,dp(l.getContext(),2),0); l.addView(b,p);
    }
    static TextView heading(Context c,String value,int size) {
        TextView t=text(c,value,size,INK); t.setTypeface(null,Typeface.BOLD); return t;
    }
    static void activityInsets(View view,int padding) {
        view.setOnApplyWindowInsetsListener((v,window)->{
            android.graphics.Insets insets=window.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.ime());
            v.setPadding(padding+insets.left,padding+insets.top,padding+insets.right,padding+insets.bottom);
            return window;
        });
        view.requestApplyInsets();
    }
}
