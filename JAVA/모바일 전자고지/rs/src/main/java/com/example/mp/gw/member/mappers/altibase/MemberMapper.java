package com.example.mp.gw.member.mappers.altibase;


import java.util.List;

import org.springframework.dao.DataAccessException;

import com.example.mp.gw.member.domain.Member;

/**
 * @Class Name : MemberMapper.java
 * @Description : 회원 관련 매퍼 
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
 *  2021.03.26	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public interface MemberMapper
{
	public Member findPersonMemberByCi(String ci) throws DataAccessException;

	public String getWithdrawCeaByCi(String ci) throws DataAccessException;

	public int getCeaCountByCi(String ci) throws DataAccessException;

	public int getCorpMembersCountBySvcOrgCd(List<String> rejections) throws DataAccessException;

	public int existsSameCorpMemberBySvcOrgCd(Member member) throws DataAccessException;
}
