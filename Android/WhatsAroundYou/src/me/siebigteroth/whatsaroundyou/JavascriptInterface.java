package me.siebigteroth.whatsaroundyou;

import java.io.InputStream;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;

public class JavascriptInterface {
	
	private MainActivity context;
	private JSONArray searchResults;

	public JavascriptInterface(Context context){
		this.context=(MainActivity)context;
	}
	
	public void webviewLoaded()
	{
		context.initWebview();
	}
	
	public void startService()
	{
		context.dialog = ProgressDialog.show(context, "", context.getResources().getString(R.string.starting_service), true);
		context.startService(new Intent(context,AgentService.class));
	}
	
	public void stopService()
	{
		context.showToast(context.getString(R.string.disconnected));
		context.stopService(new Intent(context,AgentService.class));
	}
	
	public void reloadTracks()
	{
		context.setTracks();
		context.executeJavascript("listTracks();");
	}
	
	public void loadTrack(String filename)
	{
		//send intent
		Intent i = new Intent();
		i.setAction("me.siebigteroth.whatsaroundyou.ServiceTask");
		i.putExtra("task",1); //load track
		i.putExtra("filename",filename);
		context.sendStickyBroadcast(i);
	}
	
	public void unloadTrack()
	{
		//send intent
		Intent i = new Intent();
		i.setAction("me.siebigteroth.whatsaroundyou.ServiceTask");
		i.putExtra("task",2); //unload track
		context.sendStickyBroadcast(i);
	}
	
	public void searchFor(String text)
	{
		try {
			URL url = new URL("http://maps.googleapis.com/maps/api/geocode/json?sensor=false&address="+text);
			InputStream inputStream = (InputStream)url.getContent();
			byte[] buffer = new byte[255];
			inputStream.read(buffer);
			String receivedData = new String(buffer, 0, buffer.length);
			JSONObject json = new JSONObject(receivedData);
			searchResults = json.getJSONArray("results");
			for (int i = 0; i < searchResults.length(); i++) {
				JSONObject result = searchResults.getJSONObject(i);
				String name = result.getString("formatted_address");
				context.executeJavascript("addSearchResult("+i+",'"+name+"');");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void loadSearchresult(String parameter)
	{
		int index = Integer.parseInt(parameter);
		try {
			JSONObject result = searchResults.getJSONObject(index);
			JSONObject geometry = result.getJSONObject("geometry");
			JSONObject location = geometry.getJSONObject("location");
			double lat = location.getDouble("lat");
			double lng = location.getDouble("lat");
			
			//send intent
			Intent i = new Intent();
			i.setAction("me.siebigteroth.whatsaroundyou.ServiceTask");
			i.putExtra("task",0); //load route
			i.putExtra("lat",lat);
			i.putExtra("lng",lng);
			context.sendStickyBroadcast(i);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
