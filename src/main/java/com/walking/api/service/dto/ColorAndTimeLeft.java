package com.walking.api.service.dto;

import com.walking.api.data.constant.TrafficColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ColorAndTimeLeft {

	private TrafficColor trafficColor;
	private Float timeLeft;
}
