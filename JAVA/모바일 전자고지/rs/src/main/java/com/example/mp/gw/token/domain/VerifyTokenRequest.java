package com.example.mp.gw.token.domain;


import javax.validation.constraints.NotBlank;

import org.springframework.util.StringUtils;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.token.exception.InvalidTokenException;
import com.example.mp.gw.token.exception.NotLguplusTokenException;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : VerifyTokenRequest.java
 * @Description : 토큰인증확인 조회 요청 (From. 비즈센터)
 * 
 * @author 조주현
 * @since 2021.05.06
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.05.06	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@ApiModel(value = "토큰인증확인 조회", description = "토큰인증확인 조회 요청 (From. 비즈센터)")
public class VerifyTokenRequest
{
	@NotBlank(message = "[토큰] 값은 필수입니다.")
	@ApiModelProperty(value = "토큰", required = true)
	private String access_token;


	@JsonIgnore
	public String getToken() throws InvalidTokenException, NotLguplusTokenException
	{
		if (!StringUtils.hasText(access_token))
			throw new InvalidTokenException();

		final String REQ_LG_UPLUS_TYPE = access_token.substring(0, 2);

		if (!Const.LGUPLUS.LG_UPLUS_TYPE.equals(REQ_LG_UPLUS_TYPE))
			throw new NotLguplusTokenException();

		return access_token.substring(2);
	}
}
