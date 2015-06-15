package com.mypcr.bootloader;

public interface BootLoaderChange
{
	final int CONNECTED		= 0x00;
	final int DISCONNECTED	= 0x01;
	
	public void OnBootLoaderChange(int MessageType, Object data);
}
