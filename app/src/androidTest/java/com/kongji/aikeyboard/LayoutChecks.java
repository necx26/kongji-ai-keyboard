package com.kongji.aikeyboard;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

/** Attached geometry: reference rows, nearest-gap dispatch and haptic event coverage. */
final class LayoutChecks {
    private final Instrumentation instrumentation;private int passed;private Activity activity;private KeyboardFrame frame;private LinearLayout rows;private final List<String> events=new ArrayList<>();
    LayoutChecks(Instrumentation value){instrumentation=value;}
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);passed++;}
    private void touch(View view,int action,float x,float y){long now=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,action,x,y,0);view.dispatchTouchEvent(event);event.recycle();}
    int run(){activity=instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Throwable[] error=new Throwable[1];
        instrumentation.runOnMainSync(()->{try{verify();}catch(Throwable failure){error[0]=failure;}});if(error[0]==null)verifyToolRow();instrumentation.runOnMainSync(activity::finish);if(error[0]!=null)throw new AssertionError(error[0]);return passed;}
    private void verifyToolRow(){KeyRow[] row=new KeyRow[1];float[] gap=new float[2];instrumentation.runOnMainSync(()->{events.clear();row[0]=new KeyRow(activity);KeyboardStyle style=new KeyboardStyle();Ui.addButton(row[0],Glass.button(activity,style,"first",false,false,v->events.add("first")),1,40);Ui.addButton(row[0],Glass.button(activity,style,"second",false,false,v->events.add("second")),1,40);activity.setContentView(row[0]);int width=Ui.dp(activity,300),height=Ui.dp(activity,40);row[0].measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));row[0].layout(0,0,width,height);gap[0]=(row[0].getChildAt(0).getRight()+row[0].getChildAt(1).getLeft())/2f;gap[1]=height/2f;touch(row[0],MotionEvent.ACTION_DOWN,gap[0]-1,gap[1]);check(events.isEmpty(),"tool-row gap press waits for release");touch(row[0],MotionEvent.ACTION_UP,gap[0]-1,gap[1]);});instrumentation.waitForIdleSync();check(events.equals(List.of("first")),"left tool-row gap activates nearest ordinary button");
        instrumentation.runOnMainSync(()->{events.clear();gap[0]=(row[0].getChildAt(0).getRight()+row[0].getChildAt(1).getLeft())/2f;gap[1]=(row[0].getChildAt(0).getTop()+row[0].getChildAt(0).getBottom())/2f;touch(row[0],MotionEvent.ACTION_DOWN,gap[0]+1,gap[1]);touch(row[0],MotionEvent.ACTION_UP,gap[0]+1,gap[1]);});instrumentation.waitForIdleSync();check(events.equals(List.of("second")),"right tool-row gap activates nearest ordinary button: "+events);
        instrumentation.runOnMainSync(()->{events.clear();touch(row[0],MotionEvent.ACTION_DOWN,gap[0],gap[1]);touch(row[0],MotionEvent.ACTION_CANCEL,gap[0],gap[1]);});instrumentation.waitForIdleSync();check(events.isEmpty(),"tool-row cancel does not activate a button");}
    private void verify(){KeyboardStyle style=new KeyboardStyle();rows=Ui.column(activity);frame=new KeyboardFrame(activity,style,false);frame.addView(rows);activity.setContentView(frame);
        KeyboardLayout.build(rows,style,true,false,false,new KeyboardLayout.Actions(){public void key(String value){events.add(value);}public void shift(){events.add("shift");}public void delete(){events.add("delete");}public void deleteRecent(){events.add("recent");}public void undo(){events.add("undo");}public void symbols(){events.add("symbols");}public void language(){events.add("language");}public void switchIme(boolean picker){events.add("switch");}public void space(){events.add("space");}public void punctuation(){}public void enter(){events.add("enter");}public void clipboard(){events.add("clipboard");}});
        int width=Ui.dp(activity,411);frame.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));frame.layout(0,0,width,frame.getMeasuredHeight());
        check(rows.getChildCount()==5,"four main rows and one footer; no extra quick-symbol row");
        List<Button> first=buttons(rows.getChildAt(0)),second=buttons(rows.getChildAt(1)),third=buttons(rows.getChildAt(2)),bottom=buttons(rows.getChildAt(3));
        check(first.size()==10&&second.size()==9&&third.size()==9&&bottom.size()==5,"reference row key counts");
        check(second.get(0).getLeft()>first.get(0).getLeft()&&second.get(8).getRight()<first.get(9).getRight(),"second row is symmetrically inset");
        check(third.get(0).getWidth()>third.get(1).getWidth()&&third.get(8).getWidth()>third.get(7).getWidth(),"shift and delete keys are wider than letters");
        check(bottom.get(2).getContentDescription().toString().equals("空格")&&bottom.get(2).getWidth()>bottom.get(0).getWidth()&&bottom.get(4).getText().toString().equals("换行"),"reference bottom order and large central space key");
        check(((Glass.Key)second.get(0)).hint.equals("-")&&((Glass.Key)second.get(8)).hint.equals("”")&&((Glass.Key)third.get(7)).hint.equals("…"),"reference secondary symbols match the photo");
        Rect q=rect(first.get(0)),w=rect(first.get(1));float middle=(q.right+w.left)/2f;tap(middle-2,q.exactCenterY());check(events.remove(0).equals("q"),"left side of q/w gap outputs q");tap(middle+2,q.exactCenterY());check(events.remove(0).equals("w"),"right side of q/w gap outputs w");
        Rect a=rect(second.get(0));tap(1,a.exactCenterY());check(events.remove(0).equals("a"),"inset row's outer gap belongs to a");
        Rect e=rect(first.get(2)),d=rect(second.get(2));tap(e.exactCenterX(),e.bottom+1);check(events.remove(0).equals("e"),"vertical gap near the upper key goes to that key");
        boolean covered=true;int dispatched=0;for(int y=1;y<frame.getHeight();y+=Ui.dp(activity,9))for(int x=1;x<width;x+=Ui.dp(activity,9)){if(frame.nearestKey(x,y)==null){covered=false;continue;}int old=events.size();tap(x,y);if(events.size()!=old+1)covered=false;dispatched++;}check(covered&&dispatched>1000,"whole keyboard grid has no dead gaps or duplicate output: "+dispatched);events.clear();
        touch(frame,MotionEvent.ACTION_DOWN,middle-2,q.exactCenterY());touch(frame,MotionEvent.ACTION_MOVE,w.exactCenterX(),w.exactCenterY());touch(frame,MotionEvent.ACTION_UP,w.exactCenterX(),w.exactCenterY());check(events.size()==1&&events.get(0).equals("q"),"gap press keeps the first chosen key while sliding sideways");events.clear();
        touch(frame,MotionEvent.ACTION_DOWN,middle,q.exactCenterY());touch(frame,MotionEvent.ACTION_CANCEL,middle,q.exactCenterY());check(events.isEmpty(),"cancelled gap touch produces no output");
        HapticKey spy=new HapticKey(activity,style);spy.setOnClickListener(v->{});spy.layout(0,0,100,100);touch(spy,MotionEvent.ACTION_DOWN,50,50);touch(spy,MotionEvent.ACTION_UP,50,50);check(spy.feedback==1,"ordinary toolbar-style button receives one haptic on press");spy.keyboard=true;style.haptic=false;touch(spy,MotionEvent.ACTION_DOWN,50,50);touch(spy,MotionEvent.ACTION_MOVE,70,50);touch(spy,MotionEvent.ACTION_UP,70,50);check(spy.feedback==2,"keyboard feedback remains enabled and is not duplicated on release");spy.setEnabled(false);touch(spy,MotionEvent.ACTION_DOWN,50,50);check(spy.feedback==2,"disabled controls do not vibrate");
    }
    private Rect rect(View view){Rect value=new Rect(0,0,view.getWidth(),view.getHeight());frame.offsetDescendantRectToMyCoords(view,value);return value;}
    private List<Button> buttons(View view){List<Button> list=new ArrayList<>();if(view instanceof Button b)list.add(b);else if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)list.addAll(buttons(group.getChildAt(i)));return list;}
    private void tap(float x,float y){touch(frame,MotionEvent.ACTION_DOWN,x,y);touch(frame,MotionEvent.ACTION_UP,x,y);}
    private static final class HapticKey extends Glass.Key{int feedback;HapticKey(Activity activity,KeyboardStyle style){super(activity,style);}@Override public boolean performHapticFeedback(int effect,int flags){feedback++;return true;}}
}
