package com.kongji.aikeyboard;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

/** Real multi-pointer events through the same frame used by the IME. */
final class OverlapChecks {
    private final Instrumentation instrumentation;
    private int passed; private KeyboardFrame frame; private final List<String> output=new ArrayList<>();
    private final float[] x=new float[3]; private float y; private long down;
    OverlapChecks(Instrumentation value){instrumentation=value;}
    private void check(boolean value,String label){if(!value)throw new AssertionError(label+": "+output);passed++;}
    private void event(int action,int[] ids,float... positions){
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[ids.length];
        for(int i=0;i<ids.length;i++){properties[i]=new MotionEvent.PointerProperties();properties[i].id=ids[i];properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i]=new MotionEvent.PointerCoords();coords[i].x=positions[i*2];coords[i].y=positions[i*2+1];coords[i].pressure=1;coords[i].size=1;}
        MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,ids.length,properties,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);frame.dispatchTouchEvent(e);e.recycle();
    }
    int run(){Activity a=instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Throwable[] error={null};
        instrumentation.runOnMainSync(()->{try{KeyboardStyle s=new KeyboardStyle();frame=new KeyboardFrame(a,s,false);LinearLayout row=Ui.row(a);frame.addView(row);for(String key:List.of("q","w","e")){Button b=Glass.button(a,s,key,false,false,v->output.add(key));Glass.release(b);KeyboardLayout.add(row,s,b,1,50);}a.setContentView(frame);int width=Ui.dp(a,300);frame.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));frame.layout(0,0,width,frame.getMeasuredHeight());for(int i=0;i<3;i++)x[i]=(row.getChildAt(i).getLeft()+row.getChildAt(i).getRight())/2f;y=frame.getHeight()/2f;down=SystemClock.uptimeMillis();
            event(MotionEvent.ACTION_DOWN,new int[]{7},x[0],y);check(output.isEmpty(),"first press remains pending");
            event(MotionEvent.ACTION_POINTER_DOWN|(1<<8),new int[]{7,11},x[0],y,x[1],y);check(output.equals(List.of("q")),"second press must immediately confirm first key");
            event(MotionEvent.ACTION_POINTER_UP,new int[]{7,11},x[0],y,x[1],y);check(output.equals(List.of("q")),"old finger release must not duplicate first key");
            event(MotionEvent.ACTION_UP,new int[]{11},x[1],y);check(output.equals(List.of("q","w")),"second release confirms second key");
            output.clear();event(MotionEvent.ACTION_DOWN,new int[]{7},x[0],y);event(MotionEvent.ACTION_POINTER_DOWN|(1<<8),new int[]{7,11},x[0],y,x[1],y);event(MotionEvent.ACTION_POINTER_DOWN|(2<<8),new int[]{7,11,19},x[0],y,x[1],y,x[2],y);check(output.equals(List.of("q","w")),"third press confirms second before release");event(MotionEvent.ACTION_POINTER_UP|(2<<8),new int[]{7,11,19},x[0],y,x[1],y,x[2],y);event(MotionEvent.ACTION_POINTER_UP|(1<<8),new int[]{7,11},x[0],y,x[1],y);event(MotionEvent.ACTION_UP,new int[]{7},x[0],y);check(output.equals(List.of("q","w","e")),"reverse finger release preserves press order");
            output.clear();event(MotionEvent.ACTION_DOWN,new int[]{7},x[0],y);event(MotionEvent.ACTION_POINTER_DOWN|(1<<8),new int[]{7,11},x[0],y,x[1],y);event(MotionEvent.ACTION_CANCEL,new int[]{7,11},x[0],y,x[1],y);check(output.equals(List.of("q")),"cancel discards only pending key");
            for(int i=0;i<100;i++){output.clear();event(MotionEvent.ACTION_DOWN,new int[]{7},x[0],y);event(MotionEvent.ACTION_POINTER_DOWN|(1<<8),new int[]{7,11},x[0],y,x[1],y);event(MotionEvent.ACTION_POINTER_UP|(1<<8),new int[]{7,11},x[0],y,x[1],y);event(MotionEvent.ACTION_UP,new int[]{7},x[0],y);check(output.equals(List.of("q","w")),"fast overlap iteration "+i);}
            output.clear();row.getChildAt(0).setOnClickListener(v->{output.add("Q");row.removeAllViews();for(String key:List.of("q","w","e")){Button b=Glass.button(a,s,key,false,false,v2->output.add(key));Glass.release(b);KeyboardLayout.add(row,s,b,1,50);}});event(MotionEvent.ACTION_DOWN,new int[]{7},x[0],y);event(MotionEvent.ACTION_POINTER_DOWN|(1<<8),new int[]{7,11},x[0],y,x[1],y);event(MotionEvent.ACTION_POINTER_UP|(1<<8),new int[]{7,11},x[0],y,x[1],y);event(MotionEvent.ACTION_UP,new int[]{7},x[0],y);check(output.equals(List.of("Q","w")),"confirmation that rebuilds keys preserves the second hit target");
        }catch(Throwable failure){error[0]=failure;}});instrumentation.runOnMainSync(a::finish);if(error[0]!=null)throw new AssertionError(error[0]);return passed;}
}
