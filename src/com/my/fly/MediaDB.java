package com.my.fly;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.ExifInterface;
import android.util.Log;

public class MediaDB
{
	protected String fileName;
	protected String path;
	protected RandomAccessFile file = null;
	protected MediaDBSql sqlDb = null;

	public MediaDB(Context context, String path)
	{
		File dbDir = new File(path);
		if (!dbDir.exists())
			dbDir.mkdirs();

		this.path = path;

		sqlDb = new MediaDBSql(context, path + "pictures.db", 1);
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
					int latInt = (int) (coords[0] * 100000.0f);
					int lonInt = (int) (coords[1] * 100000.0f);
					int altInt = (int) altInMeters;

					sqlDb.AddImageInfo(latInt, lonInt, altInt, files.get(i));
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
			if (name.indexOf(".jpg") < 0)
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

		public MediaDBSql(Context context, String databaseFullPath, int version)
		{
			super(context, databaseFullPath, null, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + tableName + " (id integer primary key, latInt integer, lonInt integer, altInt integer, name text)");
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
			getWritableDatabase().execSQL("CREATE INDEX IF NOT EXISTS " + latLonIndexName + " ON " + tableName + "(latInt, lonInt)");
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
				getWritableDatabase().execSQL("DELETE * FROM " + tableName);
				getWritableDatabase().setTransactionSuccessful();
			}
			finally
			{
				getWritableDatabase().endTransaction();
			}
		}

		public void AddImageInfo(int lat, int lon, int alt, String name)
		{
			getWritableDatabase().execSQL("INSERT INTO " + tableName + " (latInt, lonInt, altInt, name) VALUES (" + lat + "," + lon + "," + alt + ",'" + name + "')");
		}

		public void Close()
		{
			this.close();
		}
	}
}
