package com.pogoda.xmlbinding;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(strict=false)
public class ReportVars {
	@Element
	public LocationVar location;
	
	public VarData get(int iconId, int index) {
		if (location.vars != null)
			for (Var v: location.vars)
				if (v.icon == iconId)
					if (v.data != null)
						for (VarData d: v.data)
							if (d.index == index)
								return d;
		VarData res = new VarData();
		res.id = "0";
		res.value = "0";
		res.index = index;
		return res;
	}
	
}
