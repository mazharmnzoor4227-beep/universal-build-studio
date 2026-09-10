package com.mazhar.generated;
import android.app.Activity;import android.os.Bundle;import android.webkit.*;
public class MainActivity extends Activity {
 WebView web;
 public void onCreate(Bundle state){super.onCreate(state);web=new WebView(this);setContentView(web);web.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets x=i.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(x.left,x.top,x.right,x.bottom);return i;});web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.setWebViewClient(new WebViewClient(){public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){if(r.getUrl().toString().equals("https://app.invalid/index.html"))try{return new WebResourceResponse("text/html","UTF-8",getAssets().open("index.html"));}catch(Exception e){}return null;}public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !r.getUrl().getHost().equals("app.invalid");}});web.loadUrl("https://app.invalid/index.html");}
 public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}protected void onDestroy(){web.destroy();super.onDestroy();}
}
