package com.dppware.serviceA.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @Builder @NoArgsConstructor
public class TopicDescriptionDTO {
	private String name;
	
	public TopicDescriptionDTO(String name) {
		this.name=name;
	}
}
