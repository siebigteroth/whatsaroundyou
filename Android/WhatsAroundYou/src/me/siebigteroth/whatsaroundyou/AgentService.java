package me.siebigteroth.whatsaroundyou;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class AgentService extends Service {
	
	private BluetoothSocket bluetoothSocket;
	private LocationManager locationManager;
	private AgentLocationListener locationListener;
	public boolean tracking = false;
	public BluetoothThread bluetoothThread;
	public ArrayList<String> tracks;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		tracks = new ArrayList<String>();
	}

	@Override
	public void onDestroy() {	
		//send intent
		Intent i = new Intent();
		i.setAction("me.siebigteroth.whatsaroundyou.Task");
		i.putExtra("task",2); //disconnected
		sendStickyBroadcast(i);
		
		super.onDestroy();
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
		boolean not_found=true;
        if(pairedDevices.size() > 0)
        { //to do: automatisches Paaring vs. Suche in verbundenen Devices
            for(BluetoothDevice device : pairedDevices)
            {
            	Log.i("test", device.getName());
                if(device.getName().equals("TIM")) //to do: an Agent Smartwatch anpassen
                {
                	not_found=false;
                	try {
	                	UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //standard serialportserviceid
	                    bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
	                    bluetoothSocket.connect();
	                    if(bluetoothSocket.isConnected())
	                    {
	                    	bluetoothThread = new BluetoothThread(bluetoothSocket);
	                    	bluetoothThread.start();
	                    	
		                	locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		            		locationListener = new AgentLocationListener(AgentService.this);
		            		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
		                    
		            		//send intent
		            		Intent i = new Intent();
		            		i.setAction("me.siebigteroth.whatsaroundyou.Task");
		            		i.putExtra("task",1); //successfully connected
		            		sendStickyBroadcast(i);
	                    }
                	}
                	catch(Exception e)
                	{
                		e.printStackTrace();
                	}
                    break;
                }
            }
        }
        if(not_found==true)
        	throwError(getResources().getString(R.string.no_device_found));
	}
	
	private void startTracking()
	{
		tracking=true;
		tracks = new ArrayList<String>();
		tracks.add("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
		tracks.add("<gpx version=\"1.1\" creator=\"WhatsAroundYou\">");
		tracks.add("<trk>");
	}
	
	private void stopTracking()
	{
		tracking=false;
		tracks.add("</trk>");
		tracks.add("</gpx>");
		
		try {
			//create gpx file
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        String filename = simpleDateFormat.format(new Date());
			String filepath =  Environment.getExternalStorageDirectory() + File.separator + "WhatsAroundYou/tracks/"+filename+".gpx";
			File file = new File(filepath);
			if(!file.exists())
			{
				file.createNewFile();
				
				//put route as gpx-track-file into the external storage
		        FileOutputStream out = new FileOutputStream(file);
		        PrintWriter pw = new PrintWriter(out);
		        for(String line : tracks) //write line by line
		        	pw.println(line);
		        pw.close();
		        out.close();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public class BluetoothThread extends Thread {
		private OutputStream outputStream;
    	private InputStream inputStream;
      
        public BluetoothThread(BluetoothSocket bluetoothSocket) {
        	try {
				outputStream=bluetoothSocket.getOutputStream();
				inputStream=bluetoothSocket.getInputStream();
			}
        	catch (Exception e) {}
        }
        
        //receive bluetooth data from the smartwatch
        public void run() {
            byte[] buffer = new byte[255];
            while (true) {
                try {
                	inputStream.read(buffer);
                	
                	Log.i("test", "erhaltene Daten: " + new String(buffer, 0, buffer.length));
                	
                	String[] receivedData = new String(buffer, 0, buffer.length).split(",");
                	
    	        	if(locationListener!=null)
    	        		locationListener.setZoom(Integer.parseInt(receivedData[0]));
    	        	if(Integer.parseInt(receivedData[1])==1 && tracking==false)
    	        		startTracking();
    	        	else if(tracking==true)
    	        		stopTracking();
                }
                catch (Exception e) {}
            }
        }
      
        //send data to the smartwatch
        public void sendData(byte[] data) {
            try {
            	outputStream.write(data);
            }
            catch (Exception e) {}
        }
    }
	
	public void loadImage(String data) {
		//send intent
		Intent i = new Intent();
		i.setAction("me.siebigteroth.whatsaroundyou.Task");
		i.putExtra("task",3); //load image
		i.putExtra("data",data);
		sendStickyBroadcast(i);
	}
	
	//throw an error-message and reset view
	private void throwError(String msg) {
		Toast.makeText(this, getResources().getString(R.string.error)+": " + msg, Toast.LENGTH_LONG).show();
		
		//send intent
		Intent i = new Intent();
		i.setAction("me.siebigteroth.whatsaroundyou.Task");
		i.putExtra("task",0); //error
		sendStickyBroadcast(i);
		
		this.stopSelf();
	}
	
}
