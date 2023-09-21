package com.example.mp.gw.backup.mappers.altibase;


import java.util.Map;

import org.springframework.dao.DataAccessException;


public interface BackupMapper
{
	// 메시지 백업 테이블 데이터 이관
	public int insertBackupPmMsg(Map<String, String> target) throws DataAccessException;

	// 메시지 테이블 이관 데이터 삭제
	public int deletePmMsg(Map<String, String> target) throws DataAccessException;

	// 유통증명서 백업 테이블 데이터 이관
	public int insertBackupMpDcmntInfo(Map<String, String> target) throws DataAccessException;

	// 유통증명서 테이블 이관 데이터 삭제
	public int deleteMpDcmntInfo(Map<String, String> target) throws DataAccessException;

	// 리마인드 메시지 테이블 데이터 삭제
	public int deletePmRemindMsg(Map<String, String> target) throws DataAccessException;
}
