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

	// ��¥�� �α� �ڵ� ����
	// CD_LOG_DAY_TYPE(YYYYMMDD), 3(�α׷���), "./"(�α����� ���), "NLOG"(�α������̸� �պκ� ���ڿ�),
	// "DAY"(�α������̸� �޺κ� ���ڿ�)
	// �α� ���ϸ� : ./NLOG_YYYYMMDD_DAY.log
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

	// ��¥�� �α� �ڵ� ����
	// CD_LOG_DAY_TYPE(YYYYMMDD), 3(�α׷���), "./"(�α����� ���), "ELOG"(�α������̸� �պκ� ���ڿ�),
	// "DAY"(�α������̸� �޺κ� ���ڿ�)
	// �α� ���ϸ� : ./ELOG_YYYYMMDD_DAY.log
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

	// �α� ������ CDLogGetHandle() �� ����� �������� ũ�� �����Ƿ� �α� ���
	CDLog( NLOG, 3, "[%-15s 1][L:%d]\n", (char*)"Test Normal Log", __LINE__ );
	CDLog( ELOG, 3, "[%-15s 2][L:%d]\n", (char*)"Test Error Log", __LINE__ );

	// �α� ������ CDLogGetHandle() �� ����� �������� ũ�� ������ �α� ������� ����
	CDLog( NLOG, 4, "[%-15s 3][L:%d]\n", (char*)"Test Normal Log", __LINE__ );
	CDLog( ELOG, 4, "[%-15s 4][L:%d]\n", (char*)"Test Error Log", __LINE__ );

	// �α����� �ڵ� ����(free) 
	// Ps) �α� �ڵ� ������ Heap ������ �����ǹǷ� ����� ��� ������ �ݵ�� ����(free)
	CDLogDeleteHandle( NLOG );
	CDLogDeleteHandle( ELOG );

	// �ð��� �α� �ڵ� ����
	// CD_LOG_HOUR_TYPE(YYYYMMDDhh), 3(�α׷���), "./"(�α����� ���), "NLOG"(�α������̸� �պκ� ���ڿ�),
	// "HOUR"(�α������̸� �޺κ� ���ڿ�)
	// �α� ���ϸ� : ./NLOG_YYYYMMDDhh_HOUR.log
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

	// �ð��� �α� �ڵ� ����
	// CD_LOG_HOUR_TYPE(YYYYMMDDhh), 3(�α׷���), "./"(�α����� ���), "ELOG"(�α������̸� �պκ� ���ڿ�),
	// "HOUR"(�α������̸� �޺κ� ���ڿ�)
	// �α� ���ϸ� : ./ELOG_YYYYMMDDhh_HOUR.log
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

	// �α� ������ CDLogGetHandle() �� ����� �������� ũ�� �����Ƿ� �α� ���
	CDLog( NLOG, 2, "[%-15s 5][L:%d]\n", (char*)"Test Normal Log", __LINE__ );
	CDLog( ELOG, 2, "[%-15s 6][L:%d]\n", (char*)"Test Error Log", __LINE__ );

	// �α� ������ CDLogGetHandle() �� ����� �������� ũ�� ������ �α� ������� ����
	CDLog( NLOG, 5, "[%-15s 7][L:%d]\n", (char*)"Test Normal Log", __LINE__ );
	CDLog( ELOG, 5, "[%-15s 8][L:%d]\n", (char*)"Test Error Log", __LINE__ );

	// �α����� �ڵ� ����(free)
	// Ps) �α� �ڵ� ������ Heap ������ �����ǹǷ� ����� ��� ������ �ݵ�� ����(free)
	CDLogDeleteHandle( NLOG );
	CDLogDeleteHandle( ELOG );

	return 0;
}
