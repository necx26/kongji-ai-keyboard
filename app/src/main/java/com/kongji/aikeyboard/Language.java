package com.kongji.aikeyboard;

import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;

/** Local UI translations. User input, candidates and API content are never translated here. */
final class Language {
    static final String[] CODES={"system","zh","en","ja","es","ko","de"};
    static final String[] NAMES={"跟随系统","中文","English","日本語","Español","한국어","Deutsch"};
    private static final Map<String,Map<String,String>> catalogs=new ConcurrentHashMap<>();
    private Language(){}
    static String selected(Context c){if(Build.VERSION.SDK_INT>=33){LocaleList list=c.getSystemService(LocaleManager.class).getApplicationLocales();return list.isEmpty()?"system":list.get(0).getLanguage();}return c.getSharedPreferences("app_language",0).getString("language","system");}
    static String effective(Context c){String code=selected(c);if(code.equals("system"))code=Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage();for(String allowed:CODES)if(!allowed.equals("system")&&allowed.equals(code))return code;return "en";}
    static void set(Context c,String code){boolean valid=false;for(String allowed:CODES)valid|=allowed.equals(code);if(!valid)throw new IllegalArgumentException("Unsupported language");c.getSharedPreferences("app_language",0).edit().putString("language",code).commit();if(Build.VERSION.SDK_INT>=33)c.getSystemService(LocaleManager.class).setApplicationLocales(code.equals("system")?LocaleList.getEmptyLocaleList():LocaleList.forLanguageTags(code));}
    static Context wrap(Context c){Configuration config=new Configuration(c.getResources().getConfiguration());config.setLocales(LocaleList.forLanguageTags(effective(c)));return c.createConfigurationContext(config);}
    static String text(Context c,String original){if(original==null)return "";String code=effective(c);if(code.equals("zh"))return original;Map<String,String> dictionary=catalogs.get(code);if(dictionary==null){dictionary=new HashMap<>();try(var stream=c.getAssets().open("i18n/"+code+".json")){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;while((count=stream.read(buffer))!=-1)bytes.write(buffer,0,count);JSONObject json=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));var names=json.keys();while(names.hasNext()){String key=names.next();dictionary.put(key,json.getString(key));}}catch(Exception failure){throw new IllegalStateException("Missing UI translation: "+code,failure);}catalogs.put(code,dictionary);}return dictionary.getOrDefault(original,original);}
    static String message(Context c,String original){String translated=text(c,original);if(!translated.equals(original)||effective(c).equals("zh"))return translated;for(String part:new String[]{"模型接口返回 HTTP ","，请检查服务状态","屏幕读取失败（","）。页面可能受保护，或系统限制了截图。"})translated=translated.replace(part,text(c,part));return translated;}
}
