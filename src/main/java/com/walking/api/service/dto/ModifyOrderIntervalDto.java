package com.walking.api.service.dto;

import com.walking.api.data.entity.Category;
import com.walking.api.data.entity.Member;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ModifyOrderIntervalDto {

	private Member member;
	private Category category;
	private ItemAndOrder[] itemAndOrders;
}
