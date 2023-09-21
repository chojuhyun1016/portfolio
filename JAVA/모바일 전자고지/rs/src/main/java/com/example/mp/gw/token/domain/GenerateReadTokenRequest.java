package com.example.mp.gw.token.domain;


import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : GenerateReadTokenRequest.java
 * @Description : 토큰발급 요청 (From. Web)
 * 
 * @author 조주현
 * @since 2021.04.22
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.22	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@ApiModel(value = "토큰발급", description = "토큰발급 요청 (From. Web)")
public class GenerateReadTokenRequest
{
	@NotBlank(message = "[메시지 아이디] 값은 필수입니다.")
	@ApiModelProperty(value = "메시지 아이디", required = true)
	private String messageId;

	@NotBlank(message = "[메시지 수신 시간] 값은 필수입니다.")
	@ApiModelProperty(value = "메시지 수신 시간", notes = "메시지 수신 시간", example = "\"20210101000000\"", required = true)
	private String regDt;
}
