package com.example.mp.gw.bc.domain;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : ReportResult.java
 * @Description :  발송/수신 결과 조회 전송 객체
 * 
 * @author 조주현
 * @since 2021.04.06
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.06	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "발송/수신 결과 조회", description = "발송/수신 결과 조회 전송 객체")
public class MessageResult
{
	@NotNull(message = "[발송요청관리번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청관리번호")
	private Integer sndn_mgnt_seq;

	@NotNull(message = "[발송요청일련번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청일련번호")
	private Integer sndn_seq_no;

	@NotNull(message = "[MMS발송결과상태순번] 값은 필수입니다.")
	@ApiModelProperty(value = "MMS발송결과상태순번")
	private Integer mms_sndg_rslt_sqno;

	@ApiModelProperty(value = "실제발송번호(일부)")
	private String rl_mms_sndg_telno;

	@NotBlank(message = "[결과코드] 값은 필수입니다.")
	@ApiModelProperty(value = "결과코드")
	private String mms_rslt_dvcd;

	@NotBlank(message = "[결과타임스탬프] 값은 필수입니다.")
	@ApiModelProperty(value = "결과타임스탬프")
	private String mms_rslt_tmst;

	@ApiModelProperty(value = "발송 메시지 타입")
	private Integer msg_type;

	@ApiModelProperty(value = "기동의 발송여부")
	private String prev_approve_yn;

	@ApiModelProperty(value = "수신자 공인전자주소")
	private String rcv_npost;	

	@ApiModelProperty(value = "수신 중계자플랫폼ID")
	private String rcv_plfm_id;

	@ApiModelProperty(value = "클릭일시")
	private String click_dt;	

	@ApiModelProperty(value = "동의일시")
	private String approve_dt;

	@ApiModelProperty(value = "저장시간")
	private String part_mm;

	@ApiModelProperty(value = "RCS 발송 여부")
	private String rcs_yn;

	@JsonIgnore
	@ApiModelProperty(value = "메세지키")
	private String msg_key;

	@JsonIgnore
	@ApiModelProperty(value = "발송요청관리번호_방송요청일련번호")
	private String message_id;

	@JsonIgnore
	@ApiModelProperty(value = "다회선 사용자 처리여부")
	private String multi_mbl_prc_type;
}
