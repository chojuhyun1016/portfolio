package com.example.mp.gw.token.domain;


import javax.validation.constraints.NotBlank;

import org.springframework.util.StringUtils;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.validator.YYYYMMDDHHMMSS;
import com.example.mp.gw.token.exception.InvalidTokenException;
import com.example.mp.gw.token.exception.NotLguplusTokenException;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : VerifyReadTokenRequest.java
 * @Description : (토큰인증)열람확인결과 처리 요청 (From. 비즈센터)
 * 
 * @author 조주현
 * @since 2021.04.22
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.22	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@ApiModel(value = "(토큰인증)열람확인결과 처리", description = "(토큰인증)열람확인결과 처리 요청 (From. 비즈센터)")
public class VerifyReadTokenRequest
{
	@NotBlank(message = "[토큰] 값은 필수입니다.")
	@ApiModelProperty(value = "토큰", required = true)
	private String access_token;

	@NotBlank(message = "[열람타임스탬프] 값은 필수입니다.")
	@YYYYMMDDHHMMSS(message="[열람타임스탬프] 값의 형식이 올바르지 않습니다.")
	@ApiModelProperty(value = "열람타임스탬프", required = true)
	private String mms_rdg_tmst;


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
