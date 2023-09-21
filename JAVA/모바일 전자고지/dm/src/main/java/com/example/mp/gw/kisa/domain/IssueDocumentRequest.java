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
 * @Class Name : RegisterDocCirculations.java
 * @Description : 전자문서 유통증명서 등록 요청 (To. KISA)
 * 
 * @author 조주현
 * @since 2021.08.18
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.08.18	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "전자문서 유통증명서 등록", description = "전자문서 유통증명서 등록 요청 (To. KISA)")
public class IssueDocumentRequest
{
	@NotBlank(message = "[전자문서번호] 값은 필수입니다.")
	@ApiModelProperty(value="전자문서번호", required = true)
	private String edocNum;
	
	@NotBlank(message = "[유통증명서 요청 이용자 공인전자주소] 값은 필수입니다.")
	@ApiModelProperty(value="유통증명서 요청 이용자 공인전자주소", required = true)
	private String eaddr;
	
	@ApiModelProperty(value="유통증명서 발급요청 사유", required = false)
	private String reason;
}
