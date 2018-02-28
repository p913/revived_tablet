package com.pogoda.xmlbinding;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;


@Root(strict=false)
public class Location {
	@Attribute
	public String city;

	@ElementList(inline=true, name="day")
	public List<Day> days;
	
}
