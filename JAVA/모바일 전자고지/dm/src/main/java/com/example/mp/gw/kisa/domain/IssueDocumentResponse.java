package com.example.mp.gw.kisa.domain;


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
 * @Class Name : RegisterDocCirculations.java
 * @Description : 전자문서 유통증명서 등록 응답 (From. KISA)
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
@ToString(exclude = {"fileBinary"})
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "전자문서 유통증명서 등록", description = "전자문서 유통증명서 등록 응답 (From. KISA)")
public class IssueDocumentResponse
{
	@NotNull(message = "[처리결과] 값은 필수입니다.")
	@ApiModelProperty(value="처리결과", example = "1:성공, 0:실패", required = true)
	private Integer resultCode;

	@ApiModelProperty(value="유통증명서 일련번호", required = false)
	private String certNum;

	@ApiModelProperty(value="오류코드", required = false)
	private String errCode;

	@ApiModelProperty(value="오류메시지", required = false)
	private String errMsg;

	@ApiModelProperty(value="파일 바이너리", required = false)
	byte[] fileBinary;
}
