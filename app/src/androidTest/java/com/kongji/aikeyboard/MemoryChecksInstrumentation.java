package com.kongji.aikeyboard;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Exercises real Android storage and Pinyin, using separate test preferences/databases. */
public final class MemoryChecksInstrumentation extends Instrumentation {
    private Pinyin pinyin;
    private Context isolated;
    private int passed;private boolean keyboardSuite;

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments);keyboardSuite=arguments!=null&&"keyboard".equals(arguments.getString("suite")); start(); }

    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        passed++;
    }

    private void openPinyin() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        runOnMainSync(() -> pinyin = new Pinyin(isolated, ready::countDown));
        check(ready.await(20, TimeUnit.SECONDS) && pinyin.ready(), "词库加载成功");
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        if(keyboardSuite){try{result.putString("passed",Integer.toString(new KeyboardChecks(this).run()));finish(0,result);}catch(Throwable failure){result.putString("error",failure.toString());finish(1,result);}return;}
        isolated = new ContextWrapper(getTargetContext()) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("memory_test_" + name, mode);
            }
            @Override public File getDatabasePath(String name) {
                return super.getDatabasePath("memory_test_" + name);
            }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
                return getBaseContext().openOrCreateDatabase("memory_test_" + name, mode, factory);
            }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler handler) {
                return getBaseContext().openOrCreateDatabase("memory_test_" + name, mode, factory, handler);
            }
        };
        try (LearningStore store = new LearningStore(isolated)) {
            InputPreferences.prefs(isolated).edit().clear().commit();
            store.clear();
            store.add("zh", "zhi dao", "知道");
            store.add("zh", "zhi dao", "指导");
            for (int i = 0; i < 20; i++) {
                store.learn("zh", "zhi dao", "知道", "我", InputPreferences.epoch(isolated));
                store.learn("zh", "zhi dao", "指导", "老师", InputPreferences.epoch(isolated));
            }
            check(store.words().stream().allMatch(w -> w.updated > 0), "使用时间写入并读回 SQLite");
            openPinyin();
            runOnMainSync(() -> {
                check(pinyin.candidates("zhidao", "我").get(0).value.equals("知道"), "实际拼音入口按我/知道搭配排序");
                check(pinyin.candidates("zhidao", "老师").get(0).value.equals("指导"), "实际拼音入口按老师/指导搭配排序");
                pinyin.learn("zh", "kong ji ji yi ce shi", "控机记忆测试", "", true);
                check(pinyin.candidates("kongjijiyiceshi").stream().anyMatch(c -> c.value.equals("控机记忆测试")), "新词立即参与候选");
                pinyin.close();
            });
            // close queues behind pending writes; wait for that actual worker to finish.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (store.words().stream().noneMatch(w -> w.value.equals("控机记忆测试")) && System.nanoTime() < deadline) Thread.sleep(20);
            check(store.words().stream().anyMatch(w -> w.value.equals("控机记忆测试")), "普通学习异步保存到数据库");
            openPinyin();
            runOnMainSync(() -> {
                check(pinyin.candidates("kongjijiyiceshi").stream().anyMatch(c -> c.value.equals("控机记忆测试")), "重新创建输入引擎后新词仍存在");
                check(pinyin.candidates("zhidao", "老师").get(0).value.equals("指导"), "重启后上下文搭配排序恢复");
                pinyin.configure(true);
                check(pinyin.candidates("kongjijiyiceshi").stream().noneMatch(c -> c.value.equals("控机记忆测试")), "私密输入框不使用个人词");
                pinyin.learn("zh", "si mi ce shi", "私密测试", "", true);
                pinyin.configure(false);
                InputPreferences options = InputPreferences.load(isolated);options.memory = false;options.save(isolated);
                pinyin.configure(false);
                check(pinyin.candidates("kongjijiyiceshi").stream().noneMatch(c -> c.value.equals("控机记忆测试")), "关闭记忆立即停用已缓存用户词");
                pinyin.learn("zh", "guan bi ce shi", "关闭测试", "", true);
            });
            check(store.words().stream().noneMatch(w -> w.value.equals("私密测试") || w.value.equals("关闭测试")), "私密与关闭记忆均不写入记录");
            int oldEpoch = InputPreferences.epoch(isolated);
            store.clear();
            store.learn("zh", "yan chi ce shi", "延迟测试", "", oldEpoch);
            check(store.words().isEmpty() && store.next().isEmpty(), "清空后旧队列不能恢复记录");
            runOnMainSync(() -> {
                InputPreferences options = InputPreferences.load(isolated);options.memory = true;options.save(isolated);
                pinyin.configure(false);
                check(pinyin.candidates("zhidao", "我").get(0).value.equals(pinyin.candidates("zhidao", "老师").get(0).value), "清空后个人上下文和候选缓存同步失效");
            });
            SQLiteDatabase db = store.getWritableDatabase();
            db.beginTransaction();
            try {
                long updated = System.currentTimeMillis();
                for (int i = 0; i < 2001; i++) {
                    String word = "archive" + (char) ('a' + i / 676) + (char) ('a' + i / 26 % 26) + (char) ('a' + i % 26);
                    db.execSQL("INSERT INTO words VALUES(?,?,?,?,?)", new Object[]{"en", word, word, 1, updated + i});
                    if (i < 1201) db.execSQL("INSERT INTO following VALUES(?,?,?,?,?,?)", new Object[]{"en", word, "next", "next", 1, updated + i});
                }
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            store.learn("en", "retentioncheck", "retentioncheck", "previous", InputPreferences.epoch(isolated));
            check(store.words().size() == 2002 && store.words().stream().anyMatch(w -> w.value.equals("archiveaaa")), "超过原2000词限制后老记忆仍保存并能读回");
            check(store.next().size() == 1202 && store.next().stream().anyMatch(w -> w.previous.equals("archiveaaa")), "超过原1200搭配限制后老搭配仍保留");
            store.clear();
            result.putString("passed", Integer.toString(passed));
            finish(0, result);
        } catch (Throwable failure) {
            result.putString("error", failure.toString());finish(1, result);
        } finally {
            if (pinyin != null) runOnMainSync(pinyin::close);
        }
    }
}
