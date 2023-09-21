package com.example.mp.gw.common.domain;


import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @Class Name : ApiResponseResult.java
 * @Description : 공통 API 응답 객체
 * 
 * @author 조주현
 * @since 2021.04.19
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.19	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Setter
@Getter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ApiResponseResult implements ApiResponse
{
	@NotBlank(message = "[처리코드] 값은 필수입니다.")
	@ApiModelProperty(value = "처리코드")
	private String result_cd;

	@NotBlank(message = "[처리일시] 값은 필수입니다.")
	@ApiModelProperty(value = "처리일시")
	private String result_dt;

	@ApiModelProperty(value = "배열")
	private List<Map<String, Object>> errors;

	@JsonIgnore
	private HttpStatus status;


	@Override
	public void setErrors(List<Map<String,Object>> errors)
	{
		this.errors = errors;
	}

	public List<Map<String, Object>> getErrors()
	{
		return CollectionUtils.isEmpty(errors)?Collections.emptyList():errors;
	}
}
