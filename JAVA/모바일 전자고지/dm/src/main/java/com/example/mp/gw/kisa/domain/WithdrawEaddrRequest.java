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
 * @Class Name : WithdrawEaddrRequest.java
 * @Description : 공인전자주소 탈퇴 요청 (To. KISA)
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "공인전자주소 탈퇴", description = "공인전자주소 탈퇴 요청 (To. KISA)")
public class WithdrawEaddrRequest
{
	@NotBlank(message = "[이용자 공인전자주소] 값은 필수입니다.")
	@ApiModelProperty(value="이용자 공인전자주소")
	private String eaddr;

	@NotBlank(message = "[이용자의 공인전자주소 서비스 탈퇴일시] 값은 필수입니다.")
	@ApiModelProperty(value="이용자의 공인전자주소 서비스 탈퇴일시")
	private String delDate;
}
