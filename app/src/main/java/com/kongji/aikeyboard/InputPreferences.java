package com.kongji.aikeyboard;

import android.content.Context;
import android.content.SharedPreferences;

final class InputPreferences {
    boolean memory=true,initials=true,finals=true,english=true,typos=true;
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("input_preferences",Context.MODE_PRIVATE);}
    static InputPreferences load(Context c){SharedPreferences p=prefs(c);InputPreferences s=new InputPreferences();s.memory=p.getBoolean("memory",true);s.initials=p.getBoolean("initials",true);s.finals=p.getBoolean("finals",true);s.english=p.getBoolean("english",true);s.typos=p.getBoolean("typos",true);return s;}
    void save(Context c){SharedPreferences p=prefs(c);p.edit().putBoolean("memory",memory).putBoolean("initials",initials).putBoolean("finals",finals).putBoolean("english",english).putBoolean("typos",typos).putInt("epoch",p.getInt("epoch",0)+1).commit();}
    static int epoch(Context c){return prefs(c).getInt("epoch",0);}
}
