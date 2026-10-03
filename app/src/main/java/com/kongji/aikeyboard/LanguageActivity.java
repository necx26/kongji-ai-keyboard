package com.kongji.aikeyboard;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;

public final class LanguageActivity extends LocalizedActivity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(Ui.BG);LinearLayout page=Ui.column(this);int pad=Ui.dp(this,20);page.setPadding(pad,pad,pad,pad);scroll.addView(page);setContentView(scroll);Ui.activityInsets(scroll,0);
        page.addView(Ui.button(this,"‹ 返回设置",false,v->finish()),new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));page.addView(Ui.heading(this,"应用语言",27));page.addView(Ui.text(this,"更改界面语言，输入记忆和键盘设置会保留。",14,Ui.MUTED));
        RadioGroup group=new RadioGroup(this);String selected=Language.selected(this);
        for(int i=0;i<Language.CODES.length;i++){String code=Language.CODES[i];RadioButton option=new RadioButton(this);option.setId(android.view.View.generateViewId());option.setText(i==0?Language.text(this,Language.NAMES[i]):Language.NAMES[i]);option.setTextSize(18);option.setTextColor(Ui.INK);option.setPadding(pad,Ui.dp(this,8),pad,Ui.dp(this,8));group.addView(option,new RadioGroup.LayoutParams(-1,Ui.dp(this,56)));option.setChecked(code.equals(selected));option.setOnClickListener(v->{if(!code.equals(Language.selected(this))){Language.set(this,code);if(android.os.Build.VERSION.SDK_INT<33)recreate();}});}page.addView(group);
        page.addView(Ui.text(this,"目前完整提供六种界面语言。跟随其他系统语言时使用英语。键盘输入模式可在外观设置单独选择。",13,Ui.MUTED));
    }
}
