package com.walking.api.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public abstract class BasicItem implements Comparable<BasicItem> {

	private final Long id;
	private Long interval;

	public BasicItem(Long id, Long interval) {
		this.id = id;
		this.interval = interval;
	}

	@Override
	public int compareTo(BasicItem o) {
		return this.getInterval().compareTo(o.getInterval());
	}
}
