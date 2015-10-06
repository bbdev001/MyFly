package com.my.fly;

import java.io.File;
import java.io.IOException;
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
	
	public boolean HasFile(String fileName)
	{
		File destCheck = new File(path + fileName);
		
        return destCheck.exists();	
	}
	
	public void OpenFile(String fileName)
	{
		if (IsOpened())
			CloseFile();
		
		try 
		{
            File destCheck = new File(path + fileName);
            if (destCheck.exists()) 
                destCheck.delete();
            
            file = new RandomAccessFile(path + fileName, "rw");
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
	
	public void CloseFile()
	{
		try
		{
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
}
