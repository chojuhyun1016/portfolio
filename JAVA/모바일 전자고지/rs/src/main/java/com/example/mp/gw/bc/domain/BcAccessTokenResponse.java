package com.example.mp.gw.bc.domain;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : BcAccessTokenResponse.java
 * @Description : 토큰발행 응답 (From. 비즈센터)
 * 
 * @author 조주현
 * @since 2021.08.03
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.08.03	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "토큰발행", description = "토큰발행 응답 (From. 비즈센터)")
public class BcAccessTokenResponse
{
	@ApiModelProperty(value = "접근토큰")
	private String access_token;

	@ApiModelProperty(value = "접근토큰 유형")
	private String token_type;

	@ApiModelProperty(value = "접근토큰 유효기간")
	private String expires_in;

	@ApiModelProperty(value = "권한범위")
	private String scope;

	@ApiModelProperty(value = "접근토큰 식별자")
	private String jti;

	@ApiModelProperty(value = "에러코드")
	private String error;

	@ApiModelProperty(value = "에러메세지")
	private String error_description;

	@ApiModelProperty(value = "에러발생위치")
	private String error_location;

	@ApiModelProperty(value = "발급시간")
	private String reg_dt;
}
