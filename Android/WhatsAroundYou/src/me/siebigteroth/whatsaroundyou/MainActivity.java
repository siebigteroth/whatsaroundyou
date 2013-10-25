package me.siebigteroth.whatsaroundyou;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

import org.json.JSONArray;

import android.os.Bundle;
import android.os.Environment;
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
		webView.getSettings().setDomStorageEnabled(true);
		webView.addJavascriptInterface(new JavascriptInterface(this), "android");
		webView.setVerticalScrollBarEnabled(false);
		webView.setWebChromeClient(new WebChromeClient() {
			
			//maximum databasesize reached
			@Override
		    public void onExceededDatabaseQuota(String url, String databaseIdentifier, long currentQuota,
		    		long estimatedSize, long totalUsedQuota, android.webkit.WebStorage.QuotaUpdater quotaUpdater) {
		        quotaUpdater.updateQuota(estimatedSize * 2); //double database quota
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
		try
		{
			//create directories, if they don't exist yet
			String applicationpath = Environment.getExternalStorageDirectory() + File.separator + "WhatsAroundYou/";
			String directorypath = applicationpath + "tracks/";
			File directory = new File(directorypath);
			directory.mkdirs();
			
			//create readme file, if it doesn't exist yet
			File readme = new File(applicationpath + "readme.txt");
			if(!readme.exists())
			{
				readme.createNewFile();
				FileOutputStream fos = new FileOutputStream(readme);
				
				String readmeText = "This is the data folder of the application WhatsAroundYou.\n"+
				"You can put your own tiles into this folder to use them instead of the downloaded ones.\n"+
				"\n"+
				"All map tiles in this folder, stored by the WhatsAroundYou application, were created by Stamen Design, under CC BY 3.0.\n"+
				"Map data by OpenStreetMap, under CC BY SA.\n"+
				"CC BY 3.0: http://creativecommons.org/licenses/by/3.0\n"+
				"CC BY SA: http://creativecommons.org/licenses/by-sa/3.0\n"+
				"\n"+
				"Stamen Design: http://stamen.com/\n"+
				"OpenStreetMap: http://openstreetmap.org/\n"+
				"WhatsAroundYou: http://siebigteroth.me/\n"+
				"\n"+
				"WhatsAroundYou is an open source application, released under The MIT License (MIT):\n"+
				"\n"+
				"Copyright (c) 2013 Tim Siebigteroth\n"+
				"\n"+
				"Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n"+
				"\n"+
				"The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n"+
				"\n"+
				"THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.";
				
				byte[] readmeBytes = readmeText.getBytes("UTF-8");
				fos.write(readmeBytes);
				fos.close();
			}
		}
		catch(Exception e) {}
		
		executeJavascript("initLanguage('"+Locale.getDefault().getLanguage()+"');");
		executeJavascript("initWebview();");
	}
	
	public void executeJavascript(final String script) {
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
	
	//set javascripts var tracks
	public void setTracks()
	{
		String directorypath = Environment.getExternalStorageDirectory() + File.separator + "WhatsAroundYou/tracks/";
		File directory = new File(directorypath);
		JSONArray varTracks = new JSONArray();
		File[] files = directory.listFiles();
		for (File file : files)
			varTracks.put(file.getName());
		executeJavascript("tracks=JSON.parse("+varTracks.toString()+");");
	}
	
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
