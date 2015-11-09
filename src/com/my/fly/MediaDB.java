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
		ExifInterface exifInterface = null;
		
		for(int i = 0; i < files.size(); i++)
		{
			try 
			{
				exifInterface = new ExifInterface(path + files.get(i));
				float[] coords = null;
				if (exifInterface.getLatLong(coords))
				{
				}
				
				double altInMeters = exifInterface.getAltitude(0.0);
			}
			catch (IOException ex) 
			{
				Log.e("MediaDB", "cannot read exif", ex);
			}
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
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean IsOpened()
	{
		return file != null;
	}
	
	public class MediaDBSql  extends SQLiteOpenHelper
	{
		public MediaDBSql(Context context, String databaseFullPath, int version)
		{
			super(context, databaseFullPath , null, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) 
		{
			db.execSQL("CREATE TABLE pictures (id integer primary key, latInt integer, lonInt integer, name text)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
		{
			db.execSQL("DROP TABLE IF EXISTS pictures");
			onCreate(db);
		}	
}
	
}



