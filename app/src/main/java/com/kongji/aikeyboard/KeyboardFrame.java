package com.kongji.aikeyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/** One geometry model for preview and IME. Layout bounds are also touch bounds. */
final class KeyboardFrame extends ViewGroup {
    private KeyboardStyle style;private final boolean editor;private boolean editing,dragging,resizing;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final RectF box=new RectF();
    private float startX,startY,startWidth,startHorizontal;private int startLift,startHeight;
    KeyboardFrame(Context context,KeyboardStyle value,boolean editor){super(context);style=value;this.editor=editor;setWillNotDraw(false);setClipChildren(false);setContentDescription(Language.text(context,"键盘位置预览"));}
    void setStyle(KeyboardStyle value){style=value;requestLayout();invalidate();}
    void setEditing(boolean value){editing=value;Glass.cancelTouches(this);requestLayout();invalidate();}
    boolean editing(){return editing;}
    RectF keyboardBounds(){return new RectF(box);}
    @Override protected void onMeasure(int w,int h){
        int width=MeasureSpec.getSize(w),childWidth=Math.round(width*KeyboardStyle.limit(style.width,.82f,1));
        if(getChildCount()==0){setMeasuredDimension(width,0);return;}
        View child=getChildAt(0);child.measure(MeasureSpec.makeMeasureSpec(childWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));
        setMeasuredDimension(width,child.getMeasuredHeight()+Ui.dp(getContext(),editor&&editing?112:style.lift));
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){if(getChildCount()==0)return;View child=getChildAt(0);
        int x=Math.round((getWidth()-child.getMeasuredWidth())*KeyboardStyle.limit(style.horizontal,0,1));
        int y=getHeight()-child.getMeasuredHeight()-Ui.dp(getContext(),KeyboardStyle.clamp(style.lift,0,88));
        child.layout(x,y,x+child.getMeasuredWidth(),y+child.getMeasuredHeight());box.set(child.getLeft(),child.getTop(),child.getRight(),child.getBottom());
    }
    @Override protected void dispatchDraw(Canvas canvas){super.dispatchDraw(canvas);if(editor&&editing){
        paint.setColor(style.ink());paint.setStrokeWidth(Ui.dp(getContext(),1));paint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(box,Ui.dp(getContext(),8),Ui.dp(getContext(),8),paint);paint.setStyle(Paint.Style.FILL);
        float radius=Ui.dp(getContext(),10);canvas.drawCircle(box.right-radius,box.bottom-radius,radius,paint);
        paint.setColor(style.base());paint.setStrokeWidth(Ui.dp(getContext(),1.5f));float x=box.right-radius,y=box.bottom-radius;canvas.drawLine(x-4,y+4,x+4,y-4,paint);
        paint.setColor(style.ink());canvas.drawRoundRect(box.centerX()-Ui.dp(getContext(),15),box.top-Ui.dp(getContext(),10),box.centerX()+Ui.dp(getContext(),15),box.top-Ui.dp(getContext(),7),2,2,paint);
    }}
    @Override public boolean onInterceptTouchEvent(MotionEvent event){return editor&&editing;}
    @Override public boolean onTouchEvent(MotionEvent e){if(!editing)return false;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){startX=e.getX();startY=e.getY();startWidth=style.width;startHorizontal=style.horizontal;startLift=style.lift;startHeight=style.height;
            resizing=Math.abs(startX-box.right)<Ui.dp(getContext(),42)&&Math.abs(startY-box.bottom)<Ui.dp(getContext(),42);
            dragging=resizing||box.contains(startX,startY)||Math.abs(startY-box.top)<Ui.dp(getContext(),24);if(dragging)getParent().requestDisallowInterceptTouchEvent(true);return dragging;}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&dragging){float dx=e.getX()-startX,dy=e.getY()-startY;
            if(resizing){style.width=KeyboardStyle.limit(startWidth+dx/Math.max(1,getWidth()),.82f,1);style.height=KeyboardStyle.clamp(startHeight+Math.round(dy/getResources().getDisplayMetrics().density/4),40,64);updateRowHeights();}
            else{style.lift=KeyboardStyle.clamp(startLift-Math.round(dy/getResources().getDisplayMetrics().density),0,88);float slack=getWidth()*(1-style.width);if(slack>1)style.horizontal=KeyboardStyle.limit(startHorizontal+dx/slack,0,1);}
            requestLayout();invalidate();return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){boolean used=dragging;dragging=false;getParent().requestDisallowInterceptTouchEvent(false);performClick();return used;}
        return dragging;
    }
    @Override public boolean performClick(){return super.performClick();}
    private void updateRowHeights(){if(getChildCount()==0||!(getChildAt(0) instanceof LinearLayout rows))return;
        for(int i=0;i<rows.getChildCount();i++){if(rows.getChildAt(i) instanceof HandwritingPanel panel){panel.resize(style.height);continue;}if(rows.getChildAt(i) instanceof LinearLayout row)for(int j=0;j<row.getChildCount();j++){
            View key=row.getChildAt(j);ViewGroup.LayoutParams params=key.getLayoutParams();if(params.height>=Ui.dp(getContext(),40)){params.height=Ui.dp(getContext(),style.mode==1&&i<rows.getChildCount()-1?Math.max(48,style.height):style.height);key.setLayoutParams(params);}
        }}
    }
}
