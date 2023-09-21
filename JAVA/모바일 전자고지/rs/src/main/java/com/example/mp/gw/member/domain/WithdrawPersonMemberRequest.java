package com.example.mp.gw.member.domain;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import com.example.mp.gw.common.domain.Const.MEMBER;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @Class Name : WithdrawPersonMemberRequest.java
 * @Description : 회원 탈퇴(개인) 요청 (From. Web)
 * 
 * @author 조주현
 * @since 2022.03.17
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.03.17	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ApiModel(value = "회원 탈퇴(개인)", description = "회원 탈퇴(개인) 요청 (From. Web)")
public class WithdrawPersonMemberRequest extends WithdrawMemberRequest
{
	@NotBlank(message = "[전화번호] 값은 필수입니다.")
	@Pattern(message="[전화번호] 값의 형식이 올바르지 않습니다.", regexp = "^\\d{3}\\d{3,4}\\d{4}$")
	@ApiModelProperty(value = "전화번호", notes = "전화번호 입력", example = "01012341234", required = true)
	private String phone;

	@Pattern(message = "[생년월일] 값의 형식이 올바르지 않습니다.", regexp = "^\\d{2}(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])$")
	@ApiModelProperty(value = "생년월일", notes = "생년월일 (YYMMDD)", example = "\"890719\"")
	private String birthday;

	@ApiModelProperty(value = "성별", notes = "\"1\":남성 (1900-1999년생),\"1\":여성 (1900-1999년생), \"3\":남성 (2000년생부터),\"4\":여성 (2000년생부터),\"5\":외국남성 (1900-1999년생),\"6\":외국여성 (1900-1999년생), \"7\":외국남성 (2000년생부터),\"8\":외국여성 (2000년생부터) ", example = "")
	private String gender;

	@NotBlank(message = "[개인식별코드] 값은 필수입니다.")
	@ApiModelProperty(value = "CI", required = true)
	private String ci;

	@NotBlank(message="[가입경로] 값은 필수입니다")
	@ApiModelProperty(value = "가입경로", notes = "\"W\":웹,\"M\":문자,\"B\":비즈센터,\"A\":자동가입", example = "W", required = true)
	private String reqRoute;

	@ApiModelProperty(value = "신청일시", notes = "수신동의 등록/해제 발생일시", example = "20210101000000", required = true)
	private String approveDt;


	public WithdrawPersonMemberRequest()
	{
		super.setType(MEMBER.TYPE.PERSON.val());
	}
}

