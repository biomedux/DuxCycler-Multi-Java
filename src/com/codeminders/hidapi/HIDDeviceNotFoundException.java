package com.codeminders.hidapi;

import java.io.IOException;

/**
 * Thrown if HID Device with given criteria could not be found
 *
 * @author lord
 */
public class HIDDeviceNotFoundException extends IOException
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * Constructs a <code>HIDDeviceNotFoundException</code> with no detailed error message.
     */
    public HIDDeviceNotFoundException()
    {
    }
    
    /**
     * Constructs a <code>HIDDeviceNotFoundException</code> with the specified error message.
     */
    public HIDDeviceNotFoundException(String message)
    {
        super(message);
    }
}