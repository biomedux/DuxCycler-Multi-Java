package com.mypcr.bootloader.param;

public class GetDeviceAddressParam 
{
	private int[] type;
	private boolean[] includedInProgrammableRange;
	private boolean[] addressWasEndofRange;
	private int[] bytesPerAddressAndType;
	private int[] endDeviceAddressofRegion;
	private byte[] pPCRAMBuffer;
	private int[] byteOffset;
	
	public GetDeviceAddressParam()
	{
		type = new int[1];
		includedInProgrammableRange = new boolean[1];
		addressWasEndofRange = new boolean[1];
		bytesPerAddressAndType = new int[1];
		endDeviceAddressofRegion = new int[1];
		pPCRAMBuffer = null;
		byteOffset = new int[1];
	}
	
	public void setType(int type)
	{
		this.type[0] = type;
	}
	
	public int getType()
	{
		return type[0];
	}
	
	public void setIncludedInProgrammableRange(boolean bool)
	{
		includedInProgrammableRange[0] = bool;
	}
	
	public boolean getIncludedInProgrammableRange()
	{
		return includedInProgrammableRange[0];
	}
	
	public void setAddressWasEndofRange(boolean bool)
	{
		addressWasEndofRange[0] = bool;
	}
	
	public boolean getAddressWasEndofRange()
	{
		return addressWasEndofRange[0];
	}
	
	public void setBytesPerAddressAndType(int bytes)
	{
		bytesPerAddressAndType[0] = bytes;
	}
	
	public int getBytesPerAddressAndType()
	{
		return bytesPerAddressAndType[0];
	}
	
	public void setEndDeviceAddressofRegion(int end)
	{
		endDeviceAddressofRegion[0] = end;
	}
	
	public int getEndDeviceAddressofRegion()
	{
		return endDeviceAddressofRegion[0];
	}
	
	public void setPCRAMBuffer(byte[] buffer)
	{
		pPCRAMBuffer = buffer;
	}
	
	public byte[] getPCRAMBuffer()
	{
		return pPCRAMBuffer;
	}
	
	public void setByteOffset(int offset)
	{
		byteOffset[0] = offset;
	}
	
	public int getByteOffset()
	{
		return byteOffset[0];
	}
}
