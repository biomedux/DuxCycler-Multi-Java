package com.mypcr.bootloader.structure;

public class UnLockConfig implements Packet 
{
	private byte[] packet;

	public UnLockConfig()
	{
		packet = new byte[BUF_SIZE];
	}
	
	public void setReport(byte report)
	{
		packet[0] = report;
	}
	
	public void setCommand(byte command)
	{
		packet[1] = command;
	}
	
	public void setSetting(byte setting)
	{
		packet[2] = setting;
	}
	
	public void setData(byte[] data)
	{
		for(int i=0; i<data.length; i++)
			packet[3 + i] = data[i];
	}
	
	@Override
	public byte[] toByte() 
	{
		return packet;
	}
	
}
