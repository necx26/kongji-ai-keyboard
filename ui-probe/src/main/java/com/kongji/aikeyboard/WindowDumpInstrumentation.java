package com.kongji.aikeyboard;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Xml;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import org.xmlpull.v1.XmlSerializer;
import java.io.StringWriter;

/** Test-only window dumper. Android's legacy uiautomator dump omits this IME window. */
public class WindowDumpInstrumentation extends Instrumentation {
    private Bundle arguments;
    @Override public void onCreate(Bundle args){super.onCreate(args);arguments=args;start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            UiAutomation automation=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            AccessibilityServiceInfo info=automation.getServiceInfo();
            info.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            automation.setServiceInfo(info);
            Thread.sleep(120);
            if(arguments!=null&&arguments.containsKey("field")){
                String value=new String(android.util.Base64.decode(arguments.getString("value64"),android.util.Base64.DEFAULT),java.nio.charset.StandardCharsets.UTF_8);boolean changed=false;
                for(AccessibilityWindowInfo window:automation.getWindows()){AccessibilityNodeInfo root=window.getRoot();if(root!=null&&setField(root,arguments.getString("field"),value,0)){changed=true;break;}}
                result.putString("set_text",Boolean.toString(changed));Thread.sleep(120);
            }
            XmlSerializer xml=Xml.newSerializer();StringWriter writer=new StringWriter();xml.setOutput(writer);xml.startDocument("UTF-8",true);xml.startTag(null,"hierarchy");
            for(AccessibilityWindowInfo window:automation.getWindows()){
                AccessibilityNodeInfo root=window.getRoot();if(root!=null)dump(xml,root,0);
            }
            xml.endTag(null,"hierarchy");xml.endDocument();
            result.putString("tree",writer.toString());finish(0,result);
        } catch(Exception e){result.putString("error",e.toString());finish(1,result);}
    }
    private boolean setField(AccessibilityNodeInfo node,String description,String value,int depth){
        if(depth>80||!node.isVisibleToUser())return false;
        if(node.isEditable()&&description.contentEquals(node.getContentDescription()==null?"":node.getContentDescription())){Bundle action=new Bundle();action.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,action);}
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null&&setField(child,description,value,depth+1))return true;}return false;
    }
    private void dump(XmlSerializer xml,AccessibilityNodeInfo node,int depth)throws Exception{
        if(depth>80||!node.isVisibleToUser())return;
        Rect r=new Rect();node.getBoundsInScreen(r);xml.startTag(null,"node");
        xml.attribute(null,"text",node.isPassword()?"":node.getText()==null?"":node.getText().toString());
        xml.attribute(null,"content-desc",node.getContentDescription()==null?"":node.getContentDescription().toString());
        xml.attribute(null,"class",node.getClassName()==null?"":node.getClassName().toString());
        xml.attribute(null,"bounds","["+r.left+","+r.top+"]["+r.right+","+r.bottom+"]");
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null)dump(xml,child,depth+1);}
        xml.endTag(null,"node");
    }
}
