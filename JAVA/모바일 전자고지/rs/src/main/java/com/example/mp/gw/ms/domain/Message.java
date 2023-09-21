package com.example.mp.gw.ms.domain;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.example.mp.gw.common.validator.MaxBytes;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : Message.java
 * @Description : 본문자발송 등록 요청 객체
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
 *  2021.03.25	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Setter	
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"mms_binary", "mms_dtl_cnts", "mms_title"})
@ApiModel(value = "본문자 발송 등록", description = "본문자 발송 등록 요청 객체")
public class Message
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

	@ApiModelProperty(value = "RCS 메시지 상세내용", required = true)
	private String rcs_dtl_cnts;

	@ApiModelProperty(value = "연결 URL", required = true)
	private String url;

	@NotBlank(message = "[문서해시] 값은 필수입니다.")
	@ApiModelProperty(value = "문서해시", required = true)
	private String doc_hash;

	@ApiModelProperty(value = "MSS 바이너리")
	@MaxBytes(max = 500000l, message = "[MMS 바이너리] 값의 제한 크기(500,000 bytes)를 초과합니다.")
	private String mms_binary;

	@ApiModelProperty(value = "MMS 바이너리 파일포맷(확장자)")
	private String file_fmat;	

	@ApiModelProperty(value = "개인휴대전화번호")	
	private String mdn;

	@ApiModelProperty(value = "유통정보생성여부")	
	private String dist_info_crt_yn;

	@ApiModelProperty(value = "안내문 확인하기 치환문구")	
	private String info_cfrm_str;

	@ApiModelProperty(value = "수신거부 및 수신 휴대폰 지정하기 치환문구")	
	private String rcve_rf_str;
}
