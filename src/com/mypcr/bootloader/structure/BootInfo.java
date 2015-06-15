package com.mypcr.bootloader.structure;

public class BootInfo implements Packet
{ 	
	private byte[] packet;
	private boolean IsAnd = false;
	
	public BootInfo()
	{
		packet = new byte[BUF_SIZE];
	}
	
	public byte getCommand()
	{
		return (byte)(packet[0] & 0xff);
	}
	
	public byte getBytesPerPacket()
	{
		return (byte)(packet[1] & 0xff);
	}
	
	public byte getDeviceFamily()
	{
		return (byte)(packet[2] & 0xff);
	}
	
	public void setMemoryRegion(MemoryRegion[] memory)
	{
		for(int i=0; i<MAX_DATA_REGION; i++)
		{
			byte[] temp = memory[i].toByte();
			packet[3 + (i * 9)] = temp[0];	packet[4 + (i * 9)] = temp[1];
			packet[5 + (i * 9)] = temp[2];	packet[6 + (i * 9)] = temp[3];
			packet[7 + (i * 9)] = temp[4];	packet[8 + (i * 9)] = temp[5];
			packet[9 + (i * 9)] = temp[6];	packet[10 + (i * 9)] = temp[7];
			packet[11 + (i * 9)] = temp[8];
		}
	}
	
	public MemoryRegion[] getMemoryRegion()
	{
		MemoryRegion[] memory = new MemoryRegion[MAX_DATA_REGION];
		for(int i=0; i<MAX_DATA_REGION; i++)
		{
			memory[i] = new MemoryRegion();
			byte[] temp = memory[i].toByte();
			temp[0] = packet[3 + (i * 9)];	temp[1] = packet[4 + (i * 9)];
			temp[2] = packet[5 + (i * 9)];	temp[3] = packet[6 + (i * 9)];
			temp[4] = packet[7 + (i * 9)];	temp[5] = packet[8 + (i * 9)];
			temp[6] = packet[9 + (i * 9)];	temp[7] = packet[10 + (i * 9)];
			temp[8] = packet[11 + (i * 9)];
		}
		return memory;
	}
	
	public void setPad(byte pad)
	{
	}
	
	public byte[] getPad()
	{
		byte[] pad = new byte[8];
		for(int i=0; i<8; i++)
			pad[i] = packet[56 + i];
		return pad;
	}

	@Override
	public byte[] toByte() 
	{
		return packet;
	}

}
