package com.bikeskart.inspector;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
 private LinearLayout root;
 private String token;
 @Override public void onCreate(Bundle state){super.onCreate(state);showLogin();}
 private void base(String heading){
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(40,64,40,32);root.setBackgroundColor(Color.WHITE);
  TextView title=new TextView(this);title.setText(heading);title.setTextSize(25);title.setTextColor(Color.rgb(18,48,73));root.addView(title);
  setContentView(root);
 }
 private EditText field(String hint,boolean secret){
  EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);
  if(secret)e.setInputType(129);
  root.addView(e);return e;
 }
 private void showLogin(){
  base("BikesKart Inspector");TextView subtitle=new TextView(this);subtitle.setText("Inspector sign in · Admin-created accounts only");root.addView(subtitle);
  EditText email=field("Email",false),password=field("Password",true);
  Button signIn=new Button(this);signIn.setText("Sign in");root.addView(signIn);
  TextView error=new TextView(this);root.addView(error);
  signIn.setOnClickListener(v->{
   signIn.setEnabled(false);error.setText("Signing in…");
   new Thread(()->{
    try{
     JSONObject body=new JSONObject();body.put("email",email.getText().toString().trim());body.put("password",password.getText().toString());
     HttpURLConnection c=(HttpURLConnection)new URL(BuildConfig.API_BASE_URL+"login").openConnection();
     c.setRequestMethod("POST");c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
     try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
     int status=c.getResponseCode();String result=new String((status<400?c.getInputStream():c.getErrorStream()).readAllBytes(),StandardCharsets.UTF_8);
     c.disconnect();
     if(status!=200)throw new Exception("Login failed ("+status+"). Check account or server setup.");
     JSONObject json=new JSONObject(result);token=json.getString("accessToken");
     runOnUiThread(this::showDashboard);
    }catch(Exception e){runOnUiThread(()->{error.setText(e.getMessage());signIn.setEnabled(true);});}
   }).start();
  });
 }
 private void showDashboard(){
  base("Assigned vehicles");TextView status=new TextView(this);status.setText("Loading assignments…");root.addView(status);
  Button logout=new Button(this);logout.setText("Sign out");root.addView(logout);logout.setOnClickListener(v->{token=null;showLogin();});
  new Thread(()->{
   try{
    HttpURLConnection c=(HttpURLConnection)new URL(BuildConfig.API_BASE_URL+"assignments").openConnection();
    c.setRequestProperty("Authorization","Bearer "+token);c.setConnectTimeout(12000);c.setReadTimeout(12000);
    int code=c.getResponseCode();if(code!=200)throw new Exception("Could not load assignments ("+code+")");
    JSONObject data=new JSONObject(new String(c.getInputStream().readAllBytes(),StandardCharsets.UTF_8));c.disconnect();
    StringBuilder lines=new StringBuilder();org.json.JSONArray items=data.getJSONArray("assignments");
    if(items.length()==0)lines.append("No vehicles assigned yet.");
    for(int i=0;i<items.length();i++){JSONObject item=items.getJSONObject(i);lines.append("\n").append(item.optString("brand")).append(" ").append(item.optString("model")).append(" · ").append(item.optString("registrationNumber")).append("\nStatus: ").append(item.optString("status")).append("\n");}
    runOnUiThread(()->status.setText(lines.toString()));
   }catch(Exception e){runOnUiThread(()->status.setText(e.getMessage()));}
  }).start();
 }
}
