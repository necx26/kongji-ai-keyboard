package com.kongji.aikeyboard;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

final class DraftUndoChecks {
    private final Instrumentation instrumentation;private int passed;private Activity activity;
    DraftUndoChecks(Instrumentation instrumentation){this.instrumentation=instrumentation;}
    private void check(boolean condition,String message){if(!condition)throw new AssertionError(message);passed++;}
    int run(){
        activity=instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),PracticeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Throwable[] error=new Throwable[1];
        instrumentation.runOnMainSync(()->{try{verify();}catch(Throwable failure){error[0]=failure;}});
        instrumentation.runOnMainSync(activity::finish);
        if(error[0]!=null)throw new AssertionError("draft undo: "+error[0],error[0]);return passed;
    }
    private void verify(){
        EditText editor=new EditText(activity);activity.setContentView(editor);editor.requestFocus();
        InputConnection connection=editor.onCreateInputConnection(new EditorInfo());DraftUndo history=new DraftUndo();
        String original="第一行🙂\n第二行，光标后还有文字";editor.setText(original);editor.setSelection(2,6);
        check(history.clear(connection)==DraftUndo.Result.CLEARED,"clear accepts complete editor snapshot");
        check(editor.getText().length()==0,"clear removes both sides of cursor and selection");
        check(history.undo(connection).text().equals(original)&&editor.getText().toString().equals(original),"undo restores multiline and Unicode text exactly");
        check(editor.getSelectionStart()==2&&editor.getSelectionEnd()==6,"undo restores original selection");
        check(history.undo(connection)==null,"undo is single-use");
        editor.setText("已选词nihao还有后缀");editor.setSelection(8);connection.setComposingRegion(3,8);
        check(history.clear(connection)==DraftUndo.Result.CLEARED&&editor.getText().length()==0,"clear finishes composition before clearing entire draft");
        check(history.clear(connection)==DraftUndo.Result.EMPTY,"clearing an empty field is a no-op");
        check(history.undo(connection)!=null&&editor.getText().toString().equals("已选词nihao还有后缀"),"second empty clear does not lose the first undo");
        history.clear(connection);connection.commitText("新输入",1);
        check(history.undo(connection)==null&&editor.getText().toString().equals("新输入"),"undo cannot overwrite text typed after clear");
        editor.setText("保留内容");history.clear(connection);
        EditText other=new EditText(activity);InputConnection otherConnection=other.onCreateInputConnection(new EditorInfo());
        check(history.undo(otherConnection)==null&&other.getText().length()==0,"another input connection cannot receive saved draft");
        editor.setText("完整文档");editor.setSelection(2);
        InputConnection partial=new InputConnectionWrapper(connection,false){@Override public ExtractedText getExtractedText(ExtractedTextRequest request,int flags){ExtractedText value=super.getExtractedText(request,flags);value.startOffset=1;return value;}};
        check(history.clear(partial)==DraftUndo.Result.UNSUPPORTED&&editor.getText().toString().equals("完整文档"),"partial extraction cannot clear a document fragment");
        InputConnection truncated=new InputConnectionWrapper(connection,false){@Override public ExtractedText getExtractedText(ExtractedTextRequest request,int flags){ExtractedText value=super.getExtractedText(request,flags);value.text="完整";return value;}};
        check(history.clear(truncated)==DraftUndo.Result.UNSUPPORTED&&editor.getText().toString().equals("完整文档"),"truncated tail is rejected");
        InputConnection refused=new InputConnectionWrapper(connection,false){@Override public boolean commitText(CharSequence text,int position){return false;}};
        check(history.clear(refused)==DraftUndo.Result.FAILED&&editor.getText().toString().equals("完整文档"),"failed edit preserves original text");
        check(editor.getSelectionStart()==2&&editor.getSelectionEnd()==2,"failed edit restores selection");
        history.clear(connection);history.discard();check(history.undo(connection)==null,"discard removes saved draft");
        RecentInput recent=new RecentInput();editor.setText("前文你好后文");editor.setSelection(4);recent.remember(connection,"你好",0,history);
        check(recent.delete(connection,history)==DraftUndo.Result.CLEARED&&editor.getText().toString().equals("前文后文"),"recent word deletion preserves surrounding old text");
        check(history.undo(connection)!=null&&editor.getText().toString().equals("前文你好后文")&&editor.getSelectionStart()==4,"word undo restores only the removed word and its cursor");
        editor.setText("原有内容nihao后文");editor.setSelection(9);connection.setComposingRegion(4,9);
        check(history.deleteRange(connection,4,9)==DraftUndo.Result.CLEARED&&editor.getText().toString().equals("原有内容后文"),"composing range deletion preserves prefix and suffix");
        check(history.undo(connection)!=null&&editor.getText().toString().equals("原有内容nihao后文"),"composing range undo restores raw pinyin");
        editor.setText("旧文字刚刚确认的短语");editor.setSelection(editor.length());recent.remember(connection,"刚刚确认的短语",0,history);
        check(recent.delete(connection,history)==DraftUndo.Result.CLEARED&&editor.getText().toString().equals("旧文字"),"whole last confirmed phrase is removed");
        check(recent.delete(connection,history)==DraftUndo.Result.EMPTY&&history.available(),"recent lookup is exhausted while the range undo remains available");
        connection.commitText("新文字",1);check(history.undo(connection)==null&&editor.getText().toString().equals("旧文字新文字"),"range undo cannot overwrite newly inserted text");
        editor.setText("旧hello 后缀");editor.setSelection(7);recent.remember(connection,"hello ",0,history);check(recent.delete(connection,history)==DraftUndo.Result.CLEARED&&editor.getText().toString().equals("旧后缀"),"English choice deletes its trailing space with the last word");
        check(history.undo(connection)!=null&&editor.getText().toString().equals("旧hello 后缀"),"English word undo preserves the suffix");
        editor.setText("旧文字你好");editor.setSelection(editor.length());recent.remember(connection,"你好",0,history);editor.setSelection(1);check(recent.delete(connection,history)==DraftUndo.Result.EMPTY&&editor.getText().toString().equals("旧文字你好"),"cursor move prevents removing an old insertion elsewhere");
        editor.setSelection(editor.length());recent.remember(connection,"你好",0,history);check(recent.delete(otherConnection,history)==DraftUndo.Result.EMPTY,"recent insertion cannot cross input connections");
        editor.setText("原文");editor.setSelection(editor.length());check(recent.delete(connection,history)==DraftUndo.Result.EMPTY&&editor.getText().toString().equals("原文"),"pre-existing text is never guessed as a recent word");
        editor.setText("旧文字你好");editor.setSelection(editor.length());recent.remember(connection,"你好",0,history);recent.delete(connection,history);check(history.matchesCurrent(connection),"second-swipe clear accepts the unchanged remainder");editor.setSelection(0);check(!history.matchesCurrent(connection),"cursor move disarms second-swipe clear");editor.setSelection(3);check(history.clear(connection)==DraftUndo.Result.CLEARED&&editor.length()==0,"second-swipe clear removes the remainder only after explicit continuation");check(history.clear(connection)==DraftUndo.Result.EMPTY&&history.available(),"another empty clear keeps the last deletion");check(history.undo(connection)!=null&&editor.getText().toString().equals("旧文字"),"undo after second swipe restores the latest full clear rather than the earlier word");
    }
}
