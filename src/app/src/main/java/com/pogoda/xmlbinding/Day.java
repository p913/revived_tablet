package com.pogoda.xmlbinding;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(strict=false)
public class Day {
	protected static SimpleDateFormat dtFmt = new SimpleDateFormat("yyyyMMdd");
	
	public Date date;
	
	public Day() {
		
	}
	
	@Attribute(name="value")
	public void setValue(String value){
		try {
			date = dtFmt.parse(value);
		} catch (ParseException e) {
			date = null;
		}
	}
	
	@Attribute(name="value")
	public String getValue() {
		return dtFmt.format(date);
	}

	@ElementList(inline=true, name="hour")
	public List<Hour> hours;
	
	public Hour get(int hourIndex) {
		if (hours != null && hours.size() > hourIndex)
				return hours.get(hourIndex);		
		return null;
	}
	
}

	