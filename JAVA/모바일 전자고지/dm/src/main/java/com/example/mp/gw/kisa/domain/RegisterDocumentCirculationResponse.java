package com.example.mp.gw.kisa.domain;


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
 * @Class Name : RegisterDocCirculations.java
 * @Description : 전자문서 유통정보 [등록/열람일시 등록] 응답 객체
 * 
 * @author 조주현
 * @since 2021.08.18
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.08.18	    조주현          최초 생성
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
@ApiModel(value = "전자문서 유통정보 [등록/열람일시 등록]", description = "전자문서 유통정보 [등록/열람일시 등록] 응답 객체")
public class RegisterDocumentCirculationResponse
{
	@NotBlank(message = "[각 유통정보 별 처리결과] 값은 필수입니다.")
	@ApiModelProperty(value="전자문서번호", required = false)
	private String edocNum;

	@NotNull(message = "[각 유통정보 별 처리결과] 값은 필수입니다.")
	@ApiModelProperty(value="각 유통정보 별 처리결과", required = false)
	private Integer result;

	@JsonIgnore
	@ApiModelProperty(value="발송요청관리번호_발송요청일련번호", required = false)
	private String messageId;
}
