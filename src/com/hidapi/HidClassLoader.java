package com.hidapi;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


public class HidClassLoader 
{
	private static Boolean loaded = null;
	
	private static String getLibraryName()
	{
		String os = System.getProperty("os.name", "win").toLowerCase();
		String arch = System.getProperty("os.arch", "x86");
		boolean x64 = arch.indexOf("64") != -1;
		String Library = "";
		
		if( os.indexOf("win") != -1 )
		{
			Library = "win/hidapi-jni-";
			if( x64 )
				Library += "64.dll";
			else
				Library += "32.dll";
		}
		else if( os.indexOf("mac") != -1 )
		{
			Library = "mac/libhidapi-jni-";
			if( x64 )
				Library += "64.jnilib";
			else
				Library += "32.jnilib";
		}
		
		return Library;
	}
	
	public static boolean LoadLibrary()
	{
		if( loaded != null )
			return loaded == Boolean.TRUE;
		
		ClassLoader cl = HidClassLoader.class.getClassLoader();
		try
		{
			InputStream in = cl.getResourceAsStream("native/" + getLibraryName());
			if( in == null )
				throw new RuntimeException("Not Found Library file!");
			
			File libFile = File.createTempFile("hidapi", ".lib");
			
			byte buf[] = new byte[2048];
			OutputStream out = new FileOutputStream(libFile);
			int i;
			while( (i = in.read(buf)) > 0 )
				out.write(buf, 0, i);
			
			in.close();
			out.close();
			
			System.load(libFile.getAbsolutePath());
			loaded = Boolean.TRUE;
			
		}catch(Exception e)
		{
			e.printStackTrace();
			loaded = Boolean.FALSE;
			return false;
		}
		
		return true;
	}
}
