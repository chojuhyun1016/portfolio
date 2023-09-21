package com.example.domain;


import lombok.Data;


@Data
public class UserDto
{
	Integer    num;
	String     id;
	String     pwd;

	
	@Override
	public String toString()
	{
		return "num : "  + num  + ", " 
	         + "id : "   + id   + ", "
             + "pwd : "  + pwd
             ; 
	}
}
