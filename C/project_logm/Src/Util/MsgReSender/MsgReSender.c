// 기본
#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <unistd.h>
#include <strings.h>

// ProcessCheck 관련
#include <fcntl.h>
#include <dirent.h>
#include <procfs.h>

// IPC 관련
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/shm.h>

// 파일정보 구하기 관련
#include <sys/stat.h>

// 기본 헤더 파일
#include "MsgReSender.h"

// 시간과 문자열 관련
#include "CNBaseTime.h"

// Config 관련
#include "CNConfigRead.h"

// Signal 관련
#include "CNSignalApi.h"
#include "CNSignalInit.h"

// IPC Queue 관련
#include "CNIpcMsgQueue.h"

CN_LOG_HANDLE	H_LOG;

char			*g_cpDataPath;
char			*g_cpMovePath;
char			*g_cpFinPath;

stSmdaQueueTable	g_stSmdaQueueTable;
stSmdaRouting		g_stSmdaRouting;

int main( int argc, char **argv )
{
	int	iResult;

	// 1. 동일한 이름의 Process가 실행되고 있는지 체크
	if( ( iResult = ForkProcess( (char*)MSG_RESENDER_PROCESS_NAME ) ) < 0 )
	{
		fprintf( stderr, "[ForkProcess( %s ) Error][R:%d][L:%d]\n", MSG_RESENDER_PROCESS_NAME, iResult, __LINE__  );

		ProcessTerminate( iResult );
	}

	// 2. 기본적인 프로세스의 정보들을 Config 파일에서 읽어서 셋팅
	if( ( iResult = PrecessDataInit( (char*)MSG_RESENDER_CONFIG_PATH ) ) < 0 )
	{
		fprintf( stderr, "[ProcessDataInit( %s ) Error][R:%d][L:%d]\n", (char*)MSG_RESENDER_CONFIG_PATH, iResult, __LINE__  );

		ProcessTerminate( iResult );
	}

	CNLOG( H_LOG, 1, "[INSERT QUEUE START][L:%d]\n", __LINE__  );

	// 3. Signal Handler 등록(시그널 처리)
	if( ( iResult = CNInitSignal( SignalHandler ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[CNInitSignal( %d ) Error][R:%d][L:%d]\n", SignalHandler, iResult, __LINE__ );

		ProcessTerminate( iResult );
	}

	// 4. SMDA Queue에 대한 데이터를 읽어서 셋팅
	if( ( iResult = SmdaQueueDataInit( (char*)MSG_RESENDER_CONFIG_PATH ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[SmdaQueueDataInit( %s ) Error][R:%d][L:%d]\n", (char*)MSG_RESENDER_CONFIG_PATH, iResult, __LINE__  );

		ProcessTerminate( iResult );
	}

	// 5. SMDA Routing에 대한 데이터를 읽어서 셋팅
	if( ( iResult = SmdaRoutingDataInit( (char*)MSG_RESENDER_CONFIG_PATH ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[SmdaRoutingDataInit( %s ) Error][R:%d][L:%d]\n", (char*)SMDA_ROUTING_INFO_FILE, iResult, __LINE__  );

		ProcessTerminate( iResult );
	}

	// 6. 재처리 파일에서 데이터를 읽어서 재처리
	if( ( iResult = ReInsertQueueData( g_cpDataPath, g_cpMovePath, g_cpFinPath ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[ReSendQueueData( %s %s %s ) Error][R:%d][L:%d]\n", 
			g_cpDataPath, g_cpMovePath, g_cpFinPath, iResult, __LINE__  );

		ProcessTerminate( iResult );
	}

	CNLOG( H_LOG, 1, "[INSERT QUEUE END][L:%d]\n", __LINE__  );

	// 7. 프로세스 종료!!
	ProcessTerminate( 0 );
}


void SignalHandler( int _iSignal )
{
	// 1. 받은 시그널 정보를 출력
	CNLOG( H_LOG, 1, "[Signal Receive: %d][L:%d]\n", _iSignal, __LINE__ );

	// 2. 프로세스 종료과정 진행!!
	ProcessTerminate( _iSignal );
}


void ProcessTerminate( int _iReturnCode )
{
	// 1. Routing 테이블 데이터 삭제
	SmdaRoutingDataFree();

	// 2. Queue 테이블 데이터 삭제
	SmdaQueueDataFree();

	// 3. 로그 테이블 삭제
	CNDeleteHandle( H_LOG );

	// 4. 데이터파일 경로 삭제
	if( g_cpDataPath != NULL )
		free( g_cpDataPath );

	// 5. 데이터파일 이동 경로 삭제
	if( g_cpDataPath != NULL )
		free( g_cpDataPath );

	// 6. 데이터파일 처리 종료 경로 삭제
	if( g_cpDataPath != NULL )
		free( g_cpDataPath );

	// 7. 프로세스 종료!!
	exit( _iReturnCode );
}


int	ForkProcess( char* _cpProcessName )
{
	int		iResult			=	0;
	int		iChildPid		=	0;

	// 1. 동일한 프로세스가 이미 떠있는지 체크, 떠있으면 그냥 종료
	if( ( iResult = ProcessCheck( _cpProcessName ) ) != 1 )
	{
		fprintf( stderr, "[ProcessCheck() Error][R:%d][L:%d]\n", iResult, __LINE__ );

		return	-1;
	}

	// 2. 프로세스가 백그라운드로 동작하도록 포크
	if( ( iChildPid = fork() ) < 0 )
	{
		fprintf( stderr, "[fork() Error][R:%d][L:%d]\n", iChildPid, __LINE__ );

		return	-1;
	}

	// 3. 부모 프로세스는 죽는다, 자식프로세스가 데이터를 처리
	if( iChildPid > 0 )
		exit( 0 );

	// 4. 성공!!
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

	// 1. 프로세스(/proc) 디렉토리 정보를 읽어온다
	if( ( dpDirPoint = opendir( "/proc" ) ) == NULL )
	{
		fprintf( stderr, "[opendir( /proc) Error][E:%d][L:%d]\n", errno, __LINE__ );

		return	-1;
	}

	// 2. 프로세스 디렉토리를 뒤져서 같은 이름의 프로세스가 실행중인지 확인
	//    UNIX의 경우 프로세스가 실행 될경우 (/proc)디렉토리 안에 해당 프로세스
	//    의 PID(프로세스ID)에 해당하는 디렉토리가 생성되며 그안에는 그 프로세스
	//    에 대한 정보가 저장된다. 그 정보를 뒤짐으로써 실행중인 프로세스의
	//    정보를 확인할 수 있다.
	while( 1 )
	{
		// 2.1 프로세스(/proc) 디렉토리 정보 추출
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

		// 2.2 해당 PID를 가지는 프로세스의 정보를 저장하는 파일을 연다
		if( ( iFileFd = open( caProcFile, O_RDONLY ) ) < 0 )
			continue;

		// 2.4 해당 PID의 프로세스의 정보를 추출
		if( read( iFileFd, (void *)&stPinfo, sizeof( psinfo_t ) ) <= 0 )
		{
			close( iFileFd );

			continue;
		}

		// 2.5 추출한 프로세스의 이름이 실행중인 프로세스 이름과 동일한지 비교
		if( strcmp( _cpProcessName ,stPinfo.pr_fname ) == 0 )
			iProcessCount++;

		close( iFileFd );
	}

	closedir( dpDirPoint );

	// 3. 실행중인 같은 이름의 프로세스 수 리턴!!
	return	iProcessCount;
}


int PrecessDataInit( char* _cpConfigFilePath )
{
	int		iResult;
	int		iCenterType;

	char	caTempBuffer[512];

	char	caCenterTypeFile[512];
	char	caMsgResenderConfigFile[512];

	sprintf( caCenterTypeFile, "%s/%s", _cpConfigFilePath, LOG_MANAGER_CONFIG_FILE_NAME );
	sprintf( caMsgResenderConfigFile, "%s/%s", _cpConfigFilePath, MSG_RESENDER_CONFIG_FILE_NAME );

	// 1. 주센터 부센터 정보 추출
	if( ( iResult = CNGetConfigInt( caCenterTypeFile, (char*)"COMMON", (char*)"CENTER_TYPE", &iCenterType ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON CENTER_TYPE %d ) Error][R:%d][L:%d]\n", 
			caCenterTypeFile, &iCenterType, iResult, __LINE__ );

		return	-1;
	}

	// 2. 재처리 데이터 파일 경로 추출
	memset( caTempBuffer, 0x00, sizeof( caTempBuffer ) );

	if( ( iResult = CNGetConfigStr( caMsgResenderConfigFile, (char*)"COMMON", (char*)"DATA_DIRECTORY", caTempBuffer, sizeof(caTempBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON DATA_DIRECTORY %d %d ) Error][R:%d][L:%d]\n", 
			caMsgResenderConfigFile, caTempBuffer, sizeof( caTempBuffer ), iResult, __LINE__ );

		return	-1;
	}

	g_cpDataPath	= (char*)malloc( strlen( caTempBuffer ) + 1 );
	memcpy( g_cpDataPath, caTempBuffer, strlen( caTempBuffer ) );

	// 3. 재처리 데이터 파일 경로 추출
	memset( caTempBuffer, 0x00, sizeof( caTempBuffer ) );

	if( ( iResult = CNGetConfigStr( caMsgResenderConfigFile, (char*)"COMMON", (char*)"MOVE_DIRECTORY", caTempBuffer, sizeof(caTempBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON MOVE_DIRECTORY %d %d ) Error][R:%d][L:%d]\n", 
			caMsgResenderConfigFile, caTempBuffer, sizeof( caTempBuffer ), iResult, __LINE__ );

		return	-1;
	}

	g_cpMovePath	= (char*)malloc( strlen( caTempBuffer ) + 1 );
	memcpy( g_cpMovePath, caTempBuffer, strlen( caTempBuffer ) );

	// 4. 재처리 데이터 파일 경로 추출
	memset( caTempBuffer, 0x00, sizeof( caTempBuffer ) );

	if( ( iResult = CNGetConfigStr( caMsgResenderConfigFile, (char*)"COMMON", (char*)"FIN_DIRECTORY", caTempBuffer, sizeof(caTempBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON FIN_DIRECTORY %d %d ) Error][R:%d][L:%d]\n", 
			caMsgResenderConfigFile, caTempBuffer, sizeof( caTempBuffer ), iResult, __LINE__ );

		return	-1;
	}

	g_cpFinPath	= (char*)malloc( strlen( caTempBuffer ) + 1 );
	memcpy( g_cpFinPath, caTempBuffer, strlen( caTempBuffer ) );

	// 6. 로거 초기화 및 등록
	if( ( H_LOG = CNGetLogHandle( MSG_RESENDER_LOG_TYPE, 
								  MSG_RESENDER_LOG_LEVEL, 
								  g_cpDataPath, 
								  (char*)MSG_RESENDER_LOG_NAME, 
								  iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" ) ) == NULL )
	{
		fprintf( stderr, "[CNGetLogHandle( %d %d %s %s %s ) Error][L:%d]\n", 
			MSG_RESENDER_LOG_TYPE, 
			MSG_RESENDER_LOG_LEVEL, 
			g_cpDataPath, 
			MSG_RESENDER_LOG_NAME, 
			iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A", __LINE__ );

		return	-1;
	}

	// 7. 성공!!
	return	0;
}


int SmdaQueueDataInit( char* _cpConfigFilePath )
{
	int		iResult;
	int		iSmdaLoop;
	int		iSessionLoop;

	int		iSmdaServerNum;
	int		iSmdaSessionNum;
	int		iSmdaQueueKey;
	int		iSmdaQueueID;

	char	caConfigFile[512];
	char	caPartBuffer1[128];
	char	caPartBuffer2[128];

	// 1. SmdaQueue 구조체 맴버 초기화
	g_stSmdaQueueTable.iSmdaServerNum	= 0;
	g_stSmdaQueueTable.Info		= NULL;

	// 2. 읽어들일 Config 파일(SmdaQueue Config File)이름 셋팅
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, SMDA_QUEUE_INFO_FILE );

	// 3. 컨피그 파일에서 SMDA 서버 개수를 읽어서 저장	
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"SMDA_SERVER_NUM", &iSmdaServerNum ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[CNGetConfigInt( %s SMDA_QUEUE SMDA_QUEUE_NUM %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iSmdaServerNum, iResult, __LINE__ );

		return	-1;
	}

	// 4. SMDA 서버개수 읽기 예외처리
	if( iSmdaServerNum <= 0 )
	{
		CNLOG( H_LOG, 1, "[Read Format Error][SMDA_QUEUE_NUM: %d][L:%d]\n", iSmdaServerNum, __LINE__ );

		return	-1;
	}

	CNLOG( H_LOG, 3, "[SMDA_SERVER_NUM: %d][L:%d]\n", iSmdaServerNum, __LINE__ );

	// 5. SMDA 서버 숫자만큼 SMDA 라우팅 정보 테이블 공간을 할당
	g_stSmdaQueueTable.iSmdaServerNum	= iSmdaServerNum;
	g_stSmdaQueueTable.Info	= (stSmdaQueueInfo*)malloc( sizeof(stSmdaQueueInfo) * g_stSmdaQueueTable.iSmdaServerNum );

	// 6. SMDA 서버 라우팅 테이블 메모리할당 예외처리
	if( g_stSmdaQueueTable.Info == NULL )
	{
		CNLOG( H_LOG, 1, "[malloc( %d ) Error][E:%d][L:%d]\n", 
			sizeof(stSmdaQueueInfo) * g_stSmdaQueueTable.iSmdaServerNum, errno, __LINE__ );

		return	-1;
	}

	memset( g_stSmdaQueueTable.Info, 0x00, sizeof(stSmdaQueueInfo) * g_stSmdaQueueTable.iSmdaServerNum );

	// 7. 컨피그 파일에서 SMDA 라우팅 테이블 정보를 읽어들이고 데이터 초기화
	for( iSmdaLoop = 0; iSmdaLoop < g_stSmdaQueueTable.iSmdaServerNum; iSmdaLoop++ )
	{
		sprintf( caPartBuffer1, "SMDA_%d", iSmdaLoop + 1 );

		// 7.1 SMDA Session 개수 값을 읽어들인다
		if( ( iResult = CNGetConfigInt( caConfigFile, caPartBuffer1, (char*)"SESSION_NUM", &iSmdaSessionNum ) ) < 0 )
		{
			CNLOG( H_LOG, 1, "[CNGetConfigInt( %s %s SESSION_NUM %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer1, &iSmdaSessionNum, iResult, __LINE__ );

			return	-1;
		}

		// 7.2 SMDA Session 개수 읽기 예외처리
		if( iSmdaSessionNum <= 0 )
		{
			CNLOG( H_LOG, 1, "[Read Format Error][SESSION_NUM: %d][L:%d]\n", iSmdaSessionNum, __LINE__ );

			return	-1;
		}

		// 7.3 세션 개수만큼 세션정보 저장 메모리 공간 할당
		g_stSmdaQueueTable.Info[iSmdaLoop].iSessionNum	= iSmdaSessionNum;
		g_stSmdaQueueTable.Info[iSmdaLoop].stpQueue	= (stSmdaQueue*)malloc( sizeof(stSmdaQueue) * g_stSmdaQueueTable.Info[iSmdaLoop].iSessionNum );

		// 7.4 세션정보 저장 공간 메모리 할당 예외처리
		if( g_stSmdaQueueTable.Info[iSmdaLoop].stpQueue == NULL )
		{
			CNLOG( H_LOG, 1, "[malloc( %d ) Error][E:%d][L:%d]\n", 
				sizeof( stSmdaQueue ) * g_stSmdaQueueTable.Info[iSmdaLoop].iSessionNum, errno, __LINE__ );

			return	-1;
		}

		memset( g_stSmdaQueueTable.Info[iSmdaLoop].stpQueue, 0x00, sizeof(stSmdaQueue) * g_stSmdaQueueTable.Info[iSmdaLoop].iSessionNum );

		// 7.5 세션 개수만큼 할당된 SMDA 라우팅 테이블 메모리 공간에 컨피그에서 데이터를 읽어서 셋팅 및 Queue 연결
		for( iSessionLoop = 0; iSessionLoop < g_stSmdaQueueTable.Info[iSmdaLoop].iSessionNum; iSessionLoop++ )
		{
			sprintf( caPartBuffer2, "SMDA_QUEUE_KEY_%d", iSessionLoop + 1 );

			// 7.5.1 Smda Queue Key 값을 읽어들인다
			if( ( iResult = CNGetConfigInt( caConfigFile, caPartBuffer1, caPartBuffer2, &iSmdaQueueKey ) ) < 0 )
			{
				CNLOG( H_LOG, 1, "[CNGetConfigInt( %s %s %s %d ) Error][R:%d][L:%d]\n", 
					caConfigFile, caPartBuffer1, caPartBuffer2, &iSmdaQueueKey, iResult, __LINE__ );

				return	-1;
			}

			CNLOG( H_LOG, 3, "[%s][%s: 0x%x][L:%d]\n", caPartBuffer1, caPartBuffer2, iSmdaQueueKey, __LINE__ );

			// 7.5.2 Smda Queue Attach
			if( ( iSmdaQueueID = CNQueueOpen( iSmdaQueueKey ) ) < 0 )
			{
				CNLOG( H_LOG, 1, "[CNQueueOpen( 0x%x ) Error][R:%d][E:%d][L:%d]\n", 
					iSmdaQueueKey, iSmdaQueueID, errno, __LINE__ );

				return	-1;
			}

			CNLOG( H_LOG, 3, "[%s][SMDA_QUEUE_ID_%d: %d][L:%d]\n", caPartBuffer1, iSessionLoop + 1, iSmdaQueueID, __LINE__ );

			// 7.5.3 라우팅 테이블 정보 셋팅
			g_stSmdaQueueTable.Info[iSmdaLoop].stpQueue[iSessionLoop].iQueueKey	= iSmdaQueueKey;
			g_stSmdaQueueTable.Info[iSmdaLoop].stpQueue[iSessionLoop].iQueueID	= iSmdaQueueID;
		}
	}

	// 8. 성공!!
	return	0;
}


void SmdaQueueDataFree()
{
	int	iForLoop;

	// 1. Heap 영역에 할당한 SMDA Queue 정보 삭제
	if( g_stSmdaQueueTable.Info != NULL )
	{
		for( iForLoop = 0; iForLoop < g_stSmdaQueueTable.iSmdaServerNum; iForLoop++ )
		{
			if( g_stSmdaQueueTable.Info[iForLoop].stpQueue != NULL )
				free( g_stSmdaQueueTable.Info[iForLoop].stpQueue );
		}

		free( g_stSmdaQueueTable.Info );
	}

	// 2. 변수 초기화
	g_stSmdaQueueTable.Info				= NULL;
	g_stSmdaQueueTable.iSmdaServerNum	= 0;

	// 3. 종료!!
	return;
}


int SmdaRoutingDataInit( char* _cpConfigFilePath )
{
	int		iResult;
	int		iForLoop;
	int		iPrefixLoop;

	int		iSmartRoutingNum;
	int		iRoutingQueue;
	int		iEtcRoutingQueue;
	int		iStartPrefix;
	int		iEndPrefix;

	char	caConfigFile[512];
	char	caPartBuffer[128];

	char	caStartPrefix[8];
	char	caEndPrefix[8];

	// 1. Smart Routing 테이블 초기화
	g_stSmdaRouting.iSmdaServerNum	= 0;
	g_stSmdaRouting.Index			= NULL;

	// 2. SMDA Routing 데이터를 읽어들일 Config 파일의 파일명 셋팅
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, SMDA_ROUTING_INFO_FILE );

	// 3. 사용하는 Routing Prefix 개수 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"SMDA_ROUTING", (char*)"SMDA_ROUTING_NUM", &iSmartRoutingNum ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING SMDA_ROUTING_NUM %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iSmartRoutingNum, iResult, __LINE__ );

		return	-1;
	}

	g_stSmdaRouting.iSmdaServerNum	= iSmartRoutingNum;

	CNLOG( H_LOG, 3, "[SMDA_ROUTING_NUM: %d][L:%d]\n", g_stSmdaRouting.iSmdaServerNum, __LINE__ );

	// 3. ETC Routing Queue Number(등록되지 않은 대역의 데이터가 들어올 경우 데이터를 전송 할 Queue의 Number) 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"SMDA_ROUTING", (char*)"SMDA_ETC_ROUTING", &iEtcRoutingQueue ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING SMDA_ETC_ROUTING_QUEUE %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iEtcRoutingQueue, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( H_LOG, 3, "[ETC_SMDA_ROUTING_NUM: %d][L:%d]\n", iEtcRoutingQueue, __LINE__ );

	if( iEtcRoutingQueue < 1 || iEtcRoutingQueue > g_stSmdaQueueTable.iSmdaServerNum )
	{
		CNLOG( H_LOG, 1, "[Etc Queue Scopr Error][Etc Queue: %d][L:%d]\n",  iEtcRoutingQueue, __LINE__ );

		return	-1;
	}

	// 4. 임시로 Prefix 데이터를 저장 할 공간 할당(셋팅이 문제없이 끝나면 덮어쓴다)
	g_stSmdaRouting.Index = (int*)malloc( sizeof(int) * 100000 );

	for( iForLoop = 0; iForLoop < 100000; iForLoop++ )
		g_stSmdaRouting.Index[iForLoop]	= iEtcRoutingQueue - 1;

	// 5. 컨피그 파일에서 Prefix 정보를 읽어서 임시 저장공간에 기록
	for( iForLoop = 1; iForLoop < g_stSmdaRouting.iSmdaServerNum + 1;iForLoop++ )
	{
		// 5.1 시작 라우팅 번호 대역 셋팅(라우팅은 5자리수 라우팅)
		sprintf( caPartBuffer, "SMDA_ROUTING_STR_%d", iForLoop );

		if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"SMDA_ROUTING", caPartBuffer, caStartPrefix, sizeof(caStartPrefix) ) ) < 0 )
		{
			CNLOG( H_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING %s %d %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer, caStartPrefix, sizeof(caStartPrefix), iResult, __LINE__ );

			return	-1;
		}

		iStartPrefix = atoi( caStartPrefix );

		sprintf( caPartBuffer, "SMDA_ROUTING_END_%d", iForLoop );

		// 5.2 끝 라우팅 번호 대역 셋팅(라우팅은 5자리수 라우팅)
		if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"SMDA_ROUTING", caPartBuffer, caEndPrefix, sizeof(caEndPrefix) ) ) < 0 )
		{
			CNLOG( H_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING %s %d %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer, caEndPrefix, sizeof(caEndPrefix), iResult, __LINE__ );

			return	-1;
		}

		iEndPrefix = atoi( caEndPrefix );

		sprintf( caPartBuffer, "SMDA_ROUTING_QUEUE_%d", iForLoop );

		// 5.3 시작에서 끝 라우팅 대역까지의 데이터를 전송 할 Queue의 Number(Queue Index)
		if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"SMDA_ROUTING", caPartBuffer, &iRoutingQueue ) ) < 0 )
		{
			CNLOG( H_LOG, 1, "[CNGetConfigInt( %s SMDA_QUEUE %s %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer, &iRoutingQueue, iResult, __LINE__ );

			return	-1;
		}

		CNLOG( H_LOG, 3, "[PREFIX: %s ~ %s][QUEUE: %02d][L:%d]\n", caStartPrefix, caEndPrefix, iRoutingQueue, __LINE__ );

		if( iRoutingQueue < 1 || iRoutingQueue > g_stSmdaQueueTable.iSmdaServerNum )
		{
			CNLOG( H_LOG, 1, "[Routing Queue Scopr Error][Routing Queue: %d][L:%d]\n",  iRoutingQueue, __LINE__ );

			return	-1;
		}

		// 5.4 읽어들인 데이터로 라우팅 테이블(Smart Routing Table)을 셋팅
		for ( iPrefixLoop = iStartPrefix; iPrefixLoop < iEndPrefix + 1; iPrefixLoop++ )
			g_stSmdaRouting.Index[iPrefixLoop] = iRoutingQueue - 1;
	}

	// 9. 성공!!
	return	0;
}


void SmdaRoutingDataFree()
{
	// 1. Heap 영역에 할당 한 Routing 공간 삭제!!
	if( g_stSmdaRouting.Index != NULL )
		free( g_stSmdaRouting.Index );

	// 2. 구조체 정보 초기화
	g_stSmdaRouting.Index			= NULL;
	g_stSmdaRouting.iSmdaServerNum	= 0;

	// 3. 종료!!
	return;
}


int ReInsertQueueData( char* _cpDataFilePath, char* _cpDataFileMovePath, char* _cpDataFileFinPath )
{
	int	iResult;

	char	caDataFileName[1024];

	struct dirent*	stpDirData		=	NULL;
	DIR*			dpDirPoint		=	NULL;

	// 1. 재처리 Data 파일을 찾아서 처리 디렉토리로 파일을 이동
	if( ( iResult = OnDataFileMove( _cpDataFilePath, _cpDataFileMovePath ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[OnDataFileMove( %s %s ) Error][R:%d][L:%d]\n", 
			_cpDataFilePath, _cpDataFileMovePath, iResult, __LINE__ );

		return	-1;
	}

	sleep( 5 );

	// 2. 처리 디렉토리에 파일 정보를 읽어들인다
	if( ( dpDirPoint = opendir( _cpDataFileMovePath ) ) == NULL )
	{
		CNLOG( H_LOG, 1, "[opendir( %s) Error][E:%d][L:%d]\n", _cpDataFileMovePath, errno, __LINE__ );

		return	-1;
	}

	// 2. 해당 경로에 재처리 파일(xxx.dat)가 있을경우 재처리 실행
	while( 1 )
	{
		// 2.1 한라인씩 해당 경로의 파일 데이터 읽어들임
		stpDirData = readdir( dpDirPoint );

		// 2.2 다 읽었으면 종료
		if( !stpDirData )
			break;

		// 2.3 파일들이 아닌경우 continue
		if( strcmp( stpDirData->d_name, "." ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, ".." ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, "0" ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, "1" ) == 0 )
			continue;

		// 2.5 길이가 4보다 작을경우 넘어감(xxx.dat이므로 .dat는 최소 4자리)
		if( strlen( stpDirData->d_name ) < 4 )
			continue;

		// 2.6 길이가 4자리 이상이고 재처리 파일(.dat로 끝나는 파일)일 경우 재처리 시작
		if( strncmp( stpDirData->d_name + ( strlen( stpDirData->d_name ) ) - 4, ".dat", 4 ) == 0 )
		{
			// 2.6.1 파일이름 앞에 절대 경로까지 붙여서 절대경로 파일로 만든다
			sprintf( caDataFileName, "%s/%s", _cpDataFileMovePath, stpDirData->d_name );

			CNLOG( H_LOG, 1, "[%s File Start][L:%d]\n", caDataFileName, __LINE__ );

			// 2.6.2 OnFindDataFile()을 실행해서 재처리를 시작
			if( ( iResult = OnDataFileInsert( caDataFileName ) ) < 0 )
			{
				CNLOG( H_LOG, 1, "[OnDataFileInsert( %s ) Error][R:%d][L:%d]\n", 
					caDataFileName, iResult, __LINE__ );
			}

			// 2.6.3 처리가 끝난 Data 파일을 Fin 디렉토리로 이동
			if( ( iResult = OnDataFileFinish( caDataFileName, _cpDataFileFinPath ) ) < 0 )
			{
				CNLOG( H_LOG, 1, "[OnDataFileFinish( %s %s ) Error][R:%d][L:%d]\n", 
					caDataFileName, _cpDataFileFinPath, iResult, __LINE__ );
			}

			CNLOG( H_LOG, 1, "[%s File End][L:%d]\n", caDataFileName, __LINE__ );		
		}
	}

	closedir( dpDirPoint );

	// 3. 성공!!
	return	0;
}


int OnDataFileMove( char* _cpDataFilePath, char* _cpDataFileMovePath )
{
	int	iResult;
	int	iLength;

	char	caMoveFileName[512];

	char	caDataFileMoveCommand[1024];

	struct dirent*	stpDirData		=	NULL;
	DIR*			dpDirPoint		=	NULL;

	// 1. 프로세스 Argument로 받은 경로에 있는 파일 정보를 읽어들인다
	if( ( dpDirPoint = opendir( _cpDataFilePath ) ) == NULL )
	{
		CNLOG( H_LOG, 1, "[opendir( %s) Error][E:%d][L:%d]\n", _cpDataFilePath, errno, __LINE__ );

		return	-1;
	}

	// 2. 해당 경로에 재처리 파일(xxx.dat)가 있을경우 파일이동 시작
	while( 1 )
	{
		// 2.1 한라인씩 해당 경로의 파일 데이터 읽어들임
		stpDirData = readdir( dpDirPoint );

		// 2.2 다 읽었으면 종료
		if( !stpDirData )
			break;

		// 2.3 파일들이 아닌경우 continue
		if( strcmp( stpDirData->d_name, "." ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, ".." ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, "0" ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, "1" ) == 0 )
			continue;

		// 2.4 길이가 4보다 작을경우 넘어감(xxx.dat이므로 .dat는 최소 4자리)
		if( strlen( stpDirData->d_name ) < 4 )
			continue;

		// 2.5 길이가 4자리 이상이고 재처리 파일(.dat로 끝나는 파일)일 경우 파일이동
		if( strncmp( stpDirData->d_name + ( strlen( stpDirData->d_name ) - 4 ), ".dat", 4 ) == 0 )
		{
			// 2.6.1 파일이름 앞에 절대 경로까지 붙여서 절대경로 파일로 만든다
			sprintf( caMoveFileName, "%s/%s", _cpDataFilePath, stpDirData->d_name );

			// 2.6.2 Data파일을 이동시키는 명령어 생성
			sprintf( caDataFileMoveCommand, "mv %s %s", caMoveFileName, _cpDataFileMovePath );

			// 2.6.3 Data파일을 처리 디렉토리로 이동
			system( caDataFileMoveCommand );

			CNLOG( H_LOG, 1, "[%s][L:%d]\n", caDataFileMoveCommand, __LINE__ );
		}
	}

	closedir( dpDirPoint );

	// 3. 성공!!
	return	0;
}


int OnDataFileInsert( char* _cpDataFile )
{
	int		iResult;

	int		iPacketSize;

	int		iReadCount			= 0;
	int		iErrorCount			= 0;
	int		iInsertCount		= 0;
	int		iNotInsertCount		= 0;

	int		iPrefixIndex;
	int		iSmdaIndex;
	int		iSessionIndex;

	int		iSecToCount;

	char	caTempRouting[6];

	char	caDataFile[1024];
	char	caReportFile[1024];
	char	caErrorDataFile[1024];

	time_t	tCheckTime;

	FILE*	fpDataFile;

	struct stat		stFileStat;

	stLogPacket	stTempPacket;

	// 1. 데이터 초기화
	iPacketSize	= sizeof( stLogPacket ) - sizeof( long );

	// 2. Data파일 Error파일 Report 파일 이름 생성
	strlcpy( caDataFile, _cpDataFile, 1024 );
	sprintf( caReportFile, "%s.log", caDataFile );
	sprintf( caErrorDataFile, "%s.err", caDataFile );

	// 3. Data 파일을 연다
	if( ( fpDataFile = fopen( caDataFile, "r" ) ) == NULL )
	{
		OnWriteReport( caReportFile , "[fopen( %s, r ) Error][E:%d][L:%d]\n", caDataFile, errno, __LINE__ );

		return	-1;
	}

	iSecToCount	= 0;
	tCheckTime	= time( NULL );

	// 4. Data 파일 정보를 추출해서 Queue로 Insert 작업을 수행
	while( 1 )
	{
		// 4.1 Queue 과부하를 줄이기 위해 초당 처리건수 제한을 둔다(초당250)
		if( time( NULL ) != tCheckTime )
		{
			iSecToCount	= 0;
			tCheckTime	= time( NULL );
		}
		else
		{
			if( iSecToCount >= LIMIT_COUNT_TO_SEC )
			{
				usleep( 10000 );

				continue;
			}
		}

		// 4.2 Data파일에서 Packet 데이터를 한건씩 읽어들인다
		if( fread( &stTempPacket, sizeof( stLogPacket ), 1, fpDataFile ) != 1 )
			break;

		iReadCount++;
		iSecToCount++;

		// 4.3 Routing 대역(전송 Queue Index)을 구한다
		memset( caTempRouting, 0x00, sizeof( caTempRouting ) );
		memcpy( caTempRouting, stTempPacket.caRoutingNumber, 5 );

		iPrefixIndex	= atoi( caTempRouting );
		iSmdaIndex		= g_stSmdaRouting.Index[iPrefixIndex];
		iSessionIndex	= (++g_stSmdaQueueTable.Info[iSmdaIndex].iMsgCount) % g_stSmdaQueueTable.Info[iSmdaIndex].iSessionNum;

		// 4.4 LOG FILE TYPE(스마트 전송유무)가 1이 아닌경우 전송메시지가 아니므로 넘어간다
		if( stTempPacket.iLogFileType != 1 || iPrefixIndex == 0 )
		{
			iNotInsertCount++;

			continue;
		}

		// 4.5 Routing 에 해당하는 Queue로 Data Insert
		iResult	= CNQueueWrite( g_stSmdaQueueTable.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID, &stTempPacket, iPacketSize );

		switch( iResult )
		{
			// 4.5.1 정상적으로 전송 된 경우
			case CN_IPC_QUEUE_DATA_WRITE :
				
				iInsertCount++;

				break;

			// 4.5.2 Queue에 데이터가 가득찬 경우
			case CN_IPC_QUEUE_DATA_FULL :

				// 4.5.2.1 에러로그 출력
				OnWriteReport( caReportFile , "[CN_IPC_QUEUE_DATA_FULL][Seq: %d][R:%d][E:%d][L:%d]\n", 
					stTempPacket.stHeader.iSerialNumber, iResult, errno, __LINE__ );

				// 4.5.2.2 에러파일 출력
				OnWriteErrorFile( caReportFile, caErrorDataFile, &stTempPacket );

				// 4.5.2.3 에러통계
				iErrorCount++;

				break;

			// 4.5.2 기타 에러로 Queue Insert가 실패 한 경우
			default :

				// 4.5.2.1 에러로그 출력
				OnWriteReport( caReportFile , "[CN_IPC_QUEUE_DATA_ERROR][Seq: %d][R:%d][E:%d][L:%d]\n", 
					stTempPacket.stHeader.iSerialNumber, iResult, errno, __LINE__ );

				// 4.5.2.2 에러파일 출력
				OnWriteErrorFile( caReportFile, caErrorDataFile, &stTempPacket );

				// 4.5.2.3 에러통계
				iErrorCount++;

				break;
		}
	}

	fclose( fpDataFile );

	// 5. 한 파일에 대해서 처리가 끝났다면 해당파일의 리포트 파일데이터를 생성
	{
		if( ( iResult = stat( _cpDataFile, &stFileStat ) ) < 0 )
		{
			OnWriteReport( caReportFile , "[stat( %s %d ) Error][R:%d][E:%d][L:%d]\n", 
				_cpDataFile, &stFileStat, iResult, errno, __LINE__ );

			return	-1;
		}

		OnWriteReport( caReportFile , "[Read: %d][Insert: %d][Not Insert: %d][Error: %d][L:%d]\n", 
			iReadCount, iInsertCount, iNotInsertCount, iErrorCount, __LINE__ );

		OnWriteReport( caReportFile , "[Data File Size: %d][Read File Size: %d][L:%d]\n", 
			stFileStat.st_size, sizeof( stLogPacket ) * iReadCount, __LINE__ );
	}

	// 6. 성공!!
	return	0;
}


int OnDataFileFinish( char* _cpDataFile, char* _cpDataFileFinPath )
{
	char	caDataFile[512];
	char	caReportFile[512];
	char	caErrorDataFile[512];

	char	caMoveCommand[1024];

	// 1. Data파일 Error파일 Report파일 이름 생성
	strlcpy( caDataFile, _cpDataFile, 512 );
	sprintf( caReportFile, "%s.log", caDataFile );
	sprintf( caErrorDataFile, "%s.err", caDataFile );

	// 2. Data파일 Fin 디렉토리로 이동
	sprintf( caMoveCommand, "mv %s %s", caDataFile, _cpDataFileFinPath );

	CNLOG( H_LOG, 1, "[%s][L:%d]\n", caMoveCommand, __LINE__ );
	//OnWriteReport( caReportFile ,"[%s][L:%d]\n", caMoveCommand, __LINE__ );

	system( caMoveCommand );

	// 3. Error파일 Fin 디렉토리로 이동
	sprintf( caMoveCommand, "mv %s %s", caErrorDataFile, _cpDataFileFinPath );

	CNLOG( H_LOG, 1, "[%s][L:%d]\n", caMoveCommand, __LINE__ );
	//OnWriteReport( caReportFile ,"[%s][L:%d]\n", caMoveCommand, __LINE__ );

	system( caMoveCommand );

	// 4. Report파일 Fin 디렉토리로 이동
	sprintf( caMoveCommand, "mv %s %s", caReportFile, _cpDataFileFinPath );

	CNLOG( H_LOG, 1, "[%s][L:%d]\n", caMoveCommand, __LINE__ );
	//OnWriteReport( caReportFile ,"[%s][L:%d]\n", caMoveCommand, __LINE__ );

	system( caMoveCommand );

	// 5. 성공!!
	return	0;
}


int OnWriteErrorFile( char* _cpReportFile, char* _cpErrorFileName, stLogPacket* _stpErrorPacket )
{
	int		iResult;

	FILE*	fpErrorFile;

	// 1. Error 데이터를 기록 할 파일을 연다
	if( ( fpErrorFile = fopen( _cpErrorFileName, "a+" ) ) == NULL )
	{
		OnWriteReport( _cpReportFile , "[fopen( %s, a+ ) Error][E:%d][L:%d]\n", _cpErrorFileName, errno, __LINE__ );

		return	-1;
	}

	// 2. Error 파일 파일에 출력
	if( fwrite( _stpErrorPacket, sizeof( stLogPacket ), 1, fpErrorFile ) != 1 )
	{
		OnWriteReport( _cpReportFile , "[fwrite( %s %d 1 %d ) Error][Seq:%d][E:%d][L:%d]\n", 
			_stpErrorPacket, sizeof( stLogPacket ), fpErrorFile, _stpErrorPacket->stHeader.iSerialNumber, errno, __LINE__ );

		fclose( fpErrorFile );

		return	-1;
	}

	// 3. 파일을 닫는다
	fclose( fpErrorFile );

	// 4. 성공!!
	return	0;
}


void OnWriteReport( char* _cpLogFile, const char* fmt, ... )
{
	time_t		tTime;

	char		cpDate[32];

	FILE		*fpLog;

	va_list		vaArgs;

	va_start( vaArgs, fmt );

	// 1. 현재 시각의 문자열을 생성해서 변수에 저장한다
	tTime = time( ( time_t ) NULL );
	memset( cpDate, 0x00, sizeof( cpDate ) );
	GetNowTimeFormatStr( cpDate, (char*)"[YYYY/MM/DD:hh:mm:ss]" );
	
	// 2. Report 파일 열기 실패인 경우
	if( ( fpLog = fopen( _cpLogFile, "a+" ) ) == NULL )
	{
		// 2.1 화면에 시간+로그데이터 출력
		fprintf( stderr, "%s ", cpDate );
		vfprintf( stderr, fmt, vaArgs );
		fflush( stderr );
	}
	// 3. Report 파일 열기 성공인 경우
	else
	{
		// 3.1 파일에 시간+로그데이터 출력
		//     싱글 쓰레드 구조이므로 따로 출력해도
		//     상관없다
		fprintf( fpLog, "%s ", cpDate );
		vfprintf( fpLog, fmt, vaArgs );
		fclose( fpLog );
	}

	// 4. 가변인자 사용 종료
	va_end( vaArgs );

	// 5. 종료!!
	return;
}

