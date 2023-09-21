package com.example.mp.gw.bc.domain;


import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : SearchRejectionRequest.java
 * @Description : 수신거부조회 조회 요청 (To. 비즈센터)
 * 
 * @author 조주현
 * @since 2021.04.17
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.17	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@ApiModel(value = "수신거부 조회", description = "수신거부 조회 요청 (To. 비즈센터)")
public class StatusRejectionRequest
{
	@NotBlank(message = "[개인식별코드] 값은 필수입니다.")
	@ApiModelProperty(value = "개인식별코드")
	private String ci;
}
