package com.example.mp.gw.common.validator;


import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;


/**
 * @Class Name : YYYYMMDDHHMMSSValidator.java
 * @Description : YYYYMMDDHHMMSS validator
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


public class YYYYMMDDHHMMSSValidator implements ConstraintValidator<YYYYMMDDHHMMSS, String>
{
	final String PATTERN = "^((19|20)\\d\\d)(0?[1-9]|1[012])(0?[1-9]|[12][0-9]|3[01])([01]?\\d|2[0-3])([0-5]?\\d)([0-5]?\\d)$";

	/**
	 * @param value
	 * @param context
	 * @return
	 */
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context)
	{
		if(!StringUtils.hasText(value))
			return true;

		return Pattern.matches(PATTERN, value);
	}
}
