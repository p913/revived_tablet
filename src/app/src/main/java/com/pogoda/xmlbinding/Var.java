package com.pogoda.xmlbinding;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(strict=false)
public class Var {
	public static final int ICON_TEMPERATURE_MIN = 4;
	public static final int ICON_TEMPERATURE_MAX = 5;
	public static final int ICON_ICON = 10;
	public static final int ICON_WIND = 9;	
	
	@Element
	public String name;
	
	@Element
	public int icon;
	
	@ElementList
	public List<VarData> data;
}

