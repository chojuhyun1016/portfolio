package com.example.mp.gw.common.validator;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * @Class Name : YYYYMMDDHHMMSS.java
 * @Description : 날짜 체크  
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
 *  2021.04.14	    조주현           최초 생성
 * 
 *  </pre>
 * 
 */


@Documented
@Constraint(validatedBy = {YYYYMMDDHHMMSSValidator.class})
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface YYYYMMDDHHMMSS
{
	String message() default "날짜 형식이 올바르지 않습니다(YYYYMMDDHHMMSS)";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}
