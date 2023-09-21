package com.example.mp.gw.kisa.domain;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : RegisterEaddrRequest.java
 * @Description : 공인전자주소 등록 요청 (To. KISA)
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
@ApiModel(value = "공인전자주소 등록", description = "공인전자주소 등록 요청 (To. KISA)")
public class RegisterEaddrRequest
{
	@NotBlank(message = "[이용자 고유번호(개인:CI, 법인:사업자번호)] 값은 필수입니다.")
	@ApiModelProperty(value="이용자 고유번호(개인:CI, 법인:사업자번호)")
	private String idn;

	@NotBlank(message = "[이용자 공인전자주소] 값은 필수입니다.")
	@ApiModelProperty(value="이용자 공인전자주소")
	private String eaddr;

	@NotBlank(message = "[이용자명] 값은 필수입니다.")
	@ApiModelProperty(value="이용자명")
	private String name;

	@NotNull(message = "[이용자구분(개인:0, 법인:1, 국가기관:2, 공공기관:3, 지자체:4, 기타:9)] 값은 필수입니다.")
	@ApiModelProperty(value="이용자구분(개인:0, 법인:1, 국가기관:2, 공공기관:3, 지자체:4, 기타:9)")
	private Integer type;

	@NotBlank(message = "[가입일시] 값은 필수입니다.")
	@ApiModelProperty(value="가입일시")
	private String regDate;
}
