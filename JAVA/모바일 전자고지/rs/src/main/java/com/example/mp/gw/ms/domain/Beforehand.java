package com.example.mp.gw.ms.domain;


import javax.validation.constraints.NotBlank;
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
 * @Class Name : Beforehand.java
 * @Description : 사전문자 발송 등록 요청 객체 
 * 
 * @author 조주현
 * @since 2021.04.04
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.04	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Setter	
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "사전문자 발송 등록", description = "사전문자 발송 등록 요청 객체")
public class Beforehand
{
	@NotNull(message = "[발송요청일련번호(리스트순번)] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청일련번호(리스트순번)", required = true)
	private Integer sndn_seq_no;

	@NotBlank(message = "[개인식별코드] 값은 필수입니다.")
	@ApiModelProperty(value = "개인식별코드", required = true)
	private String ci;

	@ApiModelProperty(value = "개인휴대전화번호")
	private String mdn;

	@ApiModelProperty(value = "사전문자 본인인증 URL")	
	private String url;

	@ApiModelProperty(value = "수신거부 및 수신 휴대폰 지정하기 치환문구")	
	private String rcve_rf_str;

	@ApiModelProperty(value = "안내문 확인하기 치환문구")	
	private String info_cfrm_str;
}
