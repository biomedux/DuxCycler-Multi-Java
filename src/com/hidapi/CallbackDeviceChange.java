package com.hidapi;

import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;

public class CallbackDeviceChange extends Thread 
{
	private static CallbackDeviceChange instance = null;
	private HIDManager m_Manager = null;
	private DeviceChange m_Callback = null;
	
	private static int previous_counter = 0;

	private CallbackDeviceChange()
	{}
	private CallbackDeviceChange(HIDManager manager, DeviceChange callback)
	{
		m_Manager = manager;
		m_Callback = callback;
	}
	
	public static CallbackDeviceChange getInstance(HIDManager manager, DeviceChange callback)
	{
		if( instance == null )
			instance = new CallbackDeviceChange(manager, callback);
		return instance;
	}
	
	public void run()
	{
		String[] serials = new String[8];
		
		while(true)
		{
			try
			{
				HIDDeviceInfo[] devices = m_Manager.listDevices();
				
				try
				{
					Thread.sleep(100);
				}catch(Exception e)
				{
					e.printStackTrace();
				}
				
				int cnt = 0;
				
				if( devices != null )
				{
					for( HIDDeviceInfo device : devices )
					{
						if( device.getVendor_id() == DeviceConstant.VENDOR_ID && 
							 (device.getProduct_id() == DeviceConstant.PRODUCT_ID || device.getProduct_id() == DeviceConstant.PRODUCT_ID_BMX) )
						{
							serials[cnt] = device.getSerial_number();
							cnt++;
						}
					}
					
					if( previous_counter != cnt ){
						String[] tempSerial = null;
						
						if( cnt != 0 ){
							tempSerial = new String[cnt];
							System.arraycopy(serials, 0, tempSerial, 0, cnt);
						}
						
						if( previous_counter < cnt )
							m_Callback.OnMessage(DeviceChange.CONNECTED, tempSerial);
						else 
							m_Callback.OnMessage(DeviceChange.DISCONNECTED, tempSerial);
						
						previous_counter = cnt;
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}
