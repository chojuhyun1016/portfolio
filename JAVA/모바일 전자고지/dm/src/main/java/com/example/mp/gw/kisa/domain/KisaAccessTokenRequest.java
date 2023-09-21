package com.example.mp.gw.kisa.domain;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @Class Name : KisaAccessTokenRequest.java
 * @Description : KISA 토큰 발급 요청 객체 (KISA)
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(value = "중계자 토큰 발급 및 갱신", description = "중계자 토큰 발급 및 갱신 요청 (To. KISA)")
public class KisaAccessTokenRequest
{
	@NotNull(message = "[인증구분값] 값은 필수입니다.")
	@ApiModelProperty(value = "인증구분값 (0:발급, 1:갱신)")
	private Integer grantType;

	@NotBlank(message = "[중계자플랫폼 클라이언트 ID] 값은 필수입니다.")
	@ApiModelProperty(value = "중계자플랫폼 클라이언트 ID")
	private String clientId;

	@ApiModelProperty(value = "클라이언트 비밀번호(발급시 필수)")
	private String clientSecret;

	@ApiModelProperty(value = "리프레시 토큰(갱신시 필수)")
	private String refreshToken;
}
