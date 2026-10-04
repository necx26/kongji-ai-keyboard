package com.kongji.aikeyboard;

import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

/** One reversible range deletion, bound to the current editor. Never persisted or learned. */
final class DraftUndo {
    enum Result { CLEARED, EMPTY, UNSUPPORTED, FAILED }
    record Snapshot(String text,int selectionStart,int selectionEnd) {}
    private static final int LIMIT=65536;
    private Snapshot saved;private InputConnection owner;private String expected="",removed="";private int rangeStart;
    void discard(){saved=null;owner=null;expected="";removed="";}
    boolean available(){return saved!=null;}
    boolean matchesCurrent(InputConnection connection){Snapshot current=read(connection);return saved!=null&&connection==owner&&current!=null&&current.text.equals(expected)&&current.selectionStart==rangeStart&&current.selectionEnd==rangeStart;}
    String deletedText(){return saved==null?"":removed;}
    Snapshot read(InputConnection connection){
        if(connection==null)return null;
        ExtractedTextRequest request=new ExtractedTextRequest();request.hintMaxChars=LIMIT;request.hintMaxLines=10000;
        ExtractedText value=connection.getExtractedText(request,0);
        if(value==null||value.text==null||value.startOffset!=0||value.partialStartOffset>=0||value.text.length()>=LIMIT)return null;
        String text=value.text.toString();int start=value.selectionStart,end=value.selectionEnd;
        if(start<0||end<0||start>text.length()||end>text.length())return null;
        CharSequence before=connection.getTextBeforeCursor(LIMIT,0),after=connection.getTextAfterCursor(LIMIT,0);
        if(before==null||after==null||!text.substring(0,Math.min(start,end)).contentEquals(before)||!text.substring(Math.max(start,end)).contentEquals(after))return null;
        return new Snapshot(text,start,end);
    }
    Result clear(InputConnection connection){
        Snapshot snapshot=read(connection);if(snapshot==null)return Result.UNSUPPORTED;
        return deleteRange(connection,snapshot,0,snapshot.text.length());
    }
    Result deleteRange(InputConnection connection,int start,int end){Snapshot snapshot=read(connection);return snapshot==null?Result.UNSUPPORTED:deleteRange(connection,snapshot,start,end);}
    private Result deleteRange(InputConnection connection,Snapshot snapshot,int start,int end){
        if(start<0||end<start||end>snapshot.text.length())return Result.UNSUPPORTED;
        if(start==end)return Result.EMPTY;
        connection.beginBatchEdit();
        try{
            // Composition otherwise wins over selection in commitText and only the pinyin disappears.
            connection.finishComposingText();
            if(!connection.setSelection(start,end))return Result.FAILED;
            if(!connection.commitText("",1)){connection.setSelection(snapshot.selectionStart,snapshot.selectionEnd);return Result.FAILED;}
            saved=snapshot;owner=connection;rangeStart=start;removed=snapshot.text.substring(start,end);expected=snapshot.text.substring(0,start)+snapshot.text.substring(end);return Result.CLEARED;
        }finally{connection.endBatchEdit();}
    }
    Snapshot undo(InputConnection connection){
        if(saved==null)return null;
        if(connection!=owner){discard();return null;}
        Snapshot current=read(connection);
        // Pasted, dictated or newly typed content must never be overwritten by undo.
        if(current==null||!current.text.equals(expected)||current.selectionStart!=rangeStart||current.selectionEnd!=rangeStart){discard();return null;}
        Snapshot snapshot=saved;connection.beginBatchEdit();
        try{
            if(!connection.setSelection(rangeStart,rangeStart)||!connection.commitText(removed,1))return null;
            connection.setSelection(snapshot.selectionStart,snapshot.selectionEnd);discard();return snapshot;
        }finally{connection.endBatchEdit();}
    }
}
