package com.example.mp.gw.token.domain;


import javax.validation.constraints.NotBlank;

import com.example.mp.gw.common.validator.YYYYMMDDHHMMSS;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : VerifyReadTokenRequest.java
 * @Description : 토큰인증대체 요청 (From. Web)
 * 
 * @author 조주현
 * @since 2021.12.16
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.16	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@ApiModel(value = "토큰인증대체", description = "토큰인증대체 요청 (From. Web)")
public class VerifyReplaceTokenRequest
{
	@NotBlank(message = "[메시지 아이디] 값은 필수입니다.")
	@ApiModelProperty(value = "메시지 아이디", required = true)
	private String messageId;

	@NotBlank(message = "[메시지 수신 시간] 값은 필수입니다.")
	@ApiModelProperty(value = "메시지 수신 시간", notes = "메시지 수신 시간", example = "\"20210101000000\"", required = true)
	private String regDt;

	@NotBlank(message = "[열람타임스탬프] 값은 필수입니다.")
	@YYYYMMDDHHMMSS(message = "[열람타임스탬프] 값의 형식이 올바르지 않습니다.")
	@ApiModelProperty(value = "열람타임스탬프", required = true)
	private String mms_rdg_tmst;
}
