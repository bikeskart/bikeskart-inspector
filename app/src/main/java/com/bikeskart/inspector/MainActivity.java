package com.bikeskart.inspector;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.*;
import android.view.View;
import android.widget.Toast;
import android.net.Uri;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;

public class MainActivity extends Activity {
 private static final String PORTAL="https://auction.bikeskart.com/executive.html";
 private static final int PICK_FILE=51;
 private static final int PERMISSION_CODE=52;
 private WebView web;
 private boolean isPortalOrigin(String origin){try{Uri u=Uri.parse(origin);return "https".equalsIgnoreCase(u.getScheme())&&"auction.bikeskart.com".equalsIgnoreCase(u.getHost())&&(u.getPort()==-1||u.getPort()==443);}catch(Exception e){return false;}}
 private ValueCallback<Uri[]> pendingFiles;
 private PermissionRequest pendingWebPermission;
 private GeolocationPermissions.Callback pendingGeo;
 private String pendingGeoOrigin;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  web=new WebView(this);setContentView(web);
  WebSettings settings=web.getSettings();
  settings.setJavaScriptEnabled(true);
  settings.setDomStorageEnabled(true);
  settings.setMediaPlaybackRequiresUserGesture(false);
  settings.setAllowFileAccess(false);
  settings.setAllowContentAccess(true);
  CookieManager.getInstance().setAcceptCookie(true);
  CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
    Uri uri=request.getUrl();
    if("https".equals(uri.getScheme())&&"auction.bikeskart.com".equalsIgnoreCase(uri.getHost()))return false;
    try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception e){Toast.makeText(MainActivity.this,"Cannot open link",Toast.LENGTH_SHORT).show();}
    return true;
   }
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public void onConsoleMessage(ConsoleMessage message){
    if(message.messageLevel()==ConsoleMessage.MessageLevel.ERROR){android.util.Log.e("BikesKartWebView",message.message());}
   }
   @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params){
    if(pendingFiles!=null)pendingFiles.onReceiveValue(null);
    pendingFiles=callback;
    try{startActivityForResult(params.createIntent(),PICK_FILE);return true;}
    catch(Exception e){pendingFiles=null;callback.onReceiveValue(null);return false;}
   }
   @Override public void onPermissionRequest(PermissionRequest request){
    runOnUiThread(()->{
     if(!isPortalOrigin(request.getOrigin().toString())){android.util.Log.w("BikesKartCamera","Rejected origin: "+request.getOrigin());request.deny();return;}
     boolean camera=false;
     for(String resource:request.getResources())if(PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource))camera=true;
     if(!camera){request.deny();return;}
     pendingWebPermission=request;
     if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)grantCamera();
     else requestPermissions(new String[]{Manifest.permission.CAMERA},PERMISSION_CODE);
    });
   }
   @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback callback){
    if(!isPortalOrigin(origin)){callback.invoke(origin,false,false);return;}
    pendingGeo=callback;pendingGeoOrigin=origin;
    if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)grantGeo();
    else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_CODE);
   }
  });
  web.loadUrl(PORTAL);
 }
 private void grantCamera(){if(pendingWebPermission!=null){pendingWebPermission.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});pendingWebPermission=null;}}
 private void grantGeo(){if(pendingGeo!=null){pendingGeo.invoke(pendingGeoOrigin,true,false);pendingGeo=null;pendingGeoOrigin=null;}}
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){
  super.onRequestPermissionsResult(code,permissions,results);
  if(code!=PERMISSION_CODE)return;
  if(pendingWebPermission!=null){if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)grantCamera();else{pendingWebPermission.deny();pendingWebPermission=null;}}
  if(pendingGeo!=null){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)grantGeo();else{pendingGeo.invoke(pendingGeoOrigin,false,false);pendingGeo=null;pendingGeoOrigin=null;}}
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request==PICK_FILE&&pendingFiles!=null){pendingFiles.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));pendingFiles=null;}
 }
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override protected void onPause(){CookieManager.getInstance().flush();super.onPause();}
 @Override protected void onDestroy(){if(pendingFiles!=null)pendingFiles.onReceiveValue(null);web.destroy();super.onDestroy();}
}
