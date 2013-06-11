package com.owenobyrne.dashclock.extension.dart.model;

import org.simpleframework.xml.Element;

public class ObjStation {
	@Element public String StationDesc;
	@Element(required=false) public String StationAlias;
	@Element public double StationLatitude;
	@Element public double StationLongitude;
	@Element public String StationCode;
	@Element public int StationId;
	public String getStationDesc() {
		return StationDesc;
	}
	public void setStationDesc(String stationDesc) {
		StationDesc = stationDesc;
	}
	public String getStationAlias() {
		return StationAlias;
	}
	public void setStationAlias(String stationAlias) {
		StationAlias = stationAlias;
	}
	public double getStationLatitude() {
		return StationLatitude;
	}
	public void setStationLatitude(double stationLatitude) {
		StationLatitude = stationLatitude;
	}
	public double getStationLongitude() {
		return StationLongitude;
	}
	public void setStationLongitude(double stationLongitude) {
		StationLongitude = stationLongitude;
	}
	public String getStationCode() {
		return StationCode;
	}
	public void setStationCode(String stationCode) {
		StationCode = stationCode;
	}
	public int getStationId() {
		return StationId;
	}
	public void setStationId(int stationId) {
		StationId = stationId;
	}
	
	
	
}
