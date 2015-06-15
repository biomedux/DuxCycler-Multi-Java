package com.mypcr.bootloader.structure;

public interface Packet
{
	public static final int BUF_SIZE		=	65;
	public static final int MAX_DATA_REGION =	6; 
	
	public byte[] toByte();
}