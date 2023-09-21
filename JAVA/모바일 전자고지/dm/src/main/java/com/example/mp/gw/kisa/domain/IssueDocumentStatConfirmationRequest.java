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
 * @Class Name : IssueDocumentStatConfirmationRequest.java
 * @Description : 전자문서 유통정보 수치 확인서 발급 요청 (To. KISA)
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
@ApiModel(value = "전자문서 유통정보 수치 확인서 발급", description = "전자문서 유통정보 수치 확인서 발급 요청 (To. KISA)")
public class IssueDocumentStatConfirmationRequest
{
	@NotBlank(message = "[조회대상 공인전자주소] 값은 필수입니다.")
	@ApiModelProperty(value="조회대상 공인전자주소", required = true)
	String eaddr;

	@NotBlank(message = "[조회대상 월 (yyyy-mm 형식)] 값은 필수입니다.")
	@ApiModelProperty(value="조회대상 월 (yyyy-mm 형식)", required = true)
	String period;
}
