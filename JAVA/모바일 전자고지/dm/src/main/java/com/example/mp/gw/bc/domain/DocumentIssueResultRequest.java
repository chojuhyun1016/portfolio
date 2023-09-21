package com.example.mp.gw.bc.domain;


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
 * @Class Name : DocumentIssueResultRequest.java
 * @Description : 유통증명서발급 결과 수신 처리 전송 (To. 비즈센터)
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"file_binary"})
@ApiModel(value = "유통증명서발급 결과 수신 처리", description = "유통증명서발급 결과 수신 처리 전송 (To. 비즈센터)")
public class DocumentIssueResultRequest
{
	@NotNull(message = "[발송요청관리번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청관리번호")
	private Integer sndn_mgnt_seq;

	@NotNull(message = "[발송요청일련번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청일련번호")
	private Integer sndn_seq_no;

	@NotBlank(message = "[발급번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발급번호")
	private String iss_seq;

	@NotBlank(message = "[유통증명서 파일명] 값은 필수입니다.")
	@ApiModelProperty(value = "유통증명서 파일명")
	private String file_name;

	@NotNull(message = "[유통증명서 파일의 binary 값] 값은 필수입니다.")
	@ApiModelProperty(value = "유통증명서 파일의 binary 값")
	private byte[] file_binary;

	@NotBlank(message = "[유통증명서 발급 성공 여부] 값은 필수입니다.")
	@ApiModelProperty(value = "유통증명서 발급 성공 여부")
	private String iss_result_cd;

	@ApiModelProperty(value = "유통증명서 발급 실패 사유")
	private String iss_result_msg;
}
