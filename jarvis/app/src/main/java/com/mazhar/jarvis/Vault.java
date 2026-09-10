package com.mazhar.jarvis;
import android.content.Context;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
final class Vault {
 static javax.crypto.SecretKey key() throws Exception {
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(!ks.containsAlias("jarvis")){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("jarvis",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}
  return (javax.crypto.SecretKey)ks.getKey("jarvis",null);
 }
 static void save(Context c,String name,String value)throws Exception {Cipher x=Cipher.getInstance("AES/GCM/NoPadding");x.init(Cipher.ENCRYPT_MODE,key());c.getSharedPreferences("vault",0).edit().putString(name,Base64.encodeToString(x.getIV(),2)+":"+Base64.encodeToString(x.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)),2)).apply();}
 static String get(Context c,String name,String fallback)throws Exception {String v=c.getSharedPreferences("vault",0).getString(name,null);if(v==null)return fallback;String[] p=v.split(":");Cipher x=Cipher.getInstance("AES/GCM/NoPadding");x.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p[0],2)));return new String(x.doFinal(Base64.decode(p[1],2)),java.nio.charset.StandardCharsets.UTF_8);}
}
