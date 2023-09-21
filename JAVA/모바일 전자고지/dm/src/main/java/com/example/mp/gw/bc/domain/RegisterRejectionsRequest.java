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

/**
 * @Class Name : RegisterRejectionsRequest.java
 * @Description : 수신거부 등록 요청 (To. 비즈센터)
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
@Builder
@ApiModel(value = "수신거부 등록", description = "수신거부 등록 요청 (To. 비즈센터)")
public class RegisterRejectionsRequest
{
	@NotBlank(message = "[모바일사업자구분] 값은 필수입니다.")
	@ApiModelProperty(value = "모바일사업자구분")
	private String mbl_bzowr_dvcd;

	@Valid
	@NotEmpty(message = "[배열] 값은 필수입니다.")
	@Size(max = 200, message = "[배열] 값은 최대 200건입니다.")
	@ApiModelProperty(value = "배열")
	private List<RegisterRejection> reqs;
}
