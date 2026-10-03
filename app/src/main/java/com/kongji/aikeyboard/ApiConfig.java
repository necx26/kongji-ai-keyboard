package com.kongji.aikeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.net.URI;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class ApiConfig {
    private static final String ALIAS="kongji_api_key_v1";
    final String url, key, model;
    final boolean localHttp;
    ApiConfig(String url,String key,String model,boolean localHttp) {
        this.url=url.trim(); this.key=key.trim(); this.model=model.trim(); this.localHttp=localHttp;
    }
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("configuration",Context.MODE_PRIVATE); }
    static ApiConfig load(Context c) {
        SharedPreferences p=prefs(c); String key="";
        try {
            String enc=p.getString("key_cipher","");
            if(!enc.isEmpty()) {
                Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE,secret(),new GCMParameterSpec(128,Base64.decode(p.getString("key_iv",""),Base64.NO_WRAP)));
                key=new String(cipher.doFinal(Base64.decode(enc,Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch(Exception ignored) { /* A reset Keystore requires re-entering the key. Never log credentials. */ }
        return new ApiConfig(p.getString("url",""),key,p.getString("model",""),p.getBoolean("local_http",false));
    }
    void save(Context c) throws Exception {
        endpoint();
        if(model.isEmpty()) throw new IllegalArgumentException("请填写模型名称");
        SharedPreferences.Editor editor=prefs(c).edit().putString("url",url).putString("model",model).putBoolean("local_http",localHttp);
        if(key.isEmpty()) editor.remove("key_cipher").remove("key_iv");
        else {
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,secret());
            editor.putString("key_cipher",Base64.encodeToString(cipher.doFinal(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP));
            editor.putString("key_iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP));
        }
        if(!editor.commit()) throw new IllegalStateException("设置保存失败，请重试");
    }
    URI endpoint() {
        URI uri;
        try { uri=URI.create(url); } catch(Exception e) { throw new IllegalArgumentException("接口地址格式不正确"); }
        String scheme=uri.getScheme(),host=uri.getHost();
        if(host==null||host.isEmpty()||uri.getUserInfo()!=null||uri.getFragment()!=null||uri.getQuery()!=null)
            throw new IllegalArgumentException("请输入接口基础地址，不包含账号、查询参数或片段");
        if(!"https".equalsIgnoreCase(scheme)) {
            if(!"http".equalsIgnoreCase(scheme)||!localHttp||!isLocal(host))
                throw new IllegalArgumentException("默认需要 HTTPS；本地模型可开启局域网 HTTP");
        }
        String path=uri.getPath()==null?"":uri.getPath();
        while(path.endsWith("/")) path=path.substring(0,path.length()-1);
        if(!path.endsWith("/chat/completions")) path+="/chat/completions";
        try { return new URI(uri.getScheme(),null,host,uri.getPort(),path,null,null); }
        catch(Exception e) { throw new IllegalArgumentException("接口地址格式不正确"); }
    }
    static boolean isLocal(String h) {
        if(h.equals("localhost")||h.equals("127.0.0.1")||h.equals("[::1]")||h.equals("::1")) return true;
        String[] parts=h.split("\\."); if(parts.length!=4) return false;
        int[] n=new int[4]; try {for(int i=0;i<4;i++){n[i]=Integer.parseInt(parts[i]);if(n[i]<0||n[i]>255)return false;}}catch(Exception e){return false;}
        return n[0]==10||(n[0]==192&&n[1]==168)||(n[0]==172&&n[1]>=16&&n[1]<=31);
    }
    private static SecretKey secret() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if(ks.containsAlias(ALIAS)) return (SecretKey)ks.getKey(ALIAS,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
