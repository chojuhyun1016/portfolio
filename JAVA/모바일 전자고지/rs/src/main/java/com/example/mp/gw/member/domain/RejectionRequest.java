package com.example.mp.gw.member.domain;


import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : RejectionRequest.java
 * @Description : 수신거부 등록(신청/해지) 요청 (From. Web) 
 * 
 * @author 조주현
 * @since 2021.03.25
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.25	    조주현           최초 생성
 * 
 *  </pre>
 * 
 */


@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "수신거부 등록(신청/해지)", description = "수신거부 등록(신청/해지) 요청 (From. Web)")
public class RejectionRequest
{
	@NotBlank(message = "[전화번호] 값은 필수입니다.")
	@Pattern(message = "[전화번호] 값의 형식이 올바르지 않습니다.", regexp = "^\\d{3}\\d{3,4}\\d{4}$")
	@ApiModelProperty(value = "전화번호", notes = "전화번호 입력", example = "01012341234", required = true)
	private String phone;

	@NotBlank(message = "[개인식별코드] 값은 필수입니다.")
	@ApiModelProperty(value = "CI", required = true)
	private String ci;

	@ApiModelProperty(value = "메시지 일련번호", example="\"messageId\"", required = false)
	private String messageId;

	@NotBlank(message = "[신청일시] 값은 필수입니다.")
	@ApiModelProperty(value = "신청일시", required = false)
	private String apctDtm;

	@NotEmpty(message = "[법인 목록] 값은 필수입니다.")
	@ApiModelProperty(value = "법인 목록", notes = "법인 아이디 목록 (Array)", example = "[\"법인1\",\"법인2\"]")
	private List<String> rejections;
}
