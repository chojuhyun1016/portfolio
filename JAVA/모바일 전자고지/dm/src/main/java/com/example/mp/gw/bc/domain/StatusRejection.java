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
 * @Class Name : RejectionStatus.java
 * @Description : 수신거부 조회 응답 (From. 비즈센터)
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


@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "수신거부 조회", description = "수신거부 조회 응답 (From. 비즈센터)")
public class StatusRejection
{
	@NotBlank(message = "[서비스코드] 값은 필수입니다.")
	@ApiModelProperty(value = "서비스코드")
	private String service_cd;
 
	@NotBlank(message = "[신청일자] 값은 필수입니다.")
	@ApiModelProperty(value = "신청일자")
	private String apct_dt;
 
	@NotBlank(message = "[신청구분] 값은 필수입니다.")
	@ApiModelProperty(value = "신청구분")
	private String apct_acct_cls;
}
