package com.my.fly;

import dji.midware.data.manager.P3.ServiceManager;
import dji.midware.usb.P3.DJIUsbAccessoryReceiver;
import dji.midware.usb.P3.UsbAccessoryService;
import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.view.View;

public class MainMenu extends Activity
{
	private static boolean isStarted = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
		super.onCreate(savedInstanceState);
		
		if (!isStarted)
		{
			isStarted = true;
			ServiceManager.getInstance();
			UsbAccessoryService.registerAoaReceiver(this);
		}
        
		setContentView(R.layout.activity_main_menu);

		ListView listView = (ListView) findViewById(R.id.droneTypes);

		final String[] enueItems = new String[] { getString(R.string.ManualControl), getString(R.string.AutomaticalControl) };

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, enueItems);

		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> arg0, View v, int index, long arg3)
			{
				Intent intent = null;
                
				switch (index)
				{
					case 0:
						intent = new Intent(MainMenu.this, MainActivity.class);
						break;

					case 1:
						intent = new Intent(MainMenu.this, RoutesActivity.class);
						break;
				}

				intent.putExtra("DroneType", index);
				startActivity(intent);
				
				Intent aoaIntent = getIntent();
				if (aoaIntent != null)
				{
					String action = aoaIntent.getAction();
					if (action == UsbManager.ACTION_USB_ACCESSORY_ATTACHED || action == Intent.ACTION_MAIN)
					{
						Intent attachedIntent = new Intent();
						attachedIntent.setAction(DJIUsbAccessoryReceiver.ACTION_USB_ACCESSORY_ATTACHED);
						sendBroadcast(attachedIntent);
					}
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
