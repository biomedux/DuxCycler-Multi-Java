package com.mypcr.bootloader;

import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;
import com.mypcr.bootloader.BootLoaderConstant;

public class CallbackBootLoaderChange extends Thread 
{
	private static CallbackBootLoaderChange instance = null;
	private HIDManager m_Manager = null;
	private BootLoaderChange m_Callback = null;
	
	private boolean Callback_Flag = false;
	private boolean while_Flag = true;

	private CallbackBootLoaderChange()
	{}
	private CallbackBootLoaderChange(HIDManager manager, BootLoaderChange callback)
	{
		m_Manager = manager;
		m_Callback = callback;
	}
	
	public static CallbackBootLoaderChange getInstance(HIDManager manager, BootLoaderChange callback)
	{
		if( instance == null )
			instance = new CallbackBootLoaderChange(manager, callback);
		return instance;
	}
	
	public void unregistCallback()
	{
		while_Flag = false;
	}
	
	public void run()
	{
		while(while_Flag)
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
						if( device.getVendor_id() == BootLoaderConstant.VENDOR_ID && 
								device.getProduct_id() == BootLoaderConstant.PRODUCT_ID )
						{
							cnt++;
						}
					}
					if( cnt > 0 )
					{	
						if( !Callback_Flag )
						{
							Callback_Flag = true;
							m_Callback.OnBootLoaderChange(BootLoaderChange.CONNECTED, (cnt + ""));
						}
					}
					else
					{
						if( Callback_Flag )
						{
							Callback_Flag = false;
							m_Callback.OnBootLoaderChange(BootLoaderChange.DISCONNECTED, null);
						}
					}
				}
				else
				{
					if( Callback_Flag )
					{
						Callback_Flag = false;
						m_Callback.OnBootLoaderChange(BootLoaderChange.DISCONNECTED, null);
					}
				}
			}catch(Exception e)
			{
				if( Callback_Flag )
				{
					Callback_Flag = false;
					m_Callback.OnBootLoaderChange(BootLoaderChange.DISCONNECTED, null);
				}
			}
		}
	}
}
