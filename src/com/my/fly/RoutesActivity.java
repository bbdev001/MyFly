package com.my.fly;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.dji.wrapper.DJIGroundStation;
import com.dji.wrapper.DJIWrapper;
import com.dji.wrapper.Route;
import com.dji.wrapper.TaskBuilder;
import com.my.fly.utilities.DegPoint;
import com.my.fly.utilities.WayPoint;

import dji.sdk.api.DJIDroneTypeDef.DJIDroneType;
import dji.sdk.api.Battery.DJIBatteryProperty;
import dji.sdk.api.GroundStation.DJIGroundStationExecutionPushInfo;
import dji.sdk.api.GroundStation.DJIGroundStationFlyingInfo;
import dji.sdk.api.GroundStation.DJIGroundStationMissionPushInfo;
import dji.sdk.api.GroundStation.DJIGroundStationTask;
import dji.sdk.api.GroundStation.DJIGroundStationWayPointAction;
import dji.sdk.api.GroundStation.DJIGroundStationWaypoint;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationOnWayPointAction;
import dji.sdk.api.MainController.DJIMainControllerSystemState;
import dji.sdk.api.RemoteController.DJIRemoteControllerAttitude;
import dji.sdk.widget.DjiGLSurfaceView;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class RoutesActivity extends Activity implements OnItemClickListener, LocationListener, Handler.Callback, RouteView.OnWayPointSelected
{
	private DJIWrapper djiWrapper = new DJIWrapper();
	private static final String TAG = "RoutesActivity";
	private ScrollView scrollViewMessages = null;
	private ListView routesList = null;
	private RouteView routeView = null;
	private ArrayList<String> routes = new ArrayList<String>();
	private ArrayAdapter<String> routesListArapter = null;
	private Handler uiHandler = new Handler(this);
	private DJIGroundStationTask gsTask = new DJIGroundStationTask();
	private DJIGroundStationTask gsMyHomeTask = new DJIGroundStationTask();
	private ImageButton startRoute = null;
	private ImageButton pauseRoute = null;
	private ImageButton stopRoute = null;
	private ImageButton goHome = null;
	private ImageButton takePhoto = null;
	private LocationManager locationManager = null;
	private DegPoint userPosition = new DegPoint();
	private DegPoint homePosition = new DegPoint();
	private DegPoint lastPosition = new DegPoint();
	private TextView errorMessages = null;
	private boolean routeStarted = false;
	private boolean routePaused = false;
	private int scrollViewMessagesDefSize = 0;
	private double lastAltitude = 3.0;
	private Button errorMsgSize = null;
	private String currentRouteName = "";
	private DjiGLSurfaceView djiSurfaceView;
	private DJIDroneType droneType = DJIDroneType.DJIDrone_Inspire1;
	// private RelativeLayout djiSurfaceViewLayout;
	public String SERVER_ADDRESS = "http://192.168.1.97:8089/";
	public String BASE_PATH = Environment.getExternalStorageDirectory() + "/MyFly";
	public boolean isMapping = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		Log.e(TAG, BASE_PATH);
		setContentView(R.layout.activity_routes);

		routesList = (ListView) findViewById(R.id.routes);
		routeView = (RouteView) findViewById(R.id.routeView);
		scrollViewMessages = (ScrollView) findViewById(R.id.scrollViewMessages);
		startRoute = (ImageButton) findViewById(R.id.startRoute);
		pauseRoute = (ImageButton) findViewById(R.id.pauseRoute);
		stopRoute = (ImageButton) findViewById(R.id.stopRoute);
		goHome = (ImageButton) findViewById(R.id.goHome);
		takePhoto = (ImageButton) findViewById(R.id.takePhoto);
		errorMessages = (TextView) findViewById(R.id.errorMessages);
		errorMsgSize = (Button) findViewById(R.id.errorMsgSize);
		djiSurfaceView = (DjiGLSurfaceView) findViewById(R.id.djiSurfaceView);
		((RadioButton) findViewById(R.id.droneInspire)).setChecked(true);
		((RadioButton) findViewById(R.id.routeTypeRouting)).setChecked(true);
		
		// djiSurfaceViewLayout = (RelativeLayout)
		// findViewById(R.id.djiSurfaceViewLayout);

		errorMsgSize.setText("-");
		scrollViewMessagesDefSize = scrollViewMessages.getLayoutParams().height;
		pauseRoute.setEnabled(false);
		stopRoute.setEnabled(false);
		goHome.setEnabled(true);

		routeView.AddOnWayPointSelectedListener(this);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

		routesListArapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.mylistview_item, routes);
		routesList.setAdapter(routesListArapter);
		routesList.setOnItemClickListener(this);

		AppendString("Connecting to drone");
		if (!djiWrapper.InitSDK(droneType, getApplicationContext(), this))
			AppendString("Can't init sdk");
			
		LoadRoutesList();
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what)
		{
			case DJIWrapper.CHECK_CAMERA_STATE:
			{
				djiWrapper.GetCamera().IsConnectedOk();
				break;
			}
			case DJIWrapper.PERMISSION_STATUS:
			{
				AppendString(djiWrapper.GetPermissionErrorResultMessage);
				
				//DownloadRoutes();

				djiWrapper.ConnectDroneDevices(djiSurfaceView);
				break;
			}
			case DJIWrapper.CAMERA_TAKE_PHOTO:
			{
				takePhoto.setEnabled(true);
				AppendString("Take photo done: " + (String) msg.obj);
				break;
			}
			case DJIWrapper.CAMERA_TAKE_PHOTO_DONE:
			{
				takePhoto.setEnabled(true);
				AppendString("Take photo stopped: " + (String) msg.obj);
				break;
			}
			case DJIWrapper.GROUNDSTATION_TASK_STARTED:
			{
				startRoute.setEnabled(false);
				pauseRoute.setEnabled(true);
				stopRoute.setEnabled(true);
				goHome.setEnabled(false);

				routeStarted = true;
				routePaused = false;
				AppendString("Task started");
				break;
			}
			case DJIWrapper.GROUNDSTATION_TASK_PAUSED:
			{
				startRoute.setEnabled(true);
				goHome.setEnabled(true);
				pauseRoute.setEnabled(false);

				routePaused = true;
				AppendString("Task paused");
				break;
			}
			case DJIWrapper.GROUNDSTATION_TASK_RESUMED:
			{
				startRoute.setEnabled(false);
				goHome.setEnabled(false);
				pauseRoute.setEnabled(true);

				routePaused = false;
				AppendString("Task resumed");
				break;
			}
			case DJIWrapper.GROUNDSTATION_GO_HOME:
			{
				startRoute.setEnabled(true);
				goHome.setEnabled(true);
				pauseRoute.setEnabled(false);
				stopRoute.setEnabled(true);
				routeStarted = false;
				routePaused = false;
				AppendString("Go home");
				break;
			}
			case DJIWrapper.GROUNDSTATION_TASK_ENDED:
			{
				TaskEnded();
				break;
			}
			case DJIWrapper.ROUTES_DOWNLOADED:
			{
				Integer count = (Integer) msg.obj;

				if (count > 0)
					LoadRoutesList();

				break;
			}
			case DJIWrapper.ERROR_MESSAGE:
			{
				AppendString((String) msg.obj);

				break;
			}
			case DJIWrapper.MCU_STATUS:
			{
				DJIMainControllerSystemState status = (DJIMainControllerSystemState) msg.obj;

				lastAltitude = status.altitude;

				homePosition.Lat = status.homeLocationLatitude;
				homePosition.Lon = status.homeLocationLongitude;
				routeView.SetHomePosition(homePosition);

				lastPosition.Lat = status.droneLocationLatitude;
				lastPosition.Lon = status.droneLocationLongitude;

				float[] distance = new float[3];
				Location.distanceBetween(lastPosition.Lat, lastPosition.Lon, userPosition.Lat, userPosition.Lon, distance);
				
				if (droneType == DJIDroneType.DJIDrone_Inspire1)
					routeView.SetDronePosition(lastPosition, status.altitude / 10.0, status.speed, (double) distance[0], status.remainFlyTime, status.powerLevel, status.pitch / 10.0, status.roll / 10.0, status.yaw / 10.0);
				else
					routeView.SetDronePosition(lastPosition, status.altitude, status.speed, (double) distance[0], status.remainFlyTime, status.powerLevel, status.pitch, status.roll, status.yaw);
				
				break;
			}
			case DJIWrapper.REMOTE_CONTROLLER_STATE:
			{
				DJIRemoteControllerAttitude state = (DJIRemoteControllerAttitude) msg.obj;
				routeView.SetMode(state.mode);
				routeView.SetSattCount(state.satelliteCount);
				break;
			}
			case DJIWrapper.BATTERY_STATUS:
				DJIBatteryProperty status = (DJIBatteryProperty)msg.obj;
				routeView.SetPowerPercent(status.remainPowerPercent);
				break;
			case DJIWrapper.GROUNDSTATION_FLYING_STATUS:
				DJIGroundStationFlyingInfo flyingInfo = (DJIGroundStationFlyingInfo) msg.obj;
				if (droneType == DJIDroneType.DJIDrone_Vision)
					routeView.SetMissionFlightStatus(DJIWrapper.GetFlightMode(flyingInfo.flightMode), flyingInfo.targetWaypointIndex, "");
				break;
			case DJIWrapper.GROUNDSTATION_MISSION_STATUS:
			{
				DJIGroundStationMissionPushInfo missionStatus = (DJIGroundStationMissionPushInfo) msg.obj;
				routeView.SetMissionFlightStatus(DJIWrapper.GetMissionType(missionStatus.missionType), missionStatus.targetWayPointIndex, DJIWrapper.GetMissionExecutionState(missionStatus.currState));
				break;
			}
			case DJIWrapper.GROUNDSTATION_EXECUTION_STATUS:
			{
				DJIGroundStationExecutionPushInfo execStatus = (DJIGroundStationExecutionPushInfo) msg.obj;
				switch (execStatus.eventType.value())
				{
					case DJIWrapper.EXECUTION_STATUS_UPLOAD_FINISH:
						AppendString("Wp " + execStatus.wayPointIndex + " uploaded");
						break;
					case DJIWrapper.EXECUTION_STATUS_FINISH:
						AppendString("Route finished");
						TaskEnded();
						break;
					case DJIWrapper.EXECUTION_STATUS_REACH_POINT:
						routeView.SetReachedWayPoint(execStatus.wayPointIndex, DJIWrapper.GetMissionExecutionState(execStatus.currentState));
						break;
				}
				break;
			}
		}

		return false;
	}

	private void TaskEnded()
	{
		startRoute.setEnabled(true);
		stopRoute.setEnabled(false);
		pauseRoute.setEnabled(false);
		goHome.setEnabled(true);
		routeStarted = false;
		routePaused = false;
		AppendString("Task ended");
	}

	public void LoadRoutesList()
	{
		File file = new File(BASE_PATH);
		File[] files = file.listFiles();

		routes.clear();
		if (files == null)
			return;

		for (int i = 0; i < files.length; i++)
		{
			if (files[i].isDirectory())
				continue;

			String name = files[i].getName();
			String[] nameParts = name.split("\\.");

			if (nameParts.length < 2)
				continue;

			if (nameParts[1].equalsIgnoreCase("csv"))
				routes.add(nameParts[0]);
		}

		routesListArapter.notifyDataSetChanged();
		if (routes.size() > 0)
			LoadRoute(routes.get(0));
	}

	public void DownloadRoutes()
	{
		new Thread()
		{
			public void run()
			{
				ArrayList<String> tmpRoutes = GetData(SERVER_ADDRESS + "RoutesLoader.aspx");

				File file = new File(BASE_PATH);
				if (!file.exists())
					file.mkdir();

				for (int i = 0; i < tmpRoutes.size(); i++)
				{
					ArrayList<String> csvLines = GetData(SERVER_ADDRESS + "RoutesLoader.aspx?routeName=" + tmpRoutes.get(i));

					try
					{
						file = new File(BASE_PATH + "/" + tmpRoutes.get(i) + ".csv");

						// If file does not exists, then create it
						if (!file.exists())
							file.createNewFile();
						else
							file.delete();

						FileWriter fw = new FileWriter(file.getAbsoluteFile());
						BufferedWriter bw = new BufferedWriter(fw);

						for (int j = 0; j < csvLines.size(); j++)
						{
							bw.write(csvLines.get(j) + "\n");
						}

						bw.close();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}

					csvLines.clear();
				}

				uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ROUTES_DOWNLOADED, tmpRoutes.size()));

				tmpRoutes.clear();
			}
		}.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.routes, menu);
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

	public ArrayList<String> GetData(String url)
	{
		HttpThread t = new HttpThread(url)
		{
			public void run()
			{
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(url);
				HttpResponse response;

				try
				{
					response = httpclient.execute(httpGet);

					Log.e(TAG, response.getStatusLine().toString());

					HttpEntity entity = response.getEntity();

					if (entity != null)
					{
						InputStream instream = entity.getContent();
						responseStrings = ReadStreamToStringArray(instream);
						instream.close();
					}

				}
				catch (Exception e)
				{
					Log.e(TAG, e.toString());
				}
			}
		};

		t.start();

		try
		{
			t.join();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		return t.responseStrings;
	}

	private Route route = new Route();
	protected void LoadRoute(String curRouteName)
	{
		currentRouteName = curRouteName;
		
		route.LoadFromCSV(BASE_PATH, currentRouteName);
		
		routeView.SetRoute(route, curRouteName);
		
		((RadioButton) findViewById(R.id.routeTypeRouting)).setChecked(true);
		BuildRouteForType(false);
	}

	protected void SaveRoute(String curRouteName)
	{
		BuildRouteForType(isMapping);	
		route.SaveToCSV(BASE_PATH, currentRouteName);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		LoadRoute(((TextView) view).getText().toString());
	}

	private static ArrayList<String> ReadStreamToStringArray(InputStream is)
	{
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));

		String line = null;
		try
		{
			while ((line = reader.readLine()) != null)
			{
				result.add(line);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return result;
	}

	private void RunGoHomeCommand()
	{
		AppendString("RunGoHome");

		TaskBuilder.BuildMyHomeRoute(gsMyHomeTask, lastPosition, userPosition);
		djiWrapper.GetGroundStation().StartTask(gsMyHomeTask);
	}

	public void OnStartRoute(View v)
	{
		if (!routeStarted)
		{
			AppendString("Start task");
			djiWrapper.GetGroundStation().StartTask(gsTask);
		}
		else if (routePaused)
		{
			AppendString("Resume task");
			djiWrapper.GetGroundStation().ResumeTask();
		}
	}

	private int ConvertHeading(int heading)
	{
		int result = Math.abs(heading);

		if (result < 180)
			result = -result;
		else
			result -= 180;

		return result;
	}

	public void OnStopRoute(View v)
	{
		AppendString("Stop taks");
		djiWrapper.GetGroundStation().StopTask();
	}

	public void OnPauseRoute(View v)
	{
		AppendString("Pause taks");
		djiWrapper.GetGroundStation().PauseTask();
	}

	public void OnGoHome(View v)
	{
		RunGoHomeCommand();
	}

	public void OnErrorMsgSize(View v)
	{
		int width = djiSurfaceView.getLayoutParams().width;
		int height = djiSurfaceView.getLayoutParams().height;

		if (scrollViewMessagesDefSize == height)
		{
			height = 0;
			errorMsgSize.setText("+");
		}
		else
		{
			height = scrollViewMessagesDefSize;
			errorMsgSize.setText("-");
		}

		djiSurfaceView.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
		scrollViewMessages.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
	}

	public void OnErrorMsgClear(View v)
	{
		prevMessage = "";
		errorMessages.setText("");
		textBuilder.setLength(0);
	}

	private StringBuilder textBuilder = new StringBuilder();
	private String prevMessage = "";

	void AppendString(String text)
	{
		try
		{
			if (text.length() == 0)
				return;

			if (prevMessage.equals(text))
				return;

			prevMessage = text;

			Log.e(TAG, text);
			/*
			 * if (textBuilder.length() > 1024)
			 * textBuilder.delete(textBuilder.length() - 1024,
			 * textBuilder.length() - 1);
			 * 
			 * textBuilder.append(text); textBuilder.append("\n");
			 * errorMessages.setText(textBuilder.toString());
			 */
			Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
		}
		catch (Exception e)
		{
		}

		scrollViewMessages.post(new Runnable()
		{
			@Override
			public void run()
			{
				scrollViewMessages.fullScroll(View.FOCUS_DOWN);
			}
		});
	}

	@Override
	public void onLocationChanged(Location location)
	{
		userPosition.Lat = location.getLatitude();
		userPosition.Lon = location.getLongitude();

		routeView.SetDroneUserPosition(userPosition);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{

	}

	@Override
	public void onProviderEnabled(String provider)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderDisabled(String provider)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onPause()
	{
		super.onPause();

		djiWrapper.DisconnectDroneDevices();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		djiWrapper.ConnectDroneDevices(djiSurfaceView);
	}

	@Override
	protected void onDestroy()
	{
		djiWrapper.Destroy();

		super.onDestroy();
	}

	protected int selectedWayPointId = -1;

	@Override
	public void onWayPointSelected(int wayPointId)
	{
		Log.e(TAG, "onWayPointSelected");

		if (route.wayPoints.size() == 0)
			return;

		final WayPoint wayPoint = wayPointId >= 0 ? route.wayPoints.get(wayPointId) : route.mappingWayPoints.get(0);
		
		selectedWayPointId = wayPointId;

		final WPEditor dialog = new WPEditor(this, "Edit waypoint " + wayPointId, wayPointId, wayPoint, isMapping, new WPEditor.OnDialogClosedListener()
		{
			public void OnClosed(boolean isCancel)
			{
				Log.e(TAG, "WPEditor.OnClosed");

				if (!isCancel)
				{				
					if (isMapping)
						route.mappingAltitude = wayPoint.Alt;
					else
						routeView.SetWayPoint(selectedWayPointId, wayPoint);
					
					SaveRoute(currentRouteName);					
				}
			}
		},
		new WPEditor.OnWPDeletedListener()
		{
			@Override
			public void OnDeleted(int wayPointIndex)
			{
				route.wayPoints.remove(wayPointIndex);
				routeView.RemoveWayPoint(wayPointIndex);
				selectedWayPointId = -1;
				
				//SaveRoute(currentRouteName);
			}
		});

		dialog.show();
	}

	public void OnTakePhoto(View v)
	{
		takePhoto.setEnabled(false);
		AppendString("Take photo started");
		djiWrapper.GetCamera().TakePhoto();
	}
	
	public void OnDroneChanged(View view)
	{
		boolean checked = ((RadioButton) view).isChecked();

		switch (view.getId())
		{
			case R.id.dronePhantom:
			{
				if (checked)
					droneType = DJIDroneType.DJIDrone_Vision;

				break;
			}

			case R.id.droneInspire:
			{
				if (checked)
					droneType = DJIDroneType.DJIDrone_Inspire1;

				break;
			}
		}
		
		djiWrapper.Destroy();
		AppendString("Connecting to drone");
		djiWrapper.InitSDK(droneType, getApplicationContext(), this);
	}
	
	public void OnRouteTypeChanged(View view)
	{
		boolean checked = ((RadioButton) view).isChecked();

		switch (view.getId())
		{
			case R.id.routeTypeRouting:
			{
				if (checked)
					BuildRouteForType(false);

				break;
			}

			case R.id.routeTypeMapping:
			{
				if (checked)
					BuildRouteForType(true);

				break;
			}
		}
	}

	private void BuildRouteForType(boolean isMapping)
	{
		Log.i(TAG, "ChangeRouteType");
		this.isMapping = isMapping;
		
		if (isMapping)
		{
			TaskBuilder.BuildMappingRoute(gsTask, route);
			routeView.SetRoute(route, currentRouteName + " mapping");
		}
		else
		{
			TaskBuilder.BuildSequientialRoute(gsTask, route);
			routeView.SetRoute(route, currentRouteName);
		}
	}
}
