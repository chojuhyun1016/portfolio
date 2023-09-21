package com.example.mp.gw.bc.domain;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : VerifyTokenRequestToBizCenter.java
 * @Description : (열람확인대체시)열람확인결과수신 처리 전송 객체
 * 
 * @author 조주현
 * @since 2021.05.06
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.05.06	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@Builder
@ToString
@ApiModel(value = "(열람확인대체시)열람확인결과수신 처리", description = "(열람확인대체시)열람확인결과수신 처리 전송 객체")
public class VerifyTokenResult
{
	@NotNull(message = "[발송요청관리번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청관리번호")
	private Integer sndn_mgnt_seq;

	@NotNull(message = "[발송요청일련번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청일련번호")
	private Integer sndn_seq_no;
 
	@NotBlank(message = "[열람타임스탬프] 값은 필수입니다.")
	@ApiModelProperty(value = "열람타임스탬프")
	private String mms_rdg_tmst;
 
	@ApiModelProperty(value = "수신자 공인전자주소")
	private String rcv_npost;
 
	@ApiModelProperty(value = "수신 중계자플랫폼ID")
	private String rcv_plfm_id;
}
