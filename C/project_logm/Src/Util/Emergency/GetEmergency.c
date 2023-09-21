#include "GetEmergency.h"

// 기본
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <strings.h>

// Shared Memory 관련
#include <sys/types.h>
#include <sys/shm.h>

// ProcessCheck 관련
#include <fcntl.h>
#include <dirent.h>
#include <procfs.h>

int		*g_ipEmergencyMode;

int main( int argc, char **argv )
{
	int	iResult;
	int	iSharedID;
	int	iEmergencyKey;

	// 1. Agrument 검사
	if( argc != 1 )
	{
		fprintf( stderr, "[인자값 이상][ex: GET_EMERGENCY][L: %d]\n", __LINE__ );

		exit( -1 );
	}

	// 2. 동일한 이름의 Process가 실행되고 있는지 체크
	if( ( iResult = ProcessCheck( (char*)GET_EMERGENCY_PROCESS_NAME ) ) != 1 )
	{
		fprintf( stderr, "[동일한 프로세스가 실행중][R:%d][L:%d]\n", iResult, __LINE__  );

		exit( -1 );
	}

	// 3. GET_EMERGENCY_SHARED_KEY 값의 공유메모리 공간이 존재하는지 체크 
	if( ( iSharedID = shmget( GET_EMERGENCY_SHARED_KEY, 0, 0 ) ) < 0 )
	{
		fprintf( stderr, "[공유메모리 공간 없음][R:%d][E:%d][L:%d]\n", iSharedID, errno, __LINE__  );

		exit( -1 );
	}

	// 4. GET_EMERGENCY_SHARED_KEY 값의 공유메모리 공간이 존재하면 연결
	if( ( g_ipEmergencyMode = (int*)shmat( iSharedID, (char*)0, 0 ) ) == NULL )
	{
		fprintf( stderr, "[공유메모리 접속 실패][R:%d][E:%d][L:%d]\n", iSharedID, errno, __LINE__ );

		exit( -1 );
	}

	// 5. 공유메모리 공간의 값(Emergency Mode)를 화면에 출력
	fprintf( stderr, "[Emergency Mode : %d][L:%d]\n", *g_ipEmergencyMode, __LINE__ );

	// 6. 성공!!
	return	0;
}


int	ProcessCheck( char* _cpProcessName )
{
	int				iFileFd			=	0;
	int				iProcessCount	=	0;

	char			caProcFile[512];

	struct dirent*	stpDirData		=	NULL;
	DIR*			dpDirPoint		=	NULL;
	psinfo_t		stPinfo;

	if( ( dpDirPoint = opendir( "/proc" ) ) == NULL )
	{
		fprintf( stderr, "[opendir( /proc) Error][E:%d][L:%d]\n", errno, __LINE__ );

		return	-1;
	}

	while( 1 )
	{
		stpDirData = readdir( dpDirPoint );

		if( !stpDirData )
			break;

		if( strcmp( stpDirData->d_name, "." ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, ".." ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, "0" ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, "1" ) == 0 )
			continue;

		sprintf( caProcFile, "/proc/%s/psinfo", stpDirData->d_name );
		
		if( ( iFileFd = open( caProcFile, O_RDONLY ) ) < 0 )
			continue;

		if( read( iFileFd, (void *)&stPinfo, sizeof( psinfo_t ) ) <= 0 )
		{
			close( iFileFd );

			continue;
		}

		if( strcmp( _cpProcessName ,stPinfo.pr_fname ) == 0 )
			iProcessCount++;

		close( iFileFd );
	}

	closedir( dpDirPoint );

	return	iProcessCount;
}

