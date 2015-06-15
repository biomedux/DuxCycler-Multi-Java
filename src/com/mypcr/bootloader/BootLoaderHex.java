package com.mypcr.bootloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.mypcr.bootloader.param.GetDeviceAddressParam;
import com.mypcr.bootloader.structure.MemoryRegion;

public class BootLoaderHex 
{
	private static BootLoaderHex instance = null;
	
	private boolean IsDebug = false;
	
	public static final int ERROR_SUCCRESS						=	0x00;
	public static final int ERROR_COULD_NOT_OPEN_FILE			=	0x01;
	public static final int ERROR_NONE_IN_RANGE					=	0x02;
	public static final int ERROR_ERROR_IN_HEX_FILE				=	0x03;
	public static final int ERROR_INSUFFICIENT_MEMORY			=	0x04;
	
	public static final int HEX32_RECORD_DATA					=	0x00;
	public static final int HEX32_RECORD_END_OF_FILE			=	0x01;
	public static final int HEX32_RECORD_EXTENDED_SEGMENT_ADDR	=	0x02;
	public static final int HEX32_RECORD_EXTENDED_LINEAR_ADDR	=	0x04;
	
	private boolean hasConfigBits = false;
	
	private BootLoaderHex(){ init(); }
	private void init()
	{
		
	}
	
	public static BootLoaderHex getInstance()
	{
		if( instance == null )
			instance = new BootLoaderHex();
		return instance;
	}
	
	public boolean ImportHexFilebyPath(String path, DeviceData data, Device device)
	{
		BufferedReader in = null;
		int segmentAddress = 0;
		int byteCount;
		int lineAddress;
		int deviceAddress;
		int recordType;
		
		String hexByte;
		int wordByte;
		
		boolean importedAtLeastOneByte = false;
		
		try
		{
			in = new BufferedReader(new FileReader(new File(path)));
		}catch(IOException e)
		{
			return false;
		}
		
		String readData = null;
		
		// Parse the entire hex file, line by line!
		while(true)
		{
			try
			{
				// Fetch a line of ASCII text from the .hex file
				readData = in.readLine();
				// if this data is file end.
				if( readData == null )
					break;
			}catch(IOException e)
			{
				break;
			}
			
			// Do some error checking on the .hex file contents, to make sure the file
			// is formatted like a legitimate Intel 32-bit formatted .hex file
			if( (readData.charAt(0) != ':') || (readData.length() < 11) )
			{
				try
				{
					if( in != null )
						in.close();
				}catch(IOException e)
				{
					e.printStackTrace();
				}
				return false;
			}
			
			byteCount = Integer.parseInt(readData.substring(1, 3), 16);
			lineAddress = segmentAddress + Integer.parseInt(readData.substring(3, 7), 16);
			recordType = Integer.parseInt(readData.substring(7, 9), 16);
			
			// Error check : Verify checksum byte at the end of the .hex file line
			// is valid. Note, this is not the same checksum as MPLAB(R) IDE 
			// uses/computes for the entire hex file.
			// This is only the mini-checksum at the end of each line in the .hex file.
			int hexLineChecksum = 0;
			int i;
			for(i=0; i<(byteCount+4); i++)	// +4 correction is for byte count, 16-bit address, and record type bytes
			{
				hexByte = readData.substring(1 + (2 * i), 1 + (2 * i) + 2);	// Fetch two adjacent ASCII bytes from the .hex file
				wordByte = Integer.parseInt(hexByte, 16);	// Re-format the above two ASCII bytes into a single binary encoded byte (0x00-0xff)
				// Add the newly fetched byte to the running checksum
				hexLineChecksum += wordByte;
			}
			// Now get the two's complements of the hexLineChecksum.
			hexLineChecksum = 0 - hexLineChecksum;
			hexLineChecksum &= 0xff;	// Truncate to a single byte. we now have out computed checksum./ this should match the .hex file.
			// Fetch checksum byte from the .hex file
			hexByte = readData.substring(1 + (2 * i), 1 + (2 * i) + 2);		// Fetch the two ASCII bytes that correspond to the checksum byte
			wordByte = Integer.parseInt(hexByte, 16);	// Re-format the above two ASCII bytes into a single binary encoded byte (0x00-0xff)
			wordByte &= 0xff;
			// Now check if the checksum we computed matches the ona at the end of the line in the hex file.
			if( hexLineChecksum != wordByte )
			{
				// Checksum in the hex file doesn't match the line contents. this implies a corrupted hex file.
				// if an error is detected in the hex file formatting, the safest approach is to abort the operation
				// and force the user to supply a properly formatted hex file
				try
				{
					if( in != null )
						in.close();
				}catch(IOException e)
				{
					e.printStackTrace();
				}
				
				return false;
			}
			
			// Check the record type of the hex line, to determine how to continue parsing the data.
			if( recordType == HEX32_RECORD_END_OF_FILE )// end of file record
			{
				break;
			}
			else if((recordType == HEX32_RECORD_EXTENDED_SEGMENT_ADDR) || (recordType == HEX32_RECORD_EXTENDED_LINEAR_ADDR))	// Segment address
			{
				// ErrorCheck : Make sure the line contains the correct number of bytes for the specified record type
				if( readData.length() >= ( 11 + (2 * byteCount)) )
				{
					// Fetch the payload, which is the upper 4 or 16-bits of the 20-bit or 32-bit hex file address
					segmentAddress = Integer.parseInt(readData.substring(9, 13), 16);
					
					// Load the upper bits of the address
					if( recordType == HEX32_RECORD_EXTENDED_SEGMENT_ADDR )
						segmentAddress <<= 4;
					else
						segmentAddress <<= 16;
					
					// Update the line address, now that we know the upper bits are something new
					lineAddress = segmentAddress + Integer.parseInt(readData.substring(3, 7), 16);
				}
				else
				{
					// Length appears to be wrong in hex line entry.
					// If an error is detected in the hex file formatting, the safest approach is to
					// abort the operation and force the user to supply a properly formatted hex file.
					try
					{
						if( in != null )
							in.close();
					}catch(IOException e)
					{
						e.printStackTrace();
					}
					
					return false;
				}
			}
			else if( recordType == HEX32_RECORD_DATA )	// Data Record
			{
				// Error check to make sure line is long enough to be consistent with the specified record type
				if( readData.length() < (11 + (2 * byteCount)) )
				{
					// If an error is detected in the hex file formatting, the safest approach is to
					// abort the operation and force the user to supply a proper hex file
					try
					{
						if( in != null )
							in.close();
					}catch(IOException e)
					{
						e.printStackTrace();
					}
					
					return false;
				}
				
				// For each data payload byte we find in the hex file line, check if it is contained within
				// a progarmmable region inside the microcontroller. If so save it. If not, discard it
				
				for(i=0; i<byteCount; i++)
				{
					// Use the hex file linear byte address, to compute other information about the
					// byte/location. The GetDeviceAddressFromHexAddress() function gives us a pointer to
					// the PC RAM buffer byte that will get programmed into the microcontroller, which corresponds
					// to the specified .hex file extended address.
					// The function also returns a boolean letting us know if the address is part of a programmable memory region on the device
					
					// For Java Pointer..
					GetDeviceAddressParam[] param = new GetDeviceAddressParam[1];
					param[0] = new GetDeviceAddressParam();
					
					deviceAddress = device.GetDeviceAddressFromHexAddress(lineAddress + i, data, param);
					
					//Check if the just parsed hex byte was included in one of the microcontroller reported programmable memory regions.
					// if so, save the byte into the proper location in the PC RAM buffer, so it can be programmed later.
					if( param[0].getIncludedInProgrammableRange() && param[0].getPCRAMBuffer() != null )
					{
						if( i == 0 )
						{
							if( IsDebug )
								System.out.println("Importing .hex file line with device address : " + deviceAddress);
						}
						
						// Fetch ASCII encoded payload byte from .hex file and save the byte to out temporary RAM buffer.
						hexByte = readData.substring(9 + (2 * i), 9 + (2 * i)+2);	// Fetch two ASCII data payload bytes from the .hex file
						wordByte = Integer.parseInt(hexByte, 16);	// Re-format the above two ASCII bytes into a single binary encoded byte (0x00-0xff)
						
						byte[] tempbuf = param[0].getPCRAMBuffer();	
						tempbuf[param[0].getByteOffset()] = (byte)wordByte;	// Save the .hex file data byte into the PC RAM buffer that holds the data to be programmed.
						importedAtLeastOneByte = true;	// Set Flag so we know we imported something successfully
						
						// Check if we just parsed a config bit byte. If so, set flag so the user is no longer locked out
						// of programming the config bits section.
						if( param[0].getType() == MemoryRegion.CONFIG_MEMORY )
							hasConfigBits = true;
					}
					else if( param[0].getIncludedInProgrammableRange() && param[0].getPCRAMBuffer() != null )
					{
						// Previous memory allocation must have failed, or otherwise pPCRAMBuffer would not be = 0.
						// Since the memory allocation failed, we should bug out and let the user know)
						try
						{
							if( in != null )
								in.close();
						}catch(IOException e)
						{
							e.printStackTrace();
						}
						return false;
					}
				}
			}
		}
		
		try
		{
			if( in != null )
				in.close();
		}catch(IOException e)
		{
			e.printStackTrace();
		}
		
		// Check if we imported any data from the .hex file
		if( importedAtLeastOneByte )
		{
			if( IsDebug )
				System.out.println("Hex file imported successfully");
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean ImportHexFilebyList(ArrayList<String> list, DeviceData data, Device device)
	{
		int segmentAddress = 0;
		int byteCount;
		int lineAddress;
		int deviceAddress;
		int recordType;
		
		String hexByte;
		int wordByte;
		
		boolean importedAtLeastOneByte = false;
		
		String readData = null;
		
		int index = 0;
		int size = list.size();
		
		// Parse the entire hex file, line by line!
		while(size > index)
		{
			// Fetch a line of ASCII text from the list
			readData = list.get(index++);
			
			// Do some error checking on the .hex file contents, to make sure the file
			// is formatted like a legitimate Intel 32-bit formatted .hex file
			if( (readData.charAt(0) != ':') || (readData.length() < 11) )
			{
				return false;
			}
			
			byteCount = Integer.parseInt(readData.substring(1, 3), 16);
			lineAddress = segmentAddress + Integer.parseInt(readData.substring(3, 7), 16);
			recordType = Integer.parseInt(readData.substring(7, 9), 16);
			
			// Error check : Verify checksum byte at the end of the .hex file line
			// is valid. Note, this is not the same checksum as MPLAB(R) IDE 
			// uses/computes for the entire hex file.
			// This is only the mini-checksum at the end of each line in the .hex file.
			int hexLineChecksum = 0;
			int i;
			for(i=0; i<(byteCount+4); i++)	// +4 correction is for byte count, 16-bit address, and record type bytes
			{
				hexByte = readData.substring(1 + (2 * i), 1 + (2 * i) + 2);	// Fetch two adjacent ASCII bytes from the .hex file
				wordByte = Integer.parseInt(hexByte, 16);	// Re-format the above two ASCII bytes into a single binary encoded byte (0x00-0xff)
				// Add the newly fetched byte to the running checksum
				hexLineChecksum += wordByte;
			}
			// Now get the two's complements of the hexLineChecksum.
			hexLineChecksum = 0 - hexLineChecksum;
			hexLineChecksum &= 0xff;	// Truncate to a single byte. we now have out computed checksum./ this should match the .hex file.
			// Fetch checksum byte from the .hex file
			hexByte = readData.substring(1 + (2 * i), 1 + (2 * i) + 2);		// Fetch the two ASCII bytes that correspond to the checksum byte
			wordByte = Integer.parseInt(hexByte, 16);	// Re-format the above two ASCII bytes into a single binary encoded byte (0x00-0xff)
			wordByte &= 0xff;
			// Now check if the checksum we computed matches the ona at the end of the line in the hex file.
			if( hexLineChecksum != wordByte )
			{
				// Checksum in the hex file doesn't match the line contents. this implies a corrupted hex file.
				// if an error is detected in the hex file formatting, the safest approach is to abort the operation
				// and force the user to supply a properly formatted hex file
				
				return false;
			}
			
			// Check the record type of the hex line, to determine how to continue parsing the data.
			if( recordType == HEX32_RECORD_END_OF_FILE )// end of file record
			{
				break;
			}
			else if((recordType == HEX32_RECORD_EXTENDED_SEGMENT_ADDR) || (recordType == HEX32_RECORD_EXTENDED_LINEAR_ADDR))	// Segment address
			{
				// ErrorCheck : Make sure the line contains the correct number of bytes for the specified record type
				if( readData.length() >= ( 11 + (2 * byteCount)) )
				{
					// Fetch the payload, which is the upper 4 or 16-bits of the 20-bit or 32-bit hex file address
					segmentAddress = Integer.parseInt(readData.substring(9, 13), 16);
					
					// Load the upper bits of the address
					if( recordType == HEX32_RECORD_EXTENDED_SEGMENT_ADDR )
						segmentAddress <<= 4;
					else
						segmentAddress <<= 16;
					
					// Update the line address, now that we know the upper bits are something new
					lineAddress = segmentAddress + Integer.parseInt(readData.substring(3, 7), 16);
				}
				else
				{
					// Length appears to be wrong in hex line entry.
					// If an error is detected in the hex file formatting, the safest approach is to
					// abort the operation and force the user to supply a properly formatted hex file.
					
					return false;
				}
			}
			else if( recordType == HEX32_RECORD_DATA )	// Data Record
			{
				// Error check to make sure line is long enough to be consistent with the specified record type
				if( readData.length() < (11 + (2 * byteCount)) )
				{
					// If an error is detected in the hex file formatting, the safest approach is to
					// abort the operation and force the user to supply a proper hex file
					
					return false;
				}
				
				// For each data payload byte we find in the hex file line, check if it is contained within
				// a progarmmable region inside the microcontroller. If so save it. If not, discard it
				
				for(i=0; i<byteCount; i++)
				{
					// Use the hex file linear byte address, to compute other information about the
					// byte/location. The GetDeviceAddressFromHexAddress() function gives us a pointer to
					// the PC RAM buffer byte that will get programmed into the microcontroller, which corresponds
					// to the specified .hex file extended address.
					// The function also returns a boolean letting us know if the address is part of a programmable memory region on the device
					
					// For Java Pointer..
					GetDeviceAddressParam[] param = new GetDeviceAddressParam[1];
					param[0] = new GetDeviceAddressParam();
					
					deviceAddress = device.GetDeviceAddressFromHexAddress(lineAddress + i, data, param);
					
					//Check if the just parsed hex byte was included in one of the microcontroller reported programmable memory regions.
					// if so, save the byte into the proper location in the PC RAM buffer, so it can be programmed later.
					if( param[0].getIncludedInProgrammableRange() && param[0].getPCRAMBuffer() != null )
					{
						if( i == 0 )
						{
							if( IsDebug )
								System.out.println("Importing .hex file line with device address : " + deviceAddress);
						}
						
						// Fetch ASCII encoded payload byte from .hex file and save the byte to out temporary RAM buffer.
						hexByte = readData.substring(9 + (2 * i), 9 + (2 * i)+2);	// Fetch two ASCII data payload bytes from the .hex file
						wordByte = Integer.parseInt(hexByte, 16);	// Re-format the above two ASCII bytes into a single binary encoded byte (0x00-0xff)
						
						byte[] tempbuf = param[0].getPCRAMBuffer();	
						tempbuf[param[0].getByteOffset()] = (byte)wordByte;	// Save the .hex file data byte into the PC RAM buffer that holds the data to be programmed.
						importedAtLeastOneByte = true;	// Set Flag so we know we imported something successfully
						
						// Check if we just parsed a config bit byte. If so, set flag so the user is no longer locked out
						// of programming the config bits section.
						if( param[0].getType() == MemoryRegion.CONFIG_MEMORY )
							hasConfigBits = true;
					}
					else if( param[0].getIncludedInProgrammableRange() && param[0].getPCRAMBuffer() != null )
					{
						// Previous memory allocation must have failed, or otherwise pPCRAMBuffer would not be = 0.
						// Since the memory allocation failed, we should bug out and let the user know)
						return false;
					}
				}
			}
		}
		
		// Check if we imported any data from the .hex file
		if( importedAtLeastOneByte )
		{
			if( IsDebug )
				System.out.println("Hex file imported successfully");
			return true;
		}
		else
		{
			return false;
		}
	}
}
