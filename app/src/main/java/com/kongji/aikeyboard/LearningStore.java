package com.kongji.aikeyboard;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Persistent local learning. Only explicit clearing removes history. */
final class LearningStore extends SQLiteOpenHelper {
    static final Object LOCK=new Object();private final Context context;
    static final class Word {final String language,spelling,value;final int hits;final long updated;Word(String language,String spelling,String value,int hits){this(language,spelling,value,hits,System.currentTimeMillis());}Word(String language,String spelling,String value,int hits,long updated){this.language=language;this.spelling=spelling;this.value=value;this.hits=hits;this.updated=updated;}}
    static final class Next {final String language,previous,value,spelling;final int hits;final long updated;Next(String language,String previous,String value,String spelling,int hits){this(language,previous,value,spelling,hits,System.currentTimeMillis());}Next(String language,String previous,String value,String spelling,int hits,long updated){this.language=language;this.previous=previous;this.value=value;this.spelling=spelling;this.hits=hits;this.updated=updated;}}
    LearningStore(Context c){super(c.getApplicationContext(),"typing_history.db",null,1);context=c.getApplicationContext();}
    @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE words(language TEXT,spelling TEXT,value TEXT,hits INTEGER,updated INTEGER,PRIMARY KEY(language,spelling,value))");db.execSQL("CREATE TABLE following(language TEXT,previous TEXT,value TEXT,spelling TEXT,hits INTEGER,updated INTEGER,PRIMARY KEY(language,previous,value))");}
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new IllegalStateException("Unsupported history schema upgrade");}
    List<Word> words(){synchronized(LOCK){List<Word> result=new ArrayList<>();try(Cursor cursor=getReadableDatabase().rawQuery("SELECT language,spelling,value,hits,updated FROM words ORDER BY updated DESC",null)){while(cursor.moveToNext())result.add(new Word(cursor.getString(0),cursor.getString(1),cursor.getString(2),cursor.getInt(3),cursor.getLong(4)));}return result;}}
    List<Next> next(){synchronized(LOCK){List<Next> result=new ArrayList<>();try(Cursor cursor=getReadableDatabase().rawQuery("SELECT language,previous,value,spelling,hits,updated FROM following ORDER BY updated DESC",null)){while(cursor.moveToNext())result.add(new Next(cursor.getString(0),cursor.getString(1),cursor.getString(2),cursor.getString(3),cursor.getInt(4),cursor.getLong(5)));}return result;}}
    void learn(String language,String spelling,String value,String previous,int epoch){synchronized(LOCK){if(epoch!=InputPreferences.epoch(context)||!InputPreferences.load(context).memory)return;
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{long time=System.currentTimeMillis();
            db.execSQL("INSERT INTO words VALUES(?,?,?,?,?) ON CONFLICT(language,spelling,value) DO UPDATE SET hits=MIN(hits+1,10000),updated=excluded.updated",new Object[]{language,spelling,value,1,time});
            if(!previous.isEmpty()&&previous.length()<=32)db.execSQL("INSERT INTO following VALUES(?,?,?,?,?,?) ON CONFLICT(language,previous,value) DO UPDATE SET hits=MIN(hits+1,10000),spelling=excluded.spelling,updated=excluded.updated",new Object[]{language,previous,value,spelling,1,time});
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }}
    void add(String language,String spelling,String value){synchronized(LOCK){SQLiteDatabase db=getWritableDatabase();db.execSQL("INSERT INTO words VALUES(?,?,?,?,?) ON CONFLICT(language,spelling,value) DO UPDATE SET hits=MAX(hits,20),updated=excluded.updated",new Object[]{language,spelling,value,20,System.currentTimeMillis()});InputPreferences.prefs(context).edit().putInt("epoch",InputPreferences.epoch(context)+1).commit();}}
    void clear(){synchronized(LOCK){InputPreferences.prefs(context).edit().putInt("epoch",InputPreferences.epoch(context)+1).commit();SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("words",null,null);db.delete("following",null,null);db.setTransactionSuccessful();}finally{db.endTransaction();}}}
}
