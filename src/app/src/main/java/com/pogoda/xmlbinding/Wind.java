package com.pogoda.xmlbinding;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;


@Root(strict=false)
public class Wind {
	public int power;
	
	@Attribute
	public void setValue(int value) {
		power = Math.round(value * 1000 / 3600L);
	} 
	
	@Attribute
	public int getValue() {
		return Math.round(power * 3600 / 1000L);
	} 
	
	@Attribute
	public String dir; 

}
