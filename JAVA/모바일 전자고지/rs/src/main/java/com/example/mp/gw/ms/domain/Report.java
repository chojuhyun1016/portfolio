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
 * @Class Name : Report.java
 * @Description : 리포트 재전송 요청 객체
 * 
 * @author 조주현
 * @since 2023.04.24
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.24	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Setter	
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@ApiModel(value = "리포트 재전송 요청", description = "리포트 재전송 요청 객체")
public class Report
{
	@NotNull(message = "[발송요청관리번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청관리번호", required = true)
	private Integer sndn_mgnt_seq;

	@NotNull(message = "[발송요청일련번호(리스트순번)] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청일련번호(리스트순번)", required = true)
	private Integer sndn_seq_no;

	@NotBlank(message = "[발급요청 파티션 정보] 값은 필수입니다.")
	@ApiModelProperty(value = "발급요청 파티션 정보", required = true)
	private String part_mm;
}
