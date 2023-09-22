package com.example.named.lock.rsv.lecture.domain;


import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GetApplRequest
{
	private Long   let_no;

	private String appl_no;


	public Map<String, Object> toMap()
	{
		ObjectMapper objectMapper = new ObjectMapper();

		@SuppressWarnings("unchecked")
		Map<String, Object> map = objectMapper.convertValue(this, Map.class);
		
		return map;
	}
}
