package com.example.mp.gw.member.domain;


import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : CheckLguplusMemberResult.java
 * @Description : 수신자 이동통신 가입여부조회 응답 (To. 비즈센터)
 * 
 * @author 조주현
 * @since 2022.03.17
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.03.17	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@Builder
@ApiModel(value = "수신자 이동통신 가입여부조회", description = "수신자 이동통신 가입여부조회 응답 (To. 비즈센터)")
public class CheckLguplusMemberResult
{
	@NotBlank(message = "[개인식별코드] 값은 필수입니다.")
	@ApiModelProperty(value = "개인식별코드")
	private String ci; 

	@NotBlank(message = "[가입여부] 값은 필수입니다.")
	@ApiModelProperty(value = "가입여부")
	private String mdn_yn;
}
