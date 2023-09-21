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
 * @Class Name : MessageRequest.java
 * @Description : 본문자 발송 등록 요청 (From. 비즈센터)
 * 
 * @author 조주현
 * @since 2021.10.13
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.10.13	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@Builder
@AllArgsConstructor
@ToString(exclude = "mms_binary")
@ApiModel(value = "본문자 발송 등록", description = "본문자 발송 등록 요청 (From. 비즈센터)")
public class MessageRequest
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

	@NotBlank(message="[문서명] 값은 필수입니다.")
	@ApiModelProperty(value = "문서명", required = true)
	private String msg_nm;

	@NotNull(message = "[발송 메시지 타입] 값은 필수입니다.")
	@ApiModelProperty(value = "발송 메시지 타입", required = true)
	private Integer msg_type;

	@NotBlank(message="[발송시작일시] 값은 필수입니다.")
	@YYYYMMDDHHMMSS(message = "[발송시작일시] 값의 형식(YYYYMMDDHHMMSS)이 올바르지 않습니다.")
	@ApiModelProperty(value = "발송시작일시", required = true)
	private String make_dt;
	
	@NotBlank(message = "[발송마감시간] 값은 필수입니다.")
	@YYYYMMDDHHMMSS(message = "[발송마감시간] 값의 형식(YYYYMMDDHHMMSS)이 올바르지 않습니다.")
	@ApiModelProperty(value = "발송마감시간", required = true)
	private String sndn_ex_time;

	@NotBlank(message = "[열람마감시간] 값은 필수입니다.")
	@YYYYMMDDHHMMSS(message = "[열람마감시간] 값의 형식(YYYYMMDDHHMMSS)이 올바르지 않습니다.")
	@ApiModelProperty(value = "열람마감시간", required = true)
	private String ex_time;
	
	@NotBlank(message = "[발송번호(서비스기관 발송전화번호)] 값은 필수입니다.")
	@ApiModelProperty(value = "발송번호(서비스기관 발송전화번호)", required = true)
	private String snd_tel_no;
	
	@NotNull(message="[문서종류] 값은 필수입니다.")
	@Max(value = 6, message = "[문서종류] 값의 형식이 올바르지 않습니다.")
	@Min(value = 1, message = "[문서종류] 값의 형식이 올바르지 않습니다.")
	@ApiModelProperty(value="문서종류", required=true)
	private Integer m_type;

	@ApiModelProperty(value = "RCS 브랜드홈 값")
	private String brand_id;

	@NotNull(message = "[메시지 발송구분] 값은 필수입니다.")
	@Max(value = 7, message = "[메시지 발송구분] 값의 형식이 올바르지 않습니다.")
	@Min(value = 1, message = "[메시지 발송구분] 값의 형식이 올바르지 않습니다.")
	@ApiModelProperty(value = "메시지 발송구분")
	private Integer opt_type;								
	
	@ApiModelProperty(value = "토큰확인대체여부")
	private String tkn_rpmt_yn;
	
	@ApiModelProperty(value = "열람확인대체여부")
	private String rdng_rpmt_yn;
		
	@ApiModelProperty(value = "MMS 바이너리")
	private String mms_binary;

	@ApiModelProperty(value = "MMS 바이너리 파일포맷(확장자)")
	private String file_fmat;
	
	@NotNull(message = "[발송요청관리번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청관리번호", required = true)
	private Integer sndn_mgnt_seq;

	@ApiModelProperty(value = "송신자 플랫폼 ID")	
	private String snd_plfm_id;	

	@ApiModelProperty(value = "송신 공인전자주소")	
	private String snd_npost;		

	@ApiModelProperty(value = "송신일시")	
	private String snd_date;	

	@ApiModelProperty(value = "테스트 발송여부")
	private String test_sndn_yn;

	@NotBlank(message = "[다회선 사용자 처리여부] 값은 필수입니다.")
	@ApiModelProperty(value = "다회선 사용자 처리여부", required = true)
	private String multi_mbl_prc_type;

	@ApiModelProperty(value = "재열람 일수")
	private String reopen_day;

	@ApiModelProperty(value = "전자문서 유형")
	private String kisa_doc_type;

	@Valid
	@NotEmpty(message = "[배열] 값은 필수입니다.")
	@ApiModelProperty(value = "배열")
	private List<Message> reqs;

	@JsonIgnore
	private long mills;


	public MessageRequest()
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

	@AssertFalse(message = "RCS 메시지인 경우 [RCS 브랜드홈 값]는 필수값 입니다.")
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

	@AssertFalse(message = "RCS 메시지인 경우 [MMS 바이너리]는 필수값 입니다.")
	public boolean isValidMmsBinary()
	{
		boolean returnVal = false;

		if (1 == msg_type)
		{
			if (null == mms_binary || true == mms_binary.equals(""))
			{
				returnVal = true;
			}
		}

		return returnVal;
	}

	@AssertFalse(message = "RCS 메시지인 경우 [MMS 바이너리 파일포맷(확장자)]은 필수값 입니다.")
	public boolean isValidFileFmat()
	{
		boolean returnVal = false;

		if (null != mms_binary && false == mms_binary.equals("") )
		{
			if (null == file_fmat || true == file_fmat.equals("") )
			{
				returnVal = true;
			}
		}

		return returnVal;
	}

	@AssertFalse(message = "RCS 메시지인 경우 개별부 [RCS 메시지 상세내용]은 필수값 입니다.")
	public boolean isValidReqsRcsDtlCnts()
	{
		boolean returnVal = false;

		if (1 == msg_type)
		{
			for (Message req : reqs)
			{
				if (null == req.getRcs_dtl_cnts() || true == req.getRcs_dtl_cnts().equals(""))
				{
					returnVal = true;

					break;
				}
			}
		}

		return returnVal;
	}	

	@AssertFalse(message = "개별부에 [MMS 바이너리]가 존재하는 경우 [MMS 바이너리 파일포맷(확장자)]는 필수값 입니다.")
	public boolean isValidReqsFileFmat()
	{
		boolean returnVal = false;

		for (Message req : reqs)
		{
			if (null != req.getMms_binary() && false == req.getMms_binary().equals(""))
			{
				if (null == req.getFile_fmat() || true == req.getFile_fmat().equals(""))
				{
					returnVal = true;

					break;
				}
			}
		}

		return returnVal;
	}

	public Integer getOpt_type()
	{
		// opt_type 이 null 일 경우, 기본값은 1(OPT_OUT 본문자) 
		return null == opt_type ? 1 : opt_type;
	}
}
