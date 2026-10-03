package com.kongji.aikeyboard;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

public class PracticeActivity extends LocalizedActivity {
    @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout page=Ui.column(this);page.setBackgroundColor(Ui.BG);int p=Ui.dp(this,20);page.setPadding(p,p,p,p);setContentView(page);Ui.activityInsets(page,p);
        page.addView(Ui.heading(this,"键盘练习",25));page.addView(Ui.text(this,"下面是用于测试的示例对话。点击输入框，试试键盘和“帮我回答”。",14,Ui.MUTED));
        LinearLayout conversation=Ui.column(this);conversation.setPadding(p,p,p,p);conversation.setBackground(Ui.rounded(this,Color.WHITE,16));
        conversation.addView(Ui.text(this,"对方：你这个是ai写的还是你本人？",18,Ui.INK));
        conversation.addView(Ui.text(this,"我：你猜。",17,Ui.ACCENT));
        conversation.addView(Ui.text(this,"对方：AI？",18,Ui.INK));
        page.addView(conversation,new LinearLayout.LayoutParams(-1,0,1));
        EditText edit=new EditText(this);edit.setHint(Language.text(PracticeActivity.this,"点这里输入，或让 AI 帮你回复"));edit.setTextSize(17);edit.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);edit.setMinLines(2);edit.setMaxLines(4);page.addView(edit,new LinearLayout.LayoutParams(-1,-2));
    }
}
