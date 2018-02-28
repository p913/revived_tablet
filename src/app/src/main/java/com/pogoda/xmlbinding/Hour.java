package com.pogoda.xmlbinding;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;


@Root(strict=false)
public class Hour {
	public int hour;
	
	@Attribute
	public void setValue(String value) {
		try {
		  hour = Integer.parseInt(value.substring(0, 2));
		}
		catch (NumberFormatException e) {
			hour = 0;
		}
	}
	
	@Attribute
	public String getValue() {
		return String.format("%02d:00", hour);		
	}
	
	@Element
	public IntValueAndUnits temp;
	
	@Element
	public Symbol symbol;
	
	@Element
	public Wind wind;

}
