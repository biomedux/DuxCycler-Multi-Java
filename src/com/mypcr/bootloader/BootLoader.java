package com.mypcr.bootloader;

import java.io.IOException;

import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDManager;
import com.mypcr.bootloader.structure.BootInfo;
import com.mypcr.bootloader.structure.MemoryRegion;
import com.mypcr.bootloader.structure.Packet;

public class BootLoader implements BootLoaderChange
{
	private static BootLoader instance = null;
	
	// Erase -> Program -> Verify
	private boolean IsDebug = false;
	
	// 장치와 연결하기 위한 객체
	private HIDManager m_Manager = null;
	private HIDDevice m_HIDDevice = null;
	
	// BootLoader 객체
	private BootLoaderHex m_BootLoaderHex = null;
	private BootLoaderComm m_BootLoadComm = null;
	
	// BootLoader 연결 확인 콜백 스레드
	private CallbackBootLoaderChange m_Callback_BootLoaderChange = null;
	
	private boolean IsConnected = false;
	private boolean wasBootloaderMode = false;
	private boolean writeFlash = true;
	private boolean	writeConfig = false;
	private boolean writeEeprom = false;
	
	private DeviceData m_DeviceData;
	private DeviceData m_HexData;
	private Device m_Device;
	
	private BootLoader(){ init(); }
	
	private void init()
	{
		// Device 연결 체크용 콜백 함수 설정
		try
		{
			m_Manager = HIDManager.getInstance();
			m_BootLoadComm = BootLoaderComm.getInstance();
			m_BootLoaderHex = BootLoaderHex.getInstance();
			m_Callback_BootLoaderChange = CallbackBootLoaderChange.getInstance(m_Manager, this);
			m_Callback_BootLoaderChange.setDaemon(true);
			m_Callback_BootLoaderChange.start();
		}catch(IOException e)
		{
			e.printStackTrace();
		}
		// Device Data 초기화
		m_DeviceData = new DeviceData();
		m_HexData    = new DeviceData();
		m_Device     = new Device(m_DeviceData);
	}
	
	public static BootLoader getInstance()
	{
		if( instance == null )
			instance = new BootLoader();
		return instance;
	}
	
	public boolean IsConnected()
	{
		return IsConnected;
	}
	
	// 150506 disable update function
	/*
	public boolean LoadHexFile()
	{
		m_HexData.m_DataList.clear();
		
		if( IsDebug )
			System.out.println("Total Region by Device : " + m_DeviceData.m_DataList.size());
		
		for(int i=0; i<m_DeviceData.m_DataList.size(); i++)
		{
			DeviceData temp = m_DeviceData.m_DataList.get(i);
			byte[] buffer = new byte[temp.getDataLength()];
			for(int j=0; j<buffer.length; j++)
				buffer[j] = (byte)0xff;
			m_HexData.m_DataList.add(temp);
			
			if( IsDebug )
			System.out.println("Device Programmable Region : [" + m_DeviceData.m_DataList.get(i).getStart() + " - " +
								m_DeviceData.m_DataList.get(i).getEnd() + "]");
			
		}
		try
		{
			if( m_BootLoaderHex.ImportHexFilebyList(ServerParser.getHexData(), m_HexData, m_Device) )
				return true;
			else 
				return false;
		}catch(Exception e)
		{
			return false;
		}
	}
	*/
	
	public boolean LoadHexFile(String path)
	{
		m_HexData.m_DataList.clear();
		
		if( IsDebug )
		System.out.println("Total Region by Device : " + m_DeviceData.m_DataList.size());
		
		for(int i=0; i<m_DeviceData.m_DataList.size(); i++)
		{
			DeviceData temp = m_DeviceData.m_DataList.get(i);
			byte[] buffer = new byte[temp.getDataLength()];
			for(int j=0; j<buffer.length; j++)
				buffer[j] = (byte)0xff;
			m_HexData.m_DataList.add(temp);
			
			if( IsDebug )
			System.out.println("Device Programmable Region : [" + m_DeviceData.m_DataList.get(i).getStart() + " - " +
								m_DeviceData.m_DataList.get(i).getEnd() + "]");
			
		}
		
		if( m_BootLoaderHex.ImportHexFilebyPath(path, m_HexData, m_Device) )
			return true;
		else
			return false;
	}
	
	public void ResetDevice()
	{
		m_BootLoadComm.Reset();
	}
	
	public void GetQuery()
	{
		if( !IsConnected )
		{
			if( IsDebug )
				System.out.println("Query not sent, device not connected\n");
			return;
		}
		
		BootInfo bootInfo = new BootInfo();
		
		switch( m_BootLoadComm.ReadBootLoaderInfo(bootInfo) )
		{
			case BootLoaderComm.ERROR_FAIL:
			case BootLoaderComm.ERROR_INCORRECT_COMMAND:
				System.out.println("Unable to communicate with device");
				return;
			case BootLoaderComm.ERROR_TIMEOUT:
				System.out.println("Operation timed out");
				break;
			case BootLoaderComm.ERROR_SUCCESS:
				wasBootloaderMode = true;
				System.out.println("Device Ready");
				break;
			default:
				return;	
		}
		
		m_DeviceData.m_DataList.clear();
		
		// Now start parsing the bootInfo packet to learn more about the device. The bootInfo packet contains
		// the query response data from the USB Device. We will save these values into global variables
		// So other parts of the application can use the info when deciding how to do things.
		m_Device.setStatus(bootInfo.getDeviceFamily());
		m_Device.bytesPerPacket = bootInfo.getBytesPerPacket();
		
		// Set some processor family specific global variables that will be used else where ( ex : during program/verify operations ).
		switch( m_Device.getStatus() )
		{
			case Device.PIC_PIC18:
				m_Device.bytesPerWordFLASH = 2;
				m_Device.bytesPerAddressFLASH = 1;
				break;
			case Device.PIC_PIC24:
				m_Device.bytesPerWordFLASH = 4;
				m_Device.bytesPerAddressFLASH = 2;
				m_Device.bytesPerWordConfig = 4;
				m_Device.bytesPerAddressConfig = 2;
				break;
			case Device.PIC_PIC32:
				m_Device.bytesPerWordFLASH = 4;
				m_Device.bytesPerAddressFLASH = 1;
				break;
			default:
				break;
		}
		
		// Initialize the deviceData buffers and length variables, with the regions that the firmware claims are
		// reprogramable. We will need this information later, to devide what part(s) of the .hex file we
		// should look at/try to program into the device. Data sections in the .hex file that are not included
		// in these regions should be ignored.
		MemoryRegion[] memorys = bootInfo.getMemoryRegion();
		
		if( IsDebug )
			System.out.println("Set DeviceData from MemoryRegions...");
		
		for(int i=0; i<Packet.MAX_DATA_REGION; i++)
		{
			if( memorys[i].getType() == MemoryRegion.END_OF_TYPES_LIST )
				break;
			
			DeviceData range = new DeviceData();
			
			// Error Check : Check the firmware's reported size to make sure it is sensible. This ensures
			// we don't try to allocate ourselves a massive amount of RAM (capable of crashing this PC app)
			// if the firmware claimed an improper value.
			if( memorys[i].getSize() > MemoryRegion.MAXIMUM_PROGRAMMABLE_MEMORY_SEGMENT_SIZE )
				memorys[i].setSize(MemoryRegion.MAXIMUM_PROGRAMMABLE_MEMORY_SEGMENT_SIZE);
			
			// Parse the bootInfo response packet and allocate ourselves some RAM to hold the eventual data to program.
			if( memorys[i].getType() == MemoryRegion.PROGRAM_MEMORY )
			{
				range.setType((byte)MemoryRegion.PROGRAM_MEMORY);
				range.setDataLength( memorys[i].getSize() * m_Device.bytesPerAddressFLASH );
				byte[] databuffer = new byte[range.getDataLength()];
				for(int j=0; j<databuffer.length; j++)
					databuffer[j] = (byte)0xff;
				range.setDataBuffer(databuffer);
			}
			else if( memorys[i].getType() == MemoryRegion.EEPROM_MEMORY )
			{
				range.setType((byte)MemoryRegion.EEPROM_MEMORY);
				range.setDataLength( memorys[i].getSize() * m_Device.bytesPerAddressEEPROM );
				byte[] databuffer = new byte[range.getDataLength()];
				for(int j=0; j<databuffer.length; j++)
					databuffer[j] = (byte)0xff;
				range.setDataBuffer(databuffer);
			}
			else if( memorys[i].getType() == MemoryRegion.CONFIG_MEMORY )
			{
				range.setType((byte)MemoryRegion.CONFIG_MEMORY);
				range.setDataLength( memorys[i].getSize() * m_Device.bytesPerAddressConfig );
				byte[] databuffer = new byte[range.getDataLength()];
				for(int j=0; j<databuffer.length; j++)
					databuffer[j] = (byte)0xff;
				range.setDataBuffer(databuffer);
			}
			
			range.setStart( memorys[i].getAddress() );
			range.setEnd( memorys[i].getAddress() + memorys[i].getSize() );
			
			m_DeviceData.m_DataList.add(range);
		}
	}
	
	public boolean EraseDevice()
	{
		int commStatus;
		BootInfo bootinfo;
		
		if( writeFlash | writeEeprom )
		{
			if( IsDebug )
			System.out.println("Erase Device..");
			
			commStatus = m_BootLoadComm.Erase();
			
			if( commStatus != BootLoaderComm.ERROR_SUCCESS )
			{
				if( IsDebug )
				System.out.println("Erase is failed!");
				return false;
			}
			
			bootinfo = new BootInfo();
			
			if( IsDebug )
				System.out.println("Reading BootLoaderInfo...");
			
			commStatus = m_BootLoadComm.ReadBootLoaderInfo(bootinfo);
			
			if( commStatus != BootLoaderComm.ERROR_SUCCESS )
			{
				if( IsDebug )
					System.out.println("Reading BootLoaderInfo is failed!");
				return false;
			}
		}
		return true;
	}
	
	public boolean ProgramDevice()
	{
		int comStatus;
		
		// First erase the entire device
		if( !EraseDevice() )
			return false;
		
		// Now being re-programming each section based on the info we obtained when
		// we parsed the user's .hex file
		for(int i=0; i<m_HexData.m_DataList.size(); i++)
		{
			DeviceData tempDevice = m_HexData.m_DataList.get(i);
			if( writeFlash && (tempDevice.getType() == MemoryRegion.PROGRAM_MEMORY ) )
			{
				comStatus = m_BootLoadComm.Program(	tempDevice.getStart(), 
													m_Device.bytesPerPacket,
													m_Device.bytesPerAddressFLASH,
													m_Device.bytesPerWordFLASH,
													m_Device.getStatus(),
													tempDevice.getEnd(),
													tempDevice.getDataBuffer());
			}
			else if(writeEeprom && (tempDevice.getType() == MemoryRegion.EEPROM_MEMORY ))
			{
				comStatus = m_BootLoadComm.Program(	tempDevice.getStart(), 
													m_Device.bytesPerPacket,
													m_Device.bytesPerAddressEEPROM,
													m_Device.bytesPerWordEEPROM,
													m_Device.getStatus(),
													tempDevice.getEnd(),
													tempDevice.getDataBuffer());
			}
			else if(writeConfig && (tempDevice.getType() == MemoryRegion.CONFIG_MEMORY ))
			{
				comStatus = m_BootLoadComm.Program(	tempDevice.getStart(), 
													m_Device.bytesPerPacket,
													m_Device.bytesPerAddressConfig,
													m_Device.bytesPerWordConfig,
													m_Device.getStatus(),
													tempDevice.getEnd(),
													tempDevice.getDataBuffer());
			}
			else 
				continue;
			
			if( comStatus != BootLoaderComm.ERROR_SUCCESS )
			{
				if( IsDebug )
					System.out.println("Programing Failed");
				return false;
			}
		}
		
		VerifyDevice();
		
		return true;
	}
	
	public void VerifyDevice()
	{
		int commStatus;
		
		for(int i=0; i<m_DeviceData.m_DataList.size(); i++)
		{
			DeviceData tempData = m_DeviceData.m_DataList.get(i);
			if( writeFlash && (tempData.getType() == MemoryRegion.PROGRAM_MEMORY) )
			{
				if( IsDebug )
				System.out.println("Verifying Device's Program Memory...");
				
				commStatus = m_BootLoadComm.GetData(tempData.getStart(), 
													m_Device.bytesPerPacket,
													m_Device.bytesPerAddressFLASH,
													m_Device.bytesPerWordFLASH,
													tempData.getEnd(),
													tempData.getDataBuffer());
				
				if( commStatus != BootLoaderComm.ERROR_SUCCESS )
				{
					if( IsDebug )
					System.out.println("Error Reading Device..");
					return;
				}
				
				for(int j=0; j<m_HexData.m_DataList.size(); j++)
				{
					DeviceData hexData = m_HexData.m_DataList.get(j);
					
					if( tempData.getStart() == hexData.getStart() )
					{
						for(int k=tempData.getStart(); k<tempData.getEnd(); k++)
						{
							for(int l=0; l<m_Device.bytesPerAddressFLASH; l++)
							{
								byte[] tempbuf = tempData.getDataBuffer();
								byte[] tempbuf2 = hexData.getDataBuffer();
								if( tempbuf[ ((k - tempData.getStart()) * m_Device.bytesPerAddressFLASH)+l ] != 
									tempbuf2[ ((k - tempData.getStart()) * m_Device.bytesPerAddressFLASH)+l ] )
								{
									if( (m_Device.getStatus() == Device.PIC_PIC24) && ((k%2) == 1) && (l == 1) )
									{
										if( IsDebug )
											System.out.println("Not a real verify failure");
									}
									else
									{
										if( m_Device.getStatus() == Device.PIC_PIC24 )
											System.out.println("Famliy 24...");
										else
											System.out.println("Famliy 24.. else");
										System.out.println("Failed verify at address 0x" + k);
										return;
									}
								}
							}
						}
					}
				}
				if( IsDebug )
					System.out.println("Verifying.. Success");
			}
			//else if( writeEeprom && )
		}
		
		if( IsDebug )
		{
			System.out.println("Erase/Program/Verify Completed Successfully");
			System.out.println("Please Reset the Device");
		}
	}

	@Override
	public void OnBootLoaderChange(int MessageType, Object data) 
	{
		switch(MessageType)
		{
			case CONNECTED:
				String count = (String)data;
				if( count.equals("1") )
				{
					try
					{
						if( m_HIDDevice != null )
						{
							m_HIDDevice.close();
							m_HIDDevice = null;
						}
						m_HIDDevice = m_Manager.openById(BootLoaderConstant.VENDOR_ID, BootLoaderConstant.PRODUCT_ID, null);
						if( m_Device != null )
						{
							IsConnected = true;
							m_HIDDevice.disableBlocking();
							m_BootLoadComm.SetDevice(m_HIDDevice);
							GetQuery();
						}
					}catch(IOException e)
					{
						// ignore
					}
				}
				break;
			case DISCONNECTED:
				IsConnected = false;
				break;
		}
	}
}





