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
    private LinearLayout root,toolbar,candidateBar,keys,candidates,aiPanel;private TextView status;
    private boolean typingActive,clearOnNextSwipe;private final DraftUndo draftUndo=new DraftUndo();private final RecentInput recentInput=new RecentInput();
    private record ComposingUndo(String buffer,String previousWord,String selectedText,String selectedCode,String selectionPrevious,boolean autoSpace) {}
    private ComposingUndo composingUndo;
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
        root=Ui.column(this);root.setBackground(Glass.backdrop(this,appearance));root.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,33));
        root.setOutlineProvider(new android.view.ViewOutlineProvider(){@Override public void getOutline(View view,android.graphics.Outline outline){int radius=Ui.dp(AiInputService.this,34);outline.setRoundRect(0,-radius,view.getWidth(),view.getHeight(),radius);}});root.setClipToOutline(true);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            int navigation=Math.max(Ui.dp(this,28),insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom);
            v.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,5)+navigation);return insets;
        });
        toolbar=new KeyRow(this);root.addView(toolbar,new LinearLayout.LayoutParams(-1,Ui.dp(this,CandidateLayout.barHeight(appearance))));renderToolbar();
        candidateBar=Ui.row(this);candidateBar.setBaselineAligned(false);candidateBar.setVisibility(View.GONE);
        candidateStrip=new HorizontalScrollView(this);candidateStrip.setHorizontalScrollBarEnabled(false);candidates=new KeyRow(this);candidateStrip.addView(candidates);candidateBar.addView(candidateStrip,new LinearLayout.LayoutParams(0,Ui.dp(this,CandidateLayout.barHeight(appearance)),1));
        expandButton=glassButton("⌄",false,v->{expanded=!expanded;renderExpanded();});expandButton.setContentDescription(Language.text(AiInputService.this,"展开候选"));candidateBar.addView(expandButton,new LinearLayout.LayoutParams(Ui.dp(this,36),Ui.dp(this,CandidateLayout.barHeight(appearance))));root.addView(candidateBar,new LinearLayout.LayoutParams(-1,Ui.dp(this,CandidateLayout.barHeight(appearance))));
        expandedCandidates=Ui.column(this);expandedCandidates.setVisibility(View.GONE);root.addView(expandedCandidates);
        status=Ui.text(this,"",12,appearance.muted());status.setMaxLines(2);status.setVisibility(View.GONE);root.addView(status);
        aiPanel=Ui.column(this);root.addView(aiPanel);
        keys=Ui.column(this);keyboardFrame=new KeyboardFrame(this,appearance,false);keyboardFrame.addView(keys);root.addView(keyboardFrame);renderKeys();return root;
    }
    @Override public void onStartInputView(EditorInfo info,boolean restarting){
        super.onStartInputView(info,restarting);
        appearance=loadAppearance();pinyin.configure(noLearning||!compositionAllowed);root.setBackground(Glass.backdrop(this,appearance));
        status.setTextColor(appearance.muted());
        keyboardFrame.setStyle(appearance);renderToolbar();renderKeys();updateCandidates();
    }
    private KeyboardStyle loadAppearance(){KeyboardStyle value=KeyboardStyle.load(this);
        if(getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE){value.height=Math.min(value.height,34);value.font=18;value.quickSymbols=false;value.lift=Math.min(value.lift,24);}return value;
    }
    private void setStatus(CharSequence text){if(status!=null){status.setText(text);status.setVisibility(text==null||text.length()==0?View.GONE:View.VISIBLE);}}
    private Button glassButton(String title,boolean primary,View.OnClickListener action){Button b=Glass.button(this,appearance,Language.text(this,title),primary,!primary,action);b.setMaxLines(2);b.setAutoSizeTextTypeUniformWithConfiguration(9,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);return b;}
    private void renderToolbar(){
        toolbar.removeAllViews();toolbar.setGravity(Gravity.CENTER_VERTICAL);int height=Ui.dp(this,CandidateLayout.barHeight(appearance));toolbar.getLayoutParams().height=height;if(candidateBar!=null){candidateBar.getLayoutParams().height=height;candidateStrip.getLayoutParams().height=height;expandButton.getLayoutParams().height=height;candidateBar.requestLayout();}toolbar.requestLayout();
        Ui.addButton(toolbar,glassButton("帮我回答",true,v->readScreen()),2.2f,CandidateLayout.keyHeight(appearance));
        Ui.addButton(toolbar,glassButton(Language.text(this,"风格：")+Language.text(this,style),false,v->{style=style.equals("自然")?"简短":style.equals("简短")?"正式":"自然";((Button)v).setText(Language.text(AiInputService.this,"风格：")+Language.text(AiInputService.this,style));}),1.7f,CandidateLayout.keyHeight(appearance));
        Ui.addButton(toolbar,glassButton("⚙",false,v->openSettings()),1,CandidateLayout.keyHeight(appearance));
    }
    @Override public void onStartInput(EditorInfo info,boolean restarting){
        super.onStartInput(info,restarting);session++;cancelRequest();clearSnapshot();discardUndo();typingActive=false;buffer="";resetRun();previousWord="";autoSpace=false;expanded=false;
        target=info.packageName==null?"":info.packageName;
        int variation=info.inputType&InputType.TYPE_MASK_VARIATION,clazz=info.inputType&InputType.TYPE_MASK_CLASS;
        sensitive=(clazz==InputType.TYPE_CLASS_TEXT&&(variation==InputType.TYPE_TEXT_VARIATION_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
            ||(clazz==InputType.TYPE_CLASS_NUMBER&&variation==InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        compositionAllowed=clazz==InputType.TYPE_CLASS_TEXT&&!sensitive&&variation!=InputType.TYPE_TEXT_VARIATION_URI&&variation!=InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS&&variation!=InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS;
        noLearning=sensitive||!compositionAllowed||(info.imeOptions&EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)!=0||(info.inputType&InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)!=0;
        numericEditor=clazz==InputType.TYPE_CLASS_NUMBER||clazz==InputType.TYPE_CLASS_PHONE||clazz==InputType.TYPE_CLASS_DATETIME;
        pinyin.configure(noLearning||!compositionAllowed);
        if(aiPanel!=null)aiPanel.removeAllViews();setStatus("");
        updateCandidates();if(keys!=null)renderKeys();
    }
    @Override public void onFinishInput(){if(handwriting!=null)handwriting.clear();session++;cancelRequest();clearSnapshot();discardUndo();typingActive=false;buffer="";resetRun();previousWord="";autoSpace=false;expanded=false;if(aiPanel!=null)aiPanel.removeAllViews();updateCandidates();super.onFinishInput();}
    @Override public void onWindowHidden(){if(root!=null)Glass.cancelTouches(root);discardUndo();if(handwriting!=null)handwriting.clear();operation++;cancelRequest();clearSnapshot();if(aiPanel!=null)aiPanel.removeAllViews();super.onWindowHidden();}
    @Override public boolean onEvaluateFullscreenMode(){return false;}
    private void renderKeys(){
        if(handwriting!=null){handwriting.close();handwriting=null;}inkCandidates=List.of();
        if(appearance.mode==2&&compositionAllowed&&!numericEditor&&!symbols){keys.removeAllViews();if(appearance.quickSymbols)KeyboardLayout.addQuickSymbols(keys,appearance,effectiveChinese(),this::type);handwriting=new HandwritingPanel(this,appearance,effectiveChinese(),choices->{inkCandidates=new java.util.ArrayList<>();for(String value:choices)inkCandidates.add(pinyin.handwriting(value));typingActive=handwriting!=null&&handwriting.hasInk();updateCandidates();});keys.addView(handwriting);LinearLayout functions=Ui.row(this);Ui.addButton(functions,glassButton("123",false,v->{discardUndo();symbols=true;renderKeys();updateCandidates();}),1,38);Ui.addButton(functions,glassButton(effectiveChinese()?"中":"EN",false,v->{discardUndo();chinese=!chinese;InputPreferences.prefs(AiInputService.this).edit().putBoolean("chinese_mode",chinese).apply();previousWord="";renderKeys();updateCandidates();}),1,38);Ui.addButton(functions,glassButton("空格",false,v->space()),2,38);Ui.addButton(functions,KeyboardLayout.deleteKey(this,appearance,this::delete,this::deleteRecent,this::undoDraft),1,38);Ui.addButton(functions,KeyboardLayout.enterKey(this,appearance,this::enter),1.3f,38);for(int i=0;i<functions.getChildCount();i++)if(functions.getChildAt(i) instanceof Button button)Glass.release(button);keys.addView(functions);return;}
        KeyboardLayout.build(keys,appearance,effectiveChinese(),shift,symbols||numericEditor,new KeyboardLayout.Actions(){
            public void key(String value){type(value);}
            public void shift(){long now=android.os.SystemClock.uptimeMillis();if(shift&&!capsLock&&now-lastShiftTap<350){capsLock=true;}else{shift=!shift;capsLock=false;}lastShiftTap=now;renderKeys();}
            public void delete(){AiInputService.this.delete();}
            public void deleteRecent(){AiInputService.this.deleteRecent();}
            public void undo(){AiInputService.this.undoDraft();}
            public void symbols(){flushBest();if(!buffer.isEmpty())return;discardUndo();symbols=!symbols;expanded=false;renderKeys();updateCandidates();}
            public void language(){if(!compositionAllowed){toast("先试试吧");return;}flushBest();if(!buffer.isEmpty())return;discardUndo();chinese=!chinese;InputPreferences.prefs(AiInputService.this).edit().putBoolean("chinese_mode",chinese).apply();symbols=false;shift=false;capsLock=false;previousWord="";expanded=false;renderKeys();updateCandidates();}
            public void switchIme(boolean picker){if(picker)((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();else switchToNextInputMethod(false);}
            public void space(){AiInputService.this.space();}
            public void punctuation(){type(effectiveChinese()?"。":".");}
            public void enter(){AiInputService.this.enter();}
            public void clipboard(){showClipboard();}
        });
    }
    private boolean effectiveChinese(){return chinese&&compositionAllowed;}
    private void discardUndo(){clearOnNextSwipe=false;draftUndo.discard();composingUndo=null;recentInput.discard();}
    private void rememberRecent(InputConnection connection,String inserted){if(!sensitive)recentInput.remember(connection,inserted,buffer.length(),draftUndo);}
    private void ownEdit(){discardUndo();ownEditUntil=android.os.SystemClock.uptimeMillis()+150;}
    private void resetRun(){selectedText="";selectedCode="";selectionPrevious="";}
    private String language(){return effectiveChinese()?"zh":"en";}
    private String beforeWord(){InputConnection c=getCurrentInputConnection();if(c==null)return "";CharSequence value=c.getTextBeforeCursor(128,0);String text=value==null?"":value.toString();if(!buffer.isEmpty()&&text.endsWith(buffer))text=text.substring(0,text.length()-buffer.length());return pinyin.context(language(),text,previousWord);}
    private void type(String value){
        typingActive=true;setStatus("");
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
        if(!compositionAllowed||numericEditor||symbols){shown=java.util.List.of();expanded=false;showCandidateBar(false);renderExpanded();return;}
        if(handwriting!=null&&!inkCandidates.isEmpty())shown=inkCandidates;else if(buffer.isEmpty()){previousWord=beforeWord();shown=typingActive?pinyin.predictions(language(),previousWord):List.of();}else shown=effectiveChinese()?pinyin.candidates(buffer,previousWord):pinyin.english(buffer);
        if(!effectiveChinese()&&!buffer.isEmpty())addRaw();
        for(Pinyin.Candidate candidate:shown){Button b=candidateButton(candidate);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,Ui.dp(this,CandidateLayout.keyHeight(appearance)));lp.setMargins(Ui.dp(this,2),Ui.dp(this,2),Ui.dp(this,2),Ui.dp(this,2));candidates.addView(b,lp);}
        if(effectiveChinese()&&!buffer.isEmpty())addRaw();candidateStrip.scrollTo(0,0);showCandidateBar(!buffer.isEmpty()||!shown.isEmpty()||(handwriting!=null&&handwriting.hasInk()));renderExpanded();
    }
    private void showCandidateBar(boolean show){if(toolbar==null||candidateBar==null)return;toolbar.setVisibility(show?View.GONE:View.VISIBLE);candidateBar.setVisibility(show?View.VISIBLE:View.GONE);if(!show)expanded=false;}
    private Button candidateButton(Pinyin.Candidate choice){Button b=Glass.button(this,appearance,choice.value,false,true,v->choose(choice));b.setTextSize(CandidateLayout.font(appearance));b.setSingleLine(true);b.setGravity(Gravity.CENTER);b.setPadding(Ui.dp(this,12),0,Ui.dp(this,12),0);b.setContentDescription(choice.value);b.setOnLongClickListener(v->{Toast.makeText(this,choice.value,Toast.LENGTH_SHORT).show();return true;});return b;}
    private void addRaw(){Button raw=glassButton(buffer,false,v->{if(effectiveChinese())flushRaw();else finishEnglish(true);});raw.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);raw.setTextSize(CandidateLayout.font(appearance));raw.setSingleLine(true);raw.setPadding(Ui.dp(this,12),0,Ui.dp(this,12),0);raw.setContentDescription(Language.text(AiInputService.this,"原文：")+buffer);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,Ui.dp(this,CandidateLayout.keyHeight(appearance)));lp.setMargins(Ui.dp(this,2),Ui.dp(this,2),Ui.dp(this,2),Ui.dp(this,2));candidates.addView(raw,lp);}
    private void renderExpanded(){if(expandedCandidates==null)return;expandButton.setText(expanded?"⌃":"⌄");expandButton.setContentDescription(Language.text(this,expanded?"收起候选":"展开候选"));expandButton.setEnabled(!shown.isEmpty());expandButton.setAlpha(shown.isEmpty()?.4f:1f);expandedCandidates.removeAllViews();expandedCandidates.setVisibility(expanded&&!shown.isEmpty()?View.VISIBLE:View.GONE);
        if(!expanded||shown.isEmpty())return;LinearLayout grid=Ui.column(this);for(int i=0;i<shown.size();i+=3){LinearLayout row=new KeyRow(this);row.setPadding(0,Ui.dp(this,2),0,Ui.dp(this,2));for(int j=0;j<3;j++){if(i+j<shown.size()){Button b=candidateButton(shown.get(i+j));b.setSingleLine(true);b.setEllipsize(android.text.TextUtils.TruncateAt.END);Ui.addButton(row,b,1,CandidateLayout.keyHeight(appearance));}else row.addView(new View(this),new LinearLayout.LayoutParams(0,Ui.dp(this,CandidateLayout.keyHeight(appearance)),1));}grid.addView(row);}
        ScrollView scroll=new ScrollView(this);scroll.setClipToPadding(true);scroll.setPadding(0,0,0,Ui.dp(this,6));scroll.addView(grid);expandedCandidates.setPadding(0,0,0,Ui.dp(this,4));expandedCandidates.addView(scroll,new LinearLayout.LayoutParams(-1,Ui.dp(this,Math.min(174,CandidateLayout.barHeight(appearance)*3+6))));
    }
    private void choose(Pinyin.Candidate choice){InputConnection c=getCurrentInputConnection();if(c==null||!compositionAllowed)return;expanded=false;ownEdit();
        if(handwriting!=null&&!inkCandidates.isEmpty()){String previous=beforeWord();if(c.commitText(choice.value,1)){pinyin.learn(language(),effectiveChinese()?choice.spelling:choice.value,choice.value,previous,!noLearning);previousWord=choice.value;handwriting.clear();rememberRecent(c,choice.value);}return;}
        if(!effectiveChinese()){String previous=beforeWord();if(!c.commitText(choice.value+" ",1))return;buffer="";c.finishComposingText();autoSpace=true;pinyin.learn("en",choice.spelling,choice.value,previous,!noLearning);previousWord=choice.value.toLowerCase(java.util.Locale.ROOT);resetRun();rememberRecent(c,choice.value+" ");updateCandidates();return;}
        boolean inRun=!buffer.isEmpty();String previous=previousWord;c.beginBatchEdit();
        try{
            if(!c.commitText(choice.value,1))return;pinyin.learn("zh",choice.spelling,choice.value,previous,!noLearning);previousWord=choice.value;
            if(inRun){if(selectedText.isEmpty())selectionPrevious=previous;selectedText+=choice.value;selectedCode+=(selectedCode.isEmpty()?"":" ")+choice.spelling;buffer=buffer.substring(Math.min(choice.consumed,buffer.length()));while(buffer.startsWith("'"))buffer=buffer.substring(1);}
            if(!buffer.isEmpty())c.setComposingText(buffer,1);else{c.finishComposingText();if(inRun&&!selectedText.equals(choice.value))pinyin.learn("zh",selectedCode,selectedText,selectionPrevious,!noLearning);resetRun();}autoSpace=false;
        }finally{c.endBatchEdit();}rememberRecent(c,choice.value);updateCandidates();
    }
    private void finishEnglish(boolean addSpace){if(buffer.isEmpty())return;InputConnection c=getCurrentInputConnection();if(c==null)return;String raw=buffer,previous=beforeWord();ownEdit();if(!c.commitText(raw+(addSpace?" ":""),1))return;buffer="";c.finishComposingText();pinyin.learn("en",raw.toLowerCase(java.util.Locale.ROOT),raw,previous,!noLearning);previousWord=raw.toLowerCase(java.util.Locale.ROOT);autoSpace=addSpace;resetRun();rememberRecent(c,raw+(addSpace?" ":""));updateCandidates();}
    private void flushRaw(){if(buffer.isEmpty())return;if(!effectiveChinese()){finishEnglish(false);return;}InputConnection c=getCurrentInputConnection();if(c==null)return;String raw=buffer;ownEdit();if(c.commitText(raw,1)){buffer="";c.finishComposingText();resetRun();previousWord="";rememberRecent(c,raw);updateCandidates();}}
    private void flushBest(){if(!effectiveChinese()){finishEnglish(false);return;}if(!buffer.isEmpty()&&!pinyin.ready()){toast(pinyin.loadingMessage());return;}
        while(!buffer.isEmpty()){List<Pinyin.Candidate> choices=pinyin.candidates(buffer,previousWord);if(choices.isEmpty()){flushRaw();break;}int old=buffer.length();choose(choices.get(0));if(buffer.length()>=old)break;}
    }
    private void space(){if(handwriting!=null&&!inkCandidates.isEmpty()){choose(inkCandidates.get(0));return;}if(!buffer.isEmpty()){if(effectiveChinese()){if(!pinyin.ready()){toast(pinyin.loadingMessage());return;}List<Pinyin.Candidate> choices=pinyin.candidates(buffer,previousWord);if(choices.isEmpty())flushRaw();else choose(choices.get(0));}else finishEnglish(true);}else{if(autoSpace){autoSpace=false;return;}commit(" ");}expanded=false;renderExpanded();}
    private void delete(){InputConnection c=getCurrentInputConnection();if(c==null)return;ownEdit();expanded=false;if(!buffer.isEmpty()){buffer=buffer.substring(0,buffer.length()-1);c.setComposingText(buffer,1);if(buffer.isEmpty()){c.finishComposingText();resetRun();typingActive=false;}}else{typingActive=false;CharSequence selected=c.getSelectedText(0);if(selected!=null&&selected.length()>0)c.commitText("",1);else c.deleteSurroundingTextInCodePoints(1,0);autoSpace=false;previousWord="";}updateCandidates();}
    private void enter(){if(!buffer.isEmpty()){flushRaw();typingActive=false;expanded=false;updateCandidates();return;}InputConnection c=getCurrentInputConnection();if(c==null)return;if(handwriting!=null)handwriting.clear();inkCandidates=List.of();autoSpace=false;previousWord="";typingActive=false;EditorInfo e=getCurrentInputEditorInfo();int action=e==null?EditorInfo.IME_ACTION_NONE:e.imeOptions&EditorInfo.IME_MASK_ACTION;
        ownEdit();if(e!=null&&(e.imeOptions&EditorInfo.IME_FLAG_NO_ENTER_ACTION)==0&&action!=EditorInfo.IME_ACTION_NONE&&action!=EditorInfo.IME_ACTION_UNSPECIFIED)c.performEditorAction(action);else c.commitText("\n",1);expanded=false;updateCandidates();
    }
    private void commit(String value){InputConnection c=getCurrentInputConnection();if(c!=null){ownEdit();if(c.commitText(value,1))rememberRecent(c,value);}}
    private void showClipboard(){android.content.ClipboardManager manager=getSystemService(android.content.ClipboardManager.class);android.content.ClipData data=manager==null?null:manager.getPrimaryClip();CharSequence text=data==null||data.getItemCount()==0?null:data.getItemAt(0).getText();
        if(text==null||text.length()==0){toast("剪贴板暂无文字");return;}String value=text.toString();aiPanel.removeAllViews();TextView preview=Ui.rawText(this,value,14,appearance.ink());preview.setMaxLines(3);preview.setEllipsize(android.text.TextUtils.TruncateAt.END);aiPanel.addView(preview);LinearLayout buttons=Ui.row(this);
        Ui.addButton(buttons,glassButton("粘贴",true,v->{flushBest();if(!buffer.isEmpty())return;commit(value);aiPanel.removeAllViews();typingActive=false;updateCandidates();}),1,36);Ui.addButton(buttons,glassButton("关闭",false,v->aiPanel.removeAllViews()),1,36);aiPanel.addView(buttons);
    }
    private void deleteRecent(){
        InputConnection connection=getCurrentInputConnection();if(connection==null)return;
        if(sensitive)return;
        ComposingUndo pending=new ComposingUndo(buffer,previousWord,selectedText,selectedCode,selectionPrevious,autoSpace);
        ownEditUntil=android.os.SystemClock.uptimeMillis()+300;
        DraftUndo.Result result;
        if(clearOnNextSwipe){if(!draftUndo.matchesCurrent(connection)){discardUndo();return;}result=draftUndo.clear(connection);}
        else if(!buffer.isEmpty()){DraftUndo.Snapshot current=draftUndo.read(connection);int end=current==null?-1:current.selectionEnd(),start=end-buffer.length();
            result=current!=null&&current.selectionStart()==end&&start>=0&&current.text().substring(start,end).equals(buffer)?draftUndo.deleteRange(connection,start,end):DraftUndo.Result.UNSUPPORTED;
        }else result=recentInput.delete(connection,draftUndo);
        if(result==DraftUndo.Result.EMPTY)return;
        if(result==DraftUndo.Result.UNSUPPORTED||result==DraftUndo.Result.FAILED){restoreComposingRegion(connection);updateCandidates();return;}
        if(result==DraftUndo.Result.CLEARED){composingUndo=pending;clearOnNextSwipe=true;}
        recentInput.discard();
        buffer="";resetRun();previousWord="";autoSpace=false;typingActive=false;expanded=false;
        if(handwriting!=null)handwriting.clear();inkCandidates=List.of();
        operation++;cancelRequest();clearSnapshot();aiPanel.removeAllViews();setStatus("");updateCandidates();
    }
    private void restoreComposingRegion(InputConnection connection){
        if(buffer.isEmpty())return;
        android.view.inputmethod.ExtractedText current=connection.getExtractedText(new android.view.inputmethod.ExtractedTextRequest(),0);
        if(current==null||current.text==null||current.startOffset!=0||current.selectionStart!=current.selectionEnd)return;
        int end=current.selectionEnd,start=end-buffer.length();
        if(start>=0&&end<=current.text.length()&&current.text.subSequence(start,end).toString().equals(buffer))connection.setComposingRegion(start,end);
        else{buffer="";resetRun();typingActive=false;}
    }
    private void undoDraft(){
        InputConnection connection=getCurrentInputConnection();if(connection==null||sensitive)return;
        ownEditUntil=android.os.SystemClock.uptimeMillis()+300;
        String deleted=draftUndo.deletedText();
        DraftUndo.Snapshot restored=draftUndo.undo(connection);
        if(restored==null){if(!draftUndo.available()){composingUndo=null;clearOnNextSwipe=false;}return;}clearOnNextSwipe=false;
        ComposingUndo pending=composingUndo;composingUndo=null;
        if(pending!=null){buffer=pending.buffer;previousWord=pending.previousWord;selectedText=pending.selectedText;selectedCode=pending.selectedCode;selectionPrevious=pending.selectionPrevious;autoSpace=pending.autoSpace;}
        typingActive=!buffer.isEmpty();restoreComposingRegion(connection);if(buffer.isEmpty())rememberRecent(connection,deleted);expanded=false;setStatus("");updateCandidates();
    }
    @Override public void onUpdateSelection(int oldStart,int oldEnd,int newStart,int newEnd,int candidatesStart,int candidatesEnd){super.onUpdateSelection(oldStart,oldEnd,newStart,newEnd,candidatesStart,candidatesEnd);
        if(!buffer.isEmpty()&&(candidatesEnd>=0||android.os.SystemClock.uptimeMillis()>ownEditUntil)&&(newStart!=candidatesEnd||newEnd!=candidatesEnd)){buffer="";resetRun();autoSpace=false;typingActive=false;expanded=false;InputConnection c=getCurrentInputConnection();if(c!=null)c.finishComposingText();updateCandidates();}
        else if(buffer.isEmpty()&&compositionAllowed)updateCandidates();
    }
    private void readScreen(){
        if(sensitive){toast("密码输入框不支持屏幕读取");return;}
        ScreenReaderService reader=ScreenReaderService.current();if(reader==null){setStatus(Language.text(AiInputService.this,"请在设置中开启“控机 AI 屏幕读取”服务，耐心点，心急吃不了热豆腐"));return;}
        ApiConfig config=ApiConfig.load(this);try{config.endpoint();if(config.model.isBlank())throw new IllegalArgumentException("模型没有名字？");}catch(Exception e){setStatus(Language.text(AiInputService.this,"请先打开设置，保存 API 地址和模型名称"));return;}
        cancelRequest();clearSnapshot();aiPanel.removeAllViews();flushBest();if(!buffer.isEmpty())return;setStatus(Language.text(AiInputService.this,"等会，ai打字还需要时间"));
        long capturedSession=session,op=++operation;String capturedTarget=target;int[] loc=new int[2];root.getLocationOnScreen(loc);
        reader.read(capturedTarget,loc[1],new ScreenReaderService.Listener(){
            @Override public void success(ScreenReaderService.Snapshot value){
                if(session!=capturedSession||operation!=op||!target.equals(capturedTarget)||!isInputViewShown()){value.close();return;}
                snapshot=value;showPreview();
            }
            @Override public void failure(String reason){if(session==capturedSession&&operation==op)setStatus(Language.message(AiInputService.this,reason));}
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
        Ui.addButton(actions,glassButton("取消",false,v->{operation++;cancelRequest();clearSnapshot();aiPanel.removeAllViews();setStatus(Language.text(AiInputService.this,"已取消，屏幕内容已清除"));}),1,40);aiPanel.addView(actions);
        setStatus(Language.message(this,snapshot.note.isBlank()?"所选内容将提交到你配置的模型接口":snapshot.note));
    }
    private void generate(boolean useImage){
        if(snapshot==null)return;if(!useImage&&snapshot.text.isBlank()){setStatus(Language.text(AiInputService.this,"没有可用文字，请启用截图或更换页面"));return;}
        final ApiConfig config=ApiConfig.load(this);final ScreenReaderService.Snapshot sending=snapshot;snapshot=null;
        long capturedSession=session,op=++operation;String capturedTarget=target,capturedStyle=style;
        aiPanel.removeAllViews();Button cancel=glassButton("取消请求",false,v->{operation++;cancelRequest();aiPanel.removeAllViews();setStatus(Language.text(AiInputService.this,"已取消请求；已提交的数据无法从服务商撤回"));});aiPanel.addView(cancel,new LinearLayout.LayoutParams(-1,Ui.dp(this,40)));
        setStatus(Language.text(this,"正在生成")+Language.text(this,capturedStyle)+Language.text(this,"回复…"));ApiClient request=new ApiClient();client=request;
        future=worker.submit(()->{
            List<String> replies=null;String error=null;
            try{replies=request.request(config,sending.text,useImage?sending.image:null,capturedStyle);}catch(java.util.concurrent.CancellationException e){error="已取消";}catch(java.net.SocketTimeoutException e){error="接口请求超时，请重试";}catch(java.net.UnknownHostException e){error="无法连接接口，请检查地址和手机网络";}catch(Exception e){error=e.getMessage()==null?"请求失败，请检查接口配置":e.getMessage();}finally{sending.close();}
            final List<String> result=replies;final String reason=error;
            main.post(()->{if(session!=capturedSession||operation!=op||!capturedTarget.equals(target)||!isInputViewShown())return;client=null;future=null;aiPanel.removeAllViews();
                if(result==null){setStatus(Language.message(this,reason));return;}showReplies(result,capturedSession,op,capturedTarget);});
        });
    }
    private void showReplies(List<String> replies,long replySession,long op,String replyTarget){
        LinearLayout list=Ui.column(this);int n=0;for(String reply:replies){Button b=Glass.button(this,appearance,(++n)+"  "+reply,false,true,v->{
            if(session!=replySession||operation!=op||!target.equals(replyTarget)||sensitive){toast("输入框已变化，请重新生成");return;}
            InputConnection c=getCurrentInputConnection();if(c==null){toast("请先点选输入框");return;}
            flushRaw();if(c.commitText(reply,1)){aiPanel.removeAllViews();setStatus(Language.text(AiInputService.this,"已填入，请检查后自行发送"));operation++;}else setStatus(Language.text(AiInputService.this,"填入失败，请重新点选输入框"));
        });b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setPadding(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,8));b.setMinHeight(Ui.dp(this,48));list.addView(b,new LinearLayout.LayoutParams(-1,-2));}
        ScrollView scroll=new ScrollView(this);scroll.addView(list);aiPanel.addView(scroll,new LinearLayout.LayoutParams(-1,Ui.dp(this,160)));setStatus(Language.text(AiInputService.this,"点选一条回复，填入当前输入框"));
    }
    private void cancelRequest(){if(client!=null){client.cancel();client=null;}future=null;}
    private void clearSnapshot(){if(snapshot!=null){snapshot.close();snapshot=null;}}
    private void openSettings(){flushBest();Intent intent=new Intent(this,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(intent);}
    private void toast(String message){Toast.makeText(this,Language.message(this,message),Toast.LENGTH_SHORT).show();}
    @Override public void onDestroy(){discardUndo();main.removeCallbacks(candidateRefresh);if(root!=null)Glass.cancelTouches(root);if(handwriting!=null)handwriting.close();operation++;cancelRequest();clearSnapshot();worker.shutdown();pinyin.close();super.onDestroy();}
}
