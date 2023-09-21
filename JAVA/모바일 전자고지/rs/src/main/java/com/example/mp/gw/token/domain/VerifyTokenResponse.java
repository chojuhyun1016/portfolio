package com.example.mp.gw.token.domain;


import javax.validation.constraints.NotNull;

import com.example.mp.gw.common.domain.ApiResponseResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @Class Name : VerifyTokenResponse.java
 * @Description : 토큰인증확인 조회 응답 (To. 비즈센터)
 * 
 * @author 조주현
 * @since 2023.09.12
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2023.09.12	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@ApiModel(value = "토큰인증확인 조회", description = "토큰인증확인 조회 응답 (To. 비즈센터)")
public class VerifyTokenResponse extends ApiResponseResult
{
	@NotNull(message = "[발송요청관리번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청관리번호")
	private Integer sndn_mgnt_seq;
 
	@NotNull(message = "[발송요청일련번호] 값은 필수입니다.")
	@ApiModelProperty(value = "발송요청일련번호")
	private Integer sndn_seq_no;
}
