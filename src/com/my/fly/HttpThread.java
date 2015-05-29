package com.my.fly;

import java.util.ArrayList;

public class HttpThread extends Thread
{
	public String url;
	public String result;
	public ArrayList<String> responseStrings = new ArrayList<String>();
		
	public HttpThread(String url)
	{
		this.url = url;
	}	
}

