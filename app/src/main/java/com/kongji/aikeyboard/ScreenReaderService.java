package com.kongji.aikeyboard;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.HashSet;
import java.util.Set;

public class ScreenReaderService extends AccessibilityService {
    private static volatile ScreenReaderService active;
    static ScreenReaderService current(){ScreenReaderService service=active;return service!=null&&service.getServiceInfo()!=null?service:null;}
    @Override protected void onServiceConnected(){active=this;}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){/* No passive screen collection. */}
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){if(active==this)active=null;super.onDestroy();}
    @Override public boolean onUnbind(android.content.Intent intent){if(active==this)active=null;return super.onUnbind(intent);}
    interface Listener {void success(Snapshot snapshot);void failure(String reason);}
    static final class Snapshot {
        final String text;final Bitmap image;final String note;
        Snapshot(String text,Bitmap image,String note){this.text=text;this.image=image;this.note=note;}
        void close(){if(image!=null&&!image.isRecycled())image.recycle();}
    }
    void read(String target,int keyboardTop,Listener listener){
        if(target==null||target.isEmpty()||target.equals(getPackageName())){
            // PracticeActivity is also allowed, but the IME window itself is always excluded below.
            if(target==null||target.isEmpty()){listener.failure("请先点选需要回复的输入框");return;}
        }
        StringBuilder text=new StringBuilder();Set<String> seen=new HashSet<>();int[] count={0};
        for(AccessibilityWindowInfo window:getWindows()){
            if(window.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION)continue;
            AccessibilityNodeInfo root=window.getRoot();
            if(root!=null&&target.contentEquals(root.getPackageName()==null?"":root.getPackageName()))collect(root,text,seen,count);
        }
        final String screenText=text.toString();
        takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){
            @Override public void onSuccess(ScreenshotResult result){
                Bitmap image=null;HardwareBuffer buffer=result.getHardwareBuffer();Bitmap hardware=null;
                try{
                    hardware=Bitmap.wrapHardwareBuffer(buffer,result.getColorSpace());
                    if(hardware!=null){
                        Bitmap source=hardware.copy(Bitmap.Config.ARGB_8888,false);
                        if(source!=null){
                            int height=keyboardTop>80&&keyboardTop<source.getHeight()?keyboardTop:source.getHeight();
                            Bitmap cropped=Bitmap.createBitmap(source,0,0,source.getWidth(),height);
                            float scale=Math.min(1f,1080f/cropped.getWidth());
                            image=Bitmap.createScaledBitmap(cropped,Math.max(1,Math.round(cropped.getWidth()*scale)),Math.max(1,Math.round(cropped.getHeight()*scale)),true);
                            if(cropped!=image&&cropped!=source)cropped.recycle();if(source!=image)source.recycle();
                        }
                    }
                }catch(Exception ignored){}finally{if(hardware!=null)hardware.recycle();buffer.close();}
                if(image==null&&screenText.isBlank())listener.failure("没有读到屏幕内容，请换一个页面重试");
                else listener.success(new Snapshot(screenText,image,image==null?"截图失败，本次仅使用可见文字":""));
            }
            @Override public void onFailure(int code){
                if(screenText.isBlank())listener.failure("屏幕读取失败（"+code+"）。页面可能受保护，或系统限制了截图。");
                else listener.success(new Snapshot(screenText,null,"截图不可用，本次仅使用可见文字"));
            }
        });
    }
    private void collect(AccessibilityNodeInfo node,StringBuilder out,Set<String> seen,int[] count){
        if(count[0]++>700||out.length()>6500)return;
        if(node.isVisibleToUser()&&!node.isPassword()&&!node.isEditable()){
            CharSequence value=node.getText();if(value==null||value.length()==0)value=node.getContentDescription();
            if(value!=null){String line=value.toString().trim();Rect bounds=new Rect();node.getBoundsInScreen(bounds);
                String identity=bounds.toShortString()+line;
                if(!line.isEmpty()&&seen.add(identity)){if(line.length()>600)line=line.substring(0,600);out.append('[').append(bounds.left).append(',').append(bounds.top).append("] ").append(line).append('\n');}
            }
        }
        for(int i=0;i<node.getChildCount()&&count[0]<=700&&out.length()<=6500;i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null)collect(child,out,seen,count);}
    }
}
