package com.pogoda.xmlbinding;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;


@Root(strict=false)
public class Report {
	@Element
	public Location location;

}
