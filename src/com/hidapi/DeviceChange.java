package com.hidapi;

public interface DeviceChange
{
	final int CONNECTED		= 0x00;
	final int DISCONNECTED	= 0x01;
	
	public void OnMessage(int MessageType, Object data);
}
