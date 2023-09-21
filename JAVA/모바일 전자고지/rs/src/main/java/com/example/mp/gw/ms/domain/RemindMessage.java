package com.example.mp.gw.ms.domain;


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
 * @Class Name : RemindMessage.java
 * @Description : 리마인드 문자 발송 등록 요청 객체
 * 
 * @author 조주현
 * @since 2023.01.20
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2023.01.20	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Setter	
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"mms_dtl_cnts", "mms_title"})
@ApiModel(value = "리마인드 문자 발송 등록", description = "리마인드 문자 발송 등록 요청 객체")
public class RemindMessage
{
	@NotNull(message = "[발송요청일련번호(리스트순번)] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청일련번호(리스트순번)", required = true)
	private Integer sndn_seq_no;

	@NotBlank(message = "[개인식별코드] 값은 필수입니다.")
	@ApiModelProperty(value = "개인식별코드", required = true)
	private String ci;

	@NotBlank(message = "[MMS 상세내용] 값은 필수입니다.")
	@ApiModelProperty(value = "MMS 상세내용", required = true)
	private String mms_dtl_cnts;

	@ApiModelProperty(value = "MMS 제목")
	private String mms_title;

	@ApiModelProperty(value = "개인휴대전화번호")	
	private String mdn;
}
