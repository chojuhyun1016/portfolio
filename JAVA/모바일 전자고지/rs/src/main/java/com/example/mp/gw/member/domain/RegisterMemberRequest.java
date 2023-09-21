package com.example.mp.gw.member.domain;


import java.util.HashMap;

import com.example.mp.gw.common.validator.YYYYMMDDHHMMSS;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : RegisterMemberRequest.java
 * @Description : 회원가입 (추상 객체)
 * 
 * @author 조주현
 * @since 2022.03.17
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.03.17	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("회원 가입")
public abstract class RegisterMemberRequest
{
	@YYYYMMDDHHMMSS
	@ApiModelProperty(value = "요청일시", example = "\"20210101000000\"")
	private String reqDt;
	
	@ApiModelProperty(value = "공인전자주소", hidden = true)
	private String cea;


	@SuppressWarnings("unchecked")
	public HashMap<String,Object> toHashMap()
	{
		return new ObjectMapper().convertValue(this, HashMap.class);
	}
}
