package com.example.mp.gw.member.domain;


import com.example.mp.gw.common.validator.YYYYMMDDHHMMSS;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : WithdrawMemberRequest.java
 * @Description : 회원 탈퇴 (추상 객체)
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
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("회원 탈퇴")
public abstract class WithdrawMemberRequest
{
	@YYYYMMDDHHMMSS
	@ApiModelProperty(value = "요청일시")
	private String reqDt;

	@ApiModelProperty(value = "개입/법인 구분 (\"0\":개인,\"1\":법인)", hidden = true)
	private String type;
}
