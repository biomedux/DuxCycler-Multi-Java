package com.mypcr.bootloader;

import java.io.IOException;

import com.codeminders.hidapi.HIDDevice;
import com.mypcr.bootloader.structure.BootInfo;
import com.mypcr.bootloader.structure.ReadPacket;
import com.mypcr.bootloader.structure.UnLockConfig;
import com.mypcr.bootloader.structure.WritePacket;

public class BootLoaderComm 
{
	private static BootLoaderComm instance = null;
	
	private boolean IsDebug = false;
	
	public static final int QUERY_DEVICE					=	0x02;
	public static final int UNLOCK_CONFIG					=	0x03;
	public static final int ERASE_DEVICE					=	0x04;
	public static final int PROGRAM_DEVICE					=	0x05;
	public static final int PROGRAM_COMPLETE				=	0x06;
	public static final int GET_DATA						=	0x07;
	public static final int RESET_DEVICE					=	0x08;
	
	public static final int ERROR_SUCCESS 					=	0x00;
	public static final int ERROR_NOT_CONNECTED				=	0x01;
	public static final int ERROR_FAIL	 					=	0x02;
	public static final int ERROR_INCORRECT_COMMAND 		=	0x03;
	public static final int ERROR_TIMEOUT 					=	0x04;
	public static final int ERROR_OTHER 					=	0xff;
	
	public static final int SYNC_WAIT_TIME					=	40000;
	public static final int MAX_DATA_REGIONS				=	0x06;
	
	// HID ¿¬°á °´Ã¼
	private HIDDevice m_Device = null;
	
	private BootLoaderComm(){ init(); }
	
	private void init()
	{
		
	}
	
	public static BootLoaderComm getInstance()
	{
		if( instance == null )
			instance = new BootLoaderComm();
		return instance;
	}
	
	public void SetDevice(HIDDevice device)
	{
		m_Device = device;
	}
	
	public void Reset()
	{
		byte[] sendPacket = new byte[65];
		int status;
		
		if( BootLoader.getInstance().IsConnected() )
		{
			sendPacket[1] = RESET_DEVICE;
			
			status = SendPacket(sendPacket);
			
			if( IsDebug )
			{
				if( status == ERROR_SUCCESS )
					System.out.println("Reset Success");
				else
					System.out.println("Reset Error!");
			}
		}
	}
	
	public int GetData(	long address, byte bytesPerPacket, byte bytesPerAddress,
						byte bytesPerWord, long endAddress, byte[] data)
	{
		ReadPacket readPacket;
		WritePacket writePacket;
		int status;
		int index = 0;
		
		long addressToFetch = endAddress - address;
		
		if( addressToFetch == 0 )
			addressToFetch++;
		
		if( BootLoader.getInstance().IsConnected() )
		{
			if( (data == null) || (endAddress < address) || (bytesPerPacket == 0))
			{
				if( IsDebug )
					System.out.println("Error, GetData is failed!");
				return ERROR_FAIL;
			}
			
			if( IsDebug )
				System.out.println("Starting getData..");
			
			while( address < endAddress )
			{
				writePacket = new WritePacket();
				writePacket.setCommand((byte)GET_DATA);
				writePacket.setAddress((int)address);
				
				if( (addressToFetch * bytesPerAddress) < bytesPerPacket )
					writePacket.setBytesPerPacket((byte)(addressToFetch * bytesPerAddress));
				else
					writePacket.setBytesPerPacket((byte)bytesPerPacket);
				
				status = SendPacket(writePacket.toByte());
				
				if( status != ERROR_SUCCESS )
					return status;
				
				readPacket = new ReadPacket();
				status = ReceivePacket(readPacket.toByte());
				
				if( status != ERROR_SUCCESS )
					return status;
				
				for(int i=0; i<readPacket.getBytesPerPacket(); i++)
				{
					byte[] temp = readPacket.getData();
					data[index + i] = temp[58 - readPacket.getBytesPerPacket() + i];
				}
				
				index += readPacket.getBytesPerPacket();
				
				address += readPacket.getBytesPerPacket() / bytesPerAddress;
				
				long max = endAddress - address;
				if( max <= readPacket.getBytesPerPacket() )
				{
					if( IsDebug )
					{
						System.out.println("Address : " + address);	
						System.out.println("EndAddress : " + endAddress);
						System.out.println("Max : " + max);
					}
					for(int i=0; i<max; i++)
					{
						byte[] temp = readPacket.getData();
						data[index + i] = temp[58 - readPacket.getBytesPerPacket() + i];
					}
					break;
				}
			}
			
			return ERROR_SUCCESS;
		}
		
		return ERROR_NOT_CONNECTED;
	}
	
	public int Program( long address, byte bytesPerPacket, byte bytesPerAddress, 
						byte bytesPerWord, byte deviceFamily, long endAddress, byte[] data)
	{
		WritePacket writePacket;
		int status = ERROR_SUCCESS;
		boolean allPaayloadBytesFF;
		boolean firstAllFFPacketFound = false;
		int bytesToSend;
		byte lastCommandSend = PROGRAM_DEVICE;
		int addressesToProgram;
		int startOfDataPayloadIndex;
		
		// Error check input parameters before using them
		if( (data == null) || (bytesPerAddress == 0) || (address > endAddress) || (bytesPerWord == 0) )
		{
			if( IsDebug )
				System.out.println("Program function is failed..");
			return ERROR_FAIL;
		}
		
		// Error check to make sure the requested maximum data payload size is an exact multiple of the bytesPerAddress.
		// If not, shrink the number of bytes we actually send, so that it is always an exact multiple of the
		// programmable media bytesPerAddress. This ensures that we don't "half" program any memory address (ex: if each
		// flash address is a 16-bit word address, we don't want to only program one byte of the address, we want to program
		// both bytes.
		while( (bytesPerPacket % bytesPerWord) != 0 )
		{
			bytesPerPacket--;
		//	debug.Print("bytesPerPacket : " + bytesPerPacket);
		}
		
		addressesToProgram = (int)(endAddress - address);
		if( addressesToProgram == 0 )
			addressesToProgram++;
		
		if( BootLoader.getInstance().IsConnected() )
		{
			while(address < endAddress)
			{
				writePacket = new WritePacket();
				writePacket.setCommand((byte)PROGRAM_DEVICE);
				writePacket.setAddress((int)address);
				
				byte[] packet_data = writePacket.getData();
				
				if( ((endAddress - address) * bytesPerAddress) < bytesPerPacket )
				{
					writePacket.setBytesPerPacket((byte)((endAddress-address) * bytesPerAddress));
					for(int i=0; i<writePacket.getBytesPerPacket(); i++)
						packet_data[i + 58 - writePacket.getBytesPerPacket()] = data[i];
					writePacket.setData(packet_data);
					
					while( (writePacket.getBytesPerPacket() % bytesPerWord) != 0)
					{
						if( writePacket.getBytesPerPacket() >= bytesPerPacket )
							break;
						
						for(int i=0; i<bytesPerPacket-1; i++)
							packet_data[i] = packet_data[i+1];
						
						packet_data[writePacket.getBytesPerPacket()] = (byte)0xff;
						writePacket.setBytesPerPacket( (byte)(writePacket.getBytesPerPacket()+1) );
						writePacket.setData(packet_data);
					}
					
					bytesToSend = writePacket.getBytesPerPacket();
					if( IsDebug )
						System.out.println("Preparing short packet of final program data with payload : " + writePacket.getBytesPerPacket());
				}
				else
				{
					writePacket.setBytesPerPacket( bytesPerPacket );
					bytesToSend = bytesPerPacket;
					for(int i=0; i<writePacket.getBytesPerPacket(); i++)
						packet_data[i + 58 - writePacket.getBytesPerPacket()] = data[i];
					writePacket.setData(packet_data);
				}
				
				allPaayloadBytesFF = true;
				
				startOfDataPayloadIndex = 58 - writePacket.getBytesPerPacket();
				
				for(int i=startOfDataPayloadIndex; i<(startOfDataPayloadIndex + writePacket.getBytesPerPacket()); i++)
				{
					byte[] tempdata = writePacket.getData();
					if( tempdata[i] != 0xff )
					{
						if( (((i - startOfDataPayloadIndex) % bytesPerWord) == 3) && (deviceFamily == Device.PIC_PIC24) )
						{
							//
						}
						else
						{
							allPaayloadBytesFF = false;
							break;
						}
					}
				}
				
				if( !allPaayloadBytesFF )
				{
					if( IsDebug )
						System.out.println("Send program data packet with address1..");
					
					status = SendPacket(writePacket.toByte());
					
					if( status != ERROR_SUCCESS )
					{
						if( IsDebug )
							System.out.println("Error during program sending packet with address1..");
						return status;
					}
					firstAllFFPacketFound = true;
					lastCommandSend = PROGRAM_DEVICE;
				}
				else if( allPaayloadBytesFF && firstAllFFPacketFound )
				{
					writePacket.setCommand((byte)PROGRAM_COMPLETE);
					writePacket.setBytesPerPacket((byte)0);
					firstAllFFPacketFound = false;
					if( IsDebug )
						System.out.println("Send program data packet with address2..");
					
					status = SendPacket(writePacket.toByte());
					
					if( status != ERROR_SUCCESS )
					{
						if( IsDebug )
							System.out.println("Error during program sending packet with address2..");
						return status;
					}
					lastCommandSend = PROGRAM_COMPLETE;
				}
				else
				{
					if( IsDebug )
						System.out.println("Skipping data packet with all 0xff with address..");
				}
				
				address += bytesPerPacket / bytesPerAddress;
				byte[] tempbuf = new byte[data.length - bytesToSend];
				for(int i=0; i<tempbuf.length; i++)
					tempbuf[i] = data[i+bytesToSend];
				data = tempbuf;
				
				if( address >= endAddress )
				{
					if( lastCommandSend == PROGRAM_COMPLETE )
						break;
					
					writePacket = new WritePacket();
					writePacket.setCommand((byte)PROGRAM_COMPLETE);
					writePacket.setBytesPerPacket((byte)0);
					if( IsDebug )
						System.out.println("Sending final program complete command for this region.");
					
					status = SendPacket(writePacket.toByte());
					break;
				}
			}
			
			return status;
		}
		else
		{
			return ERROR_NOT_CONNECTED;
		}
	}
	
	public int Erase()
	{
		byte[] sendPacket = new byte[65];
		int status;
		
		if( BootLoader.getInstance().IsConnected() )
		{
			if( IsDebug )
				System.out.println("Sending Erase Packet..");
			
			sendPacket[1] = ERASE_DEVICE;
			
			status = SendPacket(sendPacket);
			
			if( IsDebug )
			{
				if( status == ERROR_SUCCESS )
					System.out.println("Successfully sent erase command.");
				else
					System.out.println("Erasing Device Failed");
			}
			return status;
		}
		
		if( IsDebug )
			System.out.println("Device Not connected");
		
		return ERROR_NOT_CONNECTED;
	}
	
	public int LockUnlockConfig(boolean lock)
	{
		UnLockConfig unlockPacket;
		int status;
		
		if( BootLoader.getInstance().IsConnected() )
		{
			unlockPacket = new UnLockConfig();
			unlockPacket.setCommand((byte)UNLOCK_CONFIG);
			if( lock )
				unlockPacket.setSetting((byte)0x01);
			else
				unlockPacket.setSetting((byte)0x00);
			
			status = SendPacket(unlockPacket.toByte());
			
			return status;
		}
		
		return ERROR_NOT_CONNECTED;
	}
	
	public int ReadBootLoaderInfo(BootInfo boot)
	{
		if( BootLoader.getInstance().IsConnected() )
		{
			byte[] sendPacket = new byte[65];
			sendPacket[1] = QUERY_DEVICE;
			
			if( IsDebug )
				System.out.println("Reading BootLoaderInfo...");
			
			SendPacket(sendPacket);
			
			ReceivePacket(boot.toByte());
			
			if( boot.getCommand() != 0x02 )
			{
				System.out.println("Incorrect Command..");
				return ERROR_INCORRECT_COMMAND;
			}
			else
			{
				System.out.println("Reading BootLoaderInfo is Successfully");
			}
			
			return ERROR_SUCCESS;
		}
		
		return ERROR_NOT_CONNECTED;
	}
	
	public int SendPacket(byte[] data)
	{
		int res = 0;
		
		while( res < 1 )
		{
			try
			{
				res = m_Device.write(data);
			}catch(IOException e)
			{
				return ERROR_FAIL;
			}
			
			if( res == -1 )
			{
				return ERROR_FAIL;
			}
		}
		
		return ERROR_SUCCESS;
	}
	
	public int ReceivePacket(byte[] data)
	{
		int res = 0;
		
		while( res < 1 )
		{
			try
			{
				res = m_Device.read(data);
			}catch(IOException e)
			{
				return ERROR_FAIL;
			}
		}
		
		return ERROR_SUCCESS;
	}
}