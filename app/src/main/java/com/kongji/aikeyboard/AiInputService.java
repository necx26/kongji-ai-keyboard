package com.kongji.aikeyboard;

import android.content.Intent;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AiInputService extends InputMethodService {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable candidateRefresh=this::renderCandidatesNow;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LinearLayout root,toolbar,keys,candidates,aiPanel;private TextView composing,status;
    private KeyboardStyle appearance;private KeyboardFrame keyboardFrame;
    private HandwritingPanel handwriting;private List<Pinyin.Candidate> inkCandidates=List.of();
    private HorizontalScrollView candidateStrip;private LinearLayout expandedCandidates;private Button expandButton;
    private List<Pinyin.Candidate> shown=java.util.List.of();private boolean expanded=false,compositionAllowed=true,noLearning=false,numericEditor=false,autoSpace=false;
    private String previousWord="",selectedText="",selectedCode="",selectionPrevious="";private long ownEditUntil;
    private boolean capsLock=false;private long lastShiftTap;
    private Pinyin pinyin;private String buffer="";private boolean chinese=true,shift=false,symbols=false,sensitive=false;
    private String style="自然";private long session=0,operation=0;private String target="";
    private ScreenReaderService.Snapshot snapshot;private Future<?> future;private ApiClient client;
    @Override public void onCreate(){super.onCreate();chinese=InputPreferences.prefs(this).getBoolean("chinese_mode",true);pinyin=new Pinyin(this,()->updateCandidates());}
    @Override public View onCreateInputView(){
        appearance=loadAppearance();
        root=Ui.column(this);root.setBackground(Glass.backdrop(this,appearance));root.setPadding(Ui.dp(this,6),Ui.dp(this,8),Ui.dp(this,6),Ui.dp(this,33));
        root.setOnApplyWindowInsetsListener((v,insets)->{
            int navigation=Math.max(Ui.dp(this,28),insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom);
            v.setPadding(Ui.dp(this,6),Ui.dp(this,8),Ui.dp(this,6),Ui.dp(this,5)+navigation);return insets;
        });
        toolbar=Ui.row(this);root.addView(toolbar);renderToolbar();
        aiPanel=Ui.column(this);root.addView(aiPanel);
        status=Ui.text(this,sensitive?"密码输入框：屏幕读取已禁用":"点“帮我回答”，读取当前可见对话",12,appearance.muted());status.setMaxLines(2);root.addView(status);
        composing=Ui.text(this,"",12,appearance.ink());composing.setMaxLines(1);composing.setEllipsize(android.text.TextUtils.TruncateAt.END);root.addView(composing,new LinearLayout.LayoutParams(-1,Ui.dp(this,24)));
        LinearLayout candidateBar=Ui.row(this);candidateBar.setBaselineAligned(false);
        candidateStrip=new HorizontalScrollView(this);candidateStrip.setHorizontalScrollBarEnabled(false);candidates=Ui.row(this);candidates.setBaselineAligned(false);candidateStrip.addView(candidates);candidateBar.addView(candidateStrip,new LinearLayout.LayoutParams(0,Ui.dp(this,44),1));
        expandButton=glassButton("⌄",false,v->{expanded=!expanded;renderExpanded();});expandButton.setContentDescription(Language.text(AiInputService.this,"展开候选"));candidateBar.addView(expandButton,new LinearLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,40)));root.addView(candidateBar);
        expandedCandidates=Ui.column(this);expandedCandidates.setVisibility(View.GONE);root.addView(expandedCandidates);
        keys=Ui.column(this);keyboardFrame=new KeyboardFrame(this,appearance,false);keyboardFrame.addView(keys);root.addView(keyboardFrame);renderKeys();return root;
    }
    @Override public void onStartInputView(EditorInfo info,boolean restarting){
        super.onStartInputView(info,restarting);
        appearance=loadAppearance();pinyin.configure(noLearning||!compositionAllowed);root.setBackground(Glass.backdrop(this,appearance));
        status.setTextColor(appearance.muted());composing.setTextColor(appearance.ink());
        updateCompactView();keyboardFrame.setStyle(appearance);renderToolbar();renderKeys();updateCandidates();
    }
    private KeyboardStyle loadAppearance(){KeyboardStyle value=KeyboardStyle.load(this);
        if(getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE){value.height=Math.min(value.height,34);value.font=18;value.quickSymbols=false;value.lift=Math.min(value.lift,24);}return value;
    }
    private void updateCompactView(){boolean compact=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;status.setVisibility(compact?View.GONE:View.VISIBLE);composing.setVisibility(compact?View.GONE:View.VISIBLE);}
    private Button glassButton(String title,boolean primary,View.OnClickListener action){Button b=Glass.button(this,appearance,Language.text(this,title),primary,!primary,action);b.setMaxLines(2);b.setAutoSizeTextTypeUniformWithConfiguration(9,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);return b;}
    private void renderToolbar(){
        toolbar.removeAllViews();
        Ui.addButton(toolbar,glassButton("帮我回答",true,v->readScreen()),2.2f,38);
        Ui.addButton(toolbar,glassButton(Language.text(this,"风格：")+Language.text(this,style),false,v->{style=style.equals("自然")?"简短":style.equals("简短")?"正式":"自然";((Button)v).setText(Language.text(AiInputService.this,"风格：")+Language.text(AiInputService.this,style));}),1.7f,38);
        Ui.addButton(toolbar,glassButton("⚙",false,v->openSettings()),1,38); 
    }
    @Override public void onStartInput(EditorInfo info,boolean restarting){
        super.onStartInput(info,restarting);session++;cancelRequest();clearSnapshot();buffer="";resetRun();previousWord="";autoSpace=false;expanded=false;
        target=info.packageName==null?"":info.packageName;
        int variation=info.inputType&InputType.TYPE_MASK_VARIATION,clazz=info.inputType&InputType.TYPE_MASK_CLASS;
        sensitive=(clazz==InputType.TYPE_CLASS_TEXT&&(variation==InputType.TYPE_TEXT_VARIATION_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
            ||(clazz==InputType.TYPE_CLASS_NUMBER&&variation==InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        compositionAllowed=clazz==InputType.TYPE_CLASS_TEXT&&!sensitive&&variation!=InputType.TYPE_TEXT_VARIATION_URI&&variation!=InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS&&variation!=InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS;
        noLearning=sensitive||!compositionAllowed||(info.imeOptions&EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)!=0||(info.inputType&InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)!=0;
        numericEditor=clazz==InputType.TYPE_CLASS_NUMBER||clazz==InputType.TYPE_CLASS_PHONE||clazz==InputType.TYPE_CLASS_DATETIME;
        pinyin.configure(noLearning||!compositionAllowed);
        if(aiPanel!=null)aiPanel.removeAllViews();if(status!=null)status.setText(Language.text(this,sensitive?"密码输入框：屏幕读取已禁用":"点“帮我回答”，读取当前可见对话"));
        updateCandidates();if(keys!=null)renderKeys();
    }
    @Override public void onFinishInput(){if(handwriting!=null)handwriting.clear();session++;cancelRequest();clearSnapshot();buffer="";resetRun();previousWord="";autoSpace=false;expanded=false;if(aiPanel!=null)aiPanel.removeAllViews();updateCandidates();super.onFinishInput();}
    @Override public void onWindowHidden(){if(root!=null)Glass.cancelTouches(root);if(handwriting!=null)handwriting.clear();operation++;cancelRequest();clearSnapshot();if(aiPanel!=null)aiPanel.removeAllViews();super.onWindowHidden();}
    @Override public boolean onEvaluateFullscreenMode(){return false;}
    private void renderKeys(){
        if(handwriting!=null){handwriting.close();handwriting=null;}inkCandidates=List.of();
        if(appearance.mode==2&&compositionAllowed&&!numericEditor&&!symbols){keys.removeAllViews();if(appearance.quickSymbols)KeyboardLayout.addQuickSymbols(keys,appearance,effectiveChinese(),this::type);handwriting=new HandwritingPanel(this,appearance,effectiveChinese(),choices->{inkCandidates=new java.util.ArrayList<>();for(String value:choices)inkCandidates.add(pinyin.handwriting(value));updateCandidates();});keys.addView(handwriting);LinearLayout functions=Ui.row(this);Ui.addButton(functions,glassButton("123",false,v->{symbols=true;renderKeys();}),1,38);Ui.addButton(functions,glassButton(effectiveChinese()?"中":"EN",false,v->{chinese=!chinese;InputPreferences.prefs(AiInputService.this).edit().putBoolean("chinese_mode",chinese).apply();previousWord="";renderKeys();updateCandidates();}),1,38);Ui.addButton(functions,glassButton("空格",false,v->space()),2,38);Button deletion=glassButton("⌫",false,v->delete());deletion.setContentDescription(Language.text(this,"删除"));Glass.repeat(deletion,this::delete);Ui.addButton(functions,deletion,1,38);Ui.addButton(functions,glassButton("回车",true,v->enter()),1.3f,38);keys.addView(functions);return;}
        KeyboardLayout.build(keys,appearance,effectiveChinese(),shift,symbols||numericEditor,new KeyboardLayout.Actions(){
            public void key(String value){type(value);}
            public void shift(){long now=android.os.SystemClock.uptimeMillis();if(shift&&!capsLock&&now-lastShiftTap<350){capsLock=true;}else{shift=!shift;capsLock=false;}lastShiftTap=now;renderKeys();}
            public void delete(){AiInputService.this.delete();}
            public void clear(){AiInputService.this.delete();}
            public void symbols(){flushBest();if(!buffer.isEmpty())return;symbols=!symbols;expanded=false;renderKeys();updateCandidates();}
            public void language(){if(!compositionAllowed){toast("先试试吧");return;}flushBest();if(!buffer.isEmpty())return;chinese=!chinese;InputPreferences.prefs(AiInputService.this).edit().putBoolean("chinese_mode",chinese).apply();symbols=false;shift=false;capsLock=false;previousWord="";expanded=false;renderKeys();updateCandidates();}
            public void switchIme(boolean picker){if(picker)((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();else switchToNextInputMethod(false);}
            public void space(){AiInputService.this.space();}
            public void punctuation(){type(effectiveChinese()?"。":".");}
            public void enter(){AiInputService.this.enter();}
        });
    }
    private boolean effectiveChinese(){return chinese&&compositionAllowed;}
    private void ownEdit(){ownEditUntil=android.os.SystemClock.uptimeMillis()+150;}
    private void resetRun(){selectedText="";selectedCode="";selectionPrevious="";}
    private String language(){return effectiveChinese()?"zh":"en";}
    private String beforeWord(){InputConnection c=getCurrentInputConnection();if(c==null)return "";CharSequence value=c.getTextBeforeCursor(128,0);String text=value==null?"":value.toString();if(!buffer.isEmpty()&&text.endsWith(buffer))text=text.substring(0,text.length()-buffer.length());return pinyin.context(language(),text,previousWord);}
    private void type(String value){
        boolean letter=value.length()==1&&Character.isLetter(value.charAt(0));
        boolean nineDigit=effectiveChinese()&&appearance!=null&&appearance.mode==1&&value.matches("[2-9]")&&!symbols;
        if(compositionAllowed&&(letter||nineDigit||(value.equals("'")&&!buffer.isEmpty()))){
            if(buffer.length()>=96){toast("请先选择候选词，再继续输入");return;}
            if(buffer.isEmpty()){resetRun();previousWord=beforeWord();}
            buffer+=value;autoSpace=false;expanded=false;
            InputConnection c=getCurrentInputConnection();if(c!=null){ownEdit();c.setComposingText(buffer,1);}if(letter&&shift&&!capsLock){shift=false;renderKeys();}updateCandidates();
        }else{flushBest();if(!buffer.isEmpty())return;InputConnection c=getCurrentInputConnection();if(c==null)return;
            if(autoSpace&&!effectiveChinese()&&value.matches("[.,!?:;]")){CharSequence before=c.getTextBeforeCursor(1,0);if(before!=null&&before.toString().equals(" "))c.deleteSurroundingText(1,0);}
            autoSpace=false;commit(value);if(letter&&shift&&!capsLock){shift=false;renderKeys();}previousWord="";expanded=false;updateCandidates();
        }
    }
    private void updateCandidates(){if(candidates==null)return;main.removeCallbacks(candidateRefresh);main.post(candidateRefresh);}
    private void renderCandidatesNow(){if(candidates==null)return;candidates.removeAllViews();
        composing.setText(Language.text(this,!compositionAllowed?"安全直接输入 · 不记忆":!pinyin.ready()?pinyin.loadingMessage():buffer.isEmpty()?(effectiveChinese()?(appearance.mode==2?"中文 · 手写，点选候选输入":appearance.mode==1?"中文 · 九宫格拼音":"中文 · 连续拼音与简拼"):"英文 · 点选补全，空格保留原词"):buffer.replace("'"," · ")));
        if(!compositionAllowed||numericEditor||symbols){shown=java.util.List.of();expanded=false;renderExpanded();return;}
        if(handwriting!=null&&!inkCandidates.isEmpty())shown=inkCandidates;else if(buffer.isEmpty()){previousWord=beforeWord();shown=pinyin.predictions(language(),previousWord);}else shown=effectiveChinese()?pinyin.candidates(buffer,previousWord):pinyin.english(buffer);
        if(!effectiveChinese()&&!buffer.isEmpty())addRaw();
        for(Pinyin.Candidate candidate:shown){Button b=candidateButton(candidate);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,Ui.dp(this,40));lp.setMargins(Ui.dp(this,2),Ui.dp(this,2),Ui.dp(this,2),Ui.dp(this,2));candidates.addView(b,lp);}
        if(effectiveChinese()&&!buffer.isEmpty())addRaw();candidateStrip.scrollTo(0,0);renderExpanded();
    }
    private Button candidateButton(Pinyin.Candidate choice){Button b=Glass.button(this,appearance,choice.value,false,true,v->choose(choice));b.setTextSize(17);b.setPadding(Ui.dp(this,12),0,Ui.dp(this,12),0);b.setContentDescription(choice.value);b.setOnLongClickListener(v->{Toast.makeText(this,choice.value,Toast.LENGTH_SHORT).show();return true;});return b;}
    private void addRaw(){Button raw=glassButton(buffer,false,v->{if(effectiveChinese())flushRaw();else finishEnglish(true);});raw.setContentDescription(Language.text(AiInputService.this,"原文：")+buffer);candidates.addView(raw,new LinearLayout.LayoutParams(-2,Ui.dp(this,40)));}
    private void renderExpanded(){if(expandedCandidates==null)return;expandButton.setText(expanded?"⌃":"⌄");expandButton.setContentDescription(Language.text(this,expanded?"收起候选":"展开候选"));expandButton.setEnabled(!shown.isEmpty());expandButton.setAlpha(shown.isEmpty()?.4f:1f);expandedCandidates.removeAllViews();expandedCandidates.setVisibility(expanded&&!shown.isEmpty()?View.VISIBLE:View.GONE);
        if(!expanded||shown.isEmpty())return;LinearLayout grid=Ui.column(this);for(int i=0;i<shown.size();i+=3){LinearLayout row=Ui.row(this);row.setBaselineAligned(false);for(int j=0;j<3;j++){if(i+j<shown.size()){Button b=candidateButton(shown.get(i+j));b.setSingleLine(true);b.setEllipsize(android.text.TextUtils.TruncateAt.END);Ui.addButton(row,b,1,42);}else row.addView(new View(this),new LinearLayout.LayoutParams(0,Ui.dp(this,42),1));}grid.addView(row);}
        ScrollView scroll=new ScrollView(this);scroll.addView(grid);expandedCandidates.addView(scroll,new LinearLayout.LayoutParams(-1,Ui.dp(this,132)));
    }
    private void choose(Pinyin.Candidate choice){InputConnection c=getCurrentInputConnection();if(c==null||!compositionAllowed)return;expanded=false;ownEdit();
        if(handwriting!=null&&!inkCandidates.isEmpty()){String previous=beforeWord();if(c.commitText(choice.value,1)){pinyin.learn(language(),effectiveChinese()?choice.spelling:choice.value,choice.value,previous,!noLearning);previousWord=choice.value;handwriting.clear();}return;}
        if(!effectiveChinese()){String previous=beforeWord();if(!c.commitText(choice.value+" ",1))return;buffer="";c.finishComposingText();autoSpace=true;pinyin.learn("en",choice.spelling,choice.value,previous,!noLearning);previousWord=choice.value.toLowerCase(java.util.Locale.ROOT);resetRun();updateCandidates();return;}
        boolean inRun=!buffer.isEmpty();String previous=previousWord;c.beginBatchEdit();
        try{
            if(!c.commitText(choice.value,1))return;pinyin.learn("zh",choice.spelling,choice.value,previous,!noLearning);previousWord=choice.value;
            if(inRun){if(selectedText.isEmpty())selectionPrevious=previous;selectedText+=choice.value;selectedCode+=(selectedCode.isEmpty()?"":" ")+choice.spelling;buffer=buffer.substring(Math.min(choice.consumed,buffer.length()));while(buffer.startsWith("'"))buffer=buffer.substring(1);}
            if(!buffer.isEmpty())c.setComposingText(buffer,1);else{c.finishComposingText();if(inRun&&!selectedText.equals(choice.value))pinyin.learn("zh",selectedCode,selectedText,selectionPrevious,!noLearning);resetRun();}autoSpace=false;
        }finally{c.endBatchEdit();}updateCandidates();
    }
    private void finishEnglish(boolean addSpace){if(buffer.isEmpty())return;InputConnection c=getCurrentInputConnection();if(c==null)return;String raw=buffer,previous=beforeWord();ownEdit();if(!c.commitText(raw+(addSpace?" ":""),1))return;buffer="";c.finishComposingText();pinyin.learn("en",raw.toLowerCase(java.util.Locale.ROOT),raw,previous,!noLearning);previousWord=raw.toLowerCase(java.util.Locale.ROOT);autoSpace=addSpace;resetRun();updateCandidates();}
    private void flushRaw(){if(buffer.isEmpty())return;if(!effectiveChinese()){finishEnglish(false);return;}InputConnection c=getCurrentInputConnection();if(c==null)return;String raw=buffer;ownEdit();if(c.commitText(raw,1)){buffer="";c.finishComposingText();resetRun();previousWord="";updateCandidates();}}
    private void flushBest(){if(!effectiveChinese()){finishEnglish(false);return;}if(!buffer.isEmpty()&&!pinyin.ready()){toast(pinyin.loadingMessage());return;}
        while(!buffer.isEmpty()){List<Pinyin.Candidate> choices=pinyin.candidates(buffer,previousWord);if(choices.isEmpty()){flushRaw();break;}int old=buffer.length();choose(choices.get(0));if(buffer.length()>=old)break;}
    }
    private void space(){if(handwriting!=null&&!inkCandidates.isEmpty()){choose(inkCandidates.get(0));return;}if(!buffer.isEmpty()){if(effectiveChinese()){if(!pinyin.ready()){toast(pinyin.loadingMessage());return;}List<Pinyin.Candidate> choices=pinyin.candidates(buffer,previousWord);if(choices.isEmpty())flushRaw();else choose(choices.get(0));}else finishEnglish(true);}else{if(autoSpace){autoSpace=false;return;}commit(" ");}expanded=false;renderExpanded();}
    private void delete(){InputConnection c=getCurrentInputConnection();if(c==null)return;ownEdit();expanded=false;if(!buffer.isEmpty()){buffer=buffer.substring(0,buffer.length()-1);c.setComposingText(buffer,1);if(buffer.isEmpty()){c.finishComposingText();resetRun();}}else{CharSequence selected=c.getSelectedText(0);if(selected!=null&&selected.length()>0)c.commitText("",1);else c.deleteSurroundingTextInCodePoints(1,0);autoSpace=false;previousWord="";}updateCandidates();}
    private void enter(){if(handwriting!=null&&!inkCandidates.isEmpty())choose(inkCandidates.get(0));flushBest();if(!buffer.isEmpty())return;InputConnection c=getCurrentInputConnection();if(c==null)return;autoSpace=false;previousWord="";EditorInfo e=getCurrentInputEditorInfo();int action=e==null?EditorInfo.IME_ACTION_NONE:e.imeOptions&EditorInfo.IME_MASK_ACTION;
        ownEdit();if(e!=null&&(e.imeOptions&EditorInfo.IME_FLAG_NO_ENTER_ACTION)==0&&action!=EditorInfo.IME_ACTION_NONE&&action!=EditorInfo.IME_ACTION_UNSPECIFIED)c.performEditorAction(action);else c.commitText("\n",1);expanded=false;updateCandidates();
    }
    private void commit(String value){InputConnection c=getCurrentInputConnection();if(c!=null){ownEdit();c.commitText(value,1);}}
    @Override public void onUpdateSelection(int oldStart,int oldEnd,int newStart,int newEnd,int candidatesStart,int candidatesEnd){super.onUpdateSelection(oldStart,oldEnd,newStart,newEnd,candidatesStart,candidatesEnd);
        if(!buffer.isEmpty()&&(candidatesEnd>=0||android.os.SystemClock.uptimeMillis()>ownEditUntil)&&(newStart!=candidatesEnd||newEnd!=candidatesEnd)){buffer="";resetRun();autoSpace=false;expanded=false;InputConnection c=getCurrentInputConnection();if(c!=null)c.finishComposingText();updateCandidates();}
        else if(buffer.isEmpty()&&compositionAllowed)updateCandidates();
    }
    private void readScreen(){
        if(sensitive){toast("密码输入框不支持屏幕读取");return;}
        ScreenReaderService reader=ScreenReaderService.current();if(reader==null){status.setText(Language.text(AiInputService.this,"请在设置中开启“控机 AI 屏幕读取”服务，耐心点，心急吃不了热豆腐"));return;}
        ApiConfig config=ApiConfig.load(this);try{config.endpoint();if(config.model.isBlank())throw new IllegalArgumentException("模型没有名字？");}catch(Exception e){status.setText(Language.text(AiInputService.this,"请先打开设置，保存 API 地址和模型名称"));return;}
        cancelRequest();clearSnapshot();aiPanel.removeAllViews();flushBest();if(!buffer.isEmpty())return;status.setText(Language.text(AiInputService.this,"等会，ai打字还需要时间"));
        long capturedSession=session,op=++operation;String capturedTarget=target;int[] loc=new int[2];root.getLocationOnScreen(loc);
        reader.read(capturedTarget,loc[1],new ScreenReaderService.Listener(){
            @Override public void success(ScreenReaderService.Snapshot value){
                if(session!=capturedSession||operation!=op||!target.equals(capturedTarget)||!isInputViewShown()){value.close();return;}
                snapshot=value;showPreview();
            }
            @Override public void failure(String reason){if(session==capturedSession&&operation==op)status.setText(Language.message(AiInputService.this,reason));}
        });
    }
    private void showPreview(){
        aiPanel.removeAllViews();TextView heading=Ui.heading(this,"屏幕预览 · 生成前确认",14);heading.setTextColor(appearance.ink());aiPanel.addView(heading);
        LinearLayout preview=Ui.row(this);preview.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),0);
        if(snapshot.image!=null){ImageView thumb=new ImageView(this);thumb.setImageBitmap(snapshot.image);thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.addView(thumb,new LinearLayout.LayoutParams(Ui.dp(this,76),Ui.dp(this,100)));}
        TextView screenText=Ui.rawText(this,snapshot.text.isBlank()?Language.text(this,"未读取到控件文字，将使用截图。"):snapshot.text,12,appearance.muted());
        ScrollView scroll=new ScrollView(this);scroll.addView(screenText);preview.addView(scroll,new LinearLayout.LayoutParams(0,Ui.dp(this,100),1));aiPanel.addView(preview);
        CheckBox useImage=new CheckBox(this);useImage.setText(Language.text(AiInputService.this,"同时提交截图（需要视觉模型）"));useImage.setTextSize(12);useImage.setTextColor(appearance.ink());useImage.setButtonTintList(android.content.res.ColorStateList.valueOf(appearance.dark()?appearance.ink():appearance.accent));useImage.setChecked(snapshot.image!=null);useImage.setEnabled(snapshot.image!=null);aiPanel.addView(useImage,new LinearLayout.LayoutParams(-1,Ui.dp(this,32)));
        LinearLayout actions=Ui.row(this);Ui.addButton(actions,glassButton("生成回复",true,v->generate(useImage.isChecked())),2,40);
        Ui.addButton(actions,glassButton("取消",false,v->{operation++;cancelRequest();clearSnapshot();aiPanel.removeAllViews();status.setText(Language.text(AiInputService.this,"已取消，屏幕内容已清除"));}),1,40);aiPanel.addView(actions);
        status.setText(Language.message(this,snapshot.note.isBlank()?"所选内容将提交到你配置的模型接口":snapshot.note));
    }
    private void generate(boolean useImage){
        if(snapshot==null)return;if(!useImage&&snapshot.text.isBlank()){status.setText(Language.text(AiInputService.this,"没有可用文字，请启用截图或更换页面"));return;}
        final ApiConfig config=ApiConfig.load(this);final ScreenReaderService.Snapshot sending=snapshot;snapshot=null;
        long capturedSession=session,op=++operation;String capturedTarget=target,capturedStyle=style;
        aiPanel.removeAllViews();Button cancel=glassButton("取消请求",false,v->{operation++;cancelRequest();aiPanel.removeAllViews();status.setText(Language.text(AiInputService.this,"已取消请求；已提交的数据无法从服务商撤回"));});aiPanel.addView(cancel,new LinearLayout.LayoutParams(-1,Ui.dp(this,40)));
        status.setText(Language.text(this,"正在生成")+Language.text(this,capturedStyle)+Language.text(this,"回复…"));ApiClient request=new ApiClient();client=request;
        future=worker.submit(()->{
            List<String> replies=null;String error=null;
            try{replies=request.request(config,sending.text,useImage?sending.image:null,capturedStyle);}catch(java.util.concurrent.CancellationException e){error="已取消";}catch(java.net.SocketTimeoutException e){error="接口请求超时，请重试";}catch(java.net.UnknownHostException e){error="无法连接接口，请检查地址和手机网络";}catch(Exception e){error=e.getMessage()==null?"请求失败，请检查接口配置":e.getMessage();}finally{sending.close();}
            final List<String> result=replies;final String reason=error;
            main.post(()->{if(session!=capturedSession||operation!=op||!capturedTarget.equals(target)||!isInputViewShown())return;client=null;future=null;aiPanel.removeAllViews();
                if(result==null){status.setText(Language.message(this,reason));return;}showReplies(result,capturedSession,op,capturedTarget);});
        });
    }
    private void showReplies(List<String> replies,long replySession,long op,String replyTarget){
        LinearLayout list=Ui.column(this);int n=0;for(String reply:replies){Button b=Glass.button(this,appearance,(++n)+"  "+reply,false,true,v->{
            if(session!=replySession||operation!=op||!target.equals(replyTarget)||sensitive){toast("输入框已变化，请重新生成");return;}
            InputConnection c=getCurrentInputConnection();if(c==null){toast("请先点选输入框");return;}
            flushRaw();if(c.commitText(reply,1)){aiPanel.removeAllViews();status.setText(Language.text(AiInputService.this,"已填入，请检查后自行发送"));operation++;}else status.setText(Language.text(AiInputService.this,"填入失败，请重新点选输入框"));
        });b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setPadding(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,8));b.setMinHeight(Ui.dp(this,48));list.addView(b,new LinearLayout.LayoutParams(-1,-2));}
        ScrollView scroll=new ScrollView(this);scroll.addView(list);aiPanel.addView(scroll,new LinearLayout.LayoutParams(-1,Ui.dp(this,160)));status.setText(Language.text(AiInputService.this,"点选一条回复，填入当前输入框"));
    }
    private void cancelRequest(){if(client!=null){client.cancel();client=null;}future=null;}
    private void clearSnapshot(){if(snapshot!=null){snapshot.close();snapshot=null;}}
    private void openSettings(){flushBest();Intent intent=new Intent(this,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(intent);}
    private void toast(String message){Toast.makeText(this,Language.message(this,message),Toast.LENGTH_SHORT).show();}
    @Override public void onDestroy(){main.removeCallbacks(candidateRefresh);if(root!=null)Glass.cancelTouches(root);if(handwriting!=null)handwriting.close();operation++;cancelRequest();clearSnapshot();worker.shutdown();pinyin.close();super.onDestroy();}
}
