package com.walking.api.persistence.support;

import lombok.Getter;

@Getter
public enum TestData {
	ONE(37.2521333, 127.0698333);

	private double lat;
	private double lng;

	TestData(double lat, double lng) {
		this.lat = lat;
		this.lng = lng;
	}
}
