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
 * @Class Name : RegisterWhitelist.java
 * @Description : Whitelist 등록 요청 객체 
 * 
 * @author 조주현
 * @since 2021.12.24
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.24	    조주현          최초 생성
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
@ApiModel(value = "Whitelist 등록", description = "Whitelist 등록 요청 객체")
public class RegisterWhitelist
{
	@NotBlank(message = "[기관코드] 값은 필수입니다.")
	@ApiModelProperty(value = "기관코드")
	private String service_cd;

	@NotBlank(message = "[개인식별코드] 값은 필수입니다.")
	@ApiModelProperty(value = "개인식별코드")
	private String ci;

	@NotBlank(message = "[모바일사업자구분] 값은 필수입니다.")
	@ApiModelProperty(value = "모바일사업자구분")
	private String mbl_bzowr_dvcd;

	@NotBlank(message = "[신청구분] 값은 필수입니다.")
	@ApiModelProperty(value = "신청구분")
	private String apct_acct_cls;

	@ApiModelProperty(value = "개인휴대전화번호")
	private String mdn;	

	@ApiModelProperty(value = "유입구분")
	private String in_cls;
}
