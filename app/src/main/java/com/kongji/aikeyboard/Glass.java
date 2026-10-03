package com.kongji.aikeyboard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.widget.Button;

/** Own soft backdrop and frosted surfaces; no sampling of other apps or per-frame blur. */
final class Glass {
    private static final Bitmap GRAIN;
    static {
        GRAIN=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888);
        java.util.Random r=new java.util.Random(73);
        for(int y=0;y<48;y++)for(int x=0;x<48;x++)GRAIN.setPixel(x,y,Color.argb(7+r.nextInt(7),255,255,255));
    }
    static Drawable backdrop(KeyboardStyle s){return new Backdrop(s);}
    static Drawable surface(Context c,KeyboardStyle s,boolean primary,boolean function){
        int a=Math.round(s.opacity*2.55f);
        int start=primary?KeyboardStyle.mix(s.accent,Color.WHITE,.05f):function?KeyboardStyle.mix(s.keyColor(),s.accent,s.dark()?.20f:.10f):s.keyColor();
        int end=primary?s.accent:start;
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{KeyboardStyle.alpha(start,primary?242:a),KeyboardStyle.alpha(end,primary?230:Math.max(30,a-38))});
        d.setCornerRadius(Ui.dp(c,s.radius));d.setStroke(Ui.dp(c,1),KeyboardStyle.alpha(Color.WHITE,s.dark()?42:175));
        GradientDrawable mask=Ui.rounded(c,Color.WHITE,s.radius);
        return new RippleDrawable(ColorStateList.valueOf(KeyboardStyle.alpha(primary?Color.WHITE:s.accent,32)),d,mask);
    }
    static Button button(Context c,KeyboardStyle s,String title,boolean primary,boolean function,android.view.View.OnClickListener click){
        Key b=new Key(c,s);b.setText(title);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);
        b.setPadding(Ui.dp(c,4),0,Ui.dp(c,4),0);b.setIncludeFontPadding(false);
        b.setTextColor(primary?foreground(KeyboardStyle.mix(s.accent,Color.WHITE,.03f)):s.ink());b.setBackgroundTintList(null);b.setBackground(surface(c,s,primary,function));
        b.setElevation(Ui.dp(c,s.dark()?0:1));b.setStateListAnimator(null);b.setOnClickListener(click);return b;
    }
    static void hint(Button b,String value){if(b instanceof Key)((Key)b).hint=value;}
    static void shortcut(Button b,String value,Runnable action){if(b instanceof Key){Key key=(Key)b;key.hint=value;key.swipeAction=action;key.setOnLongClickListener(v->{key.shortcutUsed=true;action.run();return true;});key.setPadding(Ui.dp(b.getContext(),4),key.style.symbolPosition==0?Ui.dp(b.getContext(),12):0,Ui.dp(b.getContext(),4),key.style.symbolPosition==1?Ui.dp(b.getContext(),12):0);key.setAutoSizeTextTypeUniformWithConfiguration(12,Math.min(24,key.style.font),1,android.util.TypedValue.COMPLEX_UNIT_SP);key.invalidate();}}
    static int foreground(int color){
        double[] channels={Color.red(color)/255.0,Color.green(color)/255.0,Color.blue(color)/255.0};
        for(int i=0;i<3;i++)channels[i]=channels[i]<=.04045?channels[i]/12.92:Math.pow((channels[i]+.055)/1.055,2.4);
        double luminance=.2126*channels[0]+.7152*channels[1]+.0722*channels[2];
        return luminance>.179?Color.BLACK:Color.WHITE;
    }
    // Native Activity + API 30+: tint, autosize and ripple are provided directly by the framework.
    @android.annotation.SuppressLint("AppCompatCustomView")
    private static final class Key extends Button {
        final KeyboardStyle style;String hint="";final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Runnable swipeAction;float downX,downY;boolean shortcutUsed;
        Key(Context c,KeyboardStyle s){super(c);style=s;}
        @Override public boolean performClick(){return super.performClick();}
        @Override public boolean onTouchEvent(MotionEvent e){
            if(isEnabled()){
                if(e.getActionMasked()==MotionEvent.ACTION_DOWN){
                    downX=e.getX();downY=e.getY();shortcutUsed=false;
                    if(swipeAction!=null&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                    if(style.haptic)performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if(style.motion)animate().scaleX(.96f).scaleY(.94f).setDuration(70).start();
                }else if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){
                    if(style.motion)animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    if(swipeAction!=null&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
                    float dy=e.getY()-downY,dx=e.getX()-downX;
                    if(e.getActionMasked()==MotionEvent.ACTION_UP&&swipeAction!=null&&!shortcutUsed&&e.getPointerCount()==1&&Math.abs(dy)>=Ui.dp(getContext(),16)&&Math.abs(dy)>Math.abs(dx)*1.4f&&(style.swipeDirection==0?dy<0:dy>0)){
                        MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.onTouchEvent(cancel);cancel.recycle();swipeAction.run();return true;
                    }
                }
            }
            return super.onTouchEvent(e);
        }
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(!hint.isEmpty()){
            p.setTextSize(Ui.dp(getContext(),9));p.setColor(KeyboardStyle.alpha(style.muted(),175));p.setTextAlign(Paint.Align.RIGHT);
            float baseline=style.symbolPosition==0?Ui.dp(getContext(),3)-p.ascent():getHeight()-Ui.dp(getContext(),3)-p.descent();
            c.drawText(hint,getWidth()-Ui.dp(getContext(),5),baseline,p);
        }}
        @Override protected void onDetachedFromWindow(){animate().cancel();super.onDetachedFromWindow();}
    }
    private static final class Backdrop extends Drawable {
        final KeyboardStyle style;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final RectF rect=new RectF();
        Shader base,one,two;final Shader grain=new BitmapShader(GRAIN,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);
        Backdrop(KeyboardStyle s){style=s;}
        @Override protected void onBoundsChange(Rect b){
            rect.set(b);float w=Math.max(1,b.width()),h=Math.max(1,b.height());
            base=new LinearGradient(0,0,w,h,new int[]{KeyboardStyle.mix(style.base(),style.keyColor(),.18f),style.base()},null,Shader.TileMode.CLAMP);
            one=new RadialGradient(w*.18f,h*.08f,w*.9f,new int[]{KeyboardStyle.alpha(style.glow(),180),KeyboardStyle.alpha(style.glow(),0)},null,Shader.TileMode.CLAMP);
            int glow=KeyboardStyle.mix(style.accent,style.glow(),.5f);
            two=new RadialGradient(w*.95f,h*.85f,w*.75f,new int[]{KeyboardStyle.alpha(glow,style.dark()?70:45),KeyboardStyle.alpha(glow,0)},null,Shader.TileMode.CLAMP);
        }
        @Override public void draw(Canvas canvas){
            p.setShader(base);canvas.drawRect(rect,p);p.setShader(one);canvas.drawRect(rect,p);p.setShader(two);canvas.drawRect(rect,p);
            p.setShader(grain);canvas.drawRect(rect,p);p.setShader(null);p.setColor(KeyboardStyle.alpha(Color.WHITE,style.dark()?35:155));canvas.drawLine(rect.left,rect.top+.5f,rect.right,rect.top+.5f,p);
        }
        @Override public void setAlpha(int value){}
        @Override public void setColorFilter(ColorFilter filter){}
        @Override public int getOpacity(){return PixelFormat.OPAQUE;}
    }
}
