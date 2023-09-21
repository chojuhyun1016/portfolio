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
 * @Class Name : ModifyEaddrRequest.java
 * @Description : 공인전자주소 소유자정보 수정 요청 (To. KISA)
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
@ApiModel(value = "공인전자주소 소유자정보 수정", description = "공인전자주소 소유자정보 수정 요청 (To. KISA)")
public class ModifyEaddrRequest
{
	@NotBlank(message = "[이용자 공인전자주소] 값은 필수입니다.")
	@ApiModelProperty(value="이용자 공인전자주소")
	private String eaddr;

	@ApiModelProperty(value="이용자 명")
	private String name;
 
	@ApiModelProperty(value="이용자 유형(1:법인, 2:국가기관, 3:공공기관, 4:지자체, 5:개인사업자, 9:기타")
	private Integer type;

	@NotBlank(message = "[중계자시스템에서 이용자 정보가 변경된 일시] 값은 필수입니다.")
	@ApiModelProperty(value="중계자시스템에서 이용자 정보가 변경된 일시")
	private String updDate;
}
