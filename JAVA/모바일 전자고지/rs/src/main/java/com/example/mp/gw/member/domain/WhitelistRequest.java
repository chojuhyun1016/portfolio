package com.example.mp.gw.member.domain;


import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : WhiteListRequest.java
 * @Description : 화이트 리스트 등록(신청/해지) 요청 (From. Web)  
 *
 * @author 조주현
 * @since 2021.12.28
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.28	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "화이트 리스트 등록(신청/해지)", description = "화이트 리스트 등록(신청/해지) 요청 (From. Web)")
public class WhitelistRequest
{
	@NotBlank(message = "[개인식별코드] 값은 필수입니다.")
	@ApiModelProperty(value = "CI", required = true)
	private String ci;

	@NotEmpty(message = "[법인 목록] 값은 필수입니다.")
	@ApiModelProperty(value = "법인 목록", notes = "법인 아이디 목록 (Array)", example = "[\"법인1\",\"법인2\"]")
	private List<String> whitelists;

	@NotBlank(message = "[모바일 사업자 구분] 값은 필수입니다")
	@ApiModelProperty(value = "모바일 사업자 구분", example = "[\"01\",\"02\",\"03\"]", required = true)	
	private String carrier;

	@ApiModelProperty(value = "전화번호", notes = "전화번호 입력", example = "01012341234", required = true)
	private String phone;

	@ApiModelProperty(value = "유입구분", example = "[\"\",\"21\"]", required = true)	
	private String inCls;

	@ApiModelProperty(value = "메시지 일련번호", example="\"messageId\"", required = true)
	private String messageId;
}
