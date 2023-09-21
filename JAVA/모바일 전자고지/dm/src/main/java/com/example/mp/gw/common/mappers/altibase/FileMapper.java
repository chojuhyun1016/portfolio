package com.example.mp.gw.common.mappers.altibase;


import org.springframework.dao.DataAccessException;

import com.example.mp.gw.member.domain.Member;

/**
 * @Class Name : FileMapper.java
 * @Description : 파일 매퍼
 * 
 * @author 조주현
 * @since 2021.03.26
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.26	    조주현           최초 생성
 * 
 *  </pre>
 * 
 */


public interface FileMapper
{
	public Integer uploadMemberFiles(Member member) throws DataAccessException;
}
