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
 * @Class Name : SearchEaddrRequest.java
 * @Description : 공인전자주소 조회 요청 (To. KISA)
 * 
 * @author 조주현
 * @since 2022.03.12
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.03.12	    조주현          최초 생성
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
@ApiModel(value = "공인전자주소 조회", description = "공인전자주소 조회 요청 (To. KISA)")
public class SearchEaddrRequest
{
	@NotBlank(message = "[이용자 고유번호] 값은 필수입니다.")
	@ApiModelProperty(value="이용자 고유번호")
	private String idn;
 
	@ApiModelProperty(value="조회대상 중계자플랫폼 ID(타중계자의 플랫폼이 보유한 공인전자주소 조회시에만 사용")
	private String platformId;
}
