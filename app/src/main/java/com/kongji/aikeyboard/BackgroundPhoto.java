package com.kongji.aikeyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Bounded picker input is copied into app-private storage on a worker thread. */
final class BackgroundPhoto {
    private static String cachedPath="";private static Bitmap cached;
    static File directory(Context c){return new File(c.getFilesDir(),"keyboard-backgrounds");}
    static String importPhoto(Context c,Uri uri)throws IOException{
        Bitmap bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(c.getContentResolver(),uri),(decoder,info,source)->{
            int w=info.getSize().getWidth(),h=info.getSize().getHeight();float scale=Math.min(1f,1600f/Math.max(w,h));
            decoder.setTargetSize(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
        File dir=directory(c);if(!dir.isDirectory()&&!dir.mkdirs()){bitmap.recycle();throw new IOException("background directory");}
        String name="photo-"+java.util.UUID.randomUUID()+".jpg";File file=new File(dir,name);boolean written=false;
        try(FileOutputStream out=new FileOutputStream(file)){if(!bitmap.compress(Bitmap.CompressFormat.JPEG,90,out))throw new IOException("image encoding");out.flush();written=true;}
        finally{bitmap.recycle();if(!written)file.delete();}return name;
    }
    static synchronized Bitmap bitmap(Context c,String name){
        if(name==null||!name.matches("photo-[a-fA-F0-9-]+\\.jpg"))return null;
        String path=new File(directory(c),name).getAbsolutePath();if(path.equals(cachedPath)&&cached!=null)return cached;
        Bitmap value=BitmapFactory.decodeFile(path);cachedPath=path;cached=value;return value;
    }
    static void discard(Context c,String name){if(name!=null&&name.matches("photo-[a-fA-F0-9-]+\\.jpg"))new File(directory(c),name).delete();}
    private BackgroundPhoto(){}
}
