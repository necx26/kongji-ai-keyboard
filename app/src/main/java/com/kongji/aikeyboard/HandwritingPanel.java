package com.kongji.aikeyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.recognition.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Stroke recognition stays on device after an explicit model download. */
final class HandwritingPanel extends LinearLayout implements AutoCloseable {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final TextView status;
    private final Button recognize;
    private final Pad pad;
    private final Consumer<List<String>> results;
    private final Runnable delayed=this::recognize;
    private DigitalInkRecognitionModel model;
    private DigitalInkRecognizer recognizer;
    private boolean ready,closed;
    private int revision;

    HandwritingPanel(Context context,KeyboardStyle style,boolean chinese,Consumer<List<String>> results){
        super(context);this.results=results;setOrientation(VERTICAL);
        status=Ui.text(context,"在下面手写，停笔后显示候选",12,style.muted());addView(status);
        pad=new Pad(context,style);pad.setContentDescription(Language.text(HandwritingPanel.this.getContext(),"手写区域"));addView(pad,new LayoutParams(-1,Ui.dp(context,180)));
        LinearLayout controls=Ui.row(context);
        recognize=Ui.button(context,"识别",true,v->recognize());Ui.addButton(controls,recognize,1,38);
        Ui.addButton(controls,Ui.button(context,"撤销一笔",false,v->{pad.undo();changed();}),1,38);
        Ui.addButton(controls,Ui.button(context,"清除手写",false,v->clear()),1,38);
        Button download=Ui.button(context,"下载手写模型",false,v->download());Ui.addButton(controls,download,1.5f,38);addView(controls);
        try{
            var id=DigitalInkRecognitionModelIdentifier.fromLanguageTag(chinese?"zh-Hani-CN":"en-US");
            if(id==null)throw new IllegalStateException("No handwriting model");
            model=DigitalInkRecognitionModel.builder(id).build();recognizer=DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build());
            RemoteModelManager.getInstance().isModelDownloaded(model).addOnSuccessListener(found->{if(closed)return;ready=found;recognize.setEnabled(ready);status.setText(Language.text(getContext(),found?"在下面手写，停笔后显示候选":"首次使用请下载手写模型，下载后可离线识别"));});
        }catch(Exception e){status.setText(Language.text(HandwritingPanel.this.getContext(),"手写模型初始化失败，请重新打开键盘"));recognize.setEnabled(false);}
    }

    private void download(){if(model==null||closed)return;status.setText(Language.text(HandwritingPanel.this.getContext(),"正在下载手写模型，请保持网络连接"));
        RemoteModelManager.getInstance().download(model,new DownloadConditions.Builder().build()).addOnSuccessListener(v->{if(closed)return;ready=true;recognize.setEnabled(true);status.setText(Language.text(HandwritingPanel.this.getContext(),"模型已就绪，可以离线手写"));recognize();}).addOnFailureListener(e->{if(!closed)status.setText(Language.text(HandwritingPanel.this.getContext(),"手写模型下载失败，请检查网络后重试"));});
    }
    private void changed(){revision++;results.accept(List.of());handler.removeCallbacks(delayed);if(ready&&!pad.strokes.isEmpty())handler.postDelayed(delayed,800);}
    private void recognize(){handler.removeCallbacks(delayed);if(!ready||closed||pad.strokes.isEmpty()||pad.current!=null)return;int captured=revision;
        Ink.Builder ink=Ink.builder();for(Ink.Stroke stroke:pad.strokes)ink.addStroke(stroke);
        var area=new WritingArea(pad.getWidth(),pad.getHeight());var context=RecognitionContext.builder().setPreContext("").setWritingArea(area).build();
        recognizer.recognize(ink.build(),context).addOnSuccessListener(result->{if(closed||captured!=revision)return;List<String> choices=new ArrayList<>();for(var candidate:result.getCandidates()){String text=candidate.getText().trim();if(!text.isEmpty()&&!choices.contains(text))choices.add(text);if(choices.size()==12)break;}results.accept(choices);status.setText(Language.text(getContext(),choices.isEmpty()?"没有识别到文字，请重写":"点选候选文字确认输入"));}).addOnFailureListener(e->{if(!closed&&captured==revision)status.setText(Language.text(HandwritingPanel.this.getContext(),"手写识别失败，请重试"));});
    }
    void clear(){pad.strokes.clear();pad.paths.clear();pad.current=null;pad.live=null;pad.invalidate();changed();if(ready&&!closed)status.setText(Language.text(getContext(),"在下面手写，停笔后显示候选"));}
    @Override public void close(){if(closed)return;closed=true;revision++;handler.removeCallbacksAndMessages(null);if(recognizer!=null)recognizer.close();}

    private final class Pad extends View {
        final List<Ink.Stroke> strokes=new ArrayList<>();final List<Path> paths=new ArrayList<>();final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG),guide=new Paint(Paint.ANTI_ALIAS_FLAG);
        Ink.Stroke.Builder current;Path live;
        Pad(Context c,KeyboardStyle style){super(c);setBackground(Ui.rounded(c,KeyboardStyle.alpha(style.keyColor(),170),14));pen.setColor(style.ink());pen.setStrokeWidth(Ui.dp(c,3));pen.setStrokeCap(Paint.Cap.ROUND);pen.setStrokeJoin(Paint.Join.ROUND);pen.setStyle(Paint.Style.STROKE);guide.setColor(KeyboardStyle.alpha(style.muted(),60));guide.setStrokeWidth(Ui.dp(c,1));}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.drawLine(getWidth()/2f,0,getWidth()/2f,getHeight(),guide);canvas.drawLine(0,getHeight()/2f,getWidth(),getHeight()/2f,guide);for(Path p:paths)canvas.drawPath(p,pen);if(live!=null)canvas.drawPath(live,pen);}
        private void point(float x,float y,long time){current.addPoint(Ink.Point.create(Math.max(0,Math.min(getWidth(),x)),Math.max(0,Math.min(getHeight(),y)),time));}
        @Override public boolean onTouchEvent(MotionEvent event){if(closed)return false;
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN -> {handler.removeCallbacks(delayed);revision++;results.accept(List.of());getParent().requestDisallowInterceptTouchEvent(true);current=Ink.Stroke.builder();live=new Path();live.moveTo(event.getX(),event.getY());point(event.getX(),event.getY(),event.getEventTime());}
                case MotionEvent.ACTION_MOVE -> {if(current!=null){for(int i=0;i<event.getHistorySize();i++){point(event.getHistoricalX(i),event.getHistoricalY(i),event.getHistoricalEventTime(i));live.lineTo(event.getHistoricalX(i),event.getHistoricalY(i));}point(event.getX(),event.getY(),event.getEventTime());live.lineTo(event.getX(),event.getY());}}
                case MotionEvent.ACTION_UP -> {if(current!=null){point(event.getX(),event.getY(),event.getEventTime());live.lineTo(event.getX(),event.getY());strokes.add(current.build());paths.add(live);current=null;live=null;changed();}getParent().requestDisallowInterceptTouchEvent(false);performClick();}
                case MotionEvent.ACTION_CANCEL -> {current=null;live=null;revision++;handler.removeCallbacks(delayed);getParent().requestDisallowInterceptTouchEvent(false);}
                default -> {return true;}
            }invalidate();return true;
        }
        void undo(){if(!strokes.isEmpty()){strokes.remove(strokes.size()-1);paths.remove(paths.size()-1);invalidate();}}
        @Override public boolean performClick(){return super.performClick();}
    }
}
