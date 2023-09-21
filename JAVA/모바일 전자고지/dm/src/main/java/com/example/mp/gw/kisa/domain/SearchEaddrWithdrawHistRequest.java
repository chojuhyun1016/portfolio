package com.example.mp.gw.kisa.domain;


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
 * @Class Name : SearchEaddrWithdrawHistRequest.java
 * @Description : 공인전자주소 탈퇴이력 조회 요청 (To. KISA)
 * 
 * @author 조주현
 * @since 2022.04.04
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.04.04	    조주현          최초 생성
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
@ApiModel(value = "공인전자주소 탈퇴이력 조회", description = "공인전자주소 탈퇴이력 조회 요청 (To. KISA)")
public class SearchEaddrWithdrawHistRequest
{
	@NotBlank(message = "[이용자 고유번호] 값은 필수입니다.")
	@ApiModelProperty(value = "이용자 고유번호")
	private String idn;
}
