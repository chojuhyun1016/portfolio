package com.example.mp.gw.ms.domain;


import java.util.List;

import javax.validation.Valid;
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
 * @Class Name : ReportRequest.java
 * @Description : 리포트 요청 (From. 비즈센터)
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
 *  2023.04.24	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@ApiModel(value = "리포트 요청", description = "리포트 요청 (From. 비즈센터)")
public class ReportRequest
{
	@Valid
	@NotEmpty(message = "[배열] 값은 필수입니다.")
	@ApiModelProperty(value = "배열")
	private List<Report> reqs;
}
