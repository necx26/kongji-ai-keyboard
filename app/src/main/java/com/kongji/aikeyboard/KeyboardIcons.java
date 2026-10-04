package com.kongji.aikeyboard;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Small vector icons matching the supplied four-row keyboard. */
final class KeyboardIcons {
    static void draw(Canvas canvas,String name,float x,float y,float size,int color){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStrokeWidth(size*.075f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
        int save=canvas.save();canvas.translate(x-size/2,y-size/2);Path path=new Path();
        switch(name){
            case "shift"->{path.moveTo(.1f*size,.46f*size);path.lineTo(.5f*size,.08f*size);path.lineTo(.9f*size,.46f*size);path.lineTo(.68f*size,.46f*size);path.lineTo(.68f*size,.86f*size);path.lineTo(.32f*size,.86f*size);path.lineTo(.32f*size,.46f*size);path.close();canvas.drawPath(path,p);}
            case "delete"->{path.moveTo(.3f*size,.14f*size);path.lineTo(.93f*size,.14f*size);path.lineTo(.93f*size,.86f*size);path.lineTo(.3f*size,.86f*size);path.lineTo(.04f*size,.5f*size);path.close();canvas.drawPath(path,p);canvas.drawLine(.48f*size,.34f*size,.72f*size,.66f*size,p);canvas.drawLine(.48f*size,.66f*size,.72f*size,.34f*size,p);}
            case "wave"->{float[] h={.16f,.38f,.65f,.95f,.48f,.28f,.13f};for(int i=0;i<h.length;i++){float xx=(.1f+i*.13f)*size;canvas.drawLine(xx,(.5f-h[i]/2)*size,xx,(.5f+h[i]/2)*size,p);}}
            case "language"->{p.setStyle(Paint.Style.FILL);p.setTextSize(size*.43f);canvas.drawText("中",.05f*size,.43f*size,p);canvas.drawText("英",.5f*size,.94f*size,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(size*.045f);path.moveTo(.56f*size,.13f*size);path.cubicTo(.91f*size,.1f*size,.92f*size,.14f*size,.92f*size,.42f*size);canvas.drawPath(path,p);canvas.drawLine(.92f*size,.42f*size,.81f*size,.31f*size,p);canvas.drawLine(.92f*size,.42f*size,1.0f*size,.29f*size,p);path.reset();path.moveTo(.39f*size,.85f*size);path.cubicTo(.06f*size,.86f*size,.08f*size,.82f*size,.08f*size,.58f*size);canvas.drawPath(path,p);canvas.drawLine(.08f*size,.58f*size,0,.7f*size,p);canvas.drawLine(.08f*size,.58f*size,.2f*size,.7f*size,p);}
            case "keyboard"->{canvas.drawRoundRect(new RectF(.04f*size,.05f*size,.96f*size,.95f*size),size*.14f,size*.14f,p);p.setStyle(Paint.Style.FILL);for(int r=0;r<2;r++)for(int c=0;c<3;c++)canvas.drawRect((.25f+c*.2f)*size,(.26f+r*.2f)*size,(.34f+c*.2f)*size,(.35f+r*.2f)*size,p);canvas.drawRoundRect(new RectF(.24f*size,.7f*size,.77f*size,.77f*size),2,2,p);}
            case "clipboard"->{canvas.drawRoundRect(new RectF(.07f*size,.05f*size,.82f*size,.8f*size),size*.12f,size*.12f,p);canvas.drawRoundRect(new RectF(.27f*size,.25f*size,.97f*size,.98f*size),size*.12f,size*.12f,p);canvas.drawLine(.42f*size,.51f*size,.81f*size,.51f*size,p);canvas.drawLine(.42f*size,.73f*size,.81f*size,.73f*size,p);}
        }
        canvas.restoreToCount(save);
    }
}
