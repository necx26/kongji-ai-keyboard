package com.kongji.aikeyboard;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppearanceActivity extends LocalizedActivity {
    private KeyboardStyle style;private LinearLayout preview;private KeyboardFrame frame;private HandwritingPanel previewInk;
    private boolean chinese=true,shift=false,symbols=false,editing=false,importing=false;
    private final ExecutorService photoWorker=Executors.newSingleThreadExecutor();
    private final ArrayList<String> pendingPhotos=new ArrayList<>();private String originalPhoto;private Button importButton;
    @Override public void onCreate(Bundle state){super.onCreate(state);style=KeyboardStyle.load(this);originalPhoto=style.photo;
        if(state!=null){style.palette=state.getInt("theme",style.palette);style.height=state.getInt("height",style.height);style.width=state.getFloat("width",style.width);
            style.lift=state.getInt("lift",style.lift);style.horizontal=state.getFloat("horizontal",style.horizontal);style.photo=state.getString("photo",style.photo);
            style.mode=state.getInt("mode",style.mode);style.symbolPosition=state.getInt("symbolPosition",style.symbolPosition);style.swipeDirection=state.getInt("swipeDirection",style.swipeDirection);
            style.quickSymbols=state.getBoolean("quickSymbols",style.quickSymbols);style.haptic=state.getBoolean("haptic",style.haptic);style.candidateSize=state.getInt("candidateSize",style.candidateSize);style.footerLayout=state.getInt("footerLayout",style.footerLayout);editing=state.getBoolean("editing");
            ArrayList<String> photos=state.getStringArrayList("pendingPhotos");if(photos!=null)pendingPhotos.addAll(photos);}
        buildPage();
    }
    private void buildPage(){
        if(previewInk!=null){previewInk.close();previewInk=null;}applyPageTheme(style);
        LinearLayout screen=Ui.column(this);screen.setBackgroundColor(Ui.bg(this));setContentView(screen);Ui.activityInsets(screen,0);
        LinearLayout header=Ui.row(this);int pad=Ui.dp(this,20);header.setPadding(pad,Ui.dp(this,8),pad,Ui.dp(this,8));
        Button back=Ui.button(this,"‹",false,v->finish());back.setContentDescription(Language.text(this,"‹ 返回设置"));Ui.addButton(header,back,.6f,44);
        TextView title=Ui.heading(this,"外观工作室",24);title.setGravity(android.view.Gravity.CENTER_VERTICAL);title.setPadding(Ui.dp(this,16),0,0,0);header.addView(title,new LinearLayout.LayoutParams(0,-1,3.4f));screen.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout content=Ui.column(this);content.setPadding(pad,0,pad,Ui.dp(this,24));scroll.addView(content);
        content.addView(Ui.text(this,"让每一次输入，都舒服一点。",12,Ui.MUTED));
        content.addView(Ui.text(this,"选择模式，顶部预览会立即更新；点击应用后用于实际输入。",12,Ui.MUTED));
        choices(content,KeyboardStyle.MODES,style.mode,value->{style.mode=value;symbols=false;renderPreview();});
        LinearLayout editTools=Ui.row(this);Button edit=Ui.button(this,editing?"结束编辑":"拖拽编辑",editing,v->{editing=!editing;frame.setEditing(editing);((Button)v).setText(Language.text(this,editing?"结束编辑":"拖拽编辑"));});
        Ui.addButton(editTools,edit,1,48);Ui.addButton(editTools,Ui.button(this,"恢复默认外观",false,v->new AlertDialog.Builder(this).setTitle(Language.text(this,"恢复默认外观？"))
            .setMessage(Language.text(this,"恢复标准尺寸和位置，点击应用后生效。")).setNegativeButton(Language.text(this,"取消"),null)
            .setPositiveButton(Language.text(this,"恢复"),(d,w)->{style.width=1;style.horizontal=.5f;style.height=50;style.lift=0;style.candidateSize=2;style.footerLayout=0;renderPreview();}).show()),1,48);content.addView(editTools);
        LinearLayout caption=Ui.row(this);caption.addView(Ui.text(this,"实时预览",12,Ui.MUTED),new LinearLayout.LayoutParams(0,-2,1));caption.addView(Ui.text(this,"点按体验手感",12,Ui.MUTED));content.addView(caption);
        preview=Ui.column(this);preview.setPadding(Ui.dp(this,5),Ui.dp(this,6),Ui.dp(this,5),Ui.dp(this,6));preview.setBackground(Glass.backdrop(this,style));content.addView(preview);

        content.addView(Ui.text(this,"拖动键盘移动，拖右下角调整大小",12,Ui.MUTED));Ui.rule(content);
        addCandidateControls(content);
        content.addView(Ui.heading(this,"底部按钮位置",18));choices(content,new String[]{"两侧","靠左","靠右"},style.footerLayout,value->{style.footerLayout=value;renderPreview();});Ui.rule(content);
        content.addView(Ui.heading(this,"主题氛围",18));choices(content,KeyboardStyle.NAMES,style.palette,value->{style.palette=value;style.accent=style.dark()?0xffeeeeee:0xff202020;buildPage();});
        importButton=Ui.button(this,"导入照片背景",false,v->{Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT);picker.setType("image/*");picker.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(picker,41);});
        content.addView(importButton,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));
        if(!style.photo.isEmpty())content.addView(Ui.navigation(this,"移除照片背景",v->{style.photo="";buildPage();}),new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));
        Ui.rule(content);LinearLayout symbolsPage=Ui.disclosure(content,"快捷符号",false);
        choices(symbolsPage,new String[]{"符号在上方","符号在下方"},style.symbolPosition,value->{style.symbolPosition=value;renderPreview();});
        choices(symbolsPage,new String[]{"上滑输入符号","下滑输入符号"},style.swipeDirection,value->{style.swipeDirection=value;renderPreview();});
        toggle(symbolsPage,"显示快捷符号栏",style.quickSymbols,value->{style.quickSymbols=value;renderPreview();});
        LinearLayout footer=Ui.row(this);footer.setPadding(pad,Ui.dp(this,8),pad,Ui.dp(this,10));
        Ui.addButton(footer,Ui.button(this,"应用到键盘",true,v->{if(persist()){Toast.makeText(this,Language.text(this,"外观已应用，再次调出键盘即可看到"),Toast.LENGTH_SHORT).show();finish();}}),1.3f,48);
        Ui.addButton(footer,Ui.button(this,"应用并试打",false,v->{if(persist()){startActivity(new Intent(this,PracticeActivity.class));finish();}}),1,48);screen.addView(footer);renderPreview();
    }
    private boolean persist(){if(importing){Toast.makeText(this,Language.text(this,"正在导入照片…"),Toast.LENGTH_SHORT).show();return false;}
        if(!style.save(this)){Toast.makeText(this,Language.text(this,"外观保存失败，请重试"),Toast.LENGTH_LONG).show();return false;}
        for(String photo:pendingPhotos)if(!photo.equals(style.photo))BackgroundPhoto.discard(this,photo);pendingPhotos.clear();
        if(!originalPhoto.equals(style.photo))BackgroundPhoto.discard(this,originalPhoto);originalPhoto=style.photo;return true;
    }
    private void addCandidateControls(LinearLayout content){
        content.addView(Ui.heading(this,"候选词大小",18));
        TextView selected=Ui.rawText(this,CandidateLayout.label(this,style.candidateSize),13,style.muted());content.addView(selected);
        LinearLayout sample=Ui.row(this);sample.setGravity(android.view.Gravity.CENTER_VERTICAL);content.addView(sample,new LinearLayout.LayoutParams(-1,Ui.dp(this,CandidateLayout.barHeight(style))));
        Runnable redraw=()->{sample.removeAllViews();sample.getLayoutParams().height=Ui.dp(this,CandidateLayout.barHeight(style));for(String value:new String[]{"你好","今天","一起出发"}){Button b=Glass.button(this,style,value,false,true,v->{});b.setTextSize(CandidateLayout.font(style));b.setSingleLine(true);Ui.addButton(sample,b,1,CandidateLayout.keyHeight(style));}sample.requestLayout();};redraw.run();
        android.widget.SeekBar slider=new android.widget.SeekBar(this);slider.setMax(4);slider.setProgress(style.candidateSize);slider.setContentDescription(Language.text(this,"候选词大小"));slider.setProgressTintList(android.content.res.ColorStateList.valueOf(style.ink()));slider.setThumbTintList(android.content.res.ColorStateList.valueOf(style.ink()));slider.setPadding(Ui.dp(this,12),0,Ui.dp(this,12),0);content.addView(slider,new LinearLayout.LayoutParams(-1,Ui.dp(this,44)));
        LinearLayout labels=Ui.row(this);for(int i=0;i<5;i++){int index=i;TextView label=Ui.rawText(this,CandidateLayout.label(this,i),12,style.muted());label.setGravity(android.view.Gravity.CENTER);label.setOnClickListener(v->slider.setProgress(index));labels.addView(label,new LinearLayout.LayoutParams(0,Ui.dp(this,32),1));}content.addView(labels);
        slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar bar,int value,boolean user){style.candidateSize=value;selected.setText(CandidateLayout.label(AppearanceActivity.this,value));redraw.run();renderPreview();}public void onStartTrackingTouch(android.widget.SeekBar bar){}public void onStopTrackingTouch(android.widget.SeekBar bar){}});
    }
    private interface Change{void accept(int value);}private interface BoolChange{void accept(boolean value);}
    private void choices(LinearLayout parent,String[] labels,int selected,Change change){LinearLayout row=Ui.row(this);
        for(int i=0;i<labels.length;i++){int index=i;Button b=Ui.button(this,labels[i],selected==i,v->{change.accept(index);if(row.getParent()!=null)for(int j=0;j<row.getChildCount();j++){View child=row.getChildAt(j);child.setBackground(Glass.surface(this,style,j==index,false));((Button)child).setTextColor(j==index?(style.dark()?0xff171717:0xffffffff):style.ink());}});Ui.addButton(row,b,1,48);}
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,Ui.dp(this,6),0,Ui.dp(this,8));parent.addView(row,params);
    }
    private void toggle(LinearLayout parent,String label,boolean checked,BoolChange change){Switch toggle=new Switch(this);toggle.setText(Language.text(this,label));toggle.setTextColor(style.ink());toggle.setTextSize(14);toggle.setChecked(checked);toggle.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),0);toggle.setOnCheckedChangeListener((button,value)->change.accept(value));parent.addView(toggle,new LinearLayout.LayoutParams(-1,Ui.dp(this,56)));}
    private void renderPreview(){if(preview==null)return;if(previewInk!=null){previewInk.close();previewInk=null;}preview.removeAllViews();preview.setBackground(Glass.backdrop(this,style));
        LinearLayout toolbar=new KeyRow(this);toolbar.setGravity(android.view.Gravity.CENTER_VERTICAL);Ui.addButton(toolbar,Glass.button(this,style,Language.text(this,"帮我回答"),true,false,v->Toast.makeText(this,Language.text(this,"这里仅预览外观，不会读取屏幕"),Toast.LENGTH_SHORT).show()),2,CandidateLayout.keyHeight(style));
        Ui.addButton(toolbar,Glass.button(this,style,Language.text(this,"自然"),false,true,v->{}),1,CandidateLayout.keyHeight(style));Ui.addButton(toolbar,Glass.button(this,style,Language.text(this,"设置"),false,true,v->{}),.9f,CandidateLayout.keyHeight(style));preview.addView(toolbar,new LinearLayout.LayoutParams(-1,Ui.dp(this,CandidateLayout.barHeight(style))));
        TextView sample=Ui.rawText(this,"",14,style.ink());sample.setGravity(android.view.Gravity.CENTER_VERTICAL);sample.setSingleLine(true);sample.setTextSize(CandidateLayout.font(style));sample.setIncludeFontPadding(false);sample.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),0);sample.setVisibility(View.GONE);preview.addView(sample,new LinearLayout.LayoutParams(-1,Ui.dp(this,CandidateLayout.barHeight(style))));
        sample.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence text,int start,int count,int after){}public void onTextChanged(CharSequence text,int start,int before,int count){boolean empty=text.length()==0;sample.setVisibility(empty?View.GONE:View.VISIBLE);toolbar.setVisibility(empty?View.VISIBLE:View.GONE);}public void afterTextChanged(android.text.Editable text){}});
        frame=new KeyboardFrame(this,style,true);frame.setEditing(editing);preview.addView(frame);LinearLayout keys=Ui.column(this);frame.addView(keys);
        if(style.mode==2&&!symbols){if(style.quickSymbols)KeyboardLayout.addQuickSymbols(keys,style,chinese,value->sample.setText(Language.text(this,"已按下 ")+value));
            previewInk=new HandwritingPanel(this,style,chinese,words->sample.setText(words.isEmpty()?Language.text(this,"在预览区手写试试"):String.join(" · ",words)));keys.addView(previewInk);return;}
        KeyboardLayout.build(keys,style,chinese,shift,symbols,new KeyboardLayout.Actions(){
            public void key(String value){sample.setText(Language.text(AppearanceActivity.this,"已按下 ")+value);}
            private String cleared="";
            public void shift(){shift=!shift;renderPreview();}public void delete(){sample.setText("");}public void deleteRecent(){if(sample.length()>0)cleared=sample.getText().toString();sample.setText("");}public void undo(){sample.setText(cleared);cleared="";}
            public void symbols(){symbols=!symbols;renderPreview();}public void language(){chinese=!chinese;symbols=false;renderPreview();}
            public void switchIme(boolean picker){sample.setText(Language.text(AppearanceActivity.this,"实际键盘可切换输入法"));}
            public void clipboard(){sample.setText(Language.text(AppearanceActivity.this,"剪贴板"));}
            public void space(){sample.setText(Language.text(AppearanceActivity.this,"空格"));}public void punctuation(){sample.setText("。");}public void enter(){sample.setText(Language.text(AppearanceActivity.this,"回车"));}
        });
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=41||result!=RESULT_OK||data==null||data.getData()==null)return;
        importing=true;importButton.setEnabled(false);importButton.setText(Language.text(this,"正在导入照片…"));android.net.Uri uri=data.getData();
        photoWorker.execute(()->{try{String name=BackgroundPhoto.importPhoto(getApplicationContext(),uri);BackgroundPhoto.bitmap(getApplicationContext(),name);
            runOnUiThread(()->{if(isDestroyed()){BackgroundPhoto.discard(getApplicationContext(),name);return;}pendingPhotos.add(name);style.photo=name;importing=false;buildPage();});}
            catch(Exception e){runOnUiThread(()->{if(isDestroyed())return;importing=false;importButton.setEnabled(true);importButton.setText(Language.text(this,"导入照片背景"));Toast.makeText(this,Language.text(this,"照片无法导入，请换一张图片"),Toast.LENGTH_LONG).show();});}
        });
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("theme",style.palette);out.putInt("height",style.height);out.putFloat("width",style.width);out.putFloat("horizontal",style.horizontal);out.putInt("lift",style.lift);out.putString("photo",style.photo);
        out.putInt("mode",style.mode);out.putInt("symbolPosition",style.symbolPosition);out.putInt("swipeDirection",style.swipeDirection);out.putBoolean("quickSymbols",style.quickSymbols);out.putBoolean("haptic",style.haptic);out.putInt("candidateSize",style.candidateSize);out.putInt("footerLayout",style.footerLayout);out.putBoolean("editing",editing);out.putStringArrayList("pendingPhotos",pendingPhotos);}
    @Override protected void onDestroy(){if(previewInk!=null)previewInk.close();photoWorker.shutdown();if(!isChangingConfigurations())for(String photo:pendingPhotos)BackgroundPhoto.discard(this,photo);super.onDestroy();}
}
