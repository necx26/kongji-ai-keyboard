package com.kongji.aikeyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends LocalizedActivity {
    private EditText url,key,model;private CheckBox local;private TextView status,appearanceSummary;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Ui.bg(this));
        LinearLayout page=Ui.column(this);int p=Ui.dp(this,20);page.setPadding(p,p,p,p);scroll.addView(page);setContentView(scroll);Ui.activityInsets(scroll,0);
        page.addView(Ui.text(this,"❤❤❤",12,Ui.ACCENT));
        page.addView(Ui.heading(this,"控机 AI 输入法",28));
        page.addView(Ui.text(this,"读取屏幕~帮回。",15,Ui.MUTED));
        status=Ui.text(this,"",14,Ui.ACCENT);page.addView(status);
        Ui.rule(page);add(page,"外观",v->startActivity(new Intent(this,AppearanceActivity.class)),false);
        appearanceSummary=Ui.text(this,"",12,Ui.MUTED);page.addView(appearanceSummary);
        add(page,"输入与词库设置",v->startActivity(new Intent(this,InputSettingsActivity.class)),false);
        add(page,"应用语言",v->startActivity(new Intent(this,LanguageActivity.class)),false);
        page.addView(Ui.heading(this,"启用输入法",18));
        page.addView(Ui.text(this,"安装后，在系统里开启“控机 AI 输入法”，再切换到它。",14,Ui.MUTED));
        add(page,"开启输入法",v->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)),false);
        add(page,"切换到控机 AI 输入法",v->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker(),true);
        Ui.rule(page);LinearLayout reader=Ui.disclosure(page,"开启屏幕读取",false);
        reader.addView(Ui.text(this,"仅在你点击“帮我回答”时读取当前页面",14,Ui.MUTED));
        add(reader,"开启屏幕读取服务",v->new AlertDialog.Builder(this).setTitle(Language.text(SettingsActivity.this,"屏幕读取说明"))
            .setMessage(Language.text(SettingsActivity.this,"此功能通过无障碍服务读取当前应用的可见文字和截图，截图可能包含聊天、头像及其他个人信息。内容仅暂存在内存；点击“生成回复”后，所选内容会提交到你填写的 API 地址。\n\n可以随时在系统设置关闭服务。请在下一页选择“控机 AI 屏幕读取”并开启。"))
            .setNegativeButton(Language.text(SettingsActivity.this,"取消"),null).setPositiveButton(Language.text(SettingsActivity.this,"去系统设置"),(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).show(),false);
        LinearLayout api=Ui.disclosure(page,"配置模型接口",false);
        api.addView(Ui.text(this,"支持 OpenAI 兼容的 Chat Completions 接口。发送截图时，模型必须支持图片；关闭截图则使用页面文字。",14,Ui.MUTED));
        ApiConfig config=ApiConfig.load(this);
        url=field(api,"接口地址","https://你的服务商地址/v1",config.url,false);
        key=field(api,"API Key","填写你的 Key，没有的话就没办法了（本地模型留空）",config.key,true);
        model=field(api,"模型名称","填写服务商提供的模型 ID",config.model,false);
        local=new CheckBox(this);local.setText(Language.text(SettingsActivity.this,"允许局域网 HTTP（本地模型，明文传输）"));local.setTextSize(13);local.setChecked(config.localHttp);api.addView(local);
        add(api,"保存 API 设置",v->save(),true);
        api.addView(Ui.text(this,"Key 使用 Android Keystore 加密保存，不写入日志。服务商的统一 Key 请放在你自己的后端；个人测试可填写自己的 Key。",12,Ui.MUTED));
        Ui.rule(page);page.addView(Ui.heading(this,"试一下喵",18));
        add(page,"打开键盘练习页",v->startActivity(new Intent(this,PracticeActivity.class)),false);
        page.addView(Ui.text(this,"连续拼音组词、简拼、模糊拼音、英文补全与本机用户词库。屏幕识别仅覆盖当前可见内容，不会读取聊天数据库或自动翻页。Android 11 及以上。",13,Ui.MUTED));
    }
    private EditText field(LinearLayout page,String title,String hint,String value,boolean password){
        page.addView(Ui.text(this,title,14,Ui.INK));EditText edit=new EditText(this);edit.setSingleLine(true);edit.setTextSize(15);edit.setTextColor(Ui.ink(this));
        edit.setContentDescription(Language.text(this,title)+Language.text(this," 输入框"));
        edit.setHint(Language.text(this,hint));edit.setText(value);edit.setInputType(password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        edit.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);edit.setBackground(Ui.rounded(this,Ui.panel(this),6));
        int pad=Ui.dp(this,12);edit.setPadding(pad,pad,pad,pad);page.addView(edit,new LinearLayout.LayoutParams(-1,Ui.dp(this,50)));return edit;
    }
    private void add(LinearLayout page,String title,android.view.View.OnClickListener action,boolean primary){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,Ui.dp(this,48));lp.setMargins(0,Ui.dp(this,4),0,Ui.dp(this,8));page.addView(primary?Ui.button(this,title,true,action):Ui.navigation(this,title,action),lp);
    }
    private void save(){try{new ApiConfig(url.getText().toString(),key.getText().toString(),model.getText().toString(),local.isChecked()).save(this);Toast.makeText(this,Language.text(SettingsActivity.this,"API 设置已保存"),Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,Language.message(this,e.getMessage()==null?"保存失败":e.getMessage()),Toast.LENGTH_LONG).show();}}
    @Override protected void onResume(){super.onResume();if(status!=null){String current=Settings.Secure.getString(getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);boolean selected=current!=null&&current.startsWith(getPackageName()+"/");status.setText(Language.text(this,"输入法：")+Language.text(this,selected?"已选中":"待切换")+Language.text(this,"    屏幕读取：")+Language.text(this,ScreenReaderService.current()!=null?"已连接":"待开启"));}if(appearanceSummary!=null){KeyboardStyle s=KeyboardStyle.load(this);appearanceSummary.setText(Language.text(this,KeyboardStyle.MODES[s.mode])+" · "+Language.text(this,KeyboardStyle.NAMES[s.palette])+(s.photo.isEmpty()?"":" · "+Language.text(this,"照片背景")));}}
}
