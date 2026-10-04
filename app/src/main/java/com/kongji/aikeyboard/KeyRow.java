package com.kongji.aikeyboard;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

/** Gap targets for tool/candidate rows; ancestor scrolling can still cancel the touch. */
final class KeyRow extends LinearLayout {
    private View owner;private final Rect bounds=new Rect();
    KeyRow(Context context){super(context);setOrientation(HORIZONTAL);setBaselineAligned(false);}
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){owner=null;float distance=Float.MAX_VALUE;
            for(int i=0;i<getChildCount();i++){View key=getChildAt(i);if(!(key instanceof Glass.Key)||key.getVisibility()!=VISIBLE||!key.isEnabled())continue;
                float dx=Math.max(Math.max(key.getLeft()-event.getX(),0),event.getX()-key.getRight()),dy=Math.max(Math.max(key.getTop()-event.getY(),0),event.getY()-key.getBottom());float next=dx*dx+dy*dy;if(next<distance){distance=next;owner=key;}}
            if(owner!=null)bounds.set(owner.getLeft(),owner.getTop(),owner.getRight(),owner.getBottom());
        }
        if(owner==null)return super.dispatchTouchEvent(event);
        MotionEvent local=MotionEvent.obtain(event);local.offsetLocation(-bounds.left,-bounds.top);boolean used=owner.dispatchTouchEvent(local);local.recycle();
        if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)owner=null;
        return used;
    }
}
