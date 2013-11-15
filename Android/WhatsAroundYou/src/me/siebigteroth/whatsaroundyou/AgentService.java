package me.siebigteroth.whatsaroundyou;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class AgentService extends Service {
	
	private IntentFilter TaskIntent;
	private BluetoothSocket bluetoothSocket;
	private LocationManager locationManager;
	private AgentLocationListener locationListener;
	public boolean tracking = false;
	public BluetoothThread bluetoothThread;
	public ArrayList<String> tracks;
	private String mode = "walk"; //driving, bicycling, walking

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		tracks = new ArrayList<String>();
		
		//set intentfilter
		TaskIntent = new IntentFilter();
		TaskIntent.addAction("me.siebigteroth.whatsaroundyou.ServiceTask");
		
		//register intent-receiver
		try {
    		registerReceiver(TaskReceiver, TaskIntent);
    	}
    	catch(Exception e) {}
	}
	
	//task-receiver
	private BroadcastReceiver TaskReceiver = new BroadcastReceiver() {
		@Override
	    public void onReceive(Context context, Intent i) {
			//get task
			Integer task = i.getIntExtra("task",0);
	    	
	    	switch(task) {
	    		case 0: //load route
	    			double lat = i.getDoubleExtra("lat", 0);
	    			double lng = i.getDoubleExtra("lng", 0);
	    			loadRoute(lat, lng);
		        	break;
	    		case 1: //load track
	    			String filename = i.getStringExtra("filename");
	    			loadTrackingFile("tracks/"+filename+".gpx");
	    			locationListener.sendTile(); //update tile
	    			break;
	    		case 2: //unload track
	    			locationListener.paths = new ArrayList<double[]>();
	    			locationListener.sendTile(); //update tile
	    			break;
	    	}
	    }
	};
	
	private void loadRoute(double lat, double lng)
	{
		try
		{
			URL url = new URL("INSERT URL HERE"+mode+"&origin="+
					locationListener.lastLocation.getLatitude()+","+locationListener.lastLocation.getLongitude()+
					"&destination="+lat+","+lng);
			InputStream inputStream = (InputStream)url.getContent();
			byte[] buffer = new byte[255];
			inputStream.read(buffer);
			String receivedData = new String(buffer, 0, buffer.length);
			JSONObject json = new JSONObject(receivedData);
			JSONArray routes = json.getJSONArray("routes");
			JSONObject firstRoute = routes.getJSONObject(0);
			JSONArray legs = firstRoute.getJSONArray("legs");
			JSONObject firstLeg = legs.getJSONObject(0);
			JSONArray steps = firstLeg.getJSONArray("steps");
			
			ArrayList<String> lines = new ArrayList<String>();
			lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
			lines.add("<gpx version=\"1.1\" creator=\"WhatsAroundYou\">");
			lines.add("<trk>");
			for (int i = 0; i < steps.length(); i++) {
				JSONObject step = steps.getJSONObject(i);
				lines.add("<trkseg>");
				JSONObject start_location = step.getJSONObject("start_location");
				lines.add("<trkpt lat=\""+start_location.getDouble("lat")+"\" lon=\""+start_location.getDouble("lng")+"\"></trkpt>");
				JSONObject end_location = step.getJSONObject("end_location");
				lines.add("<trkpt lat=\""+end_location.getDouble("lat")+"\" lon=\""+end_location.getDouble("lng")+"\"></trkpt>");
				lines.add("</trkseg>");
			}
			lines.add("</trk>");
			lines.add("</gpx>");
			
			//(re)create hidden gpx file
			String filepath =  Environment.getExternalStorageDirectory() + File.separator + "WhatsAroundYou/.search.gpx";
			File file = new File(filepath);
			if(file.exists())
				file.delete();
			file.createNewFile();
			
			//put route as gpx-track-file into the external storage
	        FileOutputStream out = new FileOutputStream(file);
	        PrintWriter pw = new PrintWriter(out);
	        for(String line : lines) //write line by line
	        	pw.println(line);
	        pw.close();
	        out.close();
	        
	        loadTrackingFile(".search.gpx");
		}
		catch(Exception e){}
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
	
	//parse gpx-file and add paths to locationlistener
	private void loadTrackingFile(String filename)
	{
		try {
			String filepath =  Environment.getExternalStorageDirectory() + File.separator + "WhatsAroundYou/" + filename + ".gpx";
			File file = new File(filepath);
			if(file.exists())
			{
				Builder parser = new Builder();
				Document doc = parser.build(filepath);
				Element gpx = doc.getRootElement();
				Elements trks = gpx.getChildElements();
				for (int i=0; i<trks.size(); i++) {
					Element trk = trks.get(i);
					if(trk.getLocalName()=="trk")
					{
						Elements trksegs = trk.getChildElements();
						for (int k=0; k<trksegs.size(); k++) {
							Element trkseg = trksegs.get(k);
							if(trkseg.getLocalName()=="trkseg")
							{
								Elements trkpts = trkseg.getChildElements();
								for (int l=0; l<trkpts.size(); l++) {
									Element trkpt = trkpts.get(l);
									if(trkpt.getLocalName()=="trkpt")
									{
										double[] location = new double[2];
										Attribute lat = trkpt.getAttribute("lat");
										location[0] = Double.parseDouble(lat.getValue());
										Attribute lng = trkpt.getAttribute("lon");
										location[1] = Double.parseDouble(lng.getValue());
										this.locationListener.paths.add(location);
									}
								}
							}
						}
					}
				}
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
