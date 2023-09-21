package com.example.mp.gw.member.domain;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.example.mp.gw.common.domain.Const.MEMBER;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : WithdrawCorpMemberRequest.java
 * @Description : 회원 탈퇴(법인) 요청 (From. Web)
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
@ToString
@ApiModel(value = "회원 탈퇴(법인)", description = "회원 탈퇴(법인) 요청 (From. Web)")
public class WithdrawCorpMemberRequest extends WithdrawMemberRequest
{
	@NotBlank(message = "[공인전자주소] 값은 필수입니다.")
	@ApiModelProperty(value = "공인전자주소", hidden = true)
	private String cea;
	
	@Size(max = 5,message = "[기관코드] 값은 최대 5자리입니다.")
	@ApiModelProperty(value = "기관코드", notes = "요청 기관코드", example = "XXXX(5자리)")
	private String bizCd;

	@Pattern(message = "[사업자번호] 값의 형식이 올바르지 않습니다.", regexp = "^\\d{3}-\\d{2}-\\d{5}$")
	@ApiModelProperty(value = "사업자번호", notes = "법인회원 사업자번호", example = "XXX-XX-XXXXX")
	private String bizNum;


	public WithdrawCorpMemberRequest()
	{
		super.setType(MEMBER.TYPE.CORP.val());
	}
}

