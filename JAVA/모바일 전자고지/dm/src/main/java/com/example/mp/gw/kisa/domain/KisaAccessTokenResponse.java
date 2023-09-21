package com.example.mp.gw.kisa.domain;


import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : KisaAccessTokenResponse.java
 * @Description : 중계자 토큰 발급 및 갱신 응답 (From. KISA)
 * 
 * @author 조주현
 * @since 2022.02.10
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.02.10	    조주현          최초 생성
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
@ApiModel(value = "중계자 토큰 발급 및 갱신", description = "중계자 토큰 발급 및 갱신 응답 (From. KISA)")
public class KisaAccessTokenResponse
{
	@NotNull(message = "[처리결과] 값은 필수입니다.")
	@ApiModelProperty(value="처리결과 (1:성공, 0:실패)")
	private Integer resultCode;

	@ApiModelProperty(value="엑세스 토큰")
	private String accessToken;

	@ApiModelProperty(value="엑세스 토큰 만료시간(분)")
	private Integer accessExpiresIn;

	@ApiModelProperty(value="엑세스 토큰 만료일시")
	private String accessExpireDate;

	@ApiModelProperty(value="리프레시 토큰")
	private String refreshToken;

	@ApiModelProperty(value="리프레시 토큰 만료시간(분)")
	private int refreshExpiresIn;

	@ApiModelProperty(value="리프레시 토큰 만료일시")
	private String refreshExpireDate;

	@ApiModelProperty(value="오류코드")
	private String errCode;

	@ApiModelProperty(value="오류메시지")
	private String errMsg;

	@ApiModelProperty(value="오류위치")
	private String errLocation;

	@ApiModelProperty(value="발급시간")
	private String reg_dt;
}
