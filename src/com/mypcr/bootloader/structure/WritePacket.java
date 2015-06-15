package com.mypcr.bootloader.structure;

public class WritePacket implements Packet
{
	private byte[] packet;
	
	public WritePacket()
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
	
	public void setAddress(int address)
	{
		int temp1 = address / 256;
		int temp2 = temp1 / 256;
		int temp3 = temp2 / 256;
		
		packet[2] = (byte)(address % 256);
		packet[3] = (byte)(temp1 % 256);
		packet[4] = (byte)(temp2 % 256);
		packet[5] = (byte)(temp3 % 256);
	}
	
	public void setBytesPerPacket(byte bytes)
	{
		packet[6] = bytes;
	}
	
	public byte getBytesPerPacket()
	{
		return packet[6];
	}
	
	public void setData(byte[] data)
	{
		for(int i=0; i<data.length; i++)
			packet[7 + i] = data[i];
	}
	
	public byte[] getData()
	{
		byte[] data = new byte[BUF_SIZE-7];
		for(int i=0; i<data.length; i++)
			data[i] = packet[7+i];
		return data;
	}
	
	@Override
	public byte[] toByte() 
	{
		return packet;
	}

}
