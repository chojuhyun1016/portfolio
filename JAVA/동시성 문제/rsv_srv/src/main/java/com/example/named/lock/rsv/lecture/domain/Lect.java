package com.example.named.lock.rsv.lecture.domain;


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
public class Lect
{
	private Long    no;

	private String  lecturer;

	private String  location;

	private Integer max_appl;

	private String  time;

	private String  content;

	private String  reg_dt;
}
