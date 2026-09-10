package com.mazhar.jarvis;
import android.app.Activity;import android.os.Bundle;import android.webkit.*;import java.nio.file.Files;
public class PreviewActivity extends Activity {
 WebView view;
 public void onCreate(Bundle b){super.onCreate(b);view=new WebView(this);setContentView(view);view.getSettings().setJavaScriptEnabled(true);view.getSettings().setAllowFileAccess(false);view.getSettings().setAllowContentAccess(false);view.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);view.setWebViewClient(new WebViewClient(){public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}});try{String html=Files.readString(new java.io.File(getFilesDir(),"preview.html").toPath());view.loadDataWithBaseURL("https://preview.invalid",html,"text/html","UTF-8",null);}catch(Exception e){finish();}}
 protected void onDestroy(){if(view!=null)view.destroy();super.onDestroy();}
}
