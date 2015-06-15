package com.mypcr.bootloader;

import java.util.ArrayList;

import com.mypcr.bootloader.param.GetDeviceAddressParam;
import com.mypcr.bootloader.structure.MemoryRegion;

public class Device 
{
	public static final int PIC_UNKNOWN	=	0x00;
	public static final int PIC_PIC18	=	0x01;
	public static final int PIC_PIC24	=	0x02;
	public static final int PIC_PIC32	=	0x03;
	
	public byte bytesPerPacket;
	public byte bytesPerWordFLASH;
	public byte bytesPerWordEEPROM;
	public byte bytesPerWordConfig;
	
	public byte bytesPerAddressFLASH;
	public byte bytesPerAddressEEPROM;
	public byte bytesPerAddressConfig;
	
	private DeviceData m_DeviceData;
	private byte m_Status;
	
	public Device(DeviceData data)
	{
		m_DeviceData = data;
		
		setUnknown();
	}
	
	public void setUnknown()
	{
		m_Status = PIC_UNKNOWN;
		
		bytesPerAddressFLASH = 1;
		bytesPerAddressEEPROM = 1;
		bytesPerAddressConfig = 1;
		
		bytesPerWordEEPROM = 1;
		bytesPerWordConfig = 2;
		bytesPerWordFLASH = 2;
	}
	
	public void setDeviceData(DeviceData data)
	{
		m_DeviceData = data;
	}
	
	public void setStatus(byte status)
	{
		m_Status = status;
	}
	
	public byte getStatus()
	{
		return m_Status;
	}
	
	public boolean hasEeprom()
	{
		ArrayList<DeviceData> list = m_DeviceData.m_DataList;
		for( int i=0; i<list.size(); i++ )
		{
			if( list.get(i).getType() == MemoryRegion.EEPROM_MEMORY )
				return true;
		}
		return false;
	}
	
	public boolean hasConfig()
	{
		ArrayList<DeviceData> list = m_DeviceData.m_DataList;
		for( int i=0; i<list.size(); i++ )
		{
			if( list.get(i).getType() == MemoryRegion.CONFIG_MEMORY )
				return true;
		}
		return false;
	}
	
	public boolean hasConfigAsFlash()
	{
		return false;
	}
	
	public boolean hasConfigAsFuses()
	{
		return false;
	}
	
	
	public int GetDeviceAddressFromHexAddress(	int hexAddress, 
												DeviceData data, 
												GetDeviceAddressParam[] param)
	{
		ArrayList<DeviceData> list = data.m_DataList;
		int flashAddress = (hexAddress / bytesPerAddressFLASH);
		int eepromAddress = (hexAddress / bytesPerAddressEEPROM);
		int configAddress = (hexAddress / bytesPerAddressConfig);
		int byteOffset;
		
		// Loop for each of the previously identified programable regions
		// based on the results of the previous Query device response packet.
		for(int i=0; i<list.size(); i++)
		{
			DeviceData tempData = list.get(i);
			// Find what address range the hex address seems to contained within
			// ( if any, could be none, in the case the .hex file contains info that
			// is not part of the bootloader re_programmable region of flash
			if( (tempData.getType() == MemoryRegion.PROGRAM_MEMORY) && 
				(flashAddress >= tempData.getStart()) &&
				(flashAddress < tempData.getEnd()) )
			{
				param[0].setIncludedInProgrammableRange(true);
				if( tempData.getStart() != 0 )
				{
					byteOffset = ( (flashAddress - tempData.getStart()) * bytesPerAddressFLASH ) + (hexAddress % bytesPerAddressFLASH);
					byte[] tempBuf = tempData.getDataBuffer();
					param[0].setByteOffset(byteOffset);
					param[0].setPCRAMBuffer(tempBuf);
				}
				else
					param[0].setPCRAMBuffer(null);
				
				param[0].setType(MemoryRegion.PROGRAM_MEMORY);
				param[0].setBytesPerAddressAndType(bytesPerAddressFLASH);
				param[0].setEndDeviceAddressofRegion(tempData.getEnd());
				
				// Check if this was the very last byte of the very last address of the region
				// We can determine this, using the below check.
				if( (flashAddress == (tempData.getEnd()-1)) && 
					((hexAddress % bytesPerAddressFLASH) == (bytesPerAddressFLASH-1)) )
					param[0].setAddressWasEndofRange(true);
				else
					param[0].setAddressWasEndofRange(false);
				return flashAddress;
			}
			
			if( (tempData.getType() == MemoryRegion.EEPROM_MEMORY) &&
				(eepromAddress >= tempData.getStart()) &&
				(eepromAddress < tempData.getEnd()) )
			{
				param[0].setIncludedInProgrammableRange(true);
				if( tempData.getStart() != 0 )
				{
					byteOffset = ((eepromAddress - tempData.getStart()) * bytesPerAddressEEPROM) + (hexAddress % bytesPerAddressEEPROM);
					byte[] tempBuf = tempData.getDataBuffer();
					param[0].setByteOffset(byteOffset);
					param[0].setPCRAMBuffer(tempBuf);
				}
				else
					param[0].setPCRAMBuffer(null);
				
				param[0].setType(MemoryRegion.EEPROM_MEMORY);
				param[0].setBytesPerAddressAndType(bytesPerAddressEEPROM);
				param[0].setEndDeviceAddressofRegion(tempData.getEnd());
				
				if( (eepromAddress == (tempData.getEnd()-1)) && 
					((eepromAddress % bytesPerAddressEEPROM) == (bytesPerAddressEEPROM-1)) )
					param[0].setAddressWasEndofRange(true);
				else
					param[0].setAddressWasEndofRange(false);
				return eepromAddress;
			}
			
			if( (tempData.getType() == MemoryRegion.CONFIG_MEMORY) && 
				(configAddress >= tempData.getStart()) &&
				(configAddress < tempData.getEnd()) )
			{
				param[0].setIncludedInProgrammableRange(true);
				if( tempData.getStart() != 0 )
				{
					byteOffset = ( (configAddress - tempData.getStart()) * bytesPerAddressConfig ) + (hexAddress % bytesPerAddressConfig);
					byte[] tempBuf = tempData.getDataBuffer();
					param[0].setByteOffset(byteOffset);
					param[0].setPCRAMBuffer(tempBuf);
				}
				else
					param[0].setPCRAMBuffer(null);
				
				param[0].setType(MemoryRegion.CONFIG_MEMORY);
				param[0].setBytesPerAddressAndType(bytesPerAddressConfig);
				param[0].setEndDeviceAddressofRegion(tempData.getEnd());
				
				if( (configAddress == (tempData.getEnd()-1)) && 
					( (configAddress % bytesPerAddressConfig) == (bytesPerAddressConfig-1) ) )
					param[0].setAddressWasEndofRange(true);
				else
					param[0].setAddressWasEndofRange(false);
				return configAddress;
			}
		}
		
		param[0].setIncludedInProgrammableRange(false);
		param[0].setAddressWasEndofRange(false);
		param[0].setPCRAMBuffer(null);
		
		return 0;
	}
	
	
}
