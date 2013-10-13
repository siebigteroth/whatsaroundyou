package me.siebigteroth.whatsaroundyou;

import java.util.Locale;

import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.webkit.ConsoleMessage;
import android.webkit.ConsoleMessage.MessageLevel;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private WebView webView;
	private IntentFilter TaskIntent;
	public ProgressDialog dialog=null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//init webview
		webView = (WebView) findViewById(R.id.webView);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setDatabaseEnabled(true);
		String databasePath = this.getApplicationContext().getDir("database",Context.MODE_PRIVATE).getPath();
		webView.getSettings().setDatabasePath(databasePath);
		webView.addJavascriptInterface(new JavascriptInterface(this), "android");
		webView.setVerticalScrollBarEnabled(false);
		webView.setWebChromeClient(new WebChromeClient() {
			@Override
		    public void onExceededDatabaseQuota(String url, String databaseIdentifier, long currentQuota,
		    		long estimatedSize, long totalUsedQuota, android.webkit.WebStorage.QuotaUpdater quotaUpdater) {
		        quotaUpdater.updateQuota(estimatedSize * 2);
		    }
			
			@Override
			public boolean onConsoleMessage(ConsoleMessage cm) {
				if(cm.messageLevel()!=MessageLevel.ERROR)
					Log.d(cm.messageLevel().name(), cm.message() + " (" + cm.sourceId() + ":" + cm.lineNumber()+")");
				else
					Log.d(cm.messageLevel().name(), cm.message());
				return true;
			}
		});
		
		//restore webview state
		if(savedInstanceState!=null)
			webView.restoreState(savedInstanceState);
		else
			webView.loadUrl("file:///android_asset/index.html");
		
		//set intentfilter
		TaskIntent = new IntentFilter();
		TaskIntent.addAction("me.siebigteroth.whatsaroundyou.Task");
		
		//register intent-receiver
		try {
    		registerReceiver(TaskReceiver, TaskIntent);
    	}
    	catch(Exception e) {}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		webView.loadUrl("javascript:switchOptionsMenu();");
		return false;
	}
	
	public void initWebview() {
		executeJavascript("initLanguage('"+Locale.getDefault().getLanguage()+"');");
		executeJavascript("initWebview();");
	}
	
	private void executeJavascript(final String script) {
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	webView.loadUrl("javascript:"+script);
            }
        });
	}
	
	@Override
	protected void onSaveInstanceState(Bundle newInstanceState) {
		webView.saveState(newInstanceState);
	}
	
	//task-receiver
	private BroadcastReceiver TaskReceiver = new BroadcastReceiver() {
		@Override
	    public void onReceive(Context context, Intent i) {
			//get task
			Integer task = i.getIntExtra("task",0);
	    	
	    	switch(task) {
	    		case 0: //error
	    			if(dialog!=null)
	    				dialog.dismiss();
	    			stopService(new Intent(MainActivity.this,AgentService.class));
	    			executeJavascript("connectionFailed();");
		        	break;
	    		case 1: //connected
	    			if(dialog!=null)
	    				dialog.dismiss();
			    	showToast(getResources().getString(R.string.connected));
			    	break;
	    		case 2: //disconnected
		        	break;
	    	}
	    }
	};
	
	@Override
    protected void onResume() {
    	try {
    		registerReceiver(TaskReceiver, TaskIntent); //register servername-receiver
    	}
    	catch(Exception e) {}
    	super.onResume();
    }
    
    @Override
    protected void onPause() {
    	try {
    		unregisterReceiver(TaskReceiver); //unregister servername-receiver
    	}
    	catch(Exception e) {}
    	super.onPause();
    }
    
    //show an onscreen message
    public void showToast(String toast) {
        Toast.makeText(getBaseContext(), toast, Toast.LENGTH_SHORT).show();
    }

}
