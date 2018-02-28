package com.pogoda.xmlbinding;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(strict=false)
public class Symbol {
	@Attribute(name="value")
	public int icon;
	
	@Attribute
	public String desc;
	

}
