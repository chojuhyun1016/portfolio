package com.example.mp.gw.bc.domain;


import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @Class Name : RegisterRejection.java
 * @Description : 수신거부 등록 요청 객체  
 * 
 * @author 조주현
 * @since 2021.04.17
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.17	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Setter
@Getter
@Builder
@ApiModel(value = "수신거부 등록", description = "수신거부 등록 요청 객체")
public class RegisterRejection
{
	@NotBlank(message = "[기관코드] 값은 필수입니다.")
	@ApiModelProperty(value = "기관코드")
	private String service_cd;

	@NotBlank(message = "[개인식별코드] 값은 필수입니다.")
	@ApiModelProperty(value = "개인식별코드")
	private String ci;

	@NotBlank(message = "[신청구분] 값은 필수입니다.")
	@ApiModelProperty(value = "신청구분")
	private String apct_acct_cls;

	@NotBlank(message = "[신청일시] 값은 필수입니다.")
	@ApiModelProperty(value = "신청일시")
	private String apct_tm;
}
