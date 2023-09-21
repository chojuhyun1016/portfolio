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
 * @Class Name : IssueDocumentStatConfirmationResponse.java
 * @Description : 전자문서 유통정보 수치 확인서 발급 응답 (From. KISA)
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
@Builder
@ToString(exclude = {"fileBinary"})
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "전자문서 유통정보 수치 확인서 발급", description = "전자문서 유통정보 수치 확인서 발급 응답 (From. KISA)")
public class IssueDocumentStatConfirmationResponse
{
	@NotNull(message = "[처리결과 (1:성공, 0:실패)] 값은 필수입니다.")
	@ApiModelProperty(value="처리결과 (1:성공, 0:실패)", required = true)
	private Integer resultCode;

	@ApiModelProperty(value="조회대상 공인전자주소", required = false)
	String eaddr;

	@ApiModelProperty(value="공인전자주소 등록자명", required = false)
	String name;

	@ApiModelProperty(value="조회대상 월 (yyyy-mm 형식)", required = false)
	String period;

	@ApiModelProperty(value="기간 내 발송한 문서 중 수신건수", required = false)
	private Integer recvCountFromSent;

	@ApiModelProperty(value="기간 내 발송한 문서 중 열람건수", required = false)
	private Integer readCountFromSent;

	@ApiModelProperty(value="기간 내 수신된 시점 기준 수신건수", required = false)
	private Integer recvCount;

	@ApiModelProperty(value="기간 내 열람된 시점 기준 열람건수", required = false)
	private Integer readCount;

	@ApiModelProperty(value="오류코드", required = false)
	private String errCode;

	@ApiModelProperty(value="오류메시지", required = false)
	private String errMsg;

	@ApiModelProperty(value="파일 바이너리", required = false)
	byte[] fileBinary;
}
