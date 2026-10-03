package com.kongji.aikeyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;

public class InputSettingsActivity extends LocalizedActivity {
    private InputPreferences options;private LearningStore store;private TextView summary;private LinearLayout recent;
    @Override public void onCreate(Bundle state){super.onCreate(state);options=InputPreferences.load(this);store=new LearningStore(this);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(Ui.BG);LinearLayout page=Ui.column(this);int p=Ui.dp(this,20);page.setPadding(p,p,p,p);scroll.addView(page);setContentView(scroll);Ui.activityInsets(scroll,0);
        add(page,"‹ 返回设置",false,v->finish());page.addView(Ui.heading(this,"输入与词库",27));page.addView(Ui.text(this,"连续组词，熟悉你的表达方式。",14,Ui.MUTED));
        page.addView(Ui.heading(this,"中文输入",18));page.addView(Ui.text(this,"连续输入完整拼音：jintianxiawuwomenqubeijing → 今天下午我们去北京。也可先选前面的词，再继续选择剩余拼音；nh 可选“你好”，xi'an 区分“西安”和“先”。",14,Ui.MUTED));
        toggle(page,"声母模糊拼音：zh/z、ch/c、sh/s",options.initials,v->{options.initials=v;options.save(this);});
        toggle(page,"韵母模糊拼音：an/ang、en/eng、in/ing",options.finals,v->{options.finals=v;options.save(this);});
        toggle(page,"中文打字纠错：字母错序与邻键误触",options.typos,v->{options.typos=v;options.save(this);});
        page.addView(Ui.text(this,"ek 可推荐“可、可以、可是”，ekshi 或 kwshi 可推荐“可是”。根据当前字母布局处理一次相邻字母交换或邻键误触，结合词频与本机习惯排序。输入越完整越准确；原拼音仍保留，点候选后才确认。",13,Ui.MUTED));
        page.addView(Ui.heading(this,"英文输入",18));toggle(page,"英文补全与拼写纠错候选",options.english,v->{options.english=v;options.save(this);});
        page.addView(Ui.text(this,"hel 可推荐 hello，teh 可推荐 the。点选候选后补一个空格；按空格保留自己输入的原词。人名、缩写不会被强行替换。",14,Ui.MUTED));
        page.addView(Ui.heading(this,"本机记忆",18));toggle(page,"记住常用词与输入习惯",options.memory,v->{options.memory=v;options.save(this);});
        page.addView(Ui.text(this,"记住你确认上屏的词、新词和前后搭配，结合使用次数与最近使用时间调整候选。输入拼音时也参考前一个词，让常用表达更靠前。记忆持续保存在本机，退出应用、重启手机和正常覆盖更新后仍保留，不会自动过期或因词条增多而删除。旧习惯降低排序权重，但记录仍在。普通打字不调用 API，密码等私密输入框不参与学习。关闭开关只暂停学习并停用个人排序；主动清空、清除应用数据或卸载应用才会删除记录。",13,Ui.MUTED));
        summary=Ui.text(this,"",14,Ui.ACCENT);page.addView(summary);
        add(page,"添加自定义词语",true,v->addWord());add(page,"清空用户词库和学习记录",false,v->new AlertDialog.Builder(this).setTitle(Language.text(InputSettingsActivity.this,"清空本机输入记忆？")).setMessage(Language.text(InputSettingsActivity.this,"永久删除学习过的词、前后搭配和手动添加的词条，无法恢复。清空后会从新输入的词重新学习。API 设置与键盘外观继续保留。")).setNegativeButton(Language.text(InputSettingsActivity.this,"取消"),null).setPositiveButton(Language.text(InputSettingsActivity.this,"清空"),(d,w)->{store.clear();showRecent();Toast.makeText(this,Language.text(InputSettingsActivity.this,"输入记忆已清空"),Toast.LENGTH_SHORT).show();}).show());
        page.addView(Ui.heading(this,"最近使用的用户词",18));recent=Ui.column(this);page.addView(recent);showRecent();
        add(page,"打开键盘练习页",false,v->startActivity(new Intent(this,PracticeActivity.class)));
        page.addView(Ui.text(this,"词库：Rime 袖珍简化字拼音（源自 Android Pinyin IME，Apache 2.0）及 SCOWL/ESDB 英文词表。许可与来源随应用打包。当前没有滑行输入、语音输入或大模型级句意纠错。",12,Ui.MUTED));
    }
    private interface Change {void apply(boolean value);}
    private void toggle(LinearLayout parent,String title,boolean checked,Change action){Switch toggle=new Switch(this);toggle.setText(Language.text(this,title));toggle.setTextSize(14);toggle.setTextColor(Ui.INK);toggle.setChecked(checked);toggle.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),0);toggle.setOnCheckedChangeListener((v,value)->action.apply(value));parent.addView(toggle,new LinearLayout.LayoutParams(-1,Ui.dp(this,60)));}
    private void add(LinearLayout parent,String title,boolean primary,android.view.View.OnClickListener click){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,Ui.dp(this,48));lp.setMargins(0,Ui.dp(this,8),0,Ui.dp(this,6));parent.addView(Ui.button(this,title,primary,click),lp);}
    private void showRecent(){if(recent==null)return;recent.removeAllViews();List<LearningStore.Word> words=store.words();summary.setText(Language.text(this,"本机用户词：")+words.size()+Language.text(this," · 持续保存，手动清空"));if(words.isEmpty())recent.addView(Ui.text(this,"选择候选或添加自定义词语后，这里会逐渐积累。",13,Ui.MUTED));
        for(LearningStore.Word word:words.subList(0,Math.min(12,words.size())))recent.addView(Ui.text(this,word.value+"  ·  "+word.spelling+"  ·  "+word.hits+Language.text(this," 次"),13,Ui.INK));}
    private void addWord(){LinearLayout fields=Ui.column(this);int p=Ui.dp(this,20);fields.setPadding(p,0,p,0);EditText word=new EditText(this);word.setHint(Language.text(InputSettingsActivity.this,"中文词语或英文单词"));word.setContentDescription(Language.text(InputSettingsActivity.this,"自定义词语输入框"));word.setSingleLine(true);fields.addView(word);
        EditText spelling=new EditText(this);spelling.setHint(Language.text(InputSettingsActivity.this,"中文拼音用空格分隔，英文可留空"));spelling.setContentDescription(Language.text(InputSettingsActivity.this,"自定义拼音输入框"));spelling.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);spelling.setSingleLine(true);fields.addView(spelling);fields.addView(Ui.text(this,"例如：控机大师 / kong ji da shi。每个汉字对应一个拼音音节。英文直接填写单词。",12,Ui.MUTED));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(Language.text(InputSettingsActivity.this,"添加自定义词语")).setView(fields).setNegativeButton(Language.text(InputSettingsActivity.this,"取消"),null).setPositiveButton(Language.text(InputSettingsActivity.this,"添加"),null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String text=word.getText().toString().trim(),code=spelling.getText().toString().trim().toLowerCase(Locale.ROOT).replace('ü','v').replace("u:","v").replace("'"," ").replaceAll(" +"," ");
            if(text.matches("[a-zA-Z]+(?:'[a-zA-Z]+)?")&&text.length()<=32){text=text.toLowerCase(Locale.ROOT);store.add("en",text,text);}
            else if(text.matches("\\p{IsHan}{1,32}")&&code.matches("[a-z]+(?: [a-z]+)*")&&code.length()<=96&&code.split(" ").length==text.codePointCount(0,text.length()))store.add("zh",code,text);
            else{word.setError(Language.text(InputSettingsActivity.this,"中文需填写对应音节的拼音；英文限 32 个字母和词内撇号"));return;}dialog.dismiss();showRecent();Toast.makeText(this,Language.text(InputSettingsActivity.this,"词条已添加，再次调出键盘即可使用"),Toast.LENGTH_SHORT).show();}));dialog.show();}
    @Override protected void onResume(){super.onResume();if(store!=null)showRecent();}
    @Override protected void onDestroy(){store.close();super.onDestroy();}
}
