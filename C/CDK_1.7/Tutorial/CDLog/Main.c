#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/atomic.h>

#include "Main.h"
#include "CDLog.h"

int main( int argc, char **argv )
{
	int				iResult;
	int				iSequence	= 0;

	CD_LOG_HANDLE	NLOG;
	CD_LOG_HANDLE	ELOG;

	// 날짜별 로그 핸들 생성
	// CD_LOG_DAY_TYPE(YYYYMMDD), 3(로그레벨), "./"(로그파일 경로), "NLOG"(로그파일이름 앞부분 문자열),
	// "DAY"(로그파일이름 뒷부분 문자열)
	// 로그 파일명 : ./NLOG_YYYYMMDD_DAY.log
	if( ( NLOG = CDLogGetHandle( CD_LOG_DAY_TYPE, 3, (char*)"./", (char*)"NLOG", (char*)"DAY" ) ) == NULL )
	{
		::fprintf( stderr, "[CD_LOG_HANDLE( %d 3 %s %s %s ) Error][E:%d][L:%d]\n", 
			CD_LOG_DAY_TYPE, 
			(char*)"./", 
			(char*)"NLOG", 
			(char*)"A_1A", 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	// 날짜별 로그 핸들 생성
	// CD_LOG_DAY_TYPE(YYYYMMDD), 3(로그레벨), "./"(로그파일 경로), "ELOG"(로그파일이름 앞부분 문자열),
	// "DAY"(로그파일이름 뒷부분 문자열)
	// 로그 파일명 : ./ELOG_YYYYMMDD_DAY.log
	if( ( ELOG = CDLogGetHandle( CD_LOG_DAY_TYPE, 3, (char*)"./", (char*)"ELOG", (char*)"DAY" ) ) == NULL )
	{
		::fprintf( stderr, "[CD_LOG_HANDLE( %d 3 %s %s %s ) Error][E:%d][L:%d]\n", 
			CD_LOG_DAY_TYPE, 
			(char*)"./", 
			(char*)"ELOG", 
			(char*)"A_1A", 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	// 로그 레벨이 CDLogGetHandle() 때 등록한 레벨보다 크지 않으므로 로그 출력
	CDLog( NLOG, 3, "[%-15s 1][L:%d]\n", (char*)"Test Normal Log", __LINE__ );
	CDLog( ELOG, 3, "[%-15s 2][L:%d]\n", (char*)"Test Error Log", __LINE__ );

	// 로그 레벨이 CDLogGetHandle() 때 등록한 레벨보다 크기 때문에 로그 출력하지 않음
	CDLog( NLOG, 4, "[%-15s 3][L:%d]\n", (char*)"Test Normal Log", __LINE__ );
	CDLog( ELOG, 4, "[%-15s 4][L:%d]\n", (char*)"Test Error Log", __LINE__ );

	// 로그파일 핸들 해제(free) 
	// Ps) 로그 핸들 정보는 Heap 공간에 생성되므로 사용이 모두 끝나면 반드시 해제(free)
	CDLogDeleteHandle( NLOG );
	CDLogDeleteHandle( ELOG );

	// 시간별 로그 핸들 생성
	// CD_LOG_HOUR_TYPE(YYYYMMDDhh), 3(로그레벨), "./"(로그파일 경로), "NLOG"(로그파일이름 앞부분 문자열),
	// "HOUR"(로그파일이름 뒷부분 문자열)
	// 로그 파일명 : ./NLOG_YYYYMMDDhh_HOUR.log
	if( ( NLOG = CDLogGetHandle( CD_LOG_HOUR_TYPE, 3, (char*)"./", (char*)"NLOG", (char*)"HOUR" ) ) == NULL )
	{
		::fprintf( stderr, "[CD_LOG_HANDLE( %d 3 %s %s %s ) Error][E:%d][L:%d]\n", 
			CD_LOG_HOUR_TYPE, 
			(char*)"./", 
			(char*)"NLOG", 
			(char*)"A_1A", 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	// 시간별 로그 핸들 생성
	// CD_LOG_HOUR_TYPE(YYYYMMDDhh), 3(로그레벨), "./"(로그파일 경로), "ELOG"(로그파일이름 앞부분 문자열),
	// "HOUR"(로그파일이름 뒷부분 문자열)
	// 로그 파일명 : ./ELOG_YYYYMMDDhh_HOUR.log
	if( ( ELOG = CDLogGetHandle( CD_LOG_HOUR_TYPE, 3, (char*)"./", (char*)"ELOG", (char*)"HOUR" ) ) == NULL )
	{
		::fprintf( stderr, "[CD_LOG_HANDLE( %d 3 %s %s %s ) Error][E:%d][L:%d]\n", 
			CD_LOG_HOUR_TYPE, 
			(char*)"./", 
			(char*)"ELOG", 
			(char*)"A_1A", 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	// 로그 레벨이 CDLogGetHandle() 때 등록한 레벨보다 크지 않으므로 로그 출력
	CDLog( NLOG, 2, "[%-15s 5][L:%d]\n", (char*)"Test Normal Log", __LINE__ );
	CDLog( ELOG, 2, "[%-15s 6][L:%d]\n", (char*)"Test Error Log", __LINE__ );

	// 로그 레벨이 CDLogGetHandle() 때 등록한 레벨보다 크기 때문에 로그 출력하지 않음
	CDLog( NLOG, 5, "[%-15s 7][L:%d]\n", (char*)"Test Normal Log", __LINE__ );
	CDLog( ELOG, 5, "[%-15s 8][L:%d]\n", (char*)"Test Error Log", __LINE__ );

	// 로그파일 핸들 해제(free)
	// Ps) 로그 핸들 정보는 Heap 공간에 생성되므로 사용이 모두 끝나면 반드시 해제(free)
	CDLogDeleteHandle( NLOG );
	CDLogDeleteHandle( ELOG );

	return 0;
}

