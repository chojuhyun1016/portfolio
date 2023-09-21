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
 * @Class Name : RegisterWhitelistsRequest.java
 * @Description : Whitelist 등록 요청 (To. 비즈센터)
 * 
 * @author 조주현
 * @since 2021.07.07
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.07.07	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */

@Getter
@Setter
@Builder
@ToString
@ApiModel(value = "Whitelist 등록", description = "Whitelist 등록 요청 (To. 비즈센터)")
public class RegisterWhitelistsRequest
{
	@NotBlank(message = "[모바일사업자구분] 값은 필수입니다.")
	@ApiModelProperty(value = "모바일사업자구분")
	private String mbl_bzowr_dvcd;

	@Valid
	@Size(max = 200, message = "[배열] 값은 최대 200건입니다.")
	@NotEmpty(message = "[배열] 값은 필수입니다.")
	@ApiModelProperty(value = "배열")
	private List<RegisterWhitelist> reqs;
}
