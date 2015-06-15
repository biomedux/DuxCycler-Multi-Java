package com.mypcr.bootloader;

import java.util.ArrayList;

public class DeviceData 
{
	private int Type;
	private int Start;
	private int End;
	private int DataLength;
	private byte[] DataBuffer;
	
	public ArrayList<DeviceData> m_DataList = new ArrayList<DeviceData>();

	public int getType() 
	{
		return Type;
	}

	public void setType(int type) 
	{
		Type = type;
	}

	public int getStart() 
	{
		return Start;
	}

	public void setStart(int start) 
	{
		Start = start;
	}

	public int getEnd()
	{
		return End;
	}

	public void setEnd(int end) 
	{
		End = end;
	}

	public int getDataLength() 
	{
		return DataLength;
	}

	public void setDataLength(int dataLength)
	{
		DataLength = dataLength;
	}

	public byte[] getDataBuffer() 
	{
		return DataBuffer;
	}

	public void setDataBuffer(byte[] dataBuffer) 
	{
		DataBuffer = dataBuffer;
	}
}
