package com.example.mp.gw.bc.domain;


import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : BizCenterAccessTokenRequest.java
 * @Description : 토큰발행 요청 (To. 비즈센터)
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
@ApiModel(value = "토큰발행", description = "토큰발행 요청 (To. 비즈센터)")
public class BcAccessTokenRequest
{
	@NotBlank(message = "[권한부여방식] 값은 필수입니다.")
	@ApiModelProperty(value = "권한부여방식")
	private String grant_type;

	@NotBlank(message = "[클라이언트ID] 값은 필수입니다.")
	@ApiModelProperty(value = "클라이언트ID")
	private String client_id;

	@NotBlank(message = "[클라이언트Secret] 값은 필수입니다.")
	@ApiModelProperty(value = "클라이언트Secret")
	private String client_secret;

	@NotBlank(message = "[권한범위] 값은 필수입니다.")
	@ApiModelProperty(value = "권한범위")
	private String scope;
}
