package com.pogoda.xmlbinding;

import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(strict=false)
public class LocationVar {
	@ElementList(inline=true, name="var")
	public List<Var> vars;
	
}

