package com.kongji.aikeyboard;

import android.view.inputmethod.InputConnection;

/** The last confirmed insertion; never guesses a word from pre-existing editor content. */
final class RecentInput {
    private InputConnection owner;private DraftUndo.Snapshot after;private int start,end;
    void discard(){owner=null;after=null;}
    void remember(InputConnection connection,String inserted,int pendingLength,DraftUndo history){
        discard();if(inserted.isEmpty())return;DraftUndo.Snapshot value=history.read(connection);if(value==null||value.selectionStart()!=value.selectionEnd())return;
        int right=value.selectionEnd()-pendingLength,left=right-inserted.length();
        if(left<0||right>value.text().length()||!value.text().substring(left,right).equals(inserted))return;
        owner=connection;after=value;start=left;end=right;
    }
    DraftUndo.Result delete(InputConnection connection,DraftUndo history){
        DraftUndo.Snapshot current=history.read(connection);
        if(owner!=connection||after==null||!after.equals(current)){discard();return DraftUndo.Result.EMPTY;}
        DraftUndo.Result result=history.deleteRange(connection,start,end);if(result==DraftUndo.Result.CLEARED)discard();return result;
    }
}
