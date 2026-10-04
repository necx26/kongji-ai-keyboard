package com.kongji.aikeyboard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;

/** Matte surfaces and keyboard gestures with one owner until release/cancel. */
final class Glass {
    static Drawable backdrop(KeyboardStyle s){return new ColorDrawable(s.base());}
    static Drawable backdrop(Context c,KeyboardStyle s){Bitmap b=BackgroundPhoto.bitmap(c,s.photo);return b==null?backdrop(s):new Photo(b,s);}
    static Drawable surface(Context c,KeyboardStyle s,boolean primary,boolean function){
        int color=primary?(s.dark()?0xffeeeeee:0xff202020):function?(s.dark()?0xff282828:0xffe7e7e4):s.keyColor();
        GradientDrawable d=Ui.rounded(c,KeyboardStyle.alpha(color,s.photo.isEmpty()?255:primary?255:238),s.radius);d.setStroke(Ui.dp(c,.5f),s.dark()?0xff484848:0xffdededb);
        return new RippleDrawable(ColorStateList.valueOf(KeyboardStyle.alpha(s.ink(),30)),d,Ui.rounded(c,Color.WHITE,s.radius));
    }
    static Button button(Context c,KeyboardStyle s,String title,boolean primary,boolean function,View.OnClickListener click){
        Key b=new Key(c,s);b.setText(title);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(Ui.dp(c,4),0,Ui.dp(c,4),0);b.setIncludeFontPadding(false);
        b.setTextColor(primary?(s.dark()?0xff171717:Color.WHITE):s.ink());b.setBackgroundTintList(null);b.setBackground(surface(c,s,primary,function));b.setElevation(0);b.setStateListAnimator(null);b.setOnClickListener(click);return b;
    }
    static void hint(Button b,String value){if(b instanceof Key k){k.hint=value;k.invalidate();}}
    static void icon(Button b,String value){if(b instanceof Key k){k.icon=value;k.setTextColor(Color.TRANSPARENT);k.invalidate();}}
    static void release(Button b){if(b instanceof Key k)k.keyboard=true;}
    static void preview(Button b){if(b instanceof Key k){k.keyboard=true;k.showPreview=true;}}
    static void repeat(Button b,Runnable action){if(b instanceof Key k){k.keyboard=true;k.repeatAction=action;}}
    static void verticalActions(Button b,String up,Runnable upAction,String down,Runnable downAction){if(b instanceof Key k){k.keyboard=true;k.upLabel=up;k.downLabel=down;k.upAction=upAction;k.downAction=downAction;}}
    static void shortcut(Button b,String value,Runnable action){if(b instanceof Key k){
        k.keyboard=true;k.showPreview=true;k.hint=value;k.swipeAction=action;
        k.setPadding(Ui.dp(b.getContext(),4),k.style.symbolPosition==0?Ui.dp(b.getContext(),12):0,Ui.dp(b.getContext(),4),k.style.symbolPosition==1?Ui.dp(b.getContext(),12):0);
        k.setAutoSizeTextTypeUniformWithConfiguration(12,Math.min(24,k.style.font),1,android.util.TypedValue.COMPLEX_UNIT_SP);k.invalidate();
    }}
    static void cancelTouches(View view){if(view instanceof Key k)k.cancel();if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)cancelTouches(group.getChildAt(i));}
    static int foreground(int color){return Color.luminance(color)>.179?Color.BLACK:Color.WHITE;}
    // Native Activity, API 30+: framework ripple and autosize are sufficient.
    @android.annotation.SuppressLint("AppCompatCustomView")
    static class Key extends Button {
        final KeyboardStyle style;String hint="",icon="";final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        boolean keyboard,showPreview,active,symbolSelected,repeated,repeatPaused,footer;int pointer=-1;
        Runnable swipeAction,repeatAction,upAction,downAction;String upLabel="",downLabel="";int verticalChoice;float downX,downY;private ViewGroup overlayHost;private TextView bubble;
        private final Handler handler=new Handler(Looper.getMainLooper());
        private final Runnable repeatTick=new Runnable(){public void run(){if(!active||repeatPaused||verticalChoice!=0||repeatAction==null||!isShown())return;repeated=true;repeatAction.run();if(active)handler.postDelayed(this,75);}};
        Key(Context c,KeyboardStyle s){super(c);style=s;}
        @Override public boolean performClick(){return super.performClick();}
        @Override public boolean onTouchEvent(MotionEvent e){
            if(!isEnabled())return false;
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN)feedback();
            if(!keyboard)return super.onTouchEvent(e);
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN->{cancel();active=true;pointer=e.getPointerId(0);downX=e.getX();downY=e.getY();repeated=false;repeatPaused=false;symbolSelected=false;verticalChoice=0;setPressed(true);
                    if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                    if(showPreview)showBubble();if(repeatAction!=null)handler.postDelayed(repeatTick,380);return true;}
                case MotionEvent.ACTION_MOVE->{if(!active)return true;int i=e.findPointerIndex(pointer);if(i<0){cancel();return true;}select(e.getX(i),e.getY(i));return true;}
                case MotionEvent.ACTION_POINTER_UP->{if(e.getPointerId(e.getActionIndex())==pointer)release(e.getX(e.getActionIndex()),e.getY(e.getActionIndex()));return true;}
                case MotionEvent.ACTION_UP->{if(active)release(e.getX(),e.getY());return true;}
                case MotionEvent.ACTION_CANCEL->{cancel();return true;}
                default->{return true;}
            }
        }
        private void select(float x,float y){float dy=y-downY,dx=x-downX;
            boolean vertical=Math.abs(dy)>=Ui.dp(getContext(),18)&&Math.abs(dy)>Math.abs(dx)*1.2f;
            if(upAction!=null||downAction!=null){
                // A deliberate slow slide must pause long-press deletion before the
                // larger selection threshold; the gesture still waits for release.
                if(repeatAction!=null){boolean traveling=Math.abs(dy)>=Ui.dp(getContext(),3)&&Math.abs(dy)>Math.abs(dx)*1.2f;
                    if(traveling&&!repeatPaused){repeatPaused=true;handler.removeCallbacks(repeatTick);}
                    else if(repeatPaused&&Math.abs(dy)<Ui.dp(getContext(),3)&&Math.abs(dx)<Ui.dp(getContext(),3)&&!repeated){repeatPaused=false;handler.postDelayed(repeatTick,380);}}
                int choice=vertical?(dy<0&&upAction!=null?-1:dy>0&&downAction!=null?1:0):0;if(choice!=verticalChoice){verticalChoice=choice;updateBubble(choice<0?upLabel:choice>0?downLabel:getText());}return;}
            boolean choice=swipeAction!=null&&vertical&&(style.swipeDirection==0?dy<0:dy>0);
            if(choice!=symbolSelected){symbolSelected=choice;updateBubble(choice?hint:getText());}
        }
        private void release(float x,float y){select(x,y);confirmPending();}
        void confirmPending(){if(!active)return;boolean symbol=symbolSelected,held=repeated;int vertical=verticalChoice;cancel();if(held)return;if(vertical<0)upAction.run();else if(vertical>0)downAction.run();else if(symbol)swipeAction.run();else performClick();}
        void cancel(){handler.removeCallbacks(repeatTick);active=false;pointer=-1;setPressed(false);if(overlayHost!=null&&bubble!=null)overlayHost.getOverlay().remove(bubble);overlayHost=null;bubble=null;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
        private void showBubble(){if(getWindowToken()==null)return;
            bubble=Ui.rawText(getContext(),getText().toString(),22,style.ink());bubble.setGravity(Gravity.CENTER);bubble.setPadding(0,0,0,0);bubble.setBackground(Ui.rounded(getContext(),style.keyColor(),8));
            bubble.setSingleLine(true);
            int w=Ui.dp(getContext(),38),h=Ui.dp(getContext(),46);
            bubble.setLayoutParams(new ViewGroup.LayoutParams(w,h));
            if(!(getRootView() instanceof ViewGroup host)){bubble=null;return;}overlayHost=host;
            int[] key=new int[2],root=new int[2];getLocationOnScreen(key);host.getLocationOnScreen(root);
            int x=Math.max(Ui.dp(getContext(),2),Math.min(host.getWidth()-w-Ui.dp(getContext(),2),key[0]-root[0]+getWidth()/2-w/2));
            int y=Math.max(0,key[1]-root[1]-h-Ui.dp(getContext(),5));
            bubble.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));
            bubble.layout(x,y,x+w,y+h);updateBubble(getText());bubble.setElevation(Ui.dp(getContext(),4));host.getOverlay().add(bubble);
        }
        private void feedback(){if(performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING))return;
            android.os.Vibrator vibrator;if(android.os.Build.VERSION.SDK_INT>=31){android.os.VibratorManager manager=getContext().getSystemService(android.os.VibratorManager.class);vibrator=manager==null?null:manager.getDefaultVibrator();}else vibrator=getContext().getSystemService(android.os.Vibrator.class);
            if(vibrator!=null&&vibrator.hasVibrator())vibrator.vibrate(android.os.VibrationEffect.createOneShot(8,80));
        }
        private void updateBubble(CharSequence text){if(bubble==null)return;
            // Overlay children do not participate in the host's normal layout pass.
            // Fit the new label and refresh its text layout immediately on each choice.
            int x=bubble.getLeft(),y=bubble.getTop(),w=bubble.getWidth(),h=bubble.getHeight();
            bubble.setText(text);bubble.setTextSize(22);
            float measured=bubble.getPaint().measureText(text.toString());float available=w-Ui.dp(getContext(),4);
            if(measured>available)bubble.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,bubble.getTextSize()*available/measured);
            bubble.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));bubble.layout(x,y,x+w,y+h);
        }
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(!icon.isEmpty())KeyboardIcons.draw(c,icon,getWidth()/2f,getHeight()/2f+Ui.dp(getContext(),footer?4:0),Ui.dp(getContext(),footer?22:icon.equals("wave")?22:27),style.ink());if(!hint.isEmpty()){p.setTextSize(Ui.dp(getContext(),10));p.setColor(style.muted());p.setTextAlign(Paint.Align.CENTER);
            float baseline=style.symbolPosition==0?Ui.dp(getContext(),3)-p.ascent():getHeight()-Ui.dp(getContext(),3)-p.descent();c.drawText(hint,getWidth()/2f,baseline,p);}}
        @Override protected void onDetachedFromWindow(){cancel();super.onDetachedFromWindow();}
        @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)cancel();}
    }
    private static final class Photo extends Drawable {
        final Bitmap bitmap;final KeyboardStyle style;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);final RectF destination=new RectF();
        Photo(Bitmap b,KeyboardStyle s){bitmap=b;style=s;}
        @Override protected void onBoundsChange(Rect rect){float scale=Math.max(rect.width()/(float)bitmap.getWidth(),rect.height()/(float)bitmap.getHeight());float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;destination.set(rect.centerX()-w/2,rect.centerY()-h/2,rect.centerX()+w/2,rect.centerY()+h/2);}
        @Override public void draw(Canvas c){int save=c.save();c.clipRect(getBounds());paint.setColor(Color.WHITE);c.drawBitmap(bitmap,null,destination,paint);paint.setColor(KeyboardStyle.alpha(style.base(),style.dark()?115:95));c.drawRect(getBounds(),paint);c.restoreToCount(save);}
        @Override public void setAlpha(int alpha){}@Override public void setColorFilter(ColorFilter filter){}@Override public int getOpacity(){return PixelFormat.OPAQUE;}
    }
}
