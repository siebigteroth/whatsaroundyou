package me.siebigteroth.whatsaroundyou;

import java.io.File;
import java.io.FileOutputStream;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private IntentFilter TaskIntent;
	private ProgressDialog progress=null;
	private Dialog dialog=null;
	private Button btn;
	private ImageView image;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//set intentfilter
		TaskIntent = new IntentFilter();
		TaskIntent.addAction("me.siebigteroth.whatsaroundyou.Task");
		
		//register intent-receiver
		try {
    		registerReceiver(TaskReceiver, TaskIntent);
    	}
    	catch(Exception e) {}
		
		//create directories and readmefile within
		createDirectories();
		
		//add button handler
		btn = (Button)findViewById(R.id.connect);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleAgentService();
			}
		});
		
		//set init button text
		btn.setText(R.string.start_service);
		
		//init image view
		image = (ImageView)findViewById(R.id.image);
		image.setImageResource(R.drawable.ic_launcher);
		
		//show credit informations on start up
		showCredit();
	}

	//show credit informations on option key press
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		showCredit();
		return false;
	}
	
	//show credit information
	private void showCredit() {
		if(dialog==null || dialog.isShowing()==false)
		{
			//new dialog
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.dialog);
			dialog.setTitle(R.string.dialogtitle);
			
			//init information icon
			ImageView image = (ImageView)dialog.findViewById(R.id.icon);
			image.setImageResource(R.drawable.information);
			
			//make links clickable
			TextView text = (TextView)dialog.findViewById(R.id.text);
			text.setMovementMethod(LinkMovementMethod.getInstance());
			
			//add close button handler
			Button btnClose = (Button)dialog.findViewById(R.id.close);
			btnClose.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
			
			dialog.show();
		}
	}
	
	//create directories and readme file within
	private void createDirectories() {
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
	}
	
	
	@Override
	protected void onSaveInstanceState(Bundle newInstanceState) {
		
	}
	
	//check if service is running and return true or false
	private boolean isAgentServiceRunning()
	{
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
	        if (AgentService.class.equals(service.service.getClass())) { //service is running
	            return true;
	        }
	    }
	    return false;
	}
	
	//check if service is running and start if true or stop
	private void toggleAgentService() {
	    if(isAgentServiceRunning())
	    	stopAgentService();
	    else
	    	startAgentService();
	}
	
	//start the agent service
	private void startAgentService()
	{
		progress = ProgressDialog.show(this, "", getResources().getString(R.string.starting_service), true);
		startService(new Intent(this, AgentService.class));
	}
	
	//stop the agent service
	private void stopAgentService()
	{
		showToast(getString(R.string.disconnected));
		stopService(new Intent(this, AgentService.class));
	}
	
	//task-receiver
	private BroadcastReceiver TaskReceiver = new BroadcastReceiver() {
		@Override
	    public void onReceive(Context context, Intent i) {
			//get task
			Integer task = i.getIntExtra("task",0);
	    	
	    	switch(task) {
	    		case 0: //error
	    			if(progress!=null)
	    				progress.dismiss();
	    			stopService(new Intent(MainActivity.this,AgentService.class));
		        	break;
	    		case 1: //connected
	    			if(progress!=null)
	    				progress.dismiss();
			    	showToast(getResources().getString(R.string.connected));
			    	btn.setText(R.string.stop_service); //change button text
			    	
			    	//reset image view
			    	image = (ImageView)findViewById(R.id.image);
					image.setImageResource(R.drawable.ic_launcher);
					
			    	break;
	    		case 2: //disconnected
	    			btn.setText(R.string.start_service); //reset button text
		        	break;
	    		case 3: //load image
	    			String imageBase64Data = i.getStringExtra("data");
	    			byte[] decodedImageData = Base64.decode(imageBase64Data, Base64.DEFAULT); //decode to bytearray
	    			Bitmap bitmap = BitmapFactory.decodeByteArray(decodedImageData, 0, decodedImageData.length); //create bitmap from bytearray
	    			image.setImageBitmap(bitmap); //replace the imageview with the current view of the smartwatches map view
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
