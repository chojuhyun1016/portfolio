package com.example.mp.gw.bc.domain;


import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : ApprovesRequest.java
 * @Description : 수신동의(등록/해제) 요청 전송 (To. 비즈센터)
 * 
 * @author 조주현
 * @since 2022.05.12
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.05.12	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@Builder
@ToString
@ApiModel(value = "수신동의(등록/해제) 정보", description = "수신동의(등록/해제) 정보 전송 (To. 비즈센터)")
public class ApprovesRequest
{
	@NotBlank(message = "[모바일사업자구분] 값은 필수입니다.")
	@ApiModelProperty(value = "모바일사업자구분", notes = "01:KT, 02:SKT, 03:LGT", example = "\"03\"")
	private String mbl_bzowr_dvcd;

	@Valid
	@Size(max = 200, message = "[배열] 값은 최대 200건입니다.")
	@NotEmpty(message = "[배열] 값은 필수입니다.")
	@ApiModelProperty(value = "배열")
	private List<Approve> reqs;
}
