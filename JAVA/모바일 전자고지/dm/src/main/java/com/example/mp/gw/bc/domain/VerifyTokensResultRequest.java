package com.example.mp.gw.bc.domain;


import java.util.List;

import javax.validation.constraints.NotEmpty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : VerifyTokenRequestToBizCenter.java
 * @Description : (열람확인대체시)열람확인결과수신 처리 전송 (To. 비즈센터)
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
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "(열람확인대체시)열람확인결과수신 처리", description = "(열람확인대체시)열람확인결과수신 처리 전송 (To. 비즈센터)")
public class VerifyTokensResultRequest
{
	@NotEmpty(message = "[배열] 값은 필수입니다.")
	@ApiModelProperty(value = "배열")
	List<VerifyTokenResult> reqs;
}
