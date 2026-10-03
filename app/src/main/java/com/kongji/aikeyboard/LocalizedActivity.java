package com.kongji.aikeyboard;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowInsetsController;

public abstract class LocalizedActivity extends Activity {
    private String createdLanguage;private boolean createdDark;KeyboardStyle pageStyle;
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(Language.wrap(base));}
    @Override protected void onCreate(Bundle state){
        pageStyle=KeyboardStyle.load(this);setTheme(pageStyle.dark()?R.style.AppThemeDark:R.style.AppTheme);
        super.onCreate(state);createdLanguage=Language.effective(this);createdDark=pageStyle.dark();applyPageTheme(pageStyle);
    }
    void applyPageTheme(KeyboardStyle style){pageStyle=style;setTheme(style.dark()?R.style.AppThemeDark:R.style.AppTheme);
        getWindow().setStatusBarColor(Ui.bg(this));getWindow().setNavigationBarColor(Ui.bg(this));
        int mask=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        getWindow().getDecorView();
        getWindow().getInsetsController().setSystemBarsAppearance(style.dark()?0:mask,mask);
    }
    @Override protected void onResume(){super.onResume();if(!Language.effective(this).equals(createdLanguage)||KeyboardStyle.load(this).dark()!=createdDark)recreate();}
}
