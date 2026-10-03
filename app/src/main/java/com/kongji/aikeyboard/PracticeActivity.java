package com.kongji.aikeyboard;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

public class PracticeActivity extends LocalizedActivity {
    @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout page=Ui.column(this);page.setBackgroundColor(Ui.bg(this));int p=Ui.dp(this,20);page.setPadding(p,p,p,p);setContentView(page);Ui.activityInsets(page,p);
        page.addView(Ui.heading(this,"键盘练习",25));page.addView(Ui.text(this,"下面是用于测试的示例对话。点击输入框，试试键盘和“帮我回答”。",14,Ui.MUTED));
        LinearLayout conversation=Ui.column(this);conversation.setPadding(Ui.dp(this,12),Ui.dp(this,20),Ui.dp(this,12),Ui.dp(this,20));conversation.setBackground(Ui.rounded(this,Ui.panel(this),0));
        conversation.addView(Ui.text(this,"对方：你这个是ai写的还是你本人？",18,Ui.INK));
        conversation.addView(Ui.text(this,"我：你猜。",17,Ui.ACCENT));
        conversation.addView(Ui.text(this,"对方：AI？",18,Ui.INK));
        page.addView(conversation,new LinearLayout.LayoutParams(-1,0,1));
        EditText edit=new EditText(this);edit.setId(android.R.id.edit);edit.setHint(Language.text(PracticeActivity.this,"点这里输入，或让 AI 帮你回复"));edit.setContentDescription(Language.text(this,"点这里输入，或让 AI 帮你回复"));edit.setTextSize(17);edit.setTextColor(Ui.ink(this));edit.setHintTextColor(Ui.muted(this));edit.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);edit.setMinLines(2);edit.setMaxLines(4);page.addView(edit,new LinearLayout.LayoutParams(-1,-2));
        if(getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE){conversation.setVisibility(android.view.View.GONE);page.getChildAt(1).setVisibility(android.view.View.GONE);page.getChildAt(0).setPadding(0,0,0,0);edit.setMinLines(1);edit.setMaxLines(2);}
    }
}
