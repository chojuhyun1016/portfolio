package com.example.mp.gw.common.validator;


import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;

/**
 * @Class Name : MaxBytesValidator.java
 * @Description : String 바이트 validator
 * 
 * @author 조주현
 * @since 2021.04.14
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.14	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class MaxBytesValidator implements ConstraintValidator<MaxBytes, String>
{
	private long max;

	/**
	 * @param constraintAnnotation
	 */
	@Override
	public void initialize(MaxBytes constraintAnnotation)
	{
		max = constraintAnnotation.max();
	}

	/**
	 * @param value
	 * @param context
	 * @return
	 */
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context)
	{
		return StringUtils.hasText(value) ? (value.getBytes().length <= max) : true;
	}
}
