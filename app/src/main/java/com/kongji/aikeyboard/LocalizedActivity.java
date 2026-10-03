package com.kongji.aikeyboard;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

public abstract class LocalizedActivity extends Activity {
    private String createdLanguage;
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(Language.wrap(base));}
    @Override protected void onCreate(Bundle state){super.onCreate(state);createdLanguage=Language.effective(this);}
    @Override protected void onResume(){super.onResume();if(!Language.effective(this).equals(createdLanguage))recreate();}
}
