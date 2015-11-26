package com.my.fly;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.my.fly.utilities.Mbr;
import com.my.fly.utilities.Utilities;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.ExifInterface;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;

public class MediaDB
{
	protected String fileName;
	protected String path;
	protected RandomAccessFile file = null;
	protected MediaDBSql sqlDb = null;
	
	public class ImageInfo
	{
		public double lat;
		public double lon;
		public double alt;
		public String name;
		
		public ImageInfo(double lat, double lon, double alt, String name)
		{
			this.lat = lat;
			this.lon = lon;
			this.alt = alt;
			this.name = name;
		}
	}
	
	public MediaDB(Context context, String path)
	{
		File dbDir = new File(path);
		if (!dbDir.exists())
			dbDir.mkdirs();

		this.path = path;

		sqlDb = new MediaDBSql(context, path, 2);
	}

	public boolean HasFile(String fileName)
	{
		File destCheck = new File(path + fileName);

		return destCheck.exists();
	}

	public String GetCurrentFileName()
	{
		return fileName;
	}

	public void OpenFile(String fileName)
	{
		if (IsOpened())
			CloseFile();

		try
		{
			this.fileName = fileName;
			String fullFileName = path + fileName;
			File destCheck = new File(fullFileName);

			if (destCheck.exists())
				destCheck.delete();

			file = new RandomAccessFile(fullFileName, "rw");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public ArrayList<ImageInfo> GetMediaNamesByRect(Mbr mbr)
	{
		return sqlDb.GetImagesByRect(mbr.Xmin, mbr.Xmax, mbr.Ymin, mbr.Ymax);
	}
	
	public void WriteFileBlock(byte[] buffer, int bufferSize)
	{
		try
		{
			file.write(buffer, 0, bufferSize);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void CommitFiles(ArrayList<String> files)
	{
		try
		{
			sqlDb.PrepareInsertion();

			ExifInterface exifInterface = null;
			float[] coords = new float[2];

			for (int i = 0; i < files.size(); i++)
			{
				exifInterface = new ExifInterface(path + files.get(i));
				double altInMeters = exifInterface.getAltitude(0.0);

				if (exifInterface.getLatLong(coords))
				{
					double lat = coords[0];
					double lon = coords[1];
					double alt = altInMeters;

					sqlDb.AddImageInfo(lat, lon, alt, files.get(i));
				}
			}

			sqlDb.DoneInsertion();
		}
		catch (Exception ex)
		{
			// Send error here
			Log.e("MediaDB", "can't commit images", ex);
		}
		finally
		{
			sqlDb.CommitInsertion();
		}
	}

	public void RebuildIndexes()
	{
		File file = new File(path);
		File[] files = file.listFiles();

		if (files == null)
			return;

		ArrayList<String> filesToCommit = new ArrayList<String>();

		for (int i = 0; i < files.length; i++)
		{
			if (files[i].isDirectory())
				continue;

			String name = files[i].getName();
			if (name.toLowerCase().indexOf(".jpg") < 0)
				continue;

			filesToCommit.add(name);
		}

		sqlDb.Clear();
		CommitFiles(filesToCommit);

		filesToCommit.clear();
	}

	public void CloseFile()
	{
		try
		{
			fileName = "";
			file.close();
			file = null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public boolean IsOpened()
	{
		return file != null;
	}
	
	public class MediaDBSql extends SQLiteOpenHelper
	{
		protected final String tableName = "pictures";
		protected final String latLonIndexName = "picturesLatLon";
		protected String path = null;
		
		public MediaDBSql(Context context, String databasePath, int version)
		{
			super(context, databasePath + "pictures.db" , null, version);
			
			path = databasePath;
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + tableName + " (id INTEGER PRIMARY KEY, lat REAL, lon REAL, alt REAL, name TEXT)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			db.execSQL("DROP TABLE IF EXISTS " + tableName);
			onCreate(db);
		}

		public void PrepareInsertion()
		{
			getWritableDatabase().beginTransaction();
			getWritableDatabase().execSQL("CREATE INDEX IF NOT EXISTS " + latLonIndexName + " ON " + tableName + "(lat, lon)");
		}

		public void DoneInsertion()
		{
			getWritableDatabase().setTransactionSuccessful();
		}

		public void CommitInsertion()
		{
			getWritableDatabase().endTransaction();
		}

		public void Clear()
		{
			try
			{
				getWritableDatabase().beginTransaction();
				getWritableDatabase().execSQL("DROP INDEX IF EXISTS " + latLonIndexName);
				getWritableDatabase().execSQL("DELETE FROM " + tableName);
				getWritableDatabase().setTransactionSuccessful();
			}
			finally
			{
				getWritableDatabase().endTransaction();
			}
		}

		public void AddImageInfo(double lat, double lon, double alt, String name)
		{
			String sql = "INSERT INTO " + tableName + " (lat, lon, alt, name) VALUES (" + lat + "," + lon + "," + alt + ",'" + name + "')";
			
			getWritableDatabase().execSQL(sql);
		}

		public ArrayList<ImageInfo> GetImagesByRect(double minX, double maxX, double minY, double maxY)
		{
			ArrayList<ImageInfo> results = new ArrayList<ImageInfo>();
	        String sql = "SELECT name, lat, lon, alt FROM " + tableName + " WHERE lat<=" + maxY + " AND lat>=" + minY + " AND lon>=" + minX + " AND lon<=" + maxX + " GROUP BY lat,lon HAVING max(id)";
	        
	        Cursor c = this.getReadableDatabase().rawQuery(sql , null);
	        
	        if(c.moveToFirst())
	        {
	            do
	            {  
	               results.add(new ImageInfo(c.getDouble(1), c.getDouble(2), c.getDouble(3), path + c.getString(0)));
	            }
	            while(c.moveToNext());
	        }
	        
	        c.close();
	        
	        return results;
		}
		
		public void Close()
		{
			this.close();
		}
	}
}
