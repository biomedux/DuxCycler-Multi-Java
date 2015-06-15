package com.mypcr.bootloader.structure;

public class MemoryRegion
{
	// Types of memory regions
	public static final int PROGRAM_MEMORY		=	0x01;
	public static final int EEPROM_MEMORY		=	0x02;
	public static final int CONFIG_MEMORY		=	0x03;
	public static final int END_OF_TYPES_LIST	=	0xff;
	
	public static final int MAXIMUM_PROGRAMMABLE_MEMORY_SEGMENT_SIZE  = 0x0FFFFFFF;
	
	
	public static final int SIZE	= 9;	
			
	private byte[] packet;
	
	public MemoryRegion()
	{
		packet = new byte[SIZE];
	}
	
	public void setType(byte type)
	{
		packet[0] = type;
	}
	
	public int getType()
	{
		return (packet[0] & 0xff);
	}
	
	public int getAddress()
	{
		int[] temp = new int[4];
		for(int i=0; i<4; i++)
			temp[i] = (packet[i+1] & 0xff);
		int address = temp[0] + temp[1] * (0xff+1) + temp[2] * (0xffff+1) + temp[3] * (0xffffff+1);
		return address;
	}
	
	public void setSize(int size)
	{
		int temp1, temp2, temp3;
		byte tsize1, tsize2, tsize3, tsize4;
		
		temp1 = size / 256;
		temp2 = temp1 / 256;
		temp3 = temp2 / 256;
		
		tsize1 = (byte)(size % 256);
		tsize2 = (byte)(temp1 % 256);
		tsize3 = (byte)(temp2 % 256);
		tsize4 = (byte)(temp3 % 256);
		
		packet[5] = tsize1;	packet[6] = tsize2;
		packet[7] = tsize3; packet[8] = tsize4;
	}
	
	public int getSize()
	{
		int[] temp = new int[4];
		for(int i=0; i<4; i++)
			temp[i] = (packet[i+5] & 0xff);
		int size = temp[0] + temp[1] * (0xff+1) + temp[2] * (0xffff+1) + temp[3] * (0xffffff+1);
		return size;
	}
	
	public byte[] toByte()
	{
		return packet;
	}
}
