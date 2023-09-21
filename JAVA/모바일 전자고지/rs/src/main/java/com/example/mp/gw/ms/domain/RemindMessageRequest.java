package com.example.mp.gw.ms.domain;


import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.example.mp.gw.common.validator.YYYYMMDDHHMMSS;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : RemindMessageRequest.java
 * @Description : 리마인드 문자 발송 등록 요청 (From. 비즈센터)
 * 
 * @author 조주현
 * @since 2023.01.20
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2023.01.20	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@ApiModel(value = "리마인드 문자 발송 등록", description = "리마인드 문자 발송 등록 요청 (From. 비즈센터)")
public class RemindMessageRequest
{
	@NotBlank(message = "[서비스코드] 값은 필수입니다.")
	@ApiModelProperty(value = "서비스코드", required = true)
	private String service_cd;

	@NotBlank(message = "[기관명] 값은 필수입니다.")
	@ApiModelProperty(value = "기관명", required = true)
	private String biz_nm;

	@NotBlank(message = "[문서코드] 값은 필수입니다.")
	@ApiModelProperty(value = "문서코드", required = true)
	private String msg_cd;

	@NotBlank(message = "[문서명] 값은 필수입니다.")
	@ApiModelProperty(value = "문서명", required = true)
	private String msg_nm;

	@NotBlank(message="[발송시작일시] 값은 필수입니다.")
	@YYYYMMDDHHMMSS(message = "[발송시작일시] 값의 형식(YYYYMMDDHHMMSS)이 올바르지 않습니다.")
	@ApiModelProperty(value = "발송시작일시", required = true)
	private String make_dt;

	@NotBlank(message = "[발송번호(서비스기관 발송전화번호)] 값은 필수입니다.")
	@ApiModelProperty(value = "발송번호(서비스기관 발송전화번호)", required = true)
	private String snd_tel_no;

	@NotNull(message="[문서종류] 값은 필수입니다.")
	@Max(value = 6, message = "[문서종류] 값의 형식이 올바르지 않습니다.")
	@Min(value = 0, message = "[문서종류] 값의 형식이 올바르지 않습니다.")
	@ApiModelProperty(value="문서종류", required=true)
	private Integer m_type;
	
	@NotNull(message = "[발송요청관리번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청관리번호", required = true)
	private Integer sndn_mgnt_seq;

	@NotBlank(message = "[다회선 사용자 처리여부] 값은 필수입니다.")
	@ApiModelProperty(value = "다회선 사용자 처리여부", required = true)
	private String multi_mbl_prc_type;

	@Valid
	@NotEmpty(message = "[배열] 값은 필수입니다.")
	@ApiModelProperty(value = "배열")
	private List<RemindMessage> reqs;

	@JsonIgnore
	private long mills;


	public RemindMessageRequest()
	{
		this.mills = System.currentTimeMillis();
	}
}
