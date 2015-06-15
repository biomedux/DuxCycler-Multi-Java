package com.mypcr.bootloader.structure;

public class ReadPacket implements Packet
{
	private byte[] packet;
	
	public ReadPacket()
	{
		packet = new byte[BUF_SIZE];
	}
	
	public byte getCommand()
	{
		return (byte)(packet[0] & 0xff);
	}
	
	public byte[] getAddress()
	{
		byte[] address = new byte[4];
		for(int i=0; i<address.length; i++)
			address[i] = (byte)(packet[1+i] & 0xff);
		return address;
	}
	
	public byte getBytesPerPacket()
	{
		return (byte)(packet[5]);
	}
	
	public byte[] getData()
	{
		byte[] data = new byte[58];
		for(int i=0; i<data.length; i++)
			data[i] = packet[6 + i];
		return data;
	}

	@Override
	public byte[] toByte() 
	{
		return packet;
	}
	
}
