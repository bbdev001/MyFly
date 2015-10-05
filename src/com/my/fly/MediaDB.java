package com.my.fly;

import java.io.File;
import java.io.RandomAccessFile;

public class MediaDB
{
	protected String path;
	RandomAccessFile file = null;
	
	public MediaDB(String path)
	{
        File dbDir = new File(path);
        if (!dbDir.exists()) 
            dbDir.mkdirs();
        
        this.path = path;
	}
	
	public void OpenFile(String fileName)
	{
		try 
		{
            File destCheck = new File(path + fileName);
            if (destCheck.exists()) 
                destCheck.delete();
            
            file = new RandomAccessFile(path + fileName, "rw");
        } 
		catch (Exception e) 
		{
			e.printStackTrace();
        }
	}
	
	public void CloseFile()
	{
		try
		{
			file.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
