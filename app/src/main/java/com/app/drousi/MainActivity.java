package com.app.drousi;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.File;

public class MainActivity extends Activity {
    private WebView webView;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private String pendingId;
    private static final int MIC=77;

    public void onCreate(Bundle b){
        super.onCreate(b);
        webView=new WebView(this);
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new Bridge(),"Android");
        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }
    private File file(String id){ return new File(getFilesDir(),"recording_"+id+".m4a"); }
    private void js(String x){ runOnUiThread(()->webView.evaluateJavascript(x,null)); }
    private void begin(String id){
        try{
            File f=file(id); if(f.exists()) f.delete();
            recorder=new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(f.getAbsolutePath());
            recorder.prepare(); recorder.start();
            js("window.onRecordingStarted&&window.onRecordingStarted()");
        }catch(Exception e){ js("window.onRecordingError&&window.onRecordingError('تعذر بدء التسجيل')"); }
    }
    private void end(){
        try{ if(recorder!=null){ recorder.stop(); recorder.release(); recorder=null; js("window.onRecordingStopped&&window.onRecordingStopped()"); }}catch(Exception e){ recorder=null; js("window.onRecordingError&&window.onRecordingError('تعذر حفظ التسجيل')"); }
    }
    private void play(String id){
        try{
            stopPlay(); player=new MediaPlayer(); player.setDataSource(file(id).getAbsolutePath());
            player.setOnCompletionListener(m->{stopPlay();});
            player.prepare(); player.start(); js("window.onRecordingPlaybackStarted&&window.onRecordingPlaybackStarted()");
        }catch(Exception e){ js("window.onRecordingError&&window.onRecordingError('تعذر تشغيل التسجيل')"); }
    }
    private void stopPlay(){ if(player!=null){ try{player.stop();}catch(Exception e){} player.release(); player=null; js("window.onRecordingPlaybackStopped&&window.onRecordingPlaybackStopped()"); } }
    public void onRequestPermissionsResult(int r,String[] p,int[] g){
        super.onRequestPermissionsResult(r,p,g);
        if(r==MIC && g.length>0 && g[0]==PackageManager.PERMISSION_GRANTED) begin(pendingId);
        else js("window.onRecordingPermissionDenied&&window.onRecordingPermissionDenied()");
    }
    protected void onDestroy(){ stopPlay(); if(recorder!=null){try{recorder.stop();}catch(Exception e){} recorder.release();} super.onDestroy(); }
    public class Bridge{
        @JavascriptInterface public void startRecording(String id){
            pendingId=id;
            if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC);
            else begin(id);
        }
        @JavascriptInterface public void stopRecording(){ end(); }
        @JavascriptInterface public boolean hasRecording(String id){ return file(id).exists(); }
        @JavascriptInterface public void playRecording(String id){ play(id); }
        @JavascriptInterface public void stopPlayback(){ stopPlay(); }
        @JavascriptInterface public void deleteRecording(String id){ File f=file(id); if(f.exists()) f.delete(); js("window.onRecordingDeleted&&window.onRecordingDeleted()"); }
    }
}