package com.owenobyrne.dashclock.extension.dart.model;

import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root
public class ArrayOfObjStation {
	// inine=true is very important here. There's no array wrapper. This took about 2 hours to figure out. hate that.
	@ElementList(inline=true) public List<ObjStation> objStation;

	public List<ObjStation> getObjStation() {
		return objStation;
	}

	public void setObjStation(List<ObjStation> objStation) {
		this.objStation = objStation;
	}
	
	
}
