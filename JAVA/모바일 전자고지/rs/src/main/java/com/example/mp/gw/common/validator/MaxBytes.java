package com.example.mp.gw.common.validator;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * @Class Name : MaxBytes.java
 * @Description : String 바이트 validator 어노테이션
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


@Documented
@Constraint(validatedBy = {MaxBytesValidator.class})
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxBytes
{
	String message() default "제한 bytes를 초과하였습니다";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
	public long max() default 0l;
}
