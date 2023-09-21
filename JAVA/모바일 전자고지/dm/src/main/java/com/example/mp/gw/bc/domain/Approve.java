package com.example.mp.gw.bc.domain;


import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : Approve.java
 * @Description : 수신동의(등록/해제) 요청 전송 객체
 * 
 * @author 조주현
 * @since 2022.05.12
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.05.12	    조주현          최초 생성
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
@ApiModel(value = "수신동의(신청/해지) 정보", description = "수신동의(등록/해제) 정보 요청 객체")
public class Approve
{
	@NotBlank(message = "[신청구분] 값은 필수입니다.")
	@ApiModelProperty(value = "신청구분")
	private String apct_acct_cls;

	@JsonIgnore
	@ApiModelProperty(value = "개인휴대전화번호")
	private String phone;

	@NotBlank(message = "[개인식별코드] 값은 필수입니다.")
	@ApiModelProperty(value = "개인식별코드")
	private String ci;

	@NotBlank(message = "[기관코드] 값은 필수입니다.")
	@ApiModelProperty(value = "기관코드")
	private String service_cd;

	@NotBlank(message = "[신청일시] 값은 필수입니다.")
	@ApiModelProperty(value = "신청일시")
	private String apct_dt;

	@JsonIgnore
	@ApiModelProperty(value = "메시지ID(발송요청관리번호_방송요청일련번호)")
	private String message_id;
}
