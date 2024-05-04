package com.walking.api.data.constant;

public enum TrafficColor {
	RED(1),
	GREEN(2);

	private final int num;

	TrafficColor(int num) {
		this.num = num;
	}

	/**
	 * 다음 신호등의 색상을 전달합니다.
	 *
	 * @return 다음 신호등 색상
	 */
	public TrafficColor getNextColor() {
		if (this.num == 1) {
			return GREEN;
		} else {
			return RED;
		}
	}

	public boolean isRed() {
		return this.equals(RED);
	}

	public boolean isGreen() {
		return this.equals(GREEN);
	}
}
