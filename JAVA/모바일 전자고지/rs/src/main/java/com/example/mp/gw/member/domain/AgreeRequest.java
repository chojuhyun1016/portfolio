package com.example.mp.gw.member.domain;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * @Class Name : AgreeRequest.java
 * @Description : 수신동의 요청 (From. Web)
 * 
 * @author 조주현
 * @since 2022.05.16
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.05.16	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@ApiModel(value = "수신동의", description = "수신동의 요청 (From. Web)")
public class AgreeRequest
{
	@NotBlank(message = "[개인휴대전화번호] 값은 필수입니다.")
	@Pattern(message = "[개인휴대전화번호] 형식이 올바르지 않습니다.", regexp = "^\\d{3}\\d{3,4}\\d{4}$")
	@ApiModelProperty(value="개인휴대전화번호", notes="개인휴대전화번호", example = "01080801234", required = true)
	private String phone;

	@NotBlank(message = "[개인식별코드] 값은 필수입니다.")
	@ApiModelProperty(value = "CI", required = true)
	private String ci;

	@NotBlank(message = "[기관코드] 값은 필수입니다.")
	@ApiModelProperty(value = "BizCenter에서 발행한 기관의 서비스 코드", required = true)
	private String svcOrgCd;

	@ApiModelProperty(value = "발송요청관리번호_발송요청일련번호(리스트순번)", notes = "발송요청관리번호_발송요청일련번호(일련번호(0~99999999))", example = "1_1", required = true)
	private String messageId;

	@NotBlank(message = "[신청일시] 값은 필수입니다.")
	@ApiModelProperty(value = "신청일시", notes = "수신동의 등록/해제 발생일시", example = "20220101000000", required = true)
	private String approveDt;
}
