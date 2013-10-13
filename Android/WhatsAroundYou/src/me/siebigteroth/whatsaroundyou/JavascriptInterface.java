package me.siebigteroth.whatsaroundyou;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;

public class JavascriptInterface {
	
	private MainActivity context;

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

}
