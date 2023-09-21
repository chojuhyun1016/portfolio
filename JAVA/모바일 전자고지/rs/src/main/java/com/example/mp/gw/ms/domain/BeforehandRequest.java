package com.example.mp.gw.ms.domain;


import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.AssertFalse;
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
 * @Class Name : BeforehandRequest.java
 * @Description : 사전문자 발송 등록 요청 (From. 비즈센터)
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


@Getter
@Setter
@Builder
@AllArgsConstructor
@ToString(exclude = {"bfh_ltr_ttl", "bfh_ltr_cnts"})
@ApiModel(value = "사전문자 발송 등록", description = "사전문자 발송 등록 요청 (From. 비즈센터)")
public class BeforehandRequest
{
	@NotBlank(message = "[서비스코드] 값은 필수입니다.")
	@ApiModelProperty(value = "서비스코드", required = true)
	private String service_cd;

	@NotBlank(message = "[기관명] 값은 필수입니다.")
	@ApiModelProperty(value = "기관명", required = true)
	private String biz_nm;

	@ApiModelProperty(value = "대행사코드")	
	private String agency_id;

	@NotBlank(message = "[문서코드] 값은 필수입니다.")
	@ApiModelProperty(value = "문서코드", required = true)
	private String msg_cd;

	@NotBlank(message = "[문서명] 값은 필수입니다.")
	@ApiModelProperty(value = "문서명", required = true)
	private String msg_nm;

	@NotNull(message = "[발송 메시지 타입] 값은 필수입니다.")
	@ApiModelProperty(value = "발송 메시지 타입", required = true)
	private Integer msg_type;

	@NotBlank(message = "[발송시작일시] 값은 필수입니다.")
	@YYYYMMDDHHMMSS(message = "[발송시작일시] 값의 형식(YYYYMMDDHHMMSS)이 올바르지 않습니다.")
	@ApiModelProperty(value = "발송시작일시", required = true)
	private String make_dt;

	@NotBlank(message="[발송마감시간] 값은 필수입니다.")
	@YYYYMMDDHHMMSS(message = "[발송마감시간] 값의 형식(YYYYMMDDHHMMSS)이 올바르지 않습니다.")
	@ApiModelProperty(value = "발송마감시간", required = true)
	private String sndn_ex_time;

	@NotBlank(message = "[발송번호(서비스기관 발송전화번호)] 값은 필수입니다.")
	@ApiModelProperty(value = "발송번호(서비스기관 발송전화번호)", required = true)
	private String snd_tel_no;

	@NotNull(message = "[문서종류] 값은 필수입니다.")
	@Max(value = 6,message = "[문서종류] 값의 형식이 올바르지 않습니다.")
	@Min(value = 1,message = "[문서종류] 값의 형식이 올바르지 않습니다.")
	@ApiModelProperty(value = "문서종류", required = true)
	private Integer m_type;

	@NotNull(message = "[메시지 발송구분] 값은 필수입니다.")
	@Max(value = 6,message = "[메시지 발송구분] 값의 형식이 올바르지 않습니다.")
	@Min(value = 2,message = "[메시지 발송구분] 값의 형식이 올바르지 않습니다.")
	@ApiModelProperty(value = "메시지 발송구분")
	private Integer opt_type;								

	@ApiModelProperty(value = "RCS 브랜드홈 값")
	private String brand_id;

	@ApiModelProperty(value = "사전문자제목")
	private String bfh_ltr_ttl;

	@NotBlank(message = "[사전문자내용] 값은 필수입니다.")
	@ApiModelProperty(value = "사전문자내용", required = true)
	private String bfh_ltr_cnts;

	@NotBlank(message = "[다회선 사용자 처리여부] 값은 필수입니다.")
	@ApiModelProperty(value = "다회선 사용자 처리여부", required = true)
	private String multi_mbl_prc_type;

	@ApiModelProperty(value = "테스트 발송여부")
	private String test_sndn_yn;

	@NotNull(message = "[발송요청관리번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청관리번호", required = true)
	private Integer sndn_mgnt_seq;

	@Valid
	@NotEmpty(message="[배열의 정보] 값은 필수입니다.")
	@ApiModelProperty(value = "배열의 정보", required = true)
	private List<Beforehand> reqs;

	@JsonIgnore
	private long mills;


	public BeforehandRequest()
	{
		this.mills = System.currentTimeMillis();
	}

	@AssertFalse(message = "RCS 메시지인 경우 [대행사코드]는 필수값 입니다.")
	public boolean isValidAgencyId()
	{
		boolean returnVal = false;

		if (1 == msg_type)
		{
			if (null == agency_id || true == agency_id.equals(""))
			{
				returnVal = true;
			}
		}

		return returnVal;
	}

	@AssertFalse(message = "RCS 메시지인 경우 [RCS 브랜드홈 값]은 필수값 입니다.")
	public boolean isValidBrandId()
	{
		boolean returnVal = false;

		if (1 == msg_type)
		{
			if (null == brand_id || true == brand_id.equals(""))
			{
				returnVal = true;
			}
		}

		return returnVal;
	}

	public Integer getOpt_type()
	{
		// opt_type 이 null 일 경우, 기본값은 2(OPT_OUT 사전문자)
		return opt_type == null ? 2 : opt_type;
	}

	// msg_type : L(2),R(1)로 구분, 사전문자는 msg_type XMS인경우 TS에서 LMS로 전송되므로 L  
	public String getMsgTypeCode()
	{
		if (2 == msg_type)
		{
			return "L";
		}
		else
		{
			return "R";
		}
	}
}
