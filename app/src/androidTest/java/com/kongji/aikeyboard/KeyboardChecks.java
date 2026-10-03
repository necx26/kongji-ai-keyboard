package com.kongji.aikeyboard;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/** Real attached views, MotionEvents, popup state, repeated deletion and shared geometry. */
final class KeyboardChecks {
    private final Instrumentation instrumentation;private int passed;private Activity activity;
    private Button letter,delete;private LinearLayout page;private final AtomicInteger letters=new AtomicInteger(),symbols=new AtomicInteger(),deleted=new AtomicInteger();
    private final KeyboardStyle style=new KeyboardStyle();
    KeyboardChecks(Instrumentation i){instrumentation=i;}
    private void check(boolean condition,String message){if(!condition)throw new AssertionError(message);passed++;}
    private void touch(View view,int action,float x,float y){Runnable send=()->{long now=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,action,x,y,0);view.dispatchTouchEvent(event);event.recycle();};if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())send.run();else instrumentation.runOnMainSync(send);}
    private String bubble()throws Exception{Field f=Glass.Key.class.getDeclaredField("bubble");f.setAccessible(true);TextView text=(TextView)f.get(letter);return text==null?"":text.getText().toString();}
    int run()throws Exception{
        activity=instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.runOnMainSync(()->{page=Ui.column(activity);page.setPadding(0,Ui.dp(activity,90),0,0);activity.setContentView(page);letter=Glass.button(activity,style,"q",false,false,v->letters.incrementAndGet());Glass.shortcut(letter,"1",symbols::incrementAndGet);page.addView(letter,new LinearLayout.LayoutParams(160,150));
            delete=Glass.button(activity,style,"⌫",false,false,v->deleted.incrementAndGet());Glass.repeat(delete,deleted::incrementAndGet);page.addView(delete,new LinearLayout.LayoutParams(160,150));});
        instrumentation.waitForIdleSync();
        try{
            touch(letter,MotionEvent.ACTION_DOWN,50,70);instrumentation.waitForIdleSync();check(letters.get()==0,"press must not output");check(bubble().equals("q"),"letter preview at press");
            checkPopupLocation();
            touch(letter,MotionEvent.ACTION_MOVE,220,70);touch(letter,MotionEvent.ACTION_UP,220,70);check(letters.get()==1&&symbols.get()==0,"release outside commits the touched key once");check(bubble().isEmpty(),"popup dismissed after release");
            touch(letter,MotionEvent.ACTION_DOWN,50,100);touch(letter,MotionEvent.ACTION_MOVE,50,5);check(bubble().equals("1"),"swipe preview updates before release");check(symbols.get()==0,"symbol waits for release");
            touch(letter,MotionEvent.ACTION_UP,50,5);check(symbols.get()==1&&letters.get()==1,"up swipe emits only symbol");
            touch(letter,MotionEvent.ACTION_DOWN,50,100);touch(letter,MotionEvent.ACTION_MOVE,50,5);touch(letter,MotionEvent.ACTION_MOVE,50,100);check(bubble().equals("q"),"return to origin restores letter preview");touch(letter,MotionEvent.ACTION_UP,50,100);check(letters.get()==2,"return to origin emits letter");
            touch(letter,MotionEvent.ACTION_DOWN,50,10);touch(letter,MotionEvent.ACTION_UP,50,110);check(letters.get()==3,"wrong direction emits letter");
            style.swipeDirection=1;touch(letter,MotionEvent.ACTION_DOWN,50,10);touch(letter,MotionEvent.ACTION_MOVE,50,110);check(bubble().equals("1"),"down swipe previews symbol");touch(letter,MotionEvent.ACTION_UP,50,110);check(symbols.get()==2,"down swipe emits symbol");
            touch(letter,MotionEvent.ACTION_DOWN,50,70);SystemClock.sleep(600);check(letters.get()==3&&symbols.get()==2,"holding a letter does not commit early or select symbol");touch(letter,MotionEvent.ACTION_UP,50,70);check(letters.get()==4,"held letter commits on release");
            touch(letter,MotionEvent.ACTION_DOWN,50,70);touch(letter,MotionEvent.ACTION_CANCEL,50,70);check(letters.get()==4&&bubble().isEmpty(),"cancel emits nothing and dismisses popup");
            checkOwnerPointer();
            touch(delete,MotionEvent.ACTION_DOWN,50,70);check(deleted.get()==0,"delete press waits");touch(delete,MotionEvent.ACTION_UP,50,70);check(deleted.get()==1,"delete tap removes one character");
            touch(delete,MotionEvent.ACTION_DOWN,50,70);SystemClock.sleep(620);int count=deleted.get();check(count>=4&&count<=6,"hold removes single characters at 75ms intervals");touch(delete,MotionEvent.ACTION_UP,50,70);check(deleted.get()==count,"release after hold does not add deletion");SystemClock.sleep(200);check(deleted.get()==count,"repeat stops on release");
            touch(delete,MotionEvent.ACTION_DOWN,50,70);SystemClock.sleep(410);touch(delete,MotionEvent.ACTION_CANCEL,50,70);count=deleted.get();SystemClock.sleep(180);check(deleted.get()==count,"repeat stops on cancellation");
            touch(delete,MotionEvent.ACTION_DOWN,50,70);instrumentation.runOnMainSync(()->page.removeView(delete));count=deleted.get();SystemClock.sleep(450);check(deleted.get()==count,"detach cancels hold timer");
            checkGeometry();checkPhoto();return passed;
        }finally{instrumentation.runOnMainSync(()->{Glass.cancelTouches(page);activity.finish();});}
    }
    private void checkPopupLocation()throws Exception{Field field=Glass.Key.class.getDeclaredField("bubble");field.setAccessible(true);TextView bubble=(TextView)field.get(letter);int[] bounds=new int[4];instrumentation.runOnMainSync(()->{int[] key=new int[2],popup=new int[2];letter.getLocationOnScreen(key);bubble.getLocationOnScreen(popup);bounds[0]=key[1];bounds[1]=popup[1];bounds[2]=bubble.getWidth();bounds[3]=bubble.getHeight();});
        check(bounds[1]+bounds[3]<=bounds[0],"preview above its key: "+java.util.Arrays.toString(bounds));check(bounds[2]==Ui.dp(activity,38)&&bounds[3]==Ui.dp(activity,46),"preview stays compact");}
    private void checkOwnerPointer(){touch(letter,MotionEvent.ACTION_DOWN,50,70);instrumentation.runOnMainSync(()->{
        MotionEvent.PointerProperties first=new MotionEvent.PointerProperties(),second=new MotionEvent.PointerProperties();first.id=0;second.id=1;first.toolType=second.toolType=MotionEvent.TOOL_TYPE_FINGER;
        MotionEvent.PointerCoords one=new MotionEvent.PointerCoords(),two=new MotionEvent.PointerCoords();one.x=50;one.y=70;two.x=100;two.y=70;one.pressure=two.pressure=1;
        long now=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,MotionEvent.ACTION_POINTER_UP,2,new MotionEvent.PointerProperties[]{first,second},new MotionEvent.PointerCoords[]{one,two},0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);letter.dispatchTouchEvent(event);event.recycle();
    });check(letters.get()==5,"owner finger release commits once during multi-touch");touch(letter,MotionEvent.ACTION_UP,100,70);check(letters.get()==5,"another finger release cannot commit the old key twice");}
    private void checkGeometry(){instrumentation.runOnMainSync(()->{
        KeyboardFrame preview=new KeyboardFrame(activity,style,true);LinearLayout keys=Ui.column(activity);View child=new View(activity);keys.addView(child,new LinearLayout.LayoutParams(-1,Ui.dp(activity,200)));preview.addView(keys);page.addView(preview);
        int w=Ui.dp(activity,350);preview.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),0);preview.layout(0,0,w,preview.getMeasuredHeight());RectF before=preview.keyboardBounds();preview.setEditing(true);
        touch(preview,MotionEvent.ACTION_DOWN,before.centerX(),before.centerY());touch(preview,MotionEvent.ACTION_MOVE,before.centerX(),before.centerY()-Ui.dp(activity,50));touch(preview,MotionEvent.ACTION_UP,before.centerX(),before.centerY()-Ui.dp(activity,50));check(style.lift==50,"drag updates vertical position without numeric input");
        preview.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),0);preview.layout(0,0,w,preview.getMeasuredHeight());RectF handle=preview.keyboardBounds();float x=handle.right-Ui.dp(activity,10),y=handle.bottom-Ui.dp(activity,10);
        touch(preview,MotionEvent.ACTION_DOWN,x,y);touch(preview,MotionEvent.ACTION_MOVE,x-Ui.dp(activity,40),y+Ui.dp(activity,60));touch(preview,MotionEvent.ACTION_UP,x-Ui.dp(activity,40),y+Ui.dp(activity,60));check(style.width<1&&style.height>46,"corner handle changes width and height graphically");
        style.width=.85f;style.horizontal=1;style.lift=50;preview.setStyle(style);preview.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),0);preview.layout(0,0,w,preview.getMeasuredHeight());RectF edited=preview.keyboardBounds();
        check(Math.abs(edited.right-w)<1&&Math.abs(edited.width()-w*.85f)<2,"narrow keyboard remains within editor bounds");
        KeyboardFrame live=new KeyboardFrame(activity,style,false);LinearLayout liveKeys=Ui.column(activity);liveKeys.addView(new View(activity),new LinearLayout.LayoutParams(-1,Ui.dp(activity,200)));live.addView(liveKeys);live.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),0);live.layout(0,0,w,live.getMeasuredHeight());RectF real=live.keyboardBounds();
        check(Math.abs(real.width()-edited.width())<1&&Math.abs(real.left-edited.left)<1,"live keyboard and editor share width and horizontal geometry");check(live.getHeight()-real.bottom==Ui.dp(activity,50),"live layout reserves requested bottom offset");
    });}
    private void checkPhoto()throws Exception{
        Context isolated=new ContextWrapper(instrumentation.getTargetContext()){
            @Override public File getFilesDir(){return new File(getCacheDir(),"keyboard-checks");}
            @Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("keyboard_checks_"+name,mode);}
        };
        check(style.save(isolated),"geometry persisted");KeyboardStyle loaded=KeyboardStyle.load(isolated);check(loaded.lift==50&&loaded.width==.85f&&loaded.horizontal==1,"geometry survives a new style instance");
        Bitmap bitmap=Bitmap.createBitmap(2400,1200,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xff668899);File source=new File(isolated.getCacheDir(),"keyboard-photo-test.png");try(FileOutputStream out=new FileOutputStream(source)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
        String name=BackgroundPhoto.importPhoto(isolated,Uri.fromFile(source));Bitmap imported=BackgroundPhoto.bitmap(isolated,name);check(imported!=null&&imported.getWidth()<=1600&&imported.getHeight()<=1600,"large photo is bounded before keyboard display");
        source.delete();check(BackgroundPhoto.bitmap(isolated,name)!=null,"import survives original source removal");check(BackgroundPhoto.bitmap(isolated,"../../outside.jpg")==null,"background names stay inside app-owned directory");BackgroundPhoto.discard(isolated,name);
    }
}
