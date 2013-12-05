package me.siebigteroth.whatsaroundyou;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class AgentService extends Service {
	
	private BluetoothSocket bluetoothSocket;
	private LocationManager locationManager;
	private AgentLocationListener locationListener;
	public boolean additionalAction = false;
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
	
	private void startAdditionalAction()
	{
		additionalAction=true;
		//to do
		//String list = "Eintrag 1, Eintrag 2, Eintrag 3";
		try
		{
			ArrayList<String> list = new ArrayList<String>();
			
			//get latitude and longitude of the lastest location
			double lat = locationListener.lastLocation.getLatitude();
			double lng = locationListener.lastLocation.getLongitude();
			
			//to do: lat, lng an Abfrage der ÖPNV-Liste übergeben; Liste konvertieren und Elemente zu list als formartierten String hinzufügen
			
			//convert list to byte array
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(list);
			
			bluetoothThread.sendData(byteArrayOutputStream.toByteArray()); //send data to smartwatch
		}
		catch (Exception e) {}
	}
	
	private void stopAdditionalAction()
	{
		additionalAction=false;
		//to do
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
                	
                	String[] receivedData = new String(buffer, 0, buffer.length).split(",");
                	
    	        	if(locationListener!=null)
    	        		locationListener.setZoom(Integer.parseInt(receivedData[0]));
    	        	if(Integer.parseInt(receivedData[1])==1 && additionalAction==false)
    	        		startAdditionalAction();
    	        	else if(additionalAction==true)
    	        		stopAdditionalAction();
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
