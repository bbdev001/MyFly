package com.my.fly;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

import geolife.android.navigationsystem.NavmiiControl;
import geolife.android.navigationsystem.NavmiiControl.Direction;
import geolife.android.navigationsystem.NavmiiControl.DirectionType;
import geolife.android.navigationsystem.NavmiiControl.MapCoord;
import geolife.android.navigationsystem.NavmiiControl.RouteCalculationStatus;

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

import com.dji.wrapper.DJICamera.CameraFileInfo;
import com.dji.wrapper.DJIGroundStation;
import com.dji.wrapper.DJIWrapper;
import com.dji.wrapper.Route;
import com.dji.wrapper.TaskBuilder;
import com.my.fly.utilities.DegPoint;
import com.my.fly.utilities.MrcPoint;
import com.my.fly.utilities.Utilities;
import com.my.fly.utilities.WayPoint;

import dji.midware.data.manager.P3.ServiceManager;
import dji.midware.usb.P3.UsbAccessoryService;
import dji.sdk.api.DJIDroneTypeDef.DJIDroneType;
import dji.sdk.api.Battery.DJIBatteryProperty;
import dji.sdk.api.Gimbal.DJIGimbalAttitude;
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Point;
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
import android.view.ViewGroup;
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

public class RoutesActivity extends Activity implements OnItemClickListener, LocationListener, Handler.Callback, NavmiiControl.ReverseLookupCallback, NavmiiControl.ControlEventListener, NavmiiControl.MapControlEventListener, NavmiiControl.UserItemsOnMapEventListener, NavmiiControl.OnRouteEventListener
{
	private DJIWrapper djiWrapper = new DJIWrapper();
	private static final String TAG = "RoutesActivity";
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
	private ViewGroup mapView = null;
	private LocationManager locationManager = null;
	private DegPoint userPosition = new DegPoint();
	private DegPoint homePosition = new DegPoint();
	private DegPoint lastPosition = new DegPoint();
	private TextView errorMessages = null;
	private boolean routeStarted = false;
	private boolean routePaused = false;
	private DjiGLSurfaceView djiSurfaceView;
	private RelativeLayout djiSurfaceViewLayout;
	private DJIDroneType droneType = DJIDroneType.DJIDrone_Phantom3_Professional;
	private NavmiiControl navigationSystem;
	private String resourcePath = "";
	
	// private RelativeLayout djiSurfaceViewLayout;
	public String SERVER_ADDRESS = "http://192.168.1.97:8089/";
	public String BASE_PATH = Environment.getExternalStorageDirectory() + "/MyFly";
	public boolean isMapping = false;
	protected int smallPreviewWidth = 0;
	protected int smallPreviewHeight = 0;
	protected int bigPreviewWidth = 0;
	protected int bigPreviewHeight = 0;
	protected boolean isPreviewSmall = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		Log.e(TAG, BASE_PATH);
		setContentView(R.layout.activity_routes);

		routesList = (ListView) findViewById(R.id.routes);
		routeView = (RouteView) findViewById(R.id.routeView);
		mapView = (ViewGroup) findViewById(R.id.mapSurface);
		startRoute = (ImageButton) findViewById(R.id.startRoute);
		pauseRoute = (ImageButton) findViewById(R.id.pauseRoute);
		stopRoute = (ImageButton) findViewById(R.id.stopRoute);
		goHome = (ImageButton) findViewById(R.id.goHome);
		takePhoto = (ImageButton) findViewById(R.id.takePhoto);
		djiSurfaceView = (DjiGLSurfaceView) findViewById(R.id.djiSurfaceView);
		djiSurfaceViewLayout = (RelativeLayout) findViewById(R.id.djiSurfaceViewLayout);
		((RadioButton) findViewById(R.id.routeTypeRouting)).setChecked(true);
		
		pauseRoute.setEnabled(false);
		stopRoute.setEnabled(false);
		goHome.setEnabled(true);

		resourcePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/navmii-assets";	
		navigationSystem = NavmiiControl.Create(this);
		
		navigationSystem.onCreate(mapView, resourcePath);
		
		navigationSystem.SetControlEventListener(this);
		navigationSystem.SetMapControlEventListener(this);
		navigationSystem.SetItemsOnMapEventListener(this);
		navigationSystem.SetOnRouteEventListener(this);

		routeView.SetNavigationSystem(navigationSystem, resourcePath);
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

		routesListArapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.mylistview_item, routes);
		routesList.setAdapter(routesListArapter);
		routesList.setOnItemClickListener(this);

		smallPreviewWidth = djiSurfaceViewLayout.getLayoutParams().width;
		smallPreviewHeight = djiSurfaceViewLayout.getLayoutParams().height;
		bigPreviewWidth = routeView.width;
		bigPreviewHeight = routeView.height;
		
		AppendString("Connecting to drone");
		if (!djiWrapper.InitSDK(droneType, getApplicationContext(), this))
			AppendString("Can't init DJI sdk");
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
			case DJIWrapper.GIMBAL_STATUS:
			{
				DJIGimbalAttitude status = (DJIGimbalAttitude)msg.obj;
				
				routeView.SetGimbalStatus(status.pitch, status.roll, status.yaw);
				break;
			}
			case DJIWrapper.MCU_STATUS:
			{
				DJIMainControllerSystemState status = (DJIMainControllerSystemState) msg.obj;

				homePosition.Lat = status.homeLocationLatitude;
				homePosition.Lon = status.homeLocationLongitude;
				routeView.SetHomePosition(homePosition);

				lastPosition.Lat = status.droneLocationLatitude;
				lastPosition.Lon = status.droneLocationLongitude;

				float[] distance = new float[3];
				Location.distanceBetween(lastPosition.Lat, lastPosition.Lon, userPosition.Lat, userPosition.Lon, distance);
				
				routeView.SetDronePosition(lastPosition, status.altitude, status.speed, (double) distance[0], status.remainFlyTime, status.powerLevel, status.pitch, status.roll, Utilities.ConvertYawToHeading(status.yaw));
				
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
				break;
			case DJIWrapper.GROUNDSTATION_TAKE_OFF_DONE:
				AppendString("Take off done");
				djiWrapper.GetGroundStation().StartTask(gsTask);
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
			case DJIWrapper.CAMERA_FILE_INFO:
			{
				CameraFileInfo info = (CameraFileInfo)msg.obj;
				AppendString("Done " + info.Name);
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
			LoadRoute(route.name.isEmpty() ? routes.get(0) : route.name);
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

	private Route route = new Route("");
	protected void LoadRoute(String curRouteName)
	{	
		if (curRouteName.isEmpty())
			return;
		
		route.LoadFromCSV(BASE_PATH, curRouteName);
					
		((RadioButton) findViewById(R.id.routeTypeRouting)).setChecked(true);
		BuildRouteForType(false);
	}

	protected void SaveRoute()
	{
		if (!route.name.isEmpty())
			route.SaveToCSV(BASE_PATH, route.name);
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
			if (djiWrapper.GetMcu().IsFlying())
				djiWrapper.GetGroundStation().StartTask(gsTask);
			else
				djiWrapper.GetGroundStation().TakeOff(7.0f);
		}
		else if (routePaused)
		{
			AppendString("Resume task");
			djiWrapper.GetGroundStation().ResumeTask();
		}
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

	public void SetPreviewSizeSmall()
	{
		//djiSurfaceViewLayout.setLayoutParams(new RelativeLayout.LayoutParams(smallPreviewWidth, smallPreviewHeight));
	}

	public void SetPreviewSizeBig()
	{
		//djiSurfaceViewLayout.setLayoutParams(new RelativeLayout.LayoutParams(bigPreviewWidth, bigPreviewHeight));
	}
	
	public void VideoPreviewOnClick(View v)
	{
		if (isPreviewSmall)
		{
			isPreviewSmall = false;
			SetPreviewSizeBig();
		}
		else
		{
			isPreviewSmall = true;
			SetPreviewSizeSmall();
		}
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
			Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
		}
		catch (Exception e)
		{
		}
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
		navigationSystem.onPause();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		djiWrapper.ConnectDroneDevices(djiSurfaceView);
		navigationSystem.onResume();
	}

	@Override
	protected void onDestroy()
	{
		djiWrapper.Destroy();

		super.onDestroy();
		navigationSystem.onDestroy();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		navigationSystem.onStop();
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		navigationSystem.onStart();
	}

	@Override
	protected void onRestart()
	{
		super.onRestart();
		navigationSystem.onRestart();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);

		navigationSystem.onConfigurationChanged(newConfig);
	}
	

	protected int selectedWayPointId = -1;


	public void WayPointSelected(final Long markerId)
	{
		int wayPointId = routeView.GetWayPointNumberByMarkerId(markerId);

		Log.e(TAG, "onWayPointSelected");

		if (route.GetWayPoints().size() == 0)
			return;

		final WayPoint wayPoint = wayPointId >= 0 ? route.GetWayPoints().get(wayPointId) : route.GetWayPoints().get(0);
		
		selectedWayPointId = wayPointId;

		final WPEditor dialog = new WPEditor(this, "Edit waypoint " + wayPointId, wayPointId, wayPoint, isMapping,
		new WPEditor.OnDialogClosedListener()
		{
			public void OnClosed(boolean isCancel)
			{
				Log.e(TAG, "WPEditor.OnClosed");

				if (!isCancel)
				{
					if (!isMapping)
					{
						routeView.SetWayPoint(markerId, wayPoint);
						routeView.SelectWayPointByMarkerId(markerId);
						BuildTask(isMapping);
					}
					else
					{
						route.mappingAltitude = wayPoint.Alt;
						BuildTask(isMapping);
						routeView.SetRoute(route, true);
					}
					
					SaveRoute();						
				}
			}
		},
		new WPEditor.OnWPDeletedListener()
		{
			@Override
			public void OnDeleted(int wayPointIndex)
			{
				if (!isMapping)
				{
					route.GetWayPoints().remove(wayPointIndex);
					routeView.SetRoute(route, false);
					
					selectedWayPointId = -1;
					
					BuildTask(isMapping);
				}
				
				SaveRoute();
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

	private void BuildTask(boolean isMapping)
	{
		if (isMapping)
			TaskBuilder.BuildMappingRoute(gsTask, route);
		else
			TaskBuilder.BuildSequientialRoute(gsTask, route, true);
		
		route.RecalculateLength();
	}
	
	private void BuildRouteForType(boolean isMapping)
	{
		Log.i(TAG, "ChangeRouteType");
		this.isMapping = isMapping;
		//String routeName = currentRouteName;
		
		BuildTask(isMapping);
	
		routeView.SetRoute(route, true);
	}

	public void OnWayPointPositionChanged(int wayPointId, DegPoint newCoord)
	{
		if (this.isMapping)
			return;

		if (wayPointId >= 0)
		{
			route.GetWayPoints().get(wayPointId).coord = newCoord;
			route.RecalculateLength();
		}
		else if (wayPointId == -2)
		{
			newCoord.CopyTo(route.viewPoint.coord);
			route.SetHeadingsToViewPoint();
		}
		
		TaskBuilder.BuildSequientialRoute(gsTask, route, true);
		routeView.SetRoute(route, false);		
	}


	@Override
	public void onUserMarkerClicked(long markerId)
	{
		if (routeView.SelectWayPointByMarkerId(markerId))	
			WayPointSelected(markerId);
	}

	@Override
	public void onReverseLookupFinished()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onControlInitialized()
	{
		navigationSystem.SetMapViewMode3D(false);	
		navigationSystem.SetMapRotation(0.0f);
		navigationSystem.SetSnapToGps(false);				
	
		LoadRoutesList();		
	}

	@Override
	public void onUserMarkerUnpressed(long markerId)
	{
		SaveRoute();	
	}

	@Override
	public void onDirectionListCreated(Direction[] arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDirectionUpdated(float arg0, DirectionType arg1, DirectionType arg2, long arg3, float arg4)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUserMarkerLongPress(long arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUserMarkerPressed(long markerId)
	{
		routeView.SelectWayPointByMarkerId(markerId);		
	}

	@Override
	public boolean onDoubleTapOnMap(Point arg0)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onLongPressOnMap(Point point)
	{
		WayPoint wayPoint = new WayPoint(routeView.TranslateScreenPointToGeoPoint(point));
		long markerId = routeView.AddWayPoint(wayPoint, route.GetWayPoints().size());	
		route.AddWayPoint(wayPoint);
		
		BuildTask(false);
		
		SaveRoute();
			
		return false;
	}

	@Override
	public void onMapCenterChanged(MapCoord arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPositionChanged(MapCoord arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRotationChanged(float arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapOnMap(Point point)
	{	
		return false;
	}

	@Override
	public void onZoomChanged(float arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRouteCalculationFinished(RouteCalculationStatus arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRouteCalculationStarted()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUserMarkerMoved(long markerId, MapCoord newPosition)
	{
		int number = routeView.GetWayPointNumberByMarkerId(markerId);
		
		if (number >= 0)
		{
			DegPoint degPoint = route.GetWayPoints().get(number).coord;
			degPoint.Lon = newPosition.lon;
			degPoint.Lat = newPosition.lat;	
		
			routeView.ChangeRoutePointPosition(number, newPosition);
		
			route.RecalculateLength();
			routeView.invalidate();
		}
		else
		{
			route.viewPoint.coord.Lon = newPosition.lon;
			route.viewPoint.coord.Lat = newPosition.lat;
			route.SetHeadingsToViewPoint();
			routeView.RefreshMarkers();
		}
	}

	@Override
	public void onReverseLookupItemAdded(String addressLine1, String addressLine2, String country, String county, String city, String adminHierarchy, String roadName, String roadNumber)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void OnAddRoute(View v)
	{
		final InputBox dialog = new InputBox(this, "Enter route name", "Route name", "My route",
		new InputBox.OnDialogClosedListener()
		{
			public void OnClosed(boolean isCancel, String result)
			{
				Log.e(TAG, "InputBox.OnClosed");

				if (!isCancel)
				{
					SaveRoute();
					
					route = new Route(result);
					BuildRouteForType(false);
					
					SaveRoute();
					
					LoadRoutesList();
				}
			}
		});
		
		dialog.show();
	}
		
	public void OnDelRoute(View v)
	{
			AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

			alertDialog.setTitle("Confirm Delete...");
			alertDialog.setMessage("Are you sure you want to delete selected route?");
			alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{		
					DeleteRoute();
					LoadRoutesList();
					
					dialog.dismiss();					
				}
			});

			// Setting Negative "NO" Button
			alertDialog.setNegativeButton("NO", null);

			// Showing Alert Message
			alertDialog.show();	
	}
	
	public void OnEditRoute(View v)
	{
		final InputBox dialog = new InputBox(this, "Enter new route name", "Route name", route.name,
		new InputBox.OnDialogClosedListener()
		{
			public void OnClosed(boolean isCancel, String result)
			{
				Log.e(TAG, "InputBox.OnClosed");

				if (!isCancel)
				{
					DeleteRoute();
					
					route.name = result;
					routeView.invalidate();
					SaveRoute();
					
					LoadRoutesList();					
				}
			}
		});
		
		dialog.show();	
	}
	
	public void DeleteRoute()
	{
		File toDelete = new File(BASE_PATH + "/" + route.name + ".csv");
		toDelete.delete();
		
		route = new Route("");
		BuildRouteForType(false);	
	}
}
