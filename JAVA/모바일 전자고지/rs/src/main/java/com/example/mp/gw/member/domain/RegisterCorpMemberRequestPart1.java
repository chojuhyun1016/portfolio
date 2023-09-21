package com.example.mp.gw.member.domain;


import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * @Class Name : RegisterCorpMemberRequestPart1.java
 * @Description : 법인(기관) 회원 가입/수정 요청 객체1
 * 
 * @author 조주현
 * @since 2022.10.17
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.10.17	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@ApiModel(value = "법인(기관) 회원 가입/수정 요청 객체1", description = "법인(기관) 회원 가입/수정 요청 객체1")
public class RegisterCorpMemberRequestPart1
{
	@NotNull(message="[요청구분] 값은 필수입니다.")
	@ApiModelProperty(value = "요청구분", notes = "요청구분", required = true)
	private Integer req_dvcd;

	@NotBlank(message = "[공인전자주소] 값은 필수입니다.")
	@ApiModelProperty(value = "공인전자주소", notes = "공인전자주소", required = true)
	private String ofap_elct_addr;

	@NotBlank(message="[서비스코드] 값은 필수입니다.")
	@ApiModelProperty(value = "서비스코드", notes = "기관코드", required = true)
	private String service_cd; 

	@NotBlank(message = "[기관명] 값은 필수입니다.")
	@ApiModelProperty(value = "기관명", notes = "기관명", required = true)
	private String biz_nm;

	@Pattern(message = "[기관구분코드] 값이 올바르지 않습니다.", regexp = "^[1-4,9]{1}|$")
	@ApiModelProperty(value = "기관구분코드", notes = "기관구분코드", required = true)
	private String type;

	@NotBlank(message = "[사업자 번호] 값은 필수입니다.")
	@ApiModelProperty(value = "사업자 번호", notes = "사업자 번호", example = "XXX-XX-XXXXX", required = true)
	private String brno;

	@ApiModelProperty(value = "사업자 명", notes = "사업자 명", example = "(주)LG", required = true)
	private String brno_nm;

	@ApiModelProperty(value = "수신동의상태 전송 여부", notes = "수신동의상태 전송 여부", required = true)
	private String agree_yn;


	@AssertFalse(message = "[이용자 유형], [사업자 명] 값은 필수입니다.")
	public boolean isValidRegMissBrnoType()
	{
		boolean returnVal = false;

		if (1 == req_dvcd)
		{
			if ((null == brno_nm || true == "".equals(brno_nm))
			 || (null == type    || true == "".equals(type))
			   )
			{
				returnVal = true;
			}
		}

		return returnVal;
	}

	@AssertFalse(message = "[이용자 유형], [사업자 명] 값은 동시 변경이 불가합니다.")
	public boolean isValidModSyncBrnoType()
	{
		boolean returnVal = false;

		if (2 == req_dvcd)
		{
			if ((null != brno_nm && true != "".equals(brno_nm))
			 && (null != type    && true != "".equals(type))
			   )
			{
				returnVal = true;
			}
		}

		return returnVal;
	}

	@AssertFalse(message = "[이용자 유형], [사업자 명] 중 한 가지 값은 필수입니다.")
	public boolean isValidModMissBrnoType()
	{
		boolean returnVal = false;

		if (2 == req_dvcd)
		{
			if ((null == brno_nm || true == "".equals(brno_nm))
			 && (null == type    || true == "".equals(type))
			   )
			{
				returnVal = true;
			}
		}

		return returnVal;
	}
}
