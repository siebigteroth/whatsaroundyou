package me.siebigteroth.whatsaroundyou;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

public class AgentLocationListener implements LocationListener {
	
	public int zoom;
	private Context context;
	public Location lastLocation;
	
	public AgentLocationListener(Context context) {
		super();
		this.zoom = 10;
		this.context = context;
		this.lastLocation = new Location("");
		
		//default location; NY
		this.lastLocation.setLatitude(40.752361);
		this.lastLocation.setLongitude(-73.966978);
	}

	@Override
	public void onProviderDisabled(String provider) {}
	
	@Override
	public void onProviderEnabled(String provider) {}
	
	@Override
	public void onLocationChanged(Location location) {	
		//update old location and tile
		this.lastLocation=location;
		sendTile();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}
	
	//set zoom and update tile
	public void setZoom(int zoom)
	{
		this.zoom=zoom;
		sendTile();
	}
	
	//send the generated tile to the smartwatch
	public void sendTile()
	{
		byte[] imageData = getMapImage(lastLocation.getLatitude(), lastLocation.getLongitude());	
		((AgentService)context).bluetoothThread.sendData(imageData); //send data to smartwatch
		
		String imageBase64Data = Base64.encodeToString(imageData, Base64.DEFAULT);
		((AgentService)context).loadImage(imageBase64Data); //(re)load map image in webview
	}
	
	//calculate the correct coordinates
	private int[] getCoordinates(int x, int y, int z) {
		if(x>Math.pow(2,z)-1)
		{
			x=0;
			y++;
		}
		else if(x<0)
		{
			x=(int)Math.pow(2,z)-1;
			y--;
		}
		if(y>Math.pow(2,z))
		{
			y=0;
			x++;
		}
		else if(y<0)
		{
			y=(int)Math.pow(2,z);
			x--;
		}
		
		if(x>Math.pow(2,z)-1 || x<0)
			return getCoordinates(x,y,z);
		else
		{
			int[] result = new int[2];
			result[0]=x;
			result[1]=y;
			return result;
		}
	}
	
	//get the tile by file or url
	private Bitmap getTile(int x, int y) {		
		try {
			//get correct coordinates
			if(zoom>18)
				zoom=18;
			else if(zoom<0)
				zoom=0;
			int[] xy = getCoordinates(x,y,zoom);
			x=xy[0];
			y=xy[1];
			
			//get tile image
			Bitmap bitmap;

			//create directories, if they don't exist yet
			String directorypath =  Environment.getExternalStorageDirectory() + File.separator + "WhatsAroundYou/" + zoom + "/" + x + "/";
			File directory = new File(directorypath);
			directory.mkdirs();
			
			String filepath = directorypath + y + ".jpg";
			File file = new File(filepath);
			
			if(file.exists())
			{
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Bitmap.Config.RGB_565;
				bitmap = BitmapFactory.decodeFile(filepath, options);
			}
			else
			{
				file.createNewFile();
				
				//get tile bitmap from url
				URL url = new URL("http://tile.stamen.com/toner/" + zoom + "/" + x + "/" + y + ".png");
			    InputStream in = (InputStream)url.getContent();
			    Drawable image = Drawable.createFromStream(in , "src");
			    in.close();
			    bitmap = ((BitmapDrawable)image).getBitmap();
			    
			    //to do: speicherplatz schaffen, falls zu voll
		    
			    //put tile as jpg into external storage
		        FileOutputStream out = new FileOutputStream(file);
		        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
		        out.close();
			}
			
		    return bitmap;
		}
		catch (Exception e) { //return empty bitmap on error
			e.printStackTrace();
			return Bitmap.createBitmap(256, 256, Config.RGB_565);
		}
	}
	
	//add own position to tile
	private Bitmap addOwnPosition(Bitmap tile, int x, int y) {
		Bitmap bitmap = tile.copy(tile.getConfig(), true);
		
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
	    
	    //add placemark
	    paint.setColor(Color.rgb(0,0,0));
	    canvas.drawCircle(x, y, 20, paint);
	    paint.setColor(Color.rgb(255,255,255));
	    canvas.drawCircle(x, y, 10, paint);
		
		return bitmap;
	}
	
	//generate the map tile
	private byte[] getMapImage(double lat, double lng){
		//get position according to tile url
		double x = (lng+180)/360*(1<<zoom);
		double y = (1-Math.log(Math.tan(Math.toRadians(lat))+1/Math.cos(Math.toRadians(lat)))/Math.PI)/2*(1<<zoom);
		
		//get main tiles url
		int xUrl = (int)Math.floor(x);
		int yUrl = (int)Math.floor(y);
		
		//get position on tile
		int xTile = ((int)Math.floor(((x * 100) % 100))*256/100);
		int yTile = ((int)Math.floor(((y * 100) % 100))*256/100);

		//margin to main tiles border; prevents loading tiles for every pixelchange
		int margin = 50;
		
		//booleans to define, which additional tiles are needed
		boolean top = yTile<256-margin && yTile<0+margin ? true : false;
		boolean bottom = yTile>256-margin && yTile>0+margin ? true : false;
		boolean left = xTile<256-margin && xTile<0+margin ? true : false;
		boolean right = xTile>256-margin && xTile>0+margin ? true : false;
		
		//create bitmap with max possible size
		Bitmap bitmap = Bitmap.createBitmap(512, 512, Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		
		//add only top
		if(top==true && bottom==false && left==false && right==false) {		
			canvas.drawBitmap(getTile(xUrl,yUrl-1), 0, 0, paint);
			canvas.drawBitmap(getTile(xUrl,yUrl), 0, 256, paint);
			
			bitmap = addOwnPosition(bitmap,xTile,255+yTile);
			bitmap = Bitmap.createBitmap(bitmap, 0, 255+yTile-128, 256, 256);
		}
		
		//add only bottom
		if(bottom==true && top==false && left==false && right==false) {
			canvas.drawBitmap(getTile(xUrl,yUrl), 0, 0, paint);
			canvas.drawBitmap(getTile(xUrl,yUrl+1), 0, 256, paint);
			
			bitmap = addOwnPosition(bitmap,xTile,yTile);
			bitmap = Bitmap.createBitmap(bitmap, 0, yTile-128, 256, 256);
		}
		
		//add only left
		else if(left==true && top==false && bottom==false && right==false) {
			canvas.drawBitmap(getTile(xUrl-1,yUrl), 0, 0, paint);
			canvas.drawBitmap(getTile(xUrl,yUrl), 256, 0, paint);
			
			bitmap = addOwnPosition(bitmap,255+xTile,yTile);
			bitmap = Bitmap.createBitmap(bitmap, 255+xTile-128, 0, 256, 256);
		}
		
		//add only right
		else if(right==true && top==false && bottom==false && left==false) {
			canvas.drawBitmap(getTile(xUrl,yUrl), 0, 0, paint);
			canvas.drawBitmap(getTile(xUrl+1,yUrl), 256, 0, paint);
			
			bitmap = addOwnPosition(bitmap,xTile,yTile);
			bitmap = Bitmap.createBitmap(bitmap, xTile-128, 0, 256, 256);
		}
		
		//add top left
		else if(top==true && left==true && bottom==false && right==false) {
			canvas.drawBitmap(getTile(xUrl-1,yUrl-1), 0, 0, paint);
			canvas.drawBitmap(getTile(xUrl,yUrl-1), 256, 0, paint);
			canvas.drawBitmap(getTile(xUrl-1,yUrl), 0, 256, paint);
			canvas.drawBitmap(getTile(xUrl,yUrl), 256, 256, paint);
			
			bitmap = addOwnPosition(bitmap,255+xTile,255+yTile);
			bitmap = Bitmap.createBitmap(bitmap, 255+xTile-128, 255+yTile-128, 256, 256);
		}
		
		//add top right
		else if(top==true && right==true && bottom==false && left==false) {
			canvas.drawBitmap(getTile(xUrl,yUrl-1), 0, 0, paint);
			canvas.drawBitmap(getTile(xUrl+1,yUrl-1), 256, 0, paint);
			canvas.drawBitmap(getTile(xUrl,yUrl), 0, 256, paint);
			canvas.drawBitmap(getTile(xUrl+1,yUrl), 256, 256, paint);
			
			bitmap = addOwnPosition(bitmap,xTile,255+yTile);
			bitmap = Bitmap.createBitmap(bitmap, xTile-128, 255+yTile-128, 256, 256);
		}
		
		//add bottom left
		else if(bottom==true && left==true && top==false && right==false) {
			canvas.drawBitmap(getTile(xUrl-1,yUrl), 0, 0, paint);
			canvas.drawBitmap(getTile(xUrl,yUrl), 256, 0, paint);
			canvas.drawBitmap(getTile(xUrl-1,yUrl+1), 0, 256, paint);
			canvas.drawBitmap(getTile(xUrl,yUrl+1), 256, 256, paint);
			
			bitmap = addOwnPosition(bitmap,255+xTile,yTile);
			bitmap = Bitmap.createBitmap(bitmap, 255+xTile-128, yTile-128, 256, 256);
		}
		
		//add bottom right
		else if(bottom==true && right==true && top==false && left==false) {
			canvas.drawBitmap(getTile(xUrl,yUrl), 0, 0, paint);
			canvas.drawBitmap(getTile(xUrl+1,yUrl), 256, 0, paint);
			canvas.drawBitmap(getTile(xUrl,yUrl+1), 0, 256, paint);
			canvas.drawBitmap(getTile(xUrl+1,yUrl+1), 256, 256, paint);
			
			bitmap = addOwnPosition(bitmap,xTile,yTile);
			bitmap = Bitmap.createBitmap(bitmap, xTile-128, yTile-128, 256, 256);
		}
		
		//only one tile
		else
			bitmap=addOwnPosition(getTile(xUrl,yUrl),xTile,yTile);

	    //resize bitmap
	    Matrix matrix = new Matrix();
	    matrix.postScale(((float)128)/bitmap.getWidth(), ((float)128)/bitmap.getHeight());
	    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
	    
	    //add background rectangle for attribution text
	    canvas = new Canvas(bitmap);
	    Path path = new Path();
	    RectF rectf = new RectF(0, 114, 128, 128);
	    paint.setColor(Color.rgb(0,0,0));
	    path.addRect(rectf, Path.Direction.CCW);
	    canvas.drawPath(path, paint);
	    
	    //add attribution text
	    paint.setColor(Color.rgb(255,255,255));
	    paint.setTextSize(6);
	    String attribution_top = "map tiles by Stamen Design under CC BY 3.0";
	    canvas.drawText(attribution_top, 2, 120, paint);
	    String attribution_bottom = "data by OpenStreetMap under CC BY SA";
	    canvas.drawText(attribution_bottom, 2, 126, paint);
	    
	    //convert bitmap to byte array and return it
	    ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
	    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, streamOut); //jpeg is the only compression also supported by .NET micro framework
	    return streamOut.toByteArray();
	}
}
