#include "SetEmergency.h"

// �⺻
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <strings.h>

// Shared Memory ����
#include <sys/types.h>
#include <sys/shm.h>

// ProcessCheck ����
#include <fcntl.h>
#include <dirent.h>
#include <procfs.h>

int		*g_ipEmergencyMode;

int main( int argc, char **argv )
{
	int	iResult;
	int	iSharedID;
	int	iEmergencyKey;
	int	iEmergencyMode;

	// 1. Agrument �˻�
	if( argc != 2 )
	{
		fprintf( stderr, "[���� ���� �̻�][ex: SET_EMERGENCY 0~2(0:Normal 1:Filing 2:No Filing][L: %d]\n", __LINE__ );

		exit( -1 );
	}

	// 2. ������ �̸��� Process�� ����ǰ� �ִ��� üũ
	if( ( iResult = ProcessCheck( (char*)SET_EMERGENCY_PROCESS_NAME ) ) != 1 )
	{
		fprintf( stderr, "[������ ���μ����� ������][R:%d][L:%d]\n", iResult, __LINE__  );

		exit( -1 );
	}

	// 3. SET_EMERGENCY_SHARED_KEY ���� �����޸� ������ �����ϴ��� üũ 
	if( ( iSharedID = shmget( SET_EMERGENCY_SHARED_KEY, 0, 0 ) ) < 0 )
	{
		fprintf( stderr, "[�����޸� ���� ����][R:%d][E:%d][L:%d]\n", iSharedID, errno, __LINE__  );

		exit( -1 );
	}

	// 4. GET_EMERGENCY_SHARED_KEY ���� �����޸� ������ �����ϸ� ����
	if( ( g_ipEmergencyMode = (int*)shmat( iSharedID, (char*)0, 0 ) ) == NULL )
	{
		fprintf( stderr, "[�����޸� ���� ����][R:%d][E:%d][L:%d]\n", iSharedID, errno, __LINE__ );

		exit( -1 );
	}

	iEmergencyMode	= atoi( argv[1] );

	// 5. �����޸� ������ ��(Emergency Mode)�� ������ �����ϰ� �̻��� ������� ����ó��
	if( iEmergencyMode < 0 || iEmergencyMode > 2 )
	{
		fprintf( stderr, "[���ڰ� ���� �̻�][ex: SET_EMERGENCY 0~2(0:Normal 1:Filing 2:No Filing][Emergency Mode: %d][L: %d]\n", 
			iEmergencyMode, __LINE__ );

		exit( -1 );
	}

	// 6. �����޸� ������ ��(Emergency Mode)�� ����
	*g_ipEmergencyMode	= iEmergencyMode;

	fprintf( stderr, "[Emergency Mode: %d][L: %d]\n", *g_ipEmergencyMode, __LINE__ );

	// 7. ����!!
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
