package com.kongji.aikeyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class AppearanceActivity extends LocalizedActivity {
    private KeyboardStyle style;
    private LinearLayout preview,themes,swatches;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable redraw=()->renderPreview();
    private boolean chinese=true,shift=false,symbols=false;
    private Button customColor;
    private HandwritingPanel previewInk;
    @Override public void onCreate(Bundle state){super.onCreate(state);style=KeyboardStyle.load(this);
        if(state!=null){style.palette=state.getInt("palette");style.accent=state.getInt("accent");style.height=state.getInt("height");style.radius=state.getInt("radius");style.gap=state.getInt("gap");style.font=state.getInt("font");style.opacity=state.getInt("opacity");style.layout=state.getInt("layout");style.numberRow=state.getBoolean("numberRow");style.haptic=state.getBoolean("haptic");style.motion=state.getBoolean("motion");style.mode=state.getInt("mode");style.symbolPosition=state.getInt("symbolPosition");style.swipeDirection=state.getInt("swipeDirection");style.quickSymbols=state.getBoolean("quickSymbols",true);}
        buildPage();
    }
    private void buildPage(){
        LinearLayout screen=Ui.column(this);screen.setBackgroundColor(Color.rgb(247,248,252));setContentView(screen);Ui.activityInsets(screen,0);
        LinearLayout header=Ui.row(this);header.setGravity(Gravity.CENTER_VERTICAL);int p=Ui.dp(this,18);header.setPadding(p,Ui.dp(this,10),p,Ui.dp(this,8));
        Button back=Ui.button(this,"‹",false,v->finish());back.setTextSize(28);back.setContentDescription(Language.text(AppearanceActivity.this,"返回设置"));header.addView(back,new LinearLayout.LayoutParams(Ui.dp(this,42),Ui.dp(this,42)));
        LinearLayout title=Ui.column(this);title.addView(Ui.heading(this,"外观工作室",25));title.addView(Ui.text(this,"让每一次输入，都舒服一点。",12,Ui.MUTED));header.addView(title,new LinearLayout.LayoutParams(0,-2,1));screen.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);LinearLayout content=Ui.column(this);content.setPadding(p,0,p,p);scroll.addView(content);screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout caption=Ui.row(this);TextView live=Ui.text(this,"实时预览",12,Ui.ACCENT);caption.addView(live,new LinearLayout.LayoutParams(0,-2,1));caption.addView(Ui.text(this,"点按体验手感",12,Ui.MUTED));content.addView(caption);
        preview=Ui.column(this);preview.setClipChildren(false);preview.setClipToOutline(true);preview.setOutlineProvider(new android.view.ViewOutlineProvider(){@Override public void getOutline(View view,android.graphics.Outline outline){outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),Ui.dp(view.getContext(),22));}});content.addView(preview);
        section(content,"键盘模式","选择模式，顶部预览会立即更新；点击应用后用于实际输入。");
        choices(content,KeyboardStyle.MODES,style.mode,v->{style.mode=v;symbols=false;renderPreview();});
        section(content,"快捷符号","26键小字与字母分区显示；长按或按设置方向滑动输入符号。");
        choices(content,new String[]{"符号在上方","符号在下方"},style.symbolPosition,v->{style.symbolPosition=v;renderPreview();});
        choices(content,new String[]{"上滑输入符号","下滑输入符号"},style.swipeDirection,v->{style.swipeDirection=v;renderPreview();});
        toggle(content,"显示快捷符号栏",style.quickSymbols,v->{style.quickSymbols=v;renderPreview();});
        section(content,"主题氛围","从柔和雾面，到安静深色。");themes=Ui.row(this);content.addView(themes);renderThemes();
        section(content,"点缀颜色","用于 AI 按钮、回车键和选中状态。");swatches=Ui.row(this);content.addView(swatches);renderSwatches();
        customColor=Ui.button(this,colorLabel(),false,v->chooseColor());addWide(content,customColor,44);
        section(content,"玻璃质感","细调透明度、圆角与按键间距。");
        slider(content,"按键透明度",40,96,style.opacity,"%",v->style.opacity=v);
        slider(content,"按键圆角",4,22,style.radius," dp",v->style.radius=v);
        slider(content,"按键间距",2,7,style.gap," dp",v->style.gap=v);
        section(content,"布局与尺寸","熟悉的错位字母行，宽一些的空格键。");
        slider(content,"按键高度",40,58,style.height," dp",v->style.height=v);
        slider(content,"字母字号",16,24,style.font," sp",v->style.font=v);
        LinearLayout layouts=Ui.row(this);for(int i=0;i<KeyboardStyle.LAYOUTS.length;i++){final int index=i;Button b=Ui.button(this,KeyboardStyle.LAYOUTS[i],false,v->{style.layout=index;for(int k=0;k<layouts.getChildCount();k++)layouts.getChildAt(k).setAlpha(k==index?1f:.55f);renderPreview();});b.setTextSize(12);b.setAlpha(style.layout==i?1f:.55f);Ui.addButton(layouts,b,1,42);}content.addView(layouts);
        toggle(content,"独立数字行",style.numberRow,v->style.numberRow=v);
        section(content,"触感与动效","轻微回弹，保持输入干净利落。");
        toggle(content,"按键轻触反馈",style.haptic,v->style.haptic=v);
        toggle(content,"按压回弹动画",style.motion,v->style.motion=v);
        addWide(content,Ui.button(this,"恢复默认外观",false,v->new AlertDialog.Builder(this).setTitle(Language.text(AppearanceActivity.this,"恢复默认外观？")).setMessage(Language.text(AppearanceActivity.this,"恢复雾紫主题和标准尺寸，点击应用后生效。")).setNegativeButton(Language.text(AppearanceActivity.this,"取消"),null).setPositiveButton(Language.text(AppearanceActivity.this,"恢复"),(d,w)->{style=new KeyboardStyle();buildPage();}).show()),44);
        TextView hint=Ui.text(this,"玻璃效果由本地柔光背景与半透明表面呈现。外观调整不会读取屏幕，也不会调用模型。",12,Ui.MUTED);content.addView(hint);
        LinearLayout footer=Ui.row(this);footer.setPadding(p,Ui.dp(this,8),p,Ui.dp(this,10));
        Button apply=Ui.button(this,"应用到键盘",true,v->{if(style.save(this)){Toast.makeText(this,Language.text(AppearanceActivity.this,"外观已应用，再次调出键盘即可看到"),Toast.LENGTH_SHORT).show();finish();}else Toast.makeText(this,Language.text(AppearanceActivity.this,"外观保存失败，请重试"),Toast.LENGTH_LONG).show();});Ui.addButton(footer,apply,2,48);
        Button tryIt=Ui.button(this,"应用并试打",false,v->{if(style.save(this)){startActivity(new Intent(this,PracticeActivity.class));finish();}else Toast.makeText(this,Language.text(AppearanceActivity.this,"外观保存失败"),Toast.LENGTH_LONG).show();});Ui.addButton(footer,tryIt,1.3f,48);screen.addView(footer);renderPreview();
    }
    private void section(LinearLayout parent,String title,String subtitle){TextView heading=Ui.heading(this,title,18);heading.setPadding(Ui.dp(this,4),Ui.dp(this,20),0,0);parent.addView(heading);parent.addView(Ui.text(this,subtitle,12,Ui.MUTED));}
    private void addWide(LinearLayout parent,View view,int height){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,Ui.dp(this,height));lp.setMargins(0,Ui.dp(this,6),0,Ui.dp(this,4));parent.addView(view,lp);}
    interface IntChange{void accept(int value);}interface BoolChange{void accept(boolean value);}
    private void choices(LinearLayout parent,String[] titles,int selected,IntChange change){LinearLayout row=Ui.row(this);row.setBaselineAligned(false);for(int i=0;i<titles.length;i++){final int value=i;Button button=Ui.button(this,titles[i],false,v->{change.accept(value);for(int j=0;j<row.getChildCount();j++)row.getChildAt(j).setAlpha(j==value?1f:.55f);});button.setAlpha(i==selected?1f:.55f);Ui.addButton(row,button,1,44);}parent.addView(row);}
    private void slider(LinearLayout parent,String title,int min,int max,int initial,String unit,IntChange change){
        LinearLayout card=Ui.column(this);card.setPadding(Ui.dp(this,10),Ui.dp(this,6),Ui.dp(this,10),Ui.dp(this,4));card.setBackground(Ui.rounded(this,Color.WHITE,16));
        LinearLayout labels=Ui.row(this);labels.addView(Ui.text(this,title,14,Ui.INK),new LinearLayout.LayoutParams(0,-2,1));TextView amount=Ui.text(this,initial+unit,13,Ui.ACCENT);labels.addView(amount);card.addView(labels);
        SeekBar seek=new SeekBar(this);seek.setContentDescription(Language.text(this,title));seek.setMax(max-min);seek.setProgress(initial-min);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int value,boolean user){change.accept(value+min);amount.setText((value+min)+unit);main.removeCallbacks(redraw);main.postDelayed(redraw,45);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){main.removeCallbacks(redraw);renderPreview();}});
        card.addView(seek,new LinearLayout.LayoutParams(-1,Ui.dp(this,34)));addWide(parent,card,84);
    }
    private void toggle(LinearLayout parent,String title,boolean checked,BoolChange change){Switch toggle=new Switch(this);toggle.setText(Language.text(this,title));toggle.setTextSize(14);toggle.setTextColor(Ui.INK);toggle.setChecked(checked);toggle.setPadding(Ui.dp(this,12),0,Ui.dp(this,12),0);toggle.setOnCheckedChangeListener((v,value)->{change.accept(value);renderPreview();});addWide(parent,toggle,48);}
    private void renderThemes(){themes.removeAllViews();int[] accents={Color.rgb(115,94,190),Color.rgb(61,137,165),Color.rgb(169,124,80),Color.rgb(125,150,215)};
        for(int i=0;i<4;i++){final int index=i;KeyboardStyle swatch=new KeyboardStyle();swatch.palette=i;swatch.accent=accents[i];Button b=Glass.button(this,swatch,Language.text(this,KeyboardStyle.NAMES[i]),false,false,v->{style.palette=index;style.accent=accents[index];renderThemes();renderSwatches();customColor.setText(colorLabel());renderPreview();});b.setBackground(Glass.backdrop(swatch));b.setAlpha(style.palette==i?1f:.6f);b.setContentDescription(Language.text(this,KeyboardStyle.NAMES[i])+Language.text(this,"主题")+(style.palette==i?Language.text(this,"已选中"):""));Ui.addButton(themes,b,1,48);}
    }
    private void renderSwatches(){swatches.removeAllViews();int[] values={0xff735ebe,0xff3d89a5,0xff4b957e,0xffb9788a,0xffa97c50,0xff7d96d7};
        for(int value:values){Button b=Ui.button(this,style.accent==value?"✓":"",false,v->{style.accent=value;renderSwatches();customColor.setText(colorLabel());renderPreview();});b.setBackground(Ui.rounded(this,value,14));b.setTextColor(Glass.foreground(value));b.setContentDescription(Language.text(AppearanceActivity.this,"主题色 ")+String.format(Locale.ROOT,"#%06X",value&0xffffff));Ui.addButton(swatches,b,1,38);}
    }
    private String colorLabel(){return Language.text(this,"自定义主题色  ")+String.format(Locale.ROOT,"#%06X",style.accent&0xffffff);}
    private void chooseColor(){EditText input=new EditText(this);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);input.setText(String.format(Locale.ROOT,"#%06X",style.accent&0xffffff));input.setSelectAllOnFocus(true);input.setContentDescription(Language.text(AppearanceActivity.this,"自定义颜色 HEX"));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(Language.text(AppearanceActivity.this,"自定义主题色")).setMessage(Language.text(AppearanceActivity.this,"输入六位颜色，例如 #735EBE。")).setView(input).setNegativeButton(Language.text(AppearanceActivity.this,"取消"),null).setPositiveButton(Language.text(AppearanceActivity.this,"确定"),null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String text=input.getText().toString().trim();if(!text.matches("#?[0-9a-fA-F]{6}")){input.setError(Language.text(AppearanceActivity.this,"请输入六位 HEX 颜色"));return;}style.accent=Color.parseColor(text.startsWith("#")?text:"#"+text);customColor.setText(colorLabel());renderSwatches();renderPreview();dialog.dismiss();}));dialog.show();}
    private void renderPreview(){if(preview==null)return;if(previewInk!=null){previewInk.close();previewInk=null;}preview.removeAllViews();preview.setBackground(Glass.backdrop(style));preview.setPadding(Ui.dp(this,5),Ui.dp(this,8),Ui.dp(this,5),Ui.dp(this,10));
        LinearLayout toolbar=Ui.row(this);Ui.addButton(toolbar,Glass.button(this,style,Language.text(this,"帮我回答"),true,false,v->Toast.makeText(this,Language.text(AppearanceActivity.this,"这里仅预览外观，不会读取屏幕"),Toast.LENGTH_SHORT).show()),2,36);Ui.addButton(toolbar,Glass.button(this,style,Language.text(this,"自然"),false,true,v->{}),1,36);Ui.addButton(toolbar,Glass.button(this,style,Language.text(this,"设置"),false,true,v->{}),.9f,36);preview.addView(toolbar);
        TextView sample=Ui.text(this,"你好，让表达更自然。",12,style.muted());sample.setPadding(Ui.dp(this,8),Ui.dp(this,6),0,Ui.dp(this,6));preview.addView(sample);
        if(style.mode==2&&!symbols){if(style.quickSymbols)KeyboardLayout.addQuickSymbols(preview,style,chinese,value->sample.setText(Language.text(this,"已按下 ")+value));previewInk=new HandwritingPanel(this,style,chinese,words->sample.setText(words.isEmpty()?Language.text(AppearanceActivity.this,"在预览区手写试试"):String.join(" · ",words)));preview.addView(previewInk);return;}
        LinearLayout keys=Ui.column(this);preview.addView(keys);KeyboardLayout.build(keys,style,chinese,shift,symbols,new KeyboardLayout.Actions(){
            public void key(String value){sample.setText(Language.text(AppearanceActivity.this,"已按下 ")+value);}public void shift(){shift=!shift;renderPreview();}public void delete(){sample.setText(Language.text(AppearanceActivity.this,"删除"));}public void clear(){sample.setText(Language.text(AppearanceActivity.this,"已清除"));}public void symbols(){symbols=!symbols;renderPreview();}public void language(){chinese=!chinese;symbols=false;renderPreview();}public void switchIme(boolean picker){sample.setText(Language.text(AppearanceActivity.this,"实际键盘可切换输入法"));}public void space(){sample.setText(Language.text(AppearanceActivity.this,"空格"));}public void punctuation(){sample.setText("。");}public void enter(){sample.setText(Language.text(AppearanceActivity.this,"回车"));}
        });
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("palette",style.palette);out.putInt("accent",style.accent);out.putInt("height",style.height);out.putInt("radius",style.radius);out.putInt("gap",style.gap);out.putInt("font",style.font);out.putInt("opacity",style.opacity);out.putInt("layout",style.layout);out.putBoolean("numberRow",style.numberRow);out.putBoolean("haptic",style.haptic);out.putBoolean("motion",style.motion);out.putInt("mode",style.mode);out.putInt("symbolPosition",style.symbolPosition);out.putInt("swipeDirection",style.swipeDirection);out.putBoolean("quickSymbols",style.quickSymbols);}
    @Override protected void onDestroy(){if(previewInk!=null)previewInk.close();main.removeCallbacks(redraw);super.onDestroy();}
}
