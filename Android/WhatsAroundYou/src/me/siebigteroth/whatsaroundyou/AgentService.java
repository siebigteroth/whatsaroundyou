package me.siebigteroth.whatsaroundyou;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class AgentService extends Service {
	
	private BluetoothSocket bluetoothSocket;
	private LocationManager locationManager;
	private AgentLocationListener locationListener;
	private boolean tracking=false;
	public BluetoothThread bluetoothThread;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		Log.i("test", "Service wurde gestoppt!");
		
		//send intent
		Intent i = new Intent();
		i.setAction("me.siebigteroth.whatsaroundyou.Task");
		i.putExtra("task",2); //disconnected
		sendStickyBroadcast(i);
		
		super.onDestroy();
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		Log.i("test", "Service wurde gestartet!");
		
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
		boolean not_found=true;
        if(pairedDevices.size() > 0)
        { //to do: automatisches Paaring vs. Suche in verbundenen Devices
            for(BluetoothDevice device : pairedDevices)
            {

            	Log.i("test", device.getName());
                if(device.getName().equals("TIM"))
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
		
	}
	
	private void stopTracking()
	{
		
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
      
        public void run() {
            byte[] buffer = new byte[255];
            while (true) {
                try {
                	inputStream.read(buffer);
                	
                	Log.i("test", "erhaltene Daten: " + new String(buffer, 0, buffer.length));
                	
                	String[] receivedData = new String(buffer, 0, buffer.length).split(",");
                	
    	        	if(locationListener!=null)
    	        		locationListener.setZoom(Integer.parseInt(receivedData[0]));
    	        	if(Integer.parseInt(receivedData[1])==1)
    	        	{
    	        		tracking=true;
    	        		startTracking();
    	        	}
    	        	else
    	        	{
    	        		tracking=false;
    	        		stopTracking();
    	        	}
                }
                catch (Exception e) {}
            }
        }
      
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
