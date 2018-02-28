package com.pogoda.xmlbinding;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;


@Root(strict=false)
public class IntValueAndUnits {
	@Attribute
	public int value;
	
	@Attribute(required=false)
	public String unit;
}
