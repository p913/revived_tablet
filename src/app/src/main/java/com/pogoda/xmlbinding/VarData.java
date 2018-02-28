package com.pogoda.xmlbinding;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(strict=false, name="forecast")
public class VarData {
	@Attribute(name="data_sequence")
	public int index;
	
	@Attribute(required=false)
	public String value;
	
	@Attribute(required=false)
	public String id;
	
}
