// 기본
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <strings.h>
#include <sys/atomic.h>

// ProcessCheck 관련
#include <fcntl.h>
#include <dirent.h>
#include <procfs.h>

// Shared Memory 관련
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/shm.h>

#include "LogManager.h"

// 시간과 문자열 관련
#include "CNBaseTime.h"

// Config 관련
#include "CNConfigRead.h"

// Signal 관련
#include "CNSignalApi.h"
#include "CNSignalInit.h"

// Thread 관련
#include "CNThread.h"

CN_LOG_HANDLE	NOR_LOG;
CN_LOG_HANDLE	ERR_LOG;

stLogThread		g_stLogThread;
stSmdaThread	g_stSmdaThread;
stSmdaRouting	g_stSmdaRouting;

int	g_iCenterType;
int	g_iMaxDiskQuantity;

int	g_iMaxLogQueueCount;
int	g_iMaxSmdaQueueCount;

int g_iSmdaRetryUseFlag;
int g_iSmdaRetryCycle;
int g_iSmdaRetryTime;
int g_iSmdaRetryCount;

int g_iEtcRoutingQueue;

int	*g_ipEmergencyMode;

char *g_cpSequenceFilePath;
char *g_cpSequenceFileName;

char *g_cpEmergencyFilePath;
char *g_cpEmergencyFileName;

char *g_cpGutsSmdaFilePath;
char *g_cpGutsSmdaFileName;

char *g_cpErrorLogThreadFilePath;
char *g_cpErrorLogThreadFileName;

char *g_cpErrorSmdaThreadFilePath;
char *g_cpErrorSmdaThreadFileName;

char *g_cpCheckQuantityPath;

#ifdef OS508
volatile unsigned int	g_iDone;

pthread_mutex_t			g_iLogThreadDoneLock;
pthread_mutex_t			g_iSmdaThreadDoneLock;

volatile unsigned int	g_iLogThreadDone;
volatile unsigned int	g_iSmdaThreadDone;

pthread_mutex_t			g_iLogThreadCountLock;
pthread_mutex_t			g_iSmdaThreadCountLock;

volatile unsigned int	g_iLogThreadCount;
volatile unsigned int	g_iSmdaThreadCount;

pthread_mutex_t			g_iMsgSerialLock;
volatile unsigned int	g_iMsgSerial;
#else
volatile unsigned int	g_iDone;

volatile unsigned int	g_iLogThreadDone;
volatile unsigned int	g_iSmdaThreadDone;

volatile unsigned int	g_iLogThreadCount;
volatile unsigned int	g_iSmdaThreadCount;

volatile unsigned int	g_iMsgSerial;
#endif


int main( int argc, char **argv )
{
	int	iResult;
	int iSmdaLoop;

	time_t	tBeforeTime;

	// 1. Agrument 검사
	if( argc != 2 )
	{
		fprintf( stderr, "[Argument Error][L: %d]\n", __LINE__ );

		exit( -1 );
	}

	// 2. 동일한 이름의 Process가 실행되고 있는지 체크
	if( ( iResult = ProcessCheck( (char*)LOG_MANAGER_PROCESS_NAME ) ) != 1 )
	{
		fprintf( stderr, "[ProcessCheck( %s ) Error][R:%d][L:%d]\n", LOG_MANAGER_PROCESS_NAME, iResult, __LINE__  );

		exit( -1 );
	}

	// 3. 변수 및 데이터 초기화
	#ifdef OS508
	fprintf( stderr, "[Solaris 5.8][L: %d]\n", __LINE__ );
	g_iDone	= 1;
	pthread_mutex_init( &g_iLogThreadDoneLock, NULL );
	pthread_mutex_init( &g_iSmdaThreadDoneLock, NULL );
	pthread_mutex_init( &g_iLogThreadCountLock, NULL );
	pthread_mutex_init( &g_iSmdaThreadCountLock, NULL );
	pthread_mutex_init( &g_iMsgSerialLock, NULL );
	#else
	fprintf( stderr, "[Solaris 5.10][L: %d]\n", __LINE__ );
	atomic_swap_uint( &g_iDone, 1 );
	#endif

	// 4. 기본적인 프로세스의 정보들을 Config 파일에서 읽어서 셋팅
	if( ( iResult = ProcessDataInit( argv[1] ) ) < 0 )
	{
		fprintf( stderr, "[ProcessDataInit( %s ) Error][R:%d][L:%d]\n", argv[1], iResult, __LINE__  );

		ProcessTerminate( -1 );
	}

	// 5. Signal Handler 등록(시그널 처리)
	if( ( iResult = CNInitSignal( SignalHandler ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNInitSignal( %d ) Error][R:%d][L:%d]\n", SignalHandler, iResult, __LINE__ );

		ProcessTerminate( -1 );
	}

	// 6. 지금까지 처리한 Sequence를 Guts 파일에서 읽어서 셋팅
	if( ( iResult = ReadSequenceFile() ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[ReadSequenceFile() Error][R:%d][L:%d]\n", iResult, __LINE__ );

		ProcessTerminate( -1 );
	}

	// 7. Emergency Mode 에 대한 정보를 셋팅
	if( ( iResult = EmergencyTableInit( argv[1] ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[EmergencyTableInit( %s ) Error][R:%d][L:%d]\n", argv[1], iResult, __LINE__ );

		ProcessTerminate( -1 );
	}

	// 8. LogQueue 및 LogQueue Thread 에 대한 데이터를 읽어서 셋팅
	if( ( iResult = LogQueueDataInit( argv[1] ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[LogQueueDataInit( %s ) Error][R:%d][L:%d]\n", argv[1], iResult, __LINE__  );

		ProcessTerminate( -1 );
	}

	// 9.  SmdaQueue 및 SmdaQueue Thread 에 대한 데이터를 읽어서 셋팅
	if( ( iResult = SmdaQueueDataInit( argv[1] ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SmdaQueueDataInit( %s ) Error][R:%d][L:%d]\n", argv[1], iResult, __LINE__  );

		ProcessTerminate( -1 );
	}

	// 10. SmdaRouting 데이터를 읽어서 셋팅
	if( ( iResult = SmdaRoutingDataInit( argv[1] ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SmdaRoutingDataInit( %s ) Error][R:%d][L:%d]\n", argv[1], iResult, __LINE__  );

		ProcessTerminate( -1 );
	}

	// 11. Smda Thread 생성
	if( g_iDone )
	{
		if( ( iResult = MakeSmdaThread( argv[1] ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[MakeSmdaThread( %s ) Error][R:%d][L:%d]\n", argv[1], iResult, __LINE__  );

			#ifdef OS508
			g_iDone	= 0;
			#else
			atomic_swap_uint( &g_iDone, 0 );
			#endif
		}
	}

	// 12. Log Thread 생성
	if( g_iDone )
	{
		if( ( iResult = MakeLogThread( argv[1] ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[MakeLogThread( %s ) Error][R:%d][L:%d]\n", argv[1], iResult, __LINE__  );

			#ifdef OS508
			g_iDone	= 0;
			#else
			atomic_swap_uint( &g_iDone, 0 );
			#endif
		}
	}

	// 13. Loop를 돌면서 주기적으로 해야 할 작업을 처리
	tBeforeTime	= time( NULL );

	while( g_iDone )
	{
		// 날짜가 변경 체크
		switch( DateCheck( tBeforeTime ) )
		{
			case 1 :

				CNLOG( NOR_LOG, 3, "[Date Change][L:%d]\n", __LINE__  );

				#ifdef OS508
				pthread_mutex_lock( &g_iMsgSerialLock );
				CNLOG( NOR_LOG, 3, "[YesterDay Total Create Serial: %d][L:%d]\n", g_iMsgSerial, __LINE__  );
				g_iMsgSerial	= 0;
				pthread_mutex_unlock( &g_iMsgSerialLock );

				for( iSmdaLoop = 0; iSmdaLoop < g_stSmdaThread.iSmdaServerNum; iSmdaLoop++ )
				{
					pthread_mutex_lock( &g_stSmdaThread.Info[iSmdaLoop].Lock );

					g_stSmdaThread.Info[iSmdaLoop].iMessageCount	= 0;

					pthread_mutex_unlock( &g_stSmdaThread.Info[iSmdaLoop].Lock );
				}

				#else
				CNLOG( NOR_LOG, 3, "[YesterDay Total Create Serial: %d][L:%d]\n", 
					atomic_swap_uint( &g_iMsgSerial, 0 ), __LINE__  );

				for( iSmdaLoop = 0; iSmdaLoop < g_stSmdaThread.iSmdaServerNum; iSmdaLoop++ )
				{
					atomic_swap_uint( &g_stSmdaThread.Info[iSmdaLoop].iMessageCount, 0 );
				}
				#endif

				tBeforeTime	= time( NULL );

				break;

			case 2 :

				#ifdef OS508
				pthread_mutex_lock( &g_iMsgSerialLock );
				CNLOG( NOR_LOG, 3, "[ToDay Create Serial: %d][L:%d]\n", g_iMsgSerial, __LINE__  );
				pthread_mutex_unlock( &g_iMsgSerialLock );
				#else
				CNLOG( NOR_LOG, 3, "[ToDay Create Serial: %d][L:%d]\n", g_iMsgSerial, __LINE__  );
				#endif

				tBeforeTime	= time( NULL );

				break;

			case 3 :

				//CNLOG( NOR_LOG, 3, "[Min Change][L:%d]\n", __LINE__  );

				tBeforeTime	= time( NULL );

				break;

			case 4 :

				tBeforeTime	= time( NULL );

				break;

			default :
				tBeforeTime	= time( NULL );

				break;
		
		}

		CNNanoSleep( 1000000000 );
	}

	// 14. 프로세스 종료처리 과정 진행(Thread를 모두 종료시킨 후 데이터 정리)
	{
		//14.1 LogThread 종료 과정 진행
		#ifdef OS508
		pthread_mutex_lock( &g_iLogThreadCountLock );
		CNLOG( NOR_LOG, 3, "[LOG Thread Terminate Start][Thread Count: %d][L:%d]\n", g_iLogThreadCount, __LINE__  );
		pthread_mutex_unlock( &g_iLogThreadCountLock );
		
		pthread_mutex_lock( &g_iLogThreadDoneLock );
		g_iLogThreadDone	= 0;
		pthread_mutex_unlock( &g_iLogThreadDoneLock );

		#else
		CNLOG( NOR_LOG, 3, "[LOG Thread Terminate Start][Thread Count: %d][L:%d]\n", g_iLogThreadCount, __LINE__  );
		atomic_swap_uint( &g_iLogThreadDone, 0 );
		#endif

		//14.2 LogThread의 개수가 0이 될때까지 기다린다 
		while( GetLogThreadCount() > 0 )
		{
			CNNanoSleep( 1000000000 );
		}

		#ifdef OS508
		pthread_mutex_lock( &g_iLogThreadCountLock );
		CNLOG( NOR_LOG, 3, "[LOG Thread Terminate Complete][Thread Count: %d][L:%d]\n", g_iLogThreadCount, __LINE__  );
		pthread_mutex_unlock( &g_iLogThreadCountLock );
		#else
		CNLOG( NOR_LOG, 3, "[LOG Thread Terminate Complete][Thread Count: %d][L:%d]\n", g_iLogThreadCount, __LINE__  );
		#endif

		//14.3 SmdaThread 종료 과정 진행
		#ifdef OS508
		pthread_mutex_lock( &g_iSmdaThreadCountLock );
		CNLOG( NOR_LOG, 3, "[SMDA Thread Terminate Start][Thread Count: %d][L:%d]\n", g_iSmdaThreadCount, __LINE__  );
		pthread_mutex_unlock( &g_iSmdaThreadCountLock );
		#else
		CNLOG( NOR_LOG, 3, "[SMDA Thread Terminate Start][Thread Count: %d][L:%d]\n", g_iSmdaThreadCount, __LINE__  );
		#endif

		#ifdef OS508
		pthread_mutex_lock( &g_iSmdaThreadDoneLock );
		g_iSmdaThreadDone	= 0;
		pthread_mutex_unlock( &g_iSmdaThreadDoneLock );
		#else
		atomic_swap_uint( &g_iSmdaThreadDone, 0 );
		#endif

		//14.4 SmdaThread의 개수가 0이 될때까지 기다린다 
		while( GetSmdaThreadCount() > 0 )
		{
			CNNanoSleep( 1000000000 );
		}

		#ifdef OS508
		pthread_mutex_lock( &g_iSmdaThreadCountLock );
		CNLOG( NOR_LOG, 3, "[SMDA Thread Terminate Complete][Thread Count: %d][L:%d]\n", g_iSmdaThreadCount, __LINE__  );
		pthread_mutex_unlock( &g_iSmdaThreadCountLock );
		#else
		CNLOG( NOR_LOG, 3, "[SMDA Thread Terminate Complete][Thread Count: %d][L:%d]\n", g_iSmdaThreadCount, __LINE__  );
		#endif
	}

	// 15. 데이터 반환, 종료작업 마무리(Heap 및 전역 데이터 정리)
	ProcessTerminate( 0 );

	return	0;
}


void SignalHandler( int _iSignal )
{
	CNLOG( ERR_LOG, 1, "[Signal Receive: %d][L:%d]\n", _iSignal, __LINE__ );

	#ifdef OS508
	g_iDone	= 0;
	#else
	atomic_swap_uint( &g_iDone, 0 );
	#endif

	return;
}


void ProcessTerminate( int _iReturnCode )
{
	// 1. 지금까지 발급한 Serial Number를 파일에 기록
	//    재기동시 읽어들인다.
	WriteSequenceFile();

	// 2. 로그와 관련된 메모리 해제
	CNDeleteHandle( NOR_LOG );
	CNDeleteHandle( ERR_LOG );

	// 3. 데이터 보관(주로 문자열) 메모리 해제
	if( g_cpSequenceFileName != NULL )
		free( g_cpSequenceFileName );

	if( g_cpSequenceFilePath != NULL )
		free( g_cpSequenceFilePath );	

	if( g_cpEmergencyFileName != NULL )
		free( g_cpEmergencyFileName );

	if( g_cpEmergencyFilePath != NULL )
		free( g_cpEmergencyFilePath );

	if( g_cpGutsSmdaFileName != NULL )
		free( g_cpGutsSmdaFileName );

	if( g_cpGutsSmdaFilePath != NULL )
		free( g_cpGutsSmdaFilePath );

	if( g_cpCheckQuantityPath != NULL )
		free( g_cpCheckQuantityPath );

	// 4. LOG Thread 정보 저장 메모리 해제
	LogQueueDataFree();

	// 5. SMDA Thread 정보 저장 메모리 해제
	SmdaQueueDataFree();

	// 6. SMDA Routing 정보 저장 메모리 해제
	SmdaRoutingDataFree();

	// 7. 종료!!
	exit( _iReturnCode );
}


int	ProcessCheck( char* _cpProcessName )
{
	int				iFileFd			=	0;
	int				iProcessCount	=	0;

	char			caProcFile[512];

	struct dirent*	stpDirData		=	NULL;
	DIR*			dpDirPoint		=	NULL;
	psinfo_t		stPinfo;

	// 1. Solaris 에서 프로세스 정보가 저장되는 디렉토리를 연다
	if( ( dpDirPoint = opendir( "/proc" ) ) == NULL )
	{
		fprintf( stderr, "[opendir( /proc) Error][E:%d][L:%d]\n", errno, __LINE__ );

		return	-1;
	}

	// 2. 디렉토리 정보를 뒤져가며 같은 이름(_cpProcessName)의 프로세스가 있는지 검색
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

		// 2.1 프로세스 정보를 저장하고 있는 파일명
		sprintf( caProcFile, "/proc/%s/psinfo", stpDirData->d_name );
		
		// 2.2 프로세스 정보를 저장하고 있는 파일을 연다
		if( ( iFileFd = open( caProcFile, O_RDONLY ) ) < 0 )
			continue;

		// 2.3 프로세스 정보를 구조체에 저장한다
		if( read( iFileFd, (void *)&stPinfo, sizeof( psinfo_t ) ) <= 0 )
		{
			close( iFileFd );

			continue;
		}

		// 2.4 만약 프로세스이름이 동일할 경우 해당 프로세스
		//     카운트를 증가시킨다.
		if( strcmp( _cpProcessName ,stPinfo.pr_fname ) == 0 )
			iProcessCount++;

		close( iFileFd );
	}

	// 3. 디렉토리 정보를 닫는다.
	closedir( dpDirPoint );

	// 4. 종료!!(실행중인 동일한 이름의 프로세스 개수 반환)
	return	iProcessCount;
}


int ProcessDataInit( char* _cpConfigFilePath )
{
	int		iResult;
	int		iLength;
	int		iLogType;
	int		iLogLevel;

	char	caBuffer1[512];
	char	caBuffer2[512];
	char	caConfigFile[512];
	char	caQuanTityBuffer[512];

	char	caSequencePathBuffer[512];
	char	caSequenceFileBuffer[512];

	char	caEmergencyPathBuffer[512];
	char	caEmergencyFileBuffer[512];

	// 1. 프로세스 컨피그 파일 셋팅
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, LOG_MANAGER_CONFIG_FILE_NAME );

	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"CENTER_TYPE", &g_iCenterType ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON CENTER_TYPE %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &g_iCenterType, iResult, __LINE__ );

		return	-1;
	}

	// 2. Sequence 데이터를 기록할 파일의 경로 셋팅(절대경로)
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"SEQUENCE_FILE_PATH", caSequencePathBuffer, sizeof(caSequencePathBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON SEQUENCE_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caSequencePathBuffer, sizeof(caSequencePathBuffer), iResult, __LINE__ );

		return	-1;
	}

	iLength = strlen( caSequencePathBuffer ) + 1;
	g_cpSequenceFilePath = (char*)malloc( iLength );
	strlcpy( g_cpSequenceFilePath, caSequencePathBuffer, iLength );

	// 3. Sequence 데이터를 기록할 파일이름 셋팅
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"SEQUENCE_FILE_NAME", caSequenceFileBuffer, sizeof(caSequenceFileBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON SEQUENCE_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caSequenceFileBuffer, sizeof(caSequenceFileBuffer), iResult, __LINE__ );

		return	-1;
	}

	iLength = strlen( caSequenceFileBuffer ) + 1;
	g_cpSequenceFileName = (char*)malloc( iLength );
	strlcpy( g_cpSequenceFileName, caSequenceFileBuffer, iLength );

	// 5. EMERGENCY 모드시 파일쓰기 한계 용량 셋팅( 디스크 사용량이 g_iMaxDiskQuantity 가 넘으면 이머전시 데이터를 쓰지 않는다.)
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"MAX_DISK_QUANTITY", &g_iMaxDiskQuantity ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON MAX_DISK_QUANTITY %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &g_iMaxDiskQuantity, iResult, __LINE__ );

		return	-1;
	}

	// 6. EMERGENCY 모드시 디스크 사용량을 체크 할 디스크 경로 셋팅(절대경로)
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"CHECK_QUANTITY_PATH", caQuanTityBuffer, sizeof(caQuanTityBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON CHECK_QUANTITY_DISK %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caQuanTityBuffer, sizeof(caQuanTityBuffer), iResult, __LINE__ );

		return	-1;
	}

	iLength	= strlen( caQuanTityBuffer ) + 1;
	g_cpCheckQuantityPath = (char*)malloc( iLength );
	strlcpy( g_cpCheckQuantityPath, caQuanTityBuffer, iLength );

	// 7. EMERGENCY 모드시 생성할 EMERGENCY 데이터 파일의 경로(절대경로)
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"EMERGENCY_FILE_PATH", caEmergencyPathBuffer, sizeof(caEmergencyPathBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON EMERGENCY_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caEmergencyPathBuffer, sizeof(caEmergencyPathBuffer), iResult, __LINE__ );

		return	-1;
	}

	iLength	= strlen( caEmergencyPathBuffer ) + 1;
	g_cpEmergencyFilePath = (char*)malloc( iLength );
	strlcpy( g_cpEmergencyFilePath, caEmergencyPathBuffer, iLength );

	// 8. EMERGENCY 모드시 생성할 EMERGENCY 데이터 파일의 이름
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"EMERGENCY_FILE_NAME", caEmergencyFileBuffer, sizeof(caEmergencyFileBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON EMERGENCY_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caEmergencyFileBuffer, sizeof(caEmergencyFileBuffer), iResult, __LINE__ );

		return	-1;
	}

	iLength	= strlen( caEmergencyFileBuffer ) + 1;
	g_cpEmergencyFileName = (char*)malloc( iLength );
	strlcpy( g_cpEmergencyFileName, caEmergencyFileBuffer, iLength );

	// 9. 컨피그 파일에서 일반 로그파일의 생성타입(날짜별,시간별) 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"NORMAL_LOG_FILE_TYPE", &iLogType ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON NORMAL_LOG_FILE_TYPE %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iLogType, iResult, __LINE__ );

		return	-1;
	}

	// 10. 컨피그 파일에서 일반 로그파일의 로그레벨(레벨에 따라서 로그가 세분화 및 간소화 된다) 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"NORMAL_LOG_FILE_LEVEL", &iLogLevel ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON NORMAL_LOG_FILE_LEVEL %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iLogLevel, iResult, __LINE__ );

		return	-1;
	}

	// 11. 컨피그 파일에서 일반 로그파일이 생성 될 경로를(절대경로) 셋팅
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"NORMAL_LOG_FILE_PATH", caBuffer1, sizeof(caBuffer1) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON NORMAL_LOG_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caBuffer1, sizeof(caBuffer1), iResult, __LINE__ );

		return	-1;
	}

	// 12. 컨피그 파일에서 일반 로그파일의 파일이름 셋팅
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"NORMAL_LOG_FILE_NAME", caBuffer2, sizeof(caBuffer2) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON NORMAL_LOG_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caBuffer2, sizeof(caBuffer2), iResult, __LINE__ );

		return	-1;
	}

	// 13. 컨피그에서 읽어들인 데이터로 일반로그 로거 초기화 및 등록(이때부터 일반로그 쓰기 가능)
	if( ( NOR_LOG = CNGetLogHandle( iLogType, iLogLevel, caBuffer1, caBuffer2, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" ) ) == NULL )
	{
		fprintf( stderr, "[CNGetLogHandle( %d %d %s %s %s ) Error][L:%d]\n", 
			iLogType, iLogLevel, caBuffer1, caBuffer2, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A", __LINE__ );

		return	-1;
	}

	// 14. 컨피그 파일에서 에러 로그파일의 생성타입(날짜별,시간별) 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"ERROR_LOG_FILE_TYPE", &iLogType ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON ERROR_LOG_FILE_TYPE %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iLogType, iResult, __LINE__ );

		return	-1;
	}

	// 15. 컨피그 파일에서 에러 로그파일의 로그레벨(레벨에 따라서 로그가 세분화 및 간소화 된다) 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"ERROR_LOG_FILE_LEVEL", &iLogLevel ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON ERROR_LOG_FILE_LEVEL %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iLogLevel, iResult, __LINE__ );

		return	-1;
	}

	// 16. 컨피그 파일에서 에러 로그파일이 생성 될 경로를(절대경로) 셋팅
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"ERROR_LOG_FILE_PATH", caBuffer1, sizeof(caBuffer1) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON ERROR_LOG_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caBuffer1, sizeof(caBuffer1), iResult, __LINE__ );

		return	-1;
	}

	// 17. 컨피그 파일에서 에러 로그파일의 파일이름 셋팅
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"ERROR_LOG_FILE_NAME", caBuffer2, sizeof(caBuffer2) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON ERROR_LOG_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caBuffer2, sizeof(caBuffer2), iResult, __LINE__ );

		return	-1;
	}

	// 18. 컨피그에서 읽어들인 데이터로 에러로그 로거 초기화 및 등록(이때부터 에러로그 쓰기 가능)
	if( ( ERR_LOG = CNGetLogHandle( iLogType, iLogLevel, caBuffer1, caBuffer2, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" ) ) == NULL )
	{
		fprintf( stderr, "[CNGetLogHandle( %d %d %s %s %s ) Error][L:%d]\n", 
			iLogType, iLogLevel, caBuffer1, caBuffer2, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A", __LINE__ );

		return	-1;
	}

	// 19. 성공!!
	return	0;
}


int	EmergencyTableInit( char* _cpConfigFilePath )
{
	int		iResult;
	int		iSharedID;
	int		iEmergencyKey;

	char	caConfigFile[512];

	// 1. 읽어들일 컨피그 파일(파일경로+파일명) 셋팅
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, LOG_MANAGER_CONFIG_FILE_NAME );

	// 2. Emergency 모드 Flag 값의 Shared Memory Key값을 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"EMERGENCY_MODE_SHM_KEY", &iEmergencyKey ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s COMMON THREAD_LOG_FILE_TYPE %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iEmergencyKey, iResult, __LINE__ );

		return	NULL;
	}

	CNLOG( NOR_LOG, 3, "[Emergency Mode Shared Memory Key: 0x%x][L:%d]\n", iEmergencyKey, __LINE__ );

	// 3. Shared Memory Key값으로 Shared Memory 의 SharedID 값을 셋팅
	if( ( iSharedID = shmget( iEmergencyKey, 0, 0 ) ) < 0 )
	{
		CNLOG( NOR_LOG, 3, "[Emergency Mode Shared Memory Not Exist][R:%d][E:%d][L:%d]\n", iSharedID, errno, __LINE__ );

		// 3.1 SharedID 셋팅에 실패한 경우(Shared Memory 공간이 할당되어있지 않은경우) Shared Memory 공간 생성
		if( ( iSharedID = shmget( (key_t)iEmergencyKey, sizeof(int), 0666 | IPC_CREAT ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[shmget( %d %d %d) Error][R:%d][E:%d][L:%d]\n", 
				iEmergencyKey, sizeof(int), 0666 | IPC_CREAT, iSharedID, errno, __LINE__ );

			return	-1;
		}

		// 3.2 Shared Memory Attach
		if( ( g_ipEmergencyMode = (int*)shmat( iSharedID, (char*)0, 0 ) ) == NULL )
		{
			CNLOG( ERR_LOG, 1, "[shmat( %d 0 0 ) Error][E:%d][L:%d]\n", 
				iSharedID, errno, __LINE__ );

			return	-1;
		}

		// 3.3 Shared Memory에 Emergency Flag를 처음 생성한 경우 Emergency 모드는 0(끔)으로 셋팅
		*g_ipEmergencyMode	= 0;
	}
	else
	{
		CNLOG( ERR_LOG, 1, "[Emergency Mode Shared Memory Exist][L:%d]\n", __LINE__ );

		// 3.4 SharedID 셋팅에 성공한 경우 Shared Memory에 Attach 시도
		if( ( g_ipEmergencyMode = (int*)shmat( iSharedID, (char*)0, 0 ) ) == NULL )
		{
			CNLOG( ERR_LOG, 1, "[shmat( %d 0 0 ) Error][E:%d][L:%d]\n", 
				iSharedID, errno, __LINE__ );

			return	-1;
		}
	}

	CNLOG( NOR_LOG, 3, "[Emergency Mode : %02d][L:%d]\n", *g_ipEmergencyMode, __LINE__ );

	// 4. 성공!!
	return	0;
}


int LogQueueDataInit( char* _cpConfigFilePath )
{
	int		iResult;
	int		iForLoop;
	int		iLogQueueNum;
	int		iLogQueueLimit;
	int		iLogQueueKey;

	char	caConfigFile[512];
	char	caPartBuffer[128];

	char	caErrorLogFilePath[256];
	char	caErrorLogFileName[128];

	// 1. LogQueue 및 LogQueue Thread 구조체 맴버 초기화
	g_stLogThread.iUseNum	= 0;
	g_stLogThread.Info		= NULL;

	// 2. LogQueue및 LogQueue Thread에 대한 데이터를 읽어들일 Config 파일이름(절대경로+파일이름) 셋팅
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, LOG_QUEUE_INFO_FILE_NAME );

	// 3. 컨피그 파일에서 Queue의 개수및 Queue를 담당하는 Thread의 개수(LOG_QUEUE_NUM)를 읽어서 저장(Queue와 Thread는 1:1 매칭)
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"LOG_QUEUE_NUM", &iLogQueueNum ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s COMMON LOG_QUEUE_NUM %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iLogQueueNum, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[LOG_THREAD NUM: %d][L:%d]\n", iLogQueueNum, __LINE__ );

	// 4. 컨피그 파일에서 LogQueue 에 저장할수 있는 최대 메시지수를 읽어서 저장
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"MAX_QUEUE_COUNT", &g_iMaxLogQueueCount ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s COMMON MAX_QUEUE_COUNT %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &g_iMaxLogQueueCount, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[MAX_LOG_QUEUE_COUNT: %d][L:%d]\n", g_iMaxLogQueueCount, __LINE__ );

	// 5. LogQueue Thread 가 처리중인 데이터가 오류가 날 경우 데이터를 기록할 파일의 경로(절대경로)
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"ERROR_FILE_PATH", caErrorLogFilePath, sizeof(caErrorLogFilePath) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s COMMON ERROR_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caErrorLogFilePath, sizeof(caErrorLogFilePath), iResult, __LINE__ );

		return	-1;
	}

	g_cpErrorLogThreadFilePath = (char*)malloc( strlen( caErrorLogFilePath ) + 1 );
	strlcpy( g_cpErrorLogThreadFilePath, caErrorLogFilePath, strlen( caErrorLogFilePath ) + 1 );

	CNLOG( NOR_LOG, 3, "[ERROR_LOG_FILE_PATH: %s][L:%d]\n", g_cpErrorLogThreadFilePath, __LINE__ );

	// 6. LogQueue Thread 가 처리중인 데이터가 오류가 날 경우 데이터를 기록할 파일의 이름
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"ERROR_FILE_NAME", caErrorLogFileName, sizeof(caErrorLogFileName) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s COMMON ERROR_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caErrorLogFileName, sizeof(caErrorLogFileName), iResult, __LINE__ );

		return	-1;
	}

	g_cpErrorLogThreadFileName = (char*)malloc( strlen( caErrorLogFileName ) + 1 );
	strlcpy( g_cpErrorLogThreadFileName, caErrorLogFileName, strlen( caErrorLogFileName ) + 1 );

	CNLOG( NOR_LOG, 3, "[ERROR_LOG_FILE_NAME: %s][L:%d]\n", g_cpErrorLogThreadFileName, __LINE__ );

	// 7. Queue 개수(Queue의 개수 = Thread의 개수)만큼 메모리를 할당(각각의 Queue 및 Thread 정보를 저장할 공간)
	g_stLogThread.iUseNum	= iLogQueueNum;
	g_stLogThread.Info		= (stLogThreadInfo*)malloc( sizeof(stLogThreadInfo) * g_stLogThread.iUseNum );

	memset( g_stLogThread.Info, 0x00, sizeof(stLogThreadInfo) * g_stLogThread.iUseNum );

	// 8. 컨피그 파일로부터 Queue 및 Thread에 대한 데이터 초기화
	for( iForLoop = 0; iForLoop < iLogQueueNum; iForLoop++ )
	{
		sprintf( caPartBuffer, "QUQUE_%d", iForLoop + 1 );

		// 8.1 Queue Key 값을 읽어들인다
		if( ( iResult = CNGetConfigInt( caConfigFile, caPartBuffer, (char*)"LOG_QUEUE_KEY", &iLogQueueKey ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s LOG_QUEUE %s %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer, &iLogQueueKey, iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[LOG_THREAD_%02d][Queue Key: 0x%x][L:%d]\n", iForLoop + 1, iLogQueueKey, __LINE__ );

		g_stLogThread.Info[iForLoop].iThreadIndex	= iForLoop;
		g_stLogThread.Info[iForLoop].iQueueKey		= iLogQueueKey;
	}

	// 9. 성공!!
	return	0;
}


int SmdaQueueDataInit( char* _cpConfigFilePath )
{
	int		iResult;
	int		iLength;
	int		iSmdaLoop;
	int		iSessionLoop;

	int		iSmdaServerNum;
	int		iSmdaSessionNum;
	int		iSmdaQueueKey;
	int		iSmdaQueueID;
	int		iSmdaPort;

	char	caConfigFile[512];
	char	caPartBuffer1[128];
	char	caPartBuffer2[128];

	char	caSmdaAdress[16];

	char	caSmdaBindID[16];
	char	caSmdaBindPW[16];

	char	caGutsSmdaFilePath[256];
	char	caGutsSmdaFileName[128];

	char	caErrorSmdaFilePath[256];
	char	caErrorSmdaFileName[128];

	// 1. SmdaQueue 및 SmdaQueue Thread 구조체 맴버 초기화
	g_stSmdaThread.iSmdaServerNum	= 0;
	g_stSmdaThread.Info		= NULL;

	// 2. 읽어들일 Config 파일(SmdaQueue Config File)이름 셋팅
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, SMDA_QUEUE_INFO_FILE_NAME );

	// 3. 컨피그 파일에서 Queue의 개수(Queue 개수 = Thread의 개수)를 읽어서 저장	
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"SMDA_SERVER_NUM", &iSmdaServerNum ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON SMDA_SERVER_NUM %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iSmdaServerNum, iResult, __LINE__ );

		return	-1;
	}

	if( iSmdaServerNum <= 0 )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][Read Config Format Error][SMDA_SERVER_NUM: %d][L:%d]\n", iSmdaServerNum, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[SMDA][SMDA_SERVER_NUM: %d][L:%d]\n", iSmdaServerNum, __LINE__ );

	// 4. 컨피그 파일에서 Log Queue에 저장 할 최대 메시지의 개수를 읽어서 저장
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"MAX_QUEUE_COUNT", &g_iMaxSmdaQueueCount ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON MAX_QUEUE_COUNT %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &g_iMaxSmdaQueueCount, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[SMDA][MAX_QUEUE_COUNT: %d][L:%d]\n", g_iMaxSmdaQueueCount, __LINE__ );

	// 5. 컨피그 파일에서 Guts 파일(쓰레드 종료 및 재시작시 재처리 데이터를 저장하는 파일)의 경로 셋팅(절대경로)
	memset( caGutsSmdaFilePath,  0x00, sizeof( caGutsSmdaFilePath ) );

	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"GUTS_FILE_PATH", caGutsSmdaFilePath, sizeof(caGutsSmdaFilePath) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON GUTS_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caGutsSmdaFilePath, sizeof(caGutsSmdaFilePath), iResult, __LINE__ );

		return	-1;
	}

	g_cpGutsSmdaFilePath = (char*)malloc( strlen( caGutsSmdaFilePath ) + 1 );
	strlcpy( g_cpGutsSmdaFilePath, caGutsSmdaFilePath, strlen( caGutsSmdaFilePath ) + 1 );

	CNLOG( NOR_LOG, 3, "[SMDA][GUTS_FILE_PATH: %s][L:%d]\n", g_cpGutsSmdaFilePath, __LINE__ );

	memset( caGutsSmdaFileName,  0x00, sizeof( caGutsSmdaFileName ) );

	// 6. 컨피그 파일에서 Guts 파일(쓰레드 종료 및 재시작시 재처리 데이터를 저장하는 파일)의 파일이름 셋팅
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"GUTS_FILE_NAME", caGutsSmdaFileName, sizeof(caGutsSmdaFileName) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON GUTS_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caGutsSmdaFileName, sizeof(caGutsSmdaFileName), iResult, __LINE__ );

		return	-1;
	}

	g_cpGutsSmdaFileName = (char*)malloc( strlen( caGutsSmdaFileName ) + 1 );
	strlcpy( g_cpGutsSmdaFileName, caGutsSmdaFileName, strlen( caGutsSmdaFileName ) + 1 );

	CNLOG( NOR_LOG, 3, "[SMDA][GUTS_FILE_NAME: %s][L:%d]\n", caGutsSmdaFileName, __LINE__ );

	// 7. SmdaQueue 데이터 재처리 기능의 사용유무 셋팅(0:미사용 0이 아닌수:사용)
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"RETRY_USE_FLAG", &g_iSmdaRetryUseFlag ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON RETRY_USE_FLAG %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &g_iSmdaRetryUseFlag, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[SMDA][RETRY_USE_FLAG: %d][L:%d]\n", g_iSmdaRetryUseFlag, __LINE__ );

	// 8. SmdaQueue 데이터 재처리 기능을 사용 할 경우 세부 데이터 셋팅
	if( g_iSmdaRetryUseFlag != 0 )
	{
		// 8.1 재처리 데이터를 처리하는 주기(초단위) 셋팅
		if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"RETRY_CYCLE", &g_iSmdaRetryCycle ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON RETRY_CYCLE %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, &g_iSmdaRetryCycle, iResult, __LINE__ );

			return	-1;
		}

		if( g_iSmdaRetryCycle <= 0 )
		{
			CNLOG( ERR_LOG, 1, "[SMDA][Retry Cycle Format Error][Cycle: %d][L:%d]\n", g_iSmdaRetryCycle, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[SMDA][RETRY_CYCLE: %d][L:%d]\n", g_iSmdaRetryCycle, __LINE__ );

		// 8.2 재처리를 진행 할 데이터의 시간(마지막 처리시간이 RETRY_TIME 이상인 데이터를 재처리)
		if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"RETRY_TIME", &g_iSmdaRetryTime ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON RETRY_TIME %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, &g_iSmdaRetryTime, iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[SMDA][RETRY_TIME: %d][L:%d]\n", g_iSmdaRetryTime, __LINE__ );

		// 8.3 재처리를 진행 할 데이터의 재처리 회수 셋팅
		if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"RETRY_COUNT", &g_iSmdaRetryCount ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON RETRY_COUNT %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, &g_iSmdaRetryCount, iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[SMDA][RETRY_COUNT: %d][L:%d]\n", g_iSmdaRetryCount, __LINE__ );
	}

	// 9. SmdaQueue 데이터 처리도중 이상(재처리 실패, 포맷에러 등등)이 생긴 데이터를 저장 할 파일의 경로(절대경로)
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"ERROR_FILE_PATH", caErrorSmdaFilePath, sizeof(caErrorSmdaFilePath) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON ERROR_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caErrorSmdaFilePath, sizeof(caErrorSmdaFilePath), iResult, __LINE__ );

		return	-1;
	}

	g_cpErrorSmdaThreadFilePath	= (char*)malloc( strlen( caErrorSmdaFilePath ) + 1 );
	strlcpy( g_cpErrorSmdaThreadFilePath, caErrorSmdaFilePath, strlen( caErrorSmdaFilePath ) + 1 );

	CNLOG( NOR_LOG, 3, "[SMDA][ERROR_FILE_PATH: %s][L:%d]\n", g_cpErrorSmdaThreadFilePath, __LINE__ );

	// 10. SmdaQueue 데이터 처리도중 이상(재처리 실패, 포맷에러 등등)이 생긴 데이터를 저장 할 파일의 이름
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"ERROR_FILE_NAME", caErrorSmdaFileName, sizeof(caErrorSmdaFileName) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON ERROR_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caErrorSmdaFileName, sizeof(caErrorSmdaFileName), iResult, __LINE__ );

		return	-1;
	}

	g_cpErrorSmdaThreadFileName	= (char*)malloc( strlen( caErrorSmdaFileName ) + 1 );
	strlcpy( g_cpErrorSmdaThreadFileName, caErrorSmdaFileName, strlen( caErrorSmdaFileName ) + 1 );

	CNLOG( NOR_LOG, 3, "[SMDA][ERROR_FILE_NAME: %s][L:%d]\n", g_cpErrorSmdaThreadFileName, __LINE__ );

	g_stSmdaThread.iSmdaServerNum = iSmdaServerNum;

	g_stSmdaThread.Info = (stSmdaThreadInfo*)malloc( sizeof(stSmdaThreadInfo) * g_stSmdaThread.iSmdaServerNum );

	if( g_stSmdaThread.Info == NULL )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][malloc( %d ) Error][L:%d]\n", sizeof(stSmdaThreadInfo) * g_stSmdaThread.iSmdaServerNum, __LINE__ );

		return	-1;
	}

	memset( g_stSmdaThread.Info, 0x00, sizeof(stSmdaThreadInfo) * g_stSmdaThread.iSmdaServerNum );

	// 11. 컨피그 파일에서 데이터를 읽어들이고 Smda Queue 및 Smda Queue Thread 데이터 초기화
	for( iSmdaLoop = 0; iSmdaLoop < g_stSmdaThread.iSmdaServerNum; iSmdaLoop++ )
	{
		sprintf( caPartBuffer1, "SMDA_%d", iSmdaLoop + 1 );

		// 11.1 Smda Session 개수 값을 읽어들인다
		if( ( iResult = CNGetConfigInt( caConfigFile, caPartBuffer1, (char*)"SESSION_NUM", &iSmdaSessionNum ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[%s][CNGetConfigInt( %s %s SESSION_NUM %d ) Error][R:%d][L:%d]\n", 
				caPartBuffer1, caConfigFile, caPartBuffer1, &iSmdaSessionNum, iResult, __LINE__ );

			return	-1;
		}

		if( iSmdaSessionNum <= 0 )
		{
			CNLOG( ERR_LOG, 1, "[SMDA][Read Format Error][SESSION_NUM: %d][L:%d]\n", iSmdaSessionNum, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[%s][SESSION_NUM: %d][L:%d]\n", caPartBuffer1, iSmdaSessionNum, __LINE__ );

		// 11.2 Smda 서버의 IP 주소를 읽어들인다
		memset( caSmdaAdress, 0x00, sizeof( caSmdaAdress ) );

		if( ( iResult = CNGetConfigStr( caConfigFile, caPartBuffer1, (char*)"SMDA_IP", caSmdaAdress, sizeof(caSmdaAdress) ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[%s][CNGetConfigInt( %s %s SMDA_IP %d %d ) Error][R:%d][L:%d]\n", 
				caPartBuffer1, caConfigFile, caPartBuffer1, caSmdaAdress, sizeof(caSmdaAdress), iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[%s][SMDA_IP: %s][L:%d]\n", caPartBuffer1, caSmdaAdress, __LINE__ );

		// 11.3 Smda 서버의 Port를 읽어들인다
		if( ( iResult = CNGetConfigInt( caConfigFile, caPartBuffer1, (char*)"SMDA_PORT", &iSmdaPort ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[%s][CNGetConfigInt( %s %s SMDA_PORT %d ) Error][R:%d][L:%d]\n", 
				caPartBuffer1, caConfigFile, caPartBuffer1, &iSmdaPort, iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[%s][SMDA_PORT: %d][L:%d]\n", caPartBuffer1, iSmdaPort, __LINE__ );

		// 11.4 Smda 서버의 BIND ID 를 읽어들인다
		memset( caSmdaBindID, 0x00, sizeof( caSmdaBindID ) );

		if( ( iResult = CNGetConfigStr( caConfigFile, caPartBuffer1, (char*)"SMDA_BIND_ID", caSmdaBindID, sizeof(caSmdaBindID) ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[%s][CNGetConfigInt( %s %s SMDA_BIND_ID %d %d ) Error][R:%d][L:%d]\n", 
				caPartBuffer1, caConfigFile, caPartBuffer1, caSmdaBindID, sizeof(caSmdaBindID), iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[%s][SMDA_BIND_ID: %s][L:%d]\n", caPartBuffer1, caSmdaBindID, __LINE__ );

		// 11.5 Smda 서버의 BIND PASSWORD 를 읽어들인다
		memset( caSmdaBindPW, 0x00, sizeof( caSmdaBindPW ) );

		if( ( iResult = CNGetConfigStr( caConfigFile, caPartBuffer1, (char*)"SMDA_BIND_PW", caSmdaBindPW, sizeof(caSmdaBindPW) ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[%s][CNGetConfigInt( %s %s SMDA_BIND_PW %d %d ) Error][R:%d][L:%d]\n", 
				caPartBuffer1, caConfigFile, caPartBuffer1, caSmdaBindPW, sizeof(caSmdaBindPW), iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[%s][SMDA_BIND_PW: %s][L:%d]\n", caPartBuffer1, caSmdaBindPW, __LINE__ );

		// 11.6 Smda Queue Thread 구조체에 데이터를 셋팅
		#ifdef OS508
		pthread_mutex_init( &(g_stSmdaThread.Info[iSmdaLoop].Lock), NULL );
		pthread_mutex_lock( &(g_stSmdaThread.Info[iSmdaLoop].Lock) );
		g_stSmdaThread.Info[iSmdaLoop].iMessageCount	= 0;
		pthread_mutex_unlock( &(g_stSmdaThread.Info[iSmdaLoop].Lock) );
		#else
		atomic_swap_uint( &g_stSmdaThread.Info[iSmdaLoop].iMessageCount, 0 );
		#endif

		g_stSmdaThread.Info[iSmdaLoop].iSmdaSessionNum	= iSmdaSessionNum;
		g_stSmdaThread.Info[iSmdaLoop].iPort			= iSmdaPort;

		strlcpy( g_stSmdaThread.Info[iSmdaLoop].caAdress, caSmdaAdress, sizeof(g_stSmdaThread.Info[iSmdaLoop].caAdress) );
		strlcpy( g_stSmdaThread.Info[iSmdaLoop].caBindID, caSmdaBindID, sizeof(g_stSmdaThread.Info[iSmdaLoop].caBindID) );
		strlcpy( g_stSmdaThread.Info[iSmdaLoop].caBindPW, caSmdaBindPW, sizeof(g_stSmdaThread.Info[iSmdaLoop].caBindPW) );

		g_stSmdaThread.Info[iSmdaLoop].stpQueue	= (stSmdaQueue*)malloc( sizeof(stSmdaQueue) * g_stSmdaThread.Info[iSmdaLoop].iSmdaSessionNum );

		if( g_stSmdaThread.Info[iSmdaLoop].stpQueue == NULL )
		{
			CNLOG( ERR_LOG, 1, "[SMDA][malloc( %d ) Error][E:%d][L:%d]\n", 
				sizeof( stSmdaQueue ) * g_stSmdaThread.Info[iSmdaLoop].iSmdaSessionNum, errno, __LINE__ );

			return	-1;
		}

		memset( g_stSmdaThread.Info[iSmdaLoop].stpQueue, 0x00, sizeof(stSmdaQueue) * g_stSmdaThread.Info[iSmdaLoop].iSmdaSessionNum );

		// 11.7 Smda Queue Thread 구조체에 Session 데이터를 셋팅
		for( iSessionLoop = 0; iSessionLoop < g_stSmdaThread.Info[iSmdaLoop].iSmdaSessionNum; iSessionLoop++ )
		{
			sprintf( caPartBuffer2, "SMDA_QUEUE_KEY_%d", iSessionLoop + 1 );

			// 11.7.1 Smda Queue Key 값을 읽어들인다
			if( ( iResult = CNGetConfigInt( caConfigFile, caPartBuffer1, caPartBuffer2, &iSmdaQueueKey ) ) < 0 )
			{
				CNLOG( ERR_LOG, 1, "[%s][CNGetConfigInt( %s %s %s %d ) Error][R:%d][L:%d]\n", 
					caPartBuffer1, caConfigFile, caPartBuffer1, caPartBuffer2, &iSmdaQueueKey, iResult, __LINE__ );

				return	-1;
			}

			CNLOG( NOR_LOG, 3, "[%s][%s: 0x%x][L:%d]\n", caPartBuffer1, caPartBuffer2, iSmdaQueueKey, __LINE__ );

			// 11.7.2 Queue 생성 및 Attach
			if( ( iSmdaQueueID = MakeAndOpenQueue( iSmdaQueueKey, g_iMaxSmdaQueueCount * sizeof( stLogPacket ) ) ) < 0 )
			{
				CNLOG( ERR_LOG, 1, "[%s][MakeAndOpenQueue( 0x%x %d ) Error][R:%d][E:%d][L:%d]\n", 
					caPartBuffer1, iSmdaQueueKey, g_iMaxSmdaQueueCount * sizeof( stLogPacket ), iSmdaQueueID, errno, __LINE__ );
			}

			CNLOG( NOR_LOG, 3, "[%s][SMDA_QUEUE_ID_%d: %d][L:%d]\n", caPartBuffer1, iSessionLoop + 1, iSmdaQueueID, __LINE__ );

			// 11.7.3 읽어들인 Queue 정보를 Smda Thread 구조체에 셋팅
			pthread_mutex_init( &(g_stSmdaThread.Info[iSmdaLoop].stpQueue[iSessionLoop].Lock), NULL );
			pthread_mutex_lock( &(g_stSmdaThread.Info[iSmdaLoop].stpQueue[iSessionLoop].Lock) );
			g_stSmdaThread.Info[iSmdaLoop].stpQueue[iSessionLoop].iQueueKey	= iSmdaQueueKey;
			g_stSmdaThread.Info[iSmdaLoop].stpQueue[iSessionLoop].iQueueID		= iSmdaQueueID;
			pthread_mutex_unlock( &(g_stSmdaThread.Info[iSmdaLoop].stpQueue[iSessionLoop].Lock) );
		}
	}

	// 12. 성공!!
	return	0;
}


int SmdaRoutingDataInit( char* _cpConfigFilePath )
{
	int		iResult;
	int		iForLoop;
	int		iPrefixLoop;

	int		iSmdaRoutingNum;
	int		iRoutingQueue;
	int		iStartPrefix;
	int		iEndPrefix;

	int		*ipRoutingTable;

	char	caConfigFile[512];
	char	caPartBuffer[128];

	char	caStartPrefix[8];
	char	caEndPrefix[8];

	// 1. SmdaRouting 데이터를 읽어들일 Config 파일의 파일명 셋팅
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, SMDA_ROUTING_INFO_FILE_NAME );

	// 2. 사용하는 Routing Prefix 개수 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"SMDA_ROUTING", (char*)"SMDA_ROUTING_NUM", &iSmdaRoutingNum ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING SMDA_ROUTING_NUM %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iSmdaRoutingNum, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[SMDA_ROUTING_NUM: %d][L:%d]\n", iSmdaRoutingNum, __LINE__ );

	// 3. ETC Routing Queue Number(등록되지 않은 대역의 데이터가 들어올 경우 데이터를 전송 할 Queue의 Number) 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"SMDA_ROUTING", (char*)"SMDA_ETC_ROUTING", &g_iEtcRoutingQueue ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING SMDA_ETC_ROUTING %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &g_iEtcRoutingQueue, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[SMDA_ETC_ROUTING: %d][L:%d]\n", g_iEtcRoutingQueue, __LINE__ );

	g_iEtcRoutingQueue	= g_iEtcRoutingQueue - 1;

	// 4. 임시로 Prefix 데이터를 저장 할 공간 할당(셋팅이 문제없이 끝나면 덮었쓴다)
	ipRoutingTable = (int*)malloc( sizeof(int) * 9999999 );

	for( iForLoop = 0; iForLoop < 9999999; iForLoop++ )
		ipRoutingTable[iForLoop]	= g_iEtcRoutingQueue;

	// 5. 컨피그 파일에서 Prefix 정보를 읽어서 임시 저장공간에 기록
	for( iForLoop = 1; iForLoop < iSmdaRoutingNum + 1;iForLoop++ )
	{
		// 5.1 시작 라우팅 번호 대역 셋팅(라우팅은 5자리수 라우팅)
		sprintf( caPartBuffer, "SMDA_ROUTING_STR_%d", iForLoop );

		if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"SMDA_ROUTING", caPartBuffer, caStartPrefix, sizeof(caStartPrefix) ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING %s %d %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer, caStartPrefix, sizeof(caStartPrefix), iResult, __LINE__ );

			return	-1;
		}

		iStartPrefix = atoi( caStartPrefix );

		sprintf( caPartBuffer, "SMDA_ROUTING_END_%d", iForLoop );

		// 5.2 끝 라우팅 번호 대역 셋팅(라우팅은 5자리수 라우팅)
		if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"SMDA_ROUTING", caPartBuffer, caEndPrefix, sizeof(caEndPrefix) ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING %s %d %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer, caEndPrefix, sizeof(caEndPrefix), iResult, __LINE__ );

			return	-1;
		}

		iEndPrefix = atoi( caEndPrefix );

		sprintf( caPartBuffer, "SMDA_ROUTING_QUEUE_%d", iForLoop );

		// 5.3 시작에서 끝 라우팅 대역까지의 데이터를 전송 할 Queue의 Number(Queue Index)
		if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"SMDA_ROUTING", caPartBuffer, &iRoutingQueue ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s SMDA_QUEUE %s %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer, &iRoutingQueue, iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[PREFIX: %s ~ %s][QUEUE: %02d][L:%d]\n", caStartPrefix, caEndPrefix, iRoutingQueue, __LINE__ );

		// 5.4 읽어들인 데이터로 라우팅 테이블(Smda Routing Table)을 셋팅
		for ( iPrefixLoop = iStartPrefix; iPrefixLoop < iEndPrefix + 1; iPrefixLoop++ )
			ipRoutingTable[iPrefixLoop] = iRoutingQueue - 1;
	}

	// 6. Smda Routing 데이터 공간이 없을경우(최초기동인 경우) 공간할당
	if( g_stSmdaRouting.Index == NULL )
		g_stSmdaRouting.Index = (int*)malloc( sizeof(int) * 99999 );
	
	// 7. 임시공간에 기록한 Routing 정보를 복사
	memcpy( g_stSmdaRouting.Index, ipRoutingTable, sizeof(int) * 99999 );

	// 8. 임시공간 해제
	free( ipRoutingTable );

	// 9. 성공!!
	return	0;
}


void LogQueueDataFree()
{
	g_stLogThread.iUseNum	= 0;

	// 1. LOG Thread 정보를 저장하는 메모리 공간 해제
	if( g_stLogThread.Info != NULL )
		free( g_stLogThread.Info );

	g_stLogThread.Info	= NULL;

	// 2. LOG Thread Error 파일이름 저장하는 메모리 공간 해제
	if( g_cpErrorLogThreadFileName != NULL )
		free( g_cpErrorLogThreadFileName );

	g_cpErrorLogThreadFileName	= NULL;

	// 3. LOG Thread Error 파일경로 저장하는 메모리 공간 해제
	if( g_cpErrorLogThreadFilePath != NULL )
		free( g_cpErrorLogThreadFilePath );

	g_cpErrorLogThreadFilePath	= NULL;

	// 4. 종료!!
	return;

}


void SmdaQueueDataFree()
{
	int	iForLoop;

	// 1. SMDA Thread 정보를 저장하는 메모리 공간이 존재하는 경우
	if( g_stSmdaThread.Info != NULL )
	{
		// 1.1 SMDA Thread 정보는 SMDA서버 숫자 만큼 생성된다(숫자만큼 돈다)
		for( iForLoop = 0; iForLoop < g_stSmdaThread.iSmdaServerNum; iForLoop++ )
		{
			// 1.1.1 SMDA 서버정보 안에는 Session 숫자만큼 Session 정보
			//       (Queue)가 생성된다. (Session 숫자만큼 돈다)
			if( g_stSmdaThread.Info[iForLoop].stpQueue != NULL )
				free( g_stSmdaThread.Info[iForLoop].stpQueue );
		}

		// 1.2 Session(Queue)정보를 모두 해제한 후에
		//     SMDA 서버정보 메모리를 해제
		free( g_stSmdaThread.Info );

		// 1.3 SMDA 서버정보 저장 공간 주소 초기화
		g_stSmdaThread.Info	= NULL;

		// 1.4 SMDA 서버숫자 초기화
		g_stSmdaThread.iSmdaServerNum	= 0;
	}

	// 2. SMDA Thread 의 Error 파일이름을 저장하는 메모리 공간을 해제
	if( g_cpErrorSmdaThreadFileName != NULL )
		free( g_cpErrorSmdaThreadFileName );

	g_cpErrorSmdaThreadFileName	= NULL;

	// 3. SMDA Thread 의 Error 파일경로를 저장하는 메모리 공간을 해제
	if( g_cpErrorSmdaThreadFilePath != NULL )
		free( g_cpErrorSmdaThreadFilePath );

	g_cpErrorSmdaThreadFilePath	= NULL;

	// 4. 종료!!
	return;
}


void SmdaRoutingDataFree()
{
	// 1. 라우팅 숫자(Config에서 읽어들일 라우팅 대역의 숫자) 초기화
	g_stSmdaRouting.iUseNum	= 0;

	// 2. 라우팅 테이블이 존재하는 경우 메모리 공간 해제
	if( g_stSmdaRouting.Index != NULL )
		free( g_stSmdaRouting.Index );

	// 3. 라우팅 테이블 주소 초기화
	g_stSmdaRouting.Index	= NULL;

	// 4. 종료!!
	return;
}


int ReadSequenceFile()
{
	int		iResult;

	char	caDeleteCommand[512];
	char	caTimeBuffer[15];
	char	caReadFile[512];
	char	caBuffer[256];

	FILE	*fpReadFile;

	// 1. Sequence파일의 이름 생성(절대경로/파일이름_시간(YYYYMMDD)_주부센터.guts)
	//    Sequence는 현재까지 발급한 Serial Number로서 재기동시 Serial을 이어서
	//    발급하기 위해서 생성한다
	sprintf( caReadFile, "%s/%s_%-.*s_%s.guts", 
		g_cpSequenceFilePath, g_cpSequenceFileName, 8, GetNowTimeStr(caTimeBuffer), g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	// 2. Sequence 파일 열기 예외처리
	if( ( fpReadFile = fopen( caReadFile, "r" ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[fopen( %s, r ) Error][최초기동][E:%d][L:%d]\n", caReadFile, errno, __LINE__ );

		return	0;
	}

	// 3. 파일에서 Sequence 데이터를 읽어들인다
	memset( caBuffer, 0x00, sizeof(caBuffer) );
	fgets( caBuffer, sizeof(caBuffer), fpReadFile );

	fclose( fpReadFile );

	// 4. 읽어들인 Sequence 데이터를 전역변수에 저장
	#ifdef OS508
	pthread_mutex_lock( &g_iMsgSerialLock );
	g_iMsgSerial	= atoi(caBuffer);
	pthread_mutex_unlock( &g_iMsgSerialLock );	
	#else
	atomic_swap_uint( &g_iMsgSerial, atoi(caBuffer) );
	#endif

	CNLOG( NOR_LOG, 1, "[Read Sequence: %d][Set Sequence: %d][L:%d]\n", atoi(caBuffer), g_iMsgSerial, __LINE__ );

	// 5. 성공적으로 데이터를 읽어들이면 기존 파일은 삭제
	sprintf( caDeleteCommand, "\\rm -f %s", caReadFile );
	system( caDeleteCommand );

	// 6. 성공!!
	return	0;
}


int WriteSequenceFile()
{
	int		iResult;

	char	caTimeBuffer[15];
	char	caWriteFile[512];

	FILE	*fpWriteFile;

	// 1. Sequence파일의 이름 생성(절대경로/파일이름_시간(YYYYMMDD)_주부센터.guts)
	//    Sequence는 현재까지 발급한 Serial Number로서 재기동시 Serial을 이어서
	//    발급하기 위해서 생성한다
	sprintf( caWriteFile, "%s/%s_%-.*s_%s.guts", 
		g_cpSequenceFilePath, g_cpSequenceFileName, 8, GetNowTimeStr(caTimeBuffer), g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	// 2. Sqeuence 파일 열기 예외처리
	if( ( fpWriteFile = fopen( caWriteFile, "w+" ) ) == NULL )
	{
		if( ERR_LOG != 0 )
			CNLOG( ERR_LOG, 1, "[fopen( %s, w+ ) Error][Seq:%d][E:%d][L:%d]\n", caWriteFile, g_iMsgSerial, errno, __LINE__ );

		return	-1;
	}

	// 3. 파일에 Sequence 데이터 기록
	fprintf( fpWriteFile, "%d", g_iMsgSerial );

	fclose( fpWriteFile );

	// 4. 데이터 확인을 위해 로그 생성
	if( NOR_LOG != 0 )
		CNLOG( NOR_LOG, 1, "[Write Sequence: %d][L:%d]\n", g_iMsgSerial, __LINE__ );	

	// 5. 성공!!
	return	0;
}


int MakeSmdaThread( char* _cpConfigFilePath )
{
	int	iResult;
	int	iSmdaLoop;
	int	iSessionLoop;
	int	iThreadCount;

	stThread				*stpThreadInfo;
	stSmdaThreadArgument	*stpThreadArg;

	// 1. 쓰레드 플래그 셋팅(1이면 Run, 0이면 종료)
	//    쓰레드 종료시 0으로 변경하면 모든 SMDA 쓰레드가
	//    종료 작업을 진행한다
	#ifdef OS508
	pthread_mutex_lock( &g_iSmdaThreadDoneLock );
	g_iSmdaThreadDone	= 1;
	pthread_mutex_unlock( &g_iSmdaThreadDoneLock );	
	#else
	atomic_swap_uint( &g_iSmdaThreadDone, 1 );
	#endif

	// 2. SMDA Thread 생성 시작(SMDA Thread 개수 : SMDA 서버수 * 각 SMDA 세션수)
	for( iSmdaLoop = 0; iSmdaLoop < g_stSmdaThread.iSmdaServerNum; iSmdaLoop++ )
	{
		// 2.1 각 SMDA 서버의 Session 숫자만큼 쓰레드 생성
		for( iSessionLoop = 0; iSessionLoop < g_stSmdaThread.Info[iSmdaLoop].iSmdaSessionNum; iSessionLoop++ )
		{
			// 2.1.1 Thread 생성 함수에 인자로 넘겨줄 Thread 정보 구조체 공간 생성(Lib 소스 참고할것,CNThread.c)
			stpThreadInfo = (stThread*)malloc( sizeof(stThread) );

			// 2.1.2 생성 실패시 실패!!
			if( stpThreadInfo == NULL )
			{
				CNLOG( ERR_LOG, 1, "[malloc( %d ) Error][E:%d][L:%d]\n", sizeof(stThread), errno, __LINE__ );

				return	-1;
			}

			// 2.1.3 Thread 에서 실행되는 Run함수들에게 인자로 넘겨줄 Argument 데이터 공간 생성
			stpThreadArg = (stSmdaThreadArgument*)malloc( sizeof( stSmdaThreadArgument ) );

			// 2.1.4 생성 실패시 실패!!
			if( stpThreadArg == NULL )
			{
				CNLOG( ERR_LOG, 1, "[malloc( %d ) Error][E:%d][L:%d]\n", sizeof( stSmdaThreadArgument ), errno, __LINE__ );

				free( stpThreadInfo );

				return	-1;
			}

			// 2.1.5 생성한 메모리 공간 초기화
			memset( stpThreadInfo, 0x00, sizeof( stThread ) );
			memset( stpThreadArg, 0x00, sizeof( stSmdaThreadArgument ) );

			// 2.1.6 각 필드의 데이터를 채워넣는다
			//       Run함수들(OnThreadStart,OnThreadRun,OnThreadTerminate)에게 
			//       전달되는 데이터들은 stpThreadArg 이며
			//       Thread 라이브러리에 전달되는 데이터는 stpThreadInfo 이다
			//       stpThreadInfo는 맴버변수로 stpThreadArg의 포인터를 저장한다
			//       로그 생성시 로그 라이브러리안의 함수가 기능적으로 각 
			//       Run함수 들에게 stpThreadArg 를 전달한다

			// 2.1.7 해당 쓰레드가 몇번째 SMDA서버에 몇번째 세션을 담당하는지 셋팅
			stpThreadArg->iSmdaIndex			= iSmdaLoop;
			stpThreadArg->iSessionIndex			= iSessionLoop;

			// 2.1.8 쓰레드 생성 함수가 사용하는 데이터 셋팅
			//       vpArgs 는 Run 함수들(OnThreadStart, OnThreadRun, OnThreadTerminate)
			//       에게 넘겨줄 Argument이다
			//       OnThreadStart, OnThreadRun, OnThreadTerminate는
			//       쓰레드 생성시 vpArgs를 넘겨받아 차례대로 실행되는
			//       함수들의 함수 포인터이다
			stpThreadInfo->vpArgs				= (void*)stpThreadArg;
			stpThreadInfo->OnThreadStart		= OnSmdaThreadStart;
			stpThreadInfo->OnThreadRun			= OnSmdaThreadRun;
			stpThreadInfo->OnThreadTerminate	= OnSmdaThreadTerminate;

			// 2.1.9 로거 핸들(쓰레드 생성후 각 쓰레드가 셋팅)
			stpThreadArg->NLOG	= NULL;
			stpThreadArg->ELOG	= NULL;

			// 2.1.10 쓰레드들이 읽어들일 컨피그 파일들의 경로 정보 공간 할당 및 셋팅
			stpThreadArg->cpConfigPath	= (char*)malloc( strlen( _cpConfigFilePath ) + 1 );
			memset( stpThreadArg->cpConfigPath, 0x00, strlen( _cpConfigFilePath ) + 1 );
			strlcpy( stpThreadArg->cpConfigPath, _cpConfigFilePath, strlen( _cpConfigFilePath ) + 1 );

			// 2.1.11 쓰레드 숫자를 한개 늘린다
			#ifdef OS508
			pthread_mutex_lock( &g_iSmdaThreadCountLock );
			g_iSmdaThreadCount++;
			pthread_mutex_unlock( &g_iSmdaThreadCountLock );
			#else
			atomic_inc_uint_nv( &g_iSmdaThreadCount );
			#endif

			// 2.1.12 쓰레드 생성
			if( ( iResult = CreateThread( stpThreadInfo ) ) < 0 )
			{
				CNLOG( ERR_LOG, 1, "[CreateThread( %d ) Error][R:%d][E:%d][L:%d]\n", stpThreadInfo, iResult, errno, __LINE__ );

				// 2.1.12.1 쓰레드 생성 실패시 쓰레드에 넘겨주기 위해 할당 했던 메모리 공간 해제
				free( stpThreadInfo );
				free( stpThreadArg );

				// 2.1.12.2 쓰레드 생성 실패시 증가시켰던 쓰레드 카운트를 다시 감소시킨다
				#ifdef OS508
				pthread_mutex_lock( &g_iSmdaThreadCountLock );
				g_iSmdaThreadCount--;
				pthread_mutex_unlock( &g_iSmdaThreadCountLock );
				#else
				atomic_dec_uint_nv( &g_iSmdaThreadCount );
				#endif

				// 2.1.12.3 실패!!
				return	-1;
			}
		}
	}

	// 3. 성공!!
	return	0;
}


int MakeLogThread( char* _cpConfigFilePath )
{
	int	iResult;
	int	iForLoop;
	int	iThreadCount;

	stThread			*stpThreadInfo;
	stLogThreadArgument	*stpThreadArg;

	// 1. 쓰레드 플래그 셋팅(1이면 Run, 0이면 종료)
	//    쓰레드 종료시 0으로 변경하면 모든 LOG 쓰레드가
	//    종료 작업을 진행한다	
	#ifdef OS508
	pthread_mutex_lock( &g_iLogThreadDoneLock );
	g_iLogThreadDone	= 1;
	pthread_mutex_unlock( &g_iLogThreadDoneLock );
	#else
	atomic_swap_uint( &g_iLogThreadDone, 1 );
	#endif

	// 2. LOG Thread 생성 시작(LOG Thread 개수 : Log Queue 개수)
	for( iForLoop = 0; iForLoop < g_stLogThread.iUseNum; iForLoop++ )
	{
		// 2.1.1 Thread 생성 함수에 인자로 넘겨줄 Thread 정보 구조체 공간 생성(Lib 소스 참고할것,CNThread.c)
		stpThreadInfo = (stThread*)malloc( sizeof(stThread) );

		// 2.1.2 생성 실패시 실패!!
		if( stpThreadInfo == NULL )
		{
			CNLOG( ERR_LOG, 1, "[malloc( %d ) Error][E:%d][L:%d]\n", sizeof(stThread), errno, __LINE__ );

			return	-1;
		}

		// 2.1.3 Thread 에서 실행되는 Run함수들에게 인자로 넘겨줄 Argument 데이터 공간 생성
		stpThreadArg = (stLogThreadArgument*)malloc( sizeof(stLogThreadArgument) );

		// 2.1.4 생성 실패시 실패!!
		if( stpThreadArg == NULL )
		{
			CNLOG( ERR_LOG, 1, "[malloc( %d ) Error][E:%d][L:%d]\n", sizeof(stLogThreadArgument), errno, __LINE__ );

			free( stpThreadInfo );

			return	-1;
		}

		// 2.1.5 생성한 메모리 공간 초기화
		memset( stpThreadInfo, 0x00, sizeof(stThread) );
		memset( stpThreadArg, 0x00, sizeof(stLogThreadArgument) );

		// 2.1.6 각 필드의 데이터를 채워넣는다
		//       Run함수들(OnThreadStart,OnThreadRun,OnThreadTerminate)에게 
		//       전달되는 데이터들은 stpThreadArg 이며
		//       Thread 라이브러리에 전달되는 데이터는 stpThreadInfo 이다
		//       stpThreadInfo는 맴버변수로 stpThreadArg의 포인터를 저장한다
		//       로그 생성시 로그 라이브러리안의 함수가 기능적으로 각 
		//       Run함수 들에게 stpThreadArg 를 전달한다

		// 2.1.7 해당 쓰레드가 몇번째 Queue를 담당하는지 셋팅
		stpThreadArg->iThreadIndex			= iForLoop;

		// 2.1.8 쓰레드 생성 함수가 사용하는 데이터 셋팅
		//       vpArgs 는 Run 함수들(OnThreadStart, OnThreadRun, OnThreadTerminate)
		//       에게 넘겨줄 Argument이다
		//       OnThreadStart, OnThreadRun, OnThreadTerminate는
		//       쓰레드 생성시 vpArgs를 넘겨받아 차례대로 실행되는
		//       함수들의 함수 포인터이다
		stpThreadInfo->vpArgs				= (void*)stpThreadArg;
		stpThreadInfo->OnThreadStart		= OnLogThreadStart;
		stpThreadInfo->OnThreadRun			= OnLogThreadRun;
		stpThreadInfo->OnThreadTerminate	= OnLogThreadTerminate;

		// 2.1.9 로거 핸들(쓰레드 생성후 각 쓰레드가 셋팅)
		stpThreadArg->NLOG	= NULL;
		stpThreadArg->ELOG	= NULL;

		// 2.1.10 쓰레드들이 읽어들일 컨피그 파일들의 경로 정보 공간 할당 및 셋팅
		stpThreadArg->cpConfigPath	= (char*)malloc( strlen( _cpConfigFilePath ) + 1 );
		memset( stpThreadArg->cpConfigPath, 0x00, strlen( _cpConfigFilePath ) + 1 );
		strlcpy( stpThreadArg->cpConfigPath, _cpConfigFilePath, strlen( _cpConfigFilePath ) + 1 );

		// 2.1.11 쓰레드 숫자를 한개 늘린다
		#ifdef OS508
		pthread_mutex_lock( &g_iLogThreadCountLock );
		g_iLogThreadCount++;
		pthread_mutex_unlock( &g_iLogThreadCountLock );
		#else
		atomic_inc_uint_nv( &g_iLogThreadCount );
		#endif

		// 2.1.12 쓰레드 생성
		if( ( iResult = CreateThread( stpThreadInfo ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[CreateThread( %d ) Error][R:%d][E:%d][L:%d]\n", stpThreadInfo, iResult, errno, __LINE__ );

			// 2.1.12.1 쓰레드 생성 실패시 쓰레드에 넘겨주기 위해 할당 했던 메모리 공간 해제
			free( stpThreadInfo );
			free( stpThreadArg );

			// 2.1.12.2 쓰레드 생성 실패시 증가시켰던 쓰레드 카운트를 다시 감소시킨다
			#ifdef OS508
			pthread_mutex_lock( &g_iLogThreadCountLock );
			g_iLogThreadCount--;
			pthread_mutex_unlock( &g_iLogThreadCountLock );
			#else
			atomic_dec_uint_nv( &g_iLogThreadCount );
			#endif

			// 2.1.12.3 실패!!
			return	-1;
		}
	}

	// 3. 성공!!
	return	0;
}


int OnLogThreadStart( void* _vpArg )
{
	int					iResult;

	int					iThreadIndex;

	char				caLogFilePath[256];
	char				caLogFileName[256];
	char				caErrLogFileName[256];

	sigset_t			stSigTemp;

	stThread			*stpThreadInfo;
	stLogThreadArgument	*stpThreadArg;

	// 1. 아규먼트 초기화, Thread 정보를 지역변수로 받는다.
	stpThreadInfo	= (stThread*)_vpArg;
	stpThreadArg	= (stLogThreadArgument*)stpThreadInfo->vpArgs;

	iThreadIndex	= stpThreadArg->iThreadIndex;

	// 2. 로그 데이터관련 변수 초기화
	strlcpy( caLogFilePath, stpThreadArg->cpConfigPath, sizeof(caLogFilePath) );
	strlcpy( caLogFileName, LOG_QUEUE_LOG_FILE_NAME, sizeof(caLogFileName) );
	strlcpy( caErrLogFileName, LOG_QUEUE_ERROR_LOG_FILE_NAME, sizeof(caErrLogFileName) );

	// 3. 일반 로그 데이터 등록(이때부터 일반로그 쓰기 가능)
	if( ( stpThreadArg->NLOG = GetLogThreadLogHandle( iThreadIndex, caLogFilePath, caLogFileName ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[T:%d][GetLogThreadLogHandle( %d %s %s ) Error][E:%d][L:%d]\n", 
			iThreadIndex + 1, iThreadIndex, caLogFilePath, caLogFileName, errno, __LINE__ );

		return	-1;
	}

	// 4. 에러 로그 데이터 등록(이때부터 에러로그 쓰기 가능)
	if( ( stpThreadArg->ELOG = GetLogThreadLogHandle( iThreadIndex, caLogFilePath, caErrLogFileName ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[T:%d][GetLogThreadLogHandle( %d %s %s ) Error][E:%d][L:%d]\n", 
			iThreadIndex + 1, iThreadIndex, caLogFilePath, caErrLogFileName, errno, __LINE__ );

		return	-1;
	}

	CNLOG( stpThreadArg->NLOG, 3, "[T:%d][LOG Thread Start][L:%d]\n", iThreadIndex + 1, __LINE__ );

	// 5. 모든 시그널 차단(Blocking), 시그널 처리는 Main Thread가 전담한다
	if( ( iResult = CNAllSignalBlock( &stSigTemp ) ) < 0 )
	{
		CNLOG( stpThreadArg->ELOG, 1, "[T:%d][CNAllSignalBlock( %d ) Error][R:%d][E:%d][L:%d]\n", 
			iThreadIndex + 1, &stSigTemp, iResult, errno, __LINE__ );

		return	-1;
	}

	// 6. 성공!!
	return	0;
}


int OnLogThreadRun( void* _vpArg )
{
	int					iThreadIndex;

	int					iQueueKey;
	int					iQueueID		= -1;

	int					iLogQueueEmpty	= 0;
	
	int					iReadPacketSize;

	stLogPacket			stTempPacket;

	stThread			*stpThreadInfo;
	stLogThreadArgument	*stpThreadArg;

	CN_LOG_HANDLE		NLOG;
	CN_LOG_HANDLE		ELOG;

	// 1. 아규먼트 초기화(Thread 정보를 지역변수로 받는다.
	stpThreadInfo	= (stThread*)_vpArg;
	stpThreadArg	= (stLogThreadArgument*)stpThreadInfo->vpArgs;

	iThreadIndex	= stpThreadArg->iThreadIndex;

	iQueueKey		= g_stLogThread.Info[iThreadIndex].iQueueKey;

	iReadPacketSize	= sizeof( stLogPacket ) - sizeof( long );

	NLOG			= stpThreadArg->NLOG;
	ELOG			= stpThreadArg->ELOG;

	CNLOG( NLOG, 3, "[T:%d][LOG Thread Run][L:%d]\n", iThreadIndex + 1, __LINE__ );

	// 2. 종료명령이 올때까지 데이터 처리
	while( GetLogThreadDone() )
	{
		// 2.1 Queue가 Attach되어있지 않을경우
		if( iQueueID < 0 )
		{
			// 2.1.2 Queue 가 없으면 생성, 있으면 Attach
			if( ( iQueueID = MakeAndOpenQueue( iQueueKey, g_iMaxLogQueueCount * sizeof( stLogPacket ) ) ) < 0 )
			{
				CNLOG( ELOG, 1, "[T:%d][MakeAndOpenQueue( 0x%x, %d ) Error][R:%d][E:%d][L:%d]\n", 
					iThreadIndex + 1, iQueueKey, g_iMaxLogQueueCount * sizeof( stLogPacket ), iQueueID, errno, __LINE__ );

				CNNanoSleep( 3000000000 );

				iLogQueueEmpty	= 0;

				continue;
			}
		}

		// 2.2 Qudud에 데이터를 체크했을때 300번 이상 데이터가 비어있었을 경우
		//     CPU 부하를 줄이기 위해 슬립을 준다
		if( iLogQueueEmpty >= 300)
		{
			CNNanoSleep( 1000000000 );

			iLogQueueEmpty	= 0;

			continue;
		}

		// 2.3 Queue에서 Packet 데이터 읽기를 시도한다
		memset( &stTempPacket, 0x00, sizeof( stLogPacket ) );

		switch( CNQueueRead( iQueueID, &stTempPacket, iReadPacketSize, 0 ) )
		{
			// 2.3.1 Queue에서 정상적으로 LogPacket 데이터를 읽었을 경우
			case CN_IPC_QUEUE_DATA_READ :

				// 2.3.1.1 읽어들인 Queue데이터를 OnLogMsgRead()로 처리(파일 기록하고 Smda Queue로 전송하는 역활 수행)
				if( OnLogMsgRead( iThreadIndex, &stTempPacket, iReadPacketSize, stpThreadArg ) < 0 )
				{
					CNLOG( ELOG, 1, "[T:%d][OnLogMsgRead( %d %d %d %d ) Error][Seq:%d][E:%d][L:%d]\n", 
						iThreadIndex + 1, iThreadIndex, &stTempPacket, iReadPacketSize, stpThreadArg, 
						stTempPacket.stHeader.iSerialNumber, errno, __LINE__ );

					// 2.3.1.1.1 OnLogMsgRead() 실패시 OnLogMsgReadError()로 예외처리(에러 데이터 파일 출력)
					if( OnLogMsgReadError( &stTempPacket, stpThreadArg ) < 0 )
					{
						CNLOG( ELOG, 1, "[T:%d][OnLogMsgReadError( %d %d ) Error][Seq:%d][E:%d][L:%d]\n", 
							iThreadIndex + 1, &stTempPacket, stpThreadArg, stTempPacket.stHeader.iSerialNumber, errno, __LINE__ );
					}
				}

				// 2.3.1.2 데이터를 읽어들인경우 Queue데이터 비어있음 Count를 0으로 초기화
				iLogQueueEmpty	= 0;

				break;
		
			// 2.3.2 Queue가 비어있을경우(Packet 데이터가 없을경우)
			case CN_IPC_QUEUE_DATA_EMPTY :

				OnLogMsgEmpty();

				iLogQueueEmpty++;

				break;

			// 2.3.3 Queue읽기도중 에러가 난 경우
			case CN_IPC_QUEUE_DATA_ERROR :

				CNLOG( ELOG, 1, "[T:%d][CNMsgQueueRead( %d %d %d 0 ) Error][R:%d][E:%d][L:%d]\n", 
					iThreadIndex + 1, iQueueID, &stTempPacket, iReadPacketSize, CN_IPC_QUEUE_DATA_ERROR, errno, __LINE__ );

				OnLogMsgError();

				iLogQueueEmpty	= 0;

				iQueueID	= -1;

				break;

			// 2.3.4 Queue읽기도중 알수없는 에러가 난 경우
			default :

				CNLOG( ELOG, 1, "[T:%d][CNMsgQueueRead( %d %d %d 0 ) Error][E:%d][L:%d]\n", 
					iThreadIndex + 1, iQueueID, &stTempPacket, iReadPacketSize, errno, __LINE__ );

				OnLogMsgError();

				iLogQueueEmpty	= 0;

				iQueueID	= -1;

				break;
		}
	}

	// 3. 종료!! OnLogThreadTerminate()가 이어서 실행(종료과정 및 데이터 해제등 담당)
	return	0;
}


int OnLogThreadTerminate( void* _vpArg )
{
	int					iThreadIndex;

	stThread			*stpThreadInfo;
	stLogThreadArgument	*stpThreadArg;

	// 1. 아규먼트 초기화(Thread 정보를 지역변수로 받는다.)
	stpThreadInfo	= (stThread*)_vpArg;
	stpThreadArg	= (stLogThreadArgument*)stpThreadInfo->vpArgs;

	iThreadIndex	= stpThreadArg->iThreadIndex;

	// 2. Heap 영역에 있는 데이터 해제
	if( stpThreadArg->NLOG != NULL )
		CNLOG( stpThreadArg->NLOG, 3, "[T:%d][LOG Thread Terminate][L:%d]\n", iThreadIndex + 1, __LINE__ );

	if( stpThreadArg->cpConfigPath != NULL )
		free( stpThreadArg->cpConfigPath );

	if( stpThreadArg->NLOG != NULL )
		free( stpThreadArg->NLOG );

	if( stpThreadArg->ELOG != NULL )
		free( stpThreadArg->ELOG );

	if( stpThreadArg != NULL )
		free( stpThreadArg );

	if( stpThreadInfo != NULL )
		free( stpThreadInfo );

	// 3. Thread의 개수를 1개 줄인다
	#ifdef OS508
	pthread_mutex_lock( &g_iLogThreadCountLock );
	g_iLogThreadCount--;
	pthread_mutex_unlock( &g_iLogThreadCountLock );
	#else
	atomic_dec_uint_nv( &g_iLogThreadCount );
	#endif

	// 4. Thread 종료!!
	return	0;
}


int OnLogMsgRead( int _iThreadIndex, stLogPacket* _stpLogPacket, int _iPacketSize, stLogThreadArgument* stpLogArg )
{
	int		iResult;

	int		iPrefixIndex;
	int		iServerRoutingIndex;
	int		iSessionRoutingIndex;

	char	caTempRouting[6];

	FILE	*fpLog;

	CN_LOG_HANDLE	NLOG;
	CN_LOG_HANDLE	ELOG;

	// 1. 로그 핸들을 초기화 한다(일반로그, 에러로그)
	NLOG	= stpLogArg->NLOG;
	ELOG	= stpLogArg->ELOG;

	// 2. Serial 넘버 발급
	#ifdef OS508
	pthread_mutex_lock( &g_iMsgSerialLock );
	_stpLogPacket->stHeader.iSerialNumber	= ++g_iMsgSerial;
	pthread_mutex_unlock( &g_iMsgSerialLock );
	#else
	_stpLogPacket->stHeader.iSerialNumber	= atomic_inc_uint_nv( &g_iMsgSerial );
	#endif

	#ifdef DEBUG
	CNLOG( NLOG, 3, "[Queue Msg Type:%ld][Log File Type:%d][Log File Name:%s][Routing Number:%s][L:%d]\n", 
		_stpLogPacket->iQueueMsgType, 
		_stpLogPacket->iLogFileType, 
		_stpLogPacket->caLogFileName, 
		_stpLogPacket->caRoutingNumber, 
		__LINE__ );

	CNLOG( NLOG, 3, "[System:0x00%x][Msg Type:0x00%x][Version:%d][Length:%d][Serial:%d][L:%d]\n", 
		_stpLogPacket->stHeader.iSystem,
		_stpLogPacket->stHeader.iMsgType,
		_stpLogPacket->stHeader.iVersion,
		_stpLogPacket->stHeader.iBodyLength,
		_stpLogPacket->stHeader.iSerialNumber,
		__LINE__ );

	CNLOG( NLOG, 3, "[Body:%s][Body Length:%d][L:%d]\n", 
		_stpLogPacket->caBody, strlen( _stpLogPacket->caBody ), __LINE__ );
	#endif

	// 3. 로그파일에 데이터(Log Pakcet) 기록
	{
		if( ( fpLog = fopen( _stpLogPacket->caLogFileName, "a+" ) ) == NULL )
		{
			CNLOG( ELOG, 1, "[T:%d][fopen( %s, a+ ) Error][Seq:%d][E:%d][L:%d]\n", 
				_iThreadIndex + 1, _stpLogPacket->caLogFileName, _stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

			return	-1;
		}

		if( fwrite( _stpLogPacket->caBody, _stpLogPacket->stHeader.iBodyLength, 1, fpLog ) != 1 )
		{
			CNLOG( ELOG, 1, "[T:%d][fwrite( %s %d 1 %d ) Error][Seq:%d][E:%d][L:%d]\n", 
				_iThreadIndex + 1, _stpLogPacket->caBody, sizeof( stLogPacket ), fpLog, _stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

			fclose( fpLog );

			return	-1;
		}

		fclose( fpLog );
	}

	// 4. ServiceType이 ServiceLog 가 아니면 SmdaQueue로 전송하지 않음
	if( _stpLogPacket->iLogFileType != 1 )
		return	0;

	// 5. 라우팅 SMDA Server(Group)를 구한다
	{
		memset( caTempRouting, 0x00, sizeof( caTempRouting ) );
		memcpy( caTempRouting, _stpLogPacket->caRoutingNumber, 5 );

		// 5.1 일반 DST번호인경우 라우팅 테이블을 조회하여 전송 Queue를 구한다
		if( strstr( caTempRouting, "#" ) == NULL )
		{
			iPrefixIndex	= atoi( caTempRouting );

			// 5.1.1 Dest가 "0"인 경우 SMDA Queue로 전송하는것을 막는다
			//      임시적으로 추가한것이라 추후에 삭제한다
			//      TELESA의 타사 REPORT ACK때문에 막아둠
			if( iPrefixIndex == 0 )
				return	0;

			iServerRoutingIndex	= g_stSmdaRouting.Index[iPrefixIndex];
		}
		// 5.2 특번 DST인 경우 ETC ROUTING 을 태워 보낸다
		else
		{
			iServerRoutingIndex	= g_iEtcRoutingQueue;
		}
	}

	// 6. 등록되어있지 않은 Server 라우팅 인덱스가 들어오면 실패처리
	if( iServerRoutingIndex < 0 || iServerRoutingIndex >= g_stSmdaThread.iSmdaServerNum )
	{
		CNLOG( ELOG, 1, "[T:%d][Server Routing Index Error][Routing Index: %d][Seq:%d][L:%d]\n", 
			_iThreadIndex + 1, iServerRoutingIndex, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );

		return	-1;
	}

	// 7. 라우팅 SMDA Session을 구한다
	{
		#ifdef OS508
		pthread_mutex_lock( &g_stSmdaThread.Info[iServerRoutingIndex].Lock );
		iSessionRoutingIndex = (++g_stSmdaThread.Info[iServerRoutingIndex].iMessageCount) % g_stSmdaThread.Info[iServerRoutingIndex].iSmdaSessionNum;
		pthread_mutex_unlock( &g_stSmdaThread.Info[iServerRoutingIndex].Lock );
		#else
		iSessionRoutingIndex = atomic_inc_uint_nv( &g_stSmdaThread.Info[iServerRoutingIndex].iMessageCount ) % g_stSmdaThread.Info[iServerRoutingIndex].iSmdaSessionNum;
		#endif
	}

	// 8. 등록되어있지 않은 Session 라우팅 인덱스가 들어오면 실패처리
	if( iSessionRoutingIndex < 0 || iSessionRoutingIndex >= g_stSmdaThread.Info[iServerRoutingIndex].iSmdaSessionNum )
	{
		CNLOG( ELOG, 1, "[T:%d][Session Routing Index Error][Routing Index : %d][Seq:%d][L:%d]\n", 
			_iThreadIndex + 1, iSessionRoutingIndex, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );

		return	-1;
	}

	// 9. 구해진 SmdaQueue로 Log Packet 전송
	{
		pthread_mutex_lock( &(g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].Lock) );
		
		// 9.1 Smda Queue 상태가 정상이 아니거나 Attach가 안된경우 에러처리
		if( g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].iQueueID < 0 )
		{
			pthread_mutex_unlock( &(g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].Lock) );

			CNLOG( ELOG, 1, "[T:%d][Routing Smda Queue Not Attach][Routing Server: %d][Session Session: %d][Seq:%d][L:%d]\n", 
				_iThreadIndex + 1, iServerRoutingIndex, iSessionRoutingIndex, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );

			return	-1;
		}

		// 9.2 Smda Queue로 데이터 전송
		iResult	= CNQueueWrite( g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].iQueueID, _stpLogPacket, _iPacketSize );

		// 9.3 전송 결과별로 처리
		switch( iResult )
		{
			// 9.3.1 SmdaQueue에 정상적으로 LogPacket 데이터를 전송
			case CN_IPC_QUEUE_DATA_WRITE :

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].Lock) );

				break;

			// 9.3.2 SmdaQueue가 가득찼을경우
			case CN_IPC_QUEUE_DATA_FULL :

				CNLOG( ELOG, 1, "[T:%d][CNQueueWrite( %d %d %d 0 ) Error][Queue Full][Seq:%d][R:%d][E:%d][L:%d]\n", 
					_iThreadIndex + 1, g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].iQueueID, 
					_stpLogPacket, _iPacketSize, _stpLogPacket->stHeader.iSerialNumber, iResult, errno, __LINE__ );

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].Lock) );

				return	-1;

				break;

			// 9.3.3 SmdaQueue 전송도중 에러가 난 경우
			case CN_IPC_QUEUE_DATA_ERROR :

				CNLOG( ELOG, 1, "[T:%d][CNQueueWrite( %d %d %d 0 ) Error][IPC Queue Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
					_iThreadIndex + 1, g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].iQueueID, 
					_stpLogPacket, _iPacketSize, _stpLogPacket->stHeader.iSerialNumber, iResult, errno, __LINE__ );

				g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].iQueueID	= -1;

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].Lock) );

				return	-1;

				break;

			// 9.3.4 SmdaQueue 전송도중 알수없는 에러가 난 경우
			default :

				CNLOG( ELOG, 1, "[T:%d][CNQueueWrite( %d %d %d 0 ) Error][UnKnown Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
					_iThreadIndex + 1, g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].iQueueID, 
					_stpLogPacket, _iPacketSize, _stpLogPacket->stHeader.iSerialNumber, iResult, errno, __LINE__ );

				g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].iQueueID	= -1;

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].Lock) );

				return	-1;

				break;
		}
	}

	// 10. 성공!!
	return	0;
}


int OnLogMsgEmpty()
{
	return	0;
}


int OnLogMsgError()
{
	return	0;
}


int OnLogMsgReadError( stLogPacket* _stpLogPacket, stLogThreadArgument* _stpThreadArg )
{
	return	OnWriteLogThreadError( _stpLogPacket, _stpThreadArg );
}


int OnWriteLogThreadError( stLogPacket* _stpLogPacket, stLogThreadArgument* _stpThreadArg )
{
	char	caWriteFile[512];
	char	caTimeBuffer[15];

	FILE	*fpWriteFile;

	// 1. 로그 핸들을 초기화 한다(일반로그, 에러로그)
	CN_LOG_HANDLE	NLOG	= _stpThreadArg->NLOG;
	CN_LOG_HANDLE	ELOG	= _stpThreadArg->ELOG;

	// 2. LogThread Error 파일 셋팅(절대경로+파일이름)
	sprintf( caWriteFile, "%s/%s_%d_%-.*s_%s.dat", 
		g_cpErrorLogThreadFilePath, g_cpErrorLogThreadFileName, _stpThreadArg->iThreadIndex + 1, 
		10, GetNowTimeStr(caTimeBuffer), g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	// 3. Error 파일 열기(없을 시 생성, 있을 시 이어쓰기)
	if( ( fpWriteFile = fopen( caWriteFile, "a+" ) ) == NULL )
	{
		CNLOG( ELOG, 1, "[T:%d][fopen( %s, a+ ) Error][Seq:%d][E:%d][L:%d]\n", 
			_stpThreadArg->iThreadIndex + 1, caWriteFile, _stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

		return	-1;
	}

	// 4. Error 데이터 파일 출력
	if( fwrite( _stpLogPacket, sizeof( stLogPacket ), 1, fpWriteFile ) != 1 )
	{
		fclose( fpWriteFile );

		CNLOG( ELOG, 1, "[T:%d][fwrite( %d %d 1 %d ) Error][Seq:%d][E:%d][L:%d]\n", 
			_stpThreadArg->iThreadIndex + 1, _stpLogPacket, sizeof( stLogPacket ), fpWriteFile, 
			_stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

		return	-1;
	}

	// 5. Error 파일 Close
	fclose( fpWriteFile );

	// 6. 성공!!
	return	0;
}


int OnSmdaThreadStart( void* _vpArg )
{
	int					iResult;
	int					iSmdaIndex;
	int					iSessionIndex;

	char				caLogFilePath[256];
	char				caLogFileName[256];
	char				caErrLogFileName[256];

	sigset_t			stSigTemp;

	stThread				*stpThreadInfo;
	stSmdaThreadArgument	*stpThreadArg;

	// 1. 아규먼트 초기화(Thread 정보를 지역변수로 받는다.
	stpThreadInfo	= (stThread*)_vpArg;
	stpThreadArg	= (stSmdaThreadArgument*)stpThreadInfo->vpArgs;

	iSmdaIndex		= stpThreadArg->iSmdaIndex;
	iSessionIndex	= stpThreadArg->iSessionIndex;

	// 2. 로그 데이터관련 변수 초기화
	strlcpy( caLogFilePath, stpThreadArg->cpConfigPath, sizeof(caLogFilePath) );
	strlcpy( caLogFileName, SMDA_QUEUE_LOG_FILE_NAME, sizeof(caLogFileName) );
	strlcpy( caErrLogFileName, SMDA_QUEUE_ERROR_LOG_FILE_NAME, sizeof(caErrLogFileName) );

	CNInitList( &(stpThreadArg->List) );

	stpThreadArg->iEmergencyFilingFlag	= 0;
	stpThreadArg->cpEmergencyTime	= (char*)malloc( 15 );	// 11(YYYYMMDDhhmmss + 1)
	memset( stpThreadArg->cpEmergencyTime, 0x00, 15 );

	// 3. 일반 로그 데이터 등록(이때부터 일반로그 쓰기 가능)
	if( ( stpThreadArg->NLOG = GetSmdaThreadLogHandle( iSmdaIndex, iSessionIndex, caLogFilePath, caLogFileName ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[T:%02d %02d][GetLogThreadLogHandle( %d %d %s %s ) Error][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, iSmdaIndex, iSessionIndex, caLogFilePath, caLogFileName, errno, __LINE__ );

		return	-1;
	}

	// 4. 에러 로그 데이터 등록(이때부터 에러로그 쓰기 가능)
	if( ( stpThreadArg->ELOG = GetSmdaThreadLogHandle( iSmdaIndex, iSessionIndex, caLogFilePath, caErrLogFileName ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[T:%02d %02d][GetLogThreadLogHandle( %d %d %s %s ) Error][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, iSmdaIndex, iSessionIndex, caLogFilePath, caErrLogFileName, errno, __LINE__ );

		return	-1;
	}

	CNLOG( stpThreadArg->NLOG, 3, "[T:%02d %02d][SMDA Thread Start][L:%d]\n", iSmdaIndex + 1, iSessionIndex + 1, __LINE__ );

	// 5. 모든 시그널 차단(Blocking), 시그널 처리는 Main Thread가 전담한다
	if( ( iResult = CNAllSignalBlock( &stSigTemp ) ) < 0 )
	{
		CNLOG( stpThreadArg->ELOG, 1, "[T:%02d %02d][CNAllSignalBlock( %d ) Error][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, &stSigTemp, iResult, errno, __LINE__ );

		return	-1;
	}

	// 6. 재처리 데이터 파일을 읽어서 메모리에 올린다(프로세스 종료시 재처리 데이터를 파일에 쓰고 재기동시
	//    다시 읽어들여 처리한다)
	if( ( iResult = OnReadGutsSmda( stpThreadArg ) ) < 0 )
	{
		CNLOG( stpThreadArg->ELOG, 1, "[T:%02d %02d][OnReadGutsSmda( %d ) Error][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, stpThreadArg, iResult, errno, __LINE__ );

		return	-1;
	}

	// 7. 성공!!
	return	0;
}


int OnSmdaThreadRun( void* _vpArg )
{
	int		iResult;
	int		iSocketFD		= -1;
	int		iSmdaIndex;
	int		iSessionIndex;
	int		iSmdaQueueEmpty	= 0;

	int		iReadPacketSize;

	int		iSmdaPort;

	time_t	tLastQueryTime		= time( NULL );
	time_t	tLastReprocessTime	= time( NULL );

	char	caSmdaAdress[16];

	char	caSmdaBindID[16];
	char	caSmdaBindPW[16];

	stLogPacket				stTempPacket;

	stThread				*stpThreadInfo;
	stSmdaThreadArgument	*stpThreadArg;

	CN_LOG_HANDLE		NLOG;
	CN_LOG_HANDLE		ELOG;

	// 1. 아규먼트 초기화(Thread 정보를 지역변수로 받는다.)
	stpThreadInfo	= (stThread*)_vpArg;
	stpThreadArg	= (stSmdaThreadArgument*)stpThreadInfo->vpArgs;

	iSmdaIndex		= stpThreadArg->iSmdaIndex;
	iSessionIndex	= stpThreadArg->iSessionIndex;

	iReadPacketSize	= sizeof( stLogPacket ) - sizeof( long );

	NLOG			= stpThreadArg->NLOG;
	ELOG			= stpThreadArg->ELOG;

	// 2. Smda 서버에 대한 접속 정보를 Argument에서 추출
	iSmdaPort	= g_stSmdaThread.Info[iSmdaIndex].iPort;

	strlcpy( caSmdaAdress, g_stSmdaThread.Info[iSmdaIndex].caAdress, sizeof( caSmdaAdress ) );
	strlcpy( caSmdaBindID, g_stSmdaThread.Info[iSmdaIndex].caBindID, sizeof( caSmdaBindID ) );
	strlcpy( caSmdaBindPW, g_stSmdaThread.Info[iSmdaIndex].caBindPW, sizeof( caSmdaBindPW ) );

	CNLOG( NLOG, 3, "[T:%02d %02d][SMDA Thread Run][L:%d]\n", iSmdaIndex + 1, iSessionIndex + 1, __LINE__ );

	// 3. Smda 서버로 접속 시도
	if( ( iSocketFD = OnConnectToSmda( iSmdaIndex, iSessionIndex, stpThreadArg, caSmdaAdress, iSmdaPort, caSmdaBindID, caSmdaBindPW ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][OnConnectToSmda( %d %d %d %s %d %s %s ) Error][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, 
			iSmdaIndex, iSessionIndex, stpThreadArg, caSmdaAdress, iSmdaPort, caSmdaBindID, caSmdaBindPW, 
			iSocketFD, errno, __LINE__ );
	}

	tLastQueryTime	= time( NULL );

	// 4. 종료명령이 올때까지 데이터 처리
	while( GetSmdaThreadDone() )
	{
		// 4.1 Smda에 접속이 되어있지 않을경우 재접속 시도
		if( *g_ipEmergencyMode == 0 && iSocketFD < 0 && ( time( NULL ) - tLastQueryTime >= 3 ) )
		{
			if( ( iSocketFD = OnConnectToSmda( iSmdaIndex, iSessionIndex, stpThreadArg, caSmdaAdress, iSmdaPort, caSmdaBindID, caSmdaBindPW ) ) < 0 )
			{
				CNLOG( ELOG, 1, "[T:%02d %02d][OnConnectToSmda( %d %d %d %s %d %s %s ) Error][R:%d][E:%d][L:%d]\n", 
					iSmdaIndex + 1, iSessionIndex + 1, 
					iSmdaIndex, iSessionIndex, stpThreadArg, caSmdaAdress, iSmdaPort, caSmdaBindID, caSmdaBindPW, 
					iSocketFD, errno, __LINE__ );
			}

			tLastQueryTime	= time( NULL );
		}

		// 4.2 특정한 주기(g_iSmdaRetryCycle)로 재처리 필요 데이터 재처리
		if( ( g_iSmdaRetryUseFlag != 0 ) && ( time( NULL ) - tLastReprocessTime >= g_iSmdaRetryCycle ) )
		{
			OnSmdaDataReprocess( stpThreadArg, &iSocketFD, &tLastQueryTime );

			tLastReprocessTime	= time( NULL );
		}

		// 4.3 특정한 시간(SYSTEM_QUERY_TIME_CYCLE)동안 Smda서버와 통신이 없을경우 System Query 전송
		if( iSocketFD > 0 && ( time( NULL ) - tLastQueryTime >= SYSTEM_QUERY_TIME_CYCLE ) )
		{
			if( OnSysQueryToSmda( iSmdaIndex, iSessionIndex, stpThreadArg, iSocketFD ) < 0 )
			{
				CNSocketClose( iSocketFD );

				iSocketFD	= -1;
			}
			
			tLastQueryTime	= time( NULL );
		}

		// 4.4 Queue에 데이터를 체크했을때 300번 이상 데이터가 비어있었을 경우
		//     CPU 부하를 줄이기 위해 슬립을 준다
		if( iSmdaQueueEmpty >= 300)
		{
			CNNanoSleep( 1000000000 );

			iSmdaQueueEmpty	= 0;

			continue;
		}

		pthread_mutex_lock( &(g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].Lock) );

		// 4.5 Queue가 Attach되어있지 않을경우
		if( g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID < 0 )
		{
			// 4.5.1 Queue 가 없으면 생성, 있으면 Attach
			if( ( g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID = MakeAndOpenQueue( g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueKey, g_iMaxSmdaQueueCount * sizeof( stLogPacket ) ) ) < 0 )
			{
				CNLOG( ELOG, 1, "[T:%02d %02d][MakeAndOpenQueue( 0x%x %d ) Error][R:%d][E:%d][L:%d]\n", 
					iSmdaIndex + 1, iSessionIndex + 1, 
					g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueKey, g_iMaxSmdaQueueCount * sizeof( stLogPacket ), 
					g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID, errno, __LINE__ );

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].Lock) );

				CNNanoSleep( 10000000000 );

				continue;
			}

			CNLOG( NLOG, 3, "[T:%02d %02d][MakeAndOpenQueue( 0x%x %d ) Success][Queue ID: %d][L:%d]\n", 
				iSmdaIndex + 1, iSessionIndex + 1, 
				g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueKey, g_iMaxSmdaQueueCount * sizeof( stLogPacket ), 
				g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID, __LINE__ );
		}

		// 4.6 Queue에서 Log Packet 데이터 읽기를 시도
		iResult	= CNQueueRead( g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID, &stTempPacket, iReadPacketSize, 0 );

		switch( iResult )
		{
			// 4.6.1 Queue에서 정상적으로 Packet 데이터를 읽었을 경우
			case CN_IPC_QUEUE_DATA_READ :

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].Lock) );

				// 4.6.1.1 읽어들인 Queue데이터를 OnSmdaMsgRead()로 처리(Smda로 데이터 전송 혹은 파일로 기록)
				if( OnSmdaMsgRead( iSmdaIndex, iSessionIndex, iSocketFD, &stTempPacket, stpThreadArg, &tLastQueryTime ) < 0 )
				{
					CNLOG( ELOG, 1, "[T:%02d %02d][OnSmdaMsgRead( %d %d %d %d %d %d ) Error][Seq:%d][E:%d][L:%d]\n", 
						iSmdaIndex + 1, iSessionIndex + 1, 
						iSmdaIndex, iSessionIndex, iSocketFD, &stTempPacket, stpThreadArg, &tLastQueryTime, 
						stTempPacket.stHeader.iSerialNumber, errno, __LINE__ );

					// 4.6.1.1.1 OnLogMsgRead() 실패시 OnSmdaMsgReadError()로 예외처리(재처리 데이터로 등록, 데이터 파일 출력)
					if( OnSmdaMsgReadError( &stTempPacket, stpThreadArg ) < 0 )
					{
						CNLOG( ELOG, 1, "[T:%02d %02d][OnSmdaMsgReadError( %d %d ) Error][Seq:%d][E:%d][L:%d]\n", 
							iSmdaIndex + 1, iSessionIndex + 1, 
							&stTempPacket, stpThreadArg, 
							stTempPacket.stHeader.iSerialNumber, errno, __LINE__ );
					}

					// 4.6.1.1.2 Smda 전송 실패시 접속 종료처리
					if( iSocketFD >= 0 )
					{
						CNSocketClose( iSocketFD );

						iSocketFD	= -1;
					}
				}

				iSmdaQueueEmpty	= 0;

				break;
			
			// 4.6.2 Queue가 비어있을경우(Packet 데이터가 없을경우)
			case CN_IPC_QUEUE_DATA_EMPTY :

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].Lock) );

				OnSmdaMsgEmpty();

				iSmdaQueueEmpty++;

				continue;

				break;

			// 4.6.3 Queue읽기도중 에러가 난 경우
			case CN_IPC_QUEUE_DATA_ERROR :

				CNLOG( ELOG, 1, "[T:%02d %02d][CNQueueRead( %d %d %d 0 ) Error][R:%d][E:%d][L:%d]\n", 
					iSmdaIndex + 1, iSessionIndex + 1, 
					g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID, &stTempPacket, iReadPacketSize, 
					iResult, errno, __LINE__ );

				g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID	= -1;

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].Lock) );

				OnSmdaMsgError();

				iSmdaQueueEmpty	= 0;

				continue;

				break;

			// 4.6.4 Queue읽기도중 알수없는 에러가 난 경우
			default :

				CNLOG( ELOG, 1, "[T:%02d %02d][CNQueueRead( %d %d %d 0 ) Unknown Error][R:%d][E:%d][L:%d]\n", 
					iSmdaIndex + 1, iSessionIndex + 1, 
					g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID, &stTempPacket, iReadPacketSize, 
					iResult, errno, __LINE__ );

				g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID	= -1;

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].Lock) );

				OnSmdaMsgError();

				iSmdaQueueEmpty	= 0;

				continue;

				break;
		}
	}

	// 5. 종료시 Smda 서버와의 연결을 종료
	if( iSocketFD >= 0 )
	{
		CNSocketClose( iSocketFD );

		iSocketFD	= -1;
	}

	// 6. 종료!! OnSmdaThreadTerminate()가 이어서 실행(종료과정 및 데이터 해제등 담당)
	return	0;
}


int OnSmdaThreadTerminate( void* _vpArg )
{
	int	iResult;
	int	iSmdaIndex;
	int	iSessionIndex;

	stThread				*stpThreadInfo;
	stSmdaThreadArgument	*stpThreadArg;

	// 1. 아규먼트 초기화(Thread 정보를 지역변수로 받는다.
	stpThreadInfo	= (stThread*)_vpArg;
	stpThreadArg	= (stSmdaThreadArgument*)stpThreadInfo->vpArgs;

	iSmdaIndex	= stpThreadArg->iSmdaIndex;

	// 2. Heap 영역 데이터 해제
	if( stpThreadArg->NLOG != NULL )
		CNLOG( stpThreadArg->NLOG, 3, "[T:%02d %02d][SMDA Thread Terminate][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, __LINE__ );

	// 3. 재처리 데이터들을 파일에 기록(재기동시 읽어서 이어서 처리)
	if( ( iResult = OnWriteGutsSmda( stpThreadArg ) ) < 0 )
	{
		// OnSmdaThreadStart에서 에러가 날 경우 로그데이터를 이니셜라이징 하지 못할수도 있으므로
		if( stpThreadArg->ELOG != NULL )
		{
			CNLOG( stpThreadArg->ELOG, 1, "[T:%02d %02d][OnWriteGutsSmda( %d ) Error][R:%d][E:%d][L:%d]\n", 
				iSmdaIndex + 1, iSessionIndex + 1, stpThreadArg, iResult, errno, __LINE__ );
		}
	}

	if( stpThreadArg->cpEmergencyTime != NULL )
		free( stpThreadArg->cpEmergencyTime );	

	if( stpThreadArg->cpConfigPath != NULL )
		free( stpThreadArg->cpConfigPath );

	if( stpThreadArg->NLOG != NULL )
		free( stpThreadArg->NLOG );

	if( stpThreadArg->ELOG != NULL )
		free( stpThreadArg->ELOG );

	if( stpThreadArg != NULL )
		free( stpThreadArg );

	if( stpThreadInfo != NULL )
		free( stpThreadInfo );

	#ifdef OS508
	pthread_mutex_lock( &g_iSmdaThreadCountLock );
	g_iSmdaThreadCount--;
	pthread_mutex_unlock( &g_iSmdaThreadCountLock );
	#else
	atomic_dec_uint_nv( &g_iSmdaThreadCount );
	#endif

	// 4. Thread 종료!!
	return	0;
}


int OnSmdaMsgRead( int _iSmdaIndex, int _iSessionIndex, int _iSocketFD, stLogPacket* _stpLogPacket, stSmdaThreadArgument* _stpThreadArg, time_t* _tpQueryTime )
{
	int	iResult;

	CN_LOG_HANDLE	NLOG;
	CN_LOG_HANDLE	ELOG;

	// 1. 아규먼트로 부터 로그핸들 초기화
	NLOG	= _stpThreadArg->NLOG;
	ELOG	= _stpThreadArg->ELOG;

	#ifdef DEBUG
	CNLOG( NLOG, 3, "[Queue Msg Type:%ld][Log File Type:%d][Log File Name:%s][Routing Number:%s][L:%d]\n", 
		_stpLogPacket->iQueueMsgType, 
		_stpLogPacket->iLogFileType, 
		_stpLogPacket->caLogFileName, 
		_stpLogPacket->caRoutingNumber, 
		__LINE__ );

	CNLOG( NLOG, 3, "[System:0x00%x][Msg Type:0x00%x][Version:%d][Length:%d][Serial:%d][L:%d]\n", 
		_stpLogPacket->stHeader.iSystem,
		_stpLogPacket->stHeader.iMsgType,
		_stpLogPacket->stHeader.iVersion,
		_stpLogPacket->stHeader.iBodyLength,
		_stpLogPacket->stHeader.iSerialNumber,
		__LINE__ );

	CNLOG( NLOG, 3, "[Body:%s][Body Length:%d][L:%d]\n", 
		_stpLogPacket->caBody, strlen( _stpLogPacket->caBody ), __LINE__ );
	#endif

	// 2. Emergency 모드인 경우 데이터를 Smda로 전송하지 않고 Emergency 처리
	if( *g_ipEmergencyMode != 0 )
		return	OnEmergencyMode( _iSmdaIndex, _iSessionIndex, _stpLogPacket, _stpThreadArg );

	// 3. Smda 서버와 접속되어있지 않을경우 데이터를 전송하지 않음
	if( _iSocketFD < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][Smda Server Not Connected][Seq:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );

		return	-1;
	}

	// 4. Smda 서버로 데이터 전송
	if( ( iResult = OnSendDataToSmda( _iSocketFD, _stpLogPacket, _stpThreadArg ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][OnSendDataToSmda( %d %d %d) Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, 
			_iSocketFD, _stpLogPacket, _stpThreadArg, _stpLogPacket->stHeader.iSerialNumber, 
			iResult, errno, __LINE__ );

		*_tpQueryTime	= time( NULL );

		return	-1;
	}

	// 5. 최종 전송시간 업데이트
	*_tpQueryTime	= time( NULL );

	// 6. 성공!!
	return	iResult;
}


int OnSmdaMsgEmpty()
{
	return	0;
}


int OnSmdaMsgError()
{
	return	0;
}


int OnSmdaMsgReadError( stLogPacket* _stpLogPacket, stSmdaThreadArgument* _stpThreadArg )
{
	stContainer		stTempContainer;
	stContainer		*stpOffset;

	// 1. 로그 핸들을 초기화 한다(일반로그, 에러로그)
	CN_LOG_HANDLE	NLOG	= _stpThreadArg->NLOG;
	CN_LOG_HANDLE	ELOG	= _stpThreadArg->ELOG;

	// 2. 데이터 재처리 기능을 사용하지 않는 경우 데이터를 Memory에 저장하지않고 
	//    Error 파일로 바로 출력
	if( g_iSmdaRetryUseFlag == 0 )
		return	OnWriteSmdaThreadError( _stpLogPacket, _stpThreadArg );

	// 3. 재처리 기능을 사용한다 하더라도 재처리 시도 횟수가 0이거나 0보다 작은경우 
	//    데이터를 Memory에 저장하지않고 Error 파일로 바로 출력
	if( g_iSmdaRetryCount <= 0 )
		return	OnWriteSmdaThreadError( _stpLogPacket, _stpThreadArg );

	// 4. Queue가 꽉찬경우 Queue에서 가장 앞의 데이터를 삭제하고 넣는다.
	//    삭제한 데이터는 Error 데이터 기록파일에 기록된다. 문제가 되면
	//    추후에 Queue를 전부 비워버리는 함수OnSmdaQueueClear()로 교체한다.
	if( _stpThreadArg->List.m_reference >= MAX_SMDA_REPROCESS_DATA_SIZE )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][SMDA Reprocess Data Full][Delete Data Seq: %d][Seq:%d][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, 
			_stpThreadArg->List.m_head.next->iSequence, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );

		OnWriteSmdaThreadError( (stLogPacket*)_stpThreadArg->List.m_head.next->data, _stpThreadArg );

		CNDeleteList( &(_stpThreadArg->List), _stpThreadArg->List.m_head.next );
	}

	// 5. 데이터를 재처리(재전송)하기 위해 Memory 공간에 저장
	stpOffset	= CNPushList( &(_stpThreadArg->List), (char*)_stpLogPacket, sizeof( stLogPacket ), _stpLogPacket->stHeader.iSerialNumber );

	if( stpOffset == NULL )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNPushList( %d %d %d %d ) Error]][Seq:%d][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, 
			&(_stpThreadArg->List), _stpLogPacket, sizeof( stLogPacket ), _stpLogPacket->stHeader.iSerialNumber, 
			_stpLogPacket->stHeader.iSerialNumber, __LINE__ );

		return	-1;
	}

	// 6. 저장후 데이터 Container 데이터 셋팅
	stpOffset->iRequestCount	= 0;
	stpOffset->tLastActTime		= time( NULL );

	// 7. 성공!!
	return	0;
}


int OnSmdaQueueClear( stSmdaThreadArgument* _stpThreadArg )
{
	int		iDeleteCount	= 0;

	stContainer		*stpOffset;
	stLogPacket		*stpDeletePacket;

	// 1. 로그 핸들을 초기화 한다(일반로그, 에러로그)
	CN_LOG_HANDLE	NLOG	= _stpThreadArg->NLOG;
	CN_LOG_HANDLE	ELOG	= _stpThreadArg->ELOG;

	CNLOG( NLOG, 3, "[T:%02d %02d][SMDA Queue Clear Start][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, __LINE__ );

	// 2. 메모리에 적재된 재처리 데이터 Error파일에 출력하고 지운다
	for( stpOffset = _stpThreadArg->List.m_head.next; stpOffset->next; stpOffset = stpOffset->next )
	{
		stpDeletePacket	= (stLogPacket*)stpOffset->data;

		CNLOG( ELOG, 3, "[T:%02d %02d][Delete Queue Full Data][Seq:%d][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, stpDeletePacket->stHeader.iSerialNumber, __LINE__ );

		OnWriteSmdaThreadError( stpDeletePacket, _stpThreadArg );

		stpOffset	= CNDeleteList( &(_stpThreadArg->List), stpOffset );

		iDeleteCount++;
	}

	// 3. 삭제한 데이터 내역을 출력
	CNLOG( NLOG, 3, "[T:%02d %02d][SMDA Queue Data Delete][Delete Count: %d][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, iDeleteCount, __LINE__ );

	CNLOG( NLOG, 3, "[T:%02d %02d][SMDA Queue Clear End][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, __LINE__ );

	// 4. 성공!!
	return	0;
}


int OnWriteSmdaThreadError( stLogPacket* _stpLogPacket, stSmdaThreadArgument* _stpThreadArg )
{
	char	caWriteFile[512];
	char	caTimeBuffer[15];

	FILE	*fpWriteFile;

	// 1. 로그 핸들을 초기화 한다(일반로그, 에러로그)
	CN_LOG_HANDLE	NLOG	= _stpThreadArg->NLOG;
	CN_LOG_HANDLE	ELOG	= _stpThreadArg->ELOG;

	// 2. SmdaThread Error 파일 셋팅(절대경로+파일이름)
	sprintf( caWriteFile, "%s/%s_%02d_%02d_%-.*s_%s.dat", 
		g_cpErrorSmdaThreadFilePath, g_cpErrorSmdaThreadFileName, _stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, 
		10, GetNowTimeStr(caTimeBuffer), g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	// 3. Error 파일 열기(없을 시 생성, 있을 시 이어쓰기)
	if( ( fpWriteFile = fopen( caWriteFile, "a+" ) ) == NULL )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][fopen( %s, a+ ) Error][Seq:%d][E:%d][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, caWriteFile, _stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

		return	-1;
	}

	// 4. Error 데이터 파일 출력
	if( fwrite( _stpLogPacket, sizeof( stLogPacket ), 1, fpWriteFile ) != 1 )
	{
		fclose( fpWriteFile );

		CNLOG( ELOG, 1, "[T:%02d %02d][fwrite( %d %d 1 %d ) Error][Seq:%d][E:%d][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, _stpLogPacket, sizeof( stLogPacket ), fpWriteFile, 
			_stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

		return	-1;
	}

	// 5. Error 파일 Close
	fclose( fpWriteFile );

	// 6. 성공!!
	return	0;
}


int OnEmergencyMode( int _iSmdaIndex, int _iSessionIndex, stLogPacket* _stpLogPacket, stSmdaThreadArgument* _stpThreadArg )
{
	int		iResult;

	char	caTimeBuffer[15];
	char	caEmergencyFile[512];

	FILE*	fpFile;

	CN_LOG_HANDLE	NLOG;
	CN_LOG_HANDLE	ELOG;

	// 1. 로그 핸들을 초기화 한다(일반로그, 에러로그)
	NLOG	= _stpThreadArg->NLOG;
	ELOG	= _stpThreadArg->ELOG;

	// 2. Emergency 모드값이 1이 아닌경우 이머젼시 파일을 생성하지 않는다.
	// Emergency Mode 0: Emergency Mode가 아님, 0인 경우 이 루트로 올수없음
	// Emergency Mode 1: Smda로 데이터를 전송하지 않고 Emergency 파일에 데이터를 기록
	// Emergency Mode 2: Smda로 데이터를 전송하지 않고 Emergency 파일도 기록하지 않음
	// Emergency Mode 값이 0, 1, 2가 아닌경우 2로 판단하여 동작한다
	if( *g_ipEmergencyMode != 1 )
		return	0;

	// 3. 현재의 시간을 구해서 버퍼에 저장
	GetNowTimeStr( caTimeBuffer );

	// 4. 이전 디스크 사용량 체크 시점이 1분이 안됐을 경우
	if( memcmp( _stpThreadArg->cpEmergencyTime, caTimeBuffer, 12 ) == 0 )
	{
		// 4.1 파일링 모드가 0일경우 파일링 하지 않는다.
		//     파일 사용량이 최대 허용 사용량을 넘어간 경우 파일링 모드가 0이 된다.
		//     파일 사용량 체크는 아래 5에서 1분마다 한번씩 체크하여 모드를 결정한다.
		if( _stpThreadArg->iEmergencyFilingFlag == 0 )
		{
			/*
			CNLOG( ELOG, 1, "[T:%02d %02d][DiskQuantity OverFlow Error][Seq:%d][L:%d]\n", 
				_iSmdaIndex + 1, _iSessionIndex + 1, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );
			*/

			return	0;
		}
	}
	// 5. 이전 디스크 사용량 체크 시점이 1분이상일 경우
	else
	{
		// 5.1 현재 시각을 디스크 사용량 체크시간에 복사해 넣는다.
		strcpy( _stpThreadArg->cpEmergencyTime, caTimeBuffer );

		// 5.2 현재 디스크 사용량이 허용 사용량을 넘어선 경우
		if( ( iResult = GetDiskQuantity( g_cpCheckQuantityPath ) ) >= g_iMaxDiskQuantity )
		{
			CNLOG( ELOG, 1, "[T:%02d %02d][DiskQuantity( %s ) Error][DiskQuantity: %d][Max DiskQuantity: %d][L:%d]\n", 
				_iSmdaIndex + 1, _iSessionIndex + 1, g_cpCheckQuantityPath, iResult, g_iMaxDiskQuantity, __LINE__ );

			// 5.2.1 같은시간(분)동안 파일링 하지 않도록 파일링 플레그를 0(파일링하지 않음)으로 변경
			_stpThreadArg->iEmergencyFilingFlag	= 0;

			return	0;
		}
		// 5.3 현재 디스크 사용량이 허용 사용량 안일경우
		else
		{
			// 5.3.1 다음 사용량 체크시간까지 파일링을 수행하도록 셋팅
			_stpThreadArg->iEmergencyFilingFlag	= 1;
		}
	}

	// 5. Emergency 파일명 셋팅(절대경로+파일명, 시간별로 생성)
	sprintf( caEmergencyFile, "%s/%s_%02d_%02d_%-.*s_%s.dat", 
		g_cpEmergencyFilePath, g_cpEmergencyFileName, _iSmdaIndex + 1, _iSessionIndex + 1, 
		10, caTimeBuffer, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	// 6. 파일 Open, 없으면 생성, 있으면 이어쓴다
	if( ( fpFile = fopen( caEmergencyFile, "a+" ) ) == NULL )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][fopen( %s, a+ ) Error][Seq:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, g_cpEmergencyFilePath, _stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

		return	0;
	}

	// 7. Emergency 데이터 기록
	if( fwrite( _stpLogPacket, sizeof(stLogPacket) , 1, fpFile ) != 1 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][fwrite( %d %d 1 %d ) Error][Seq:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, _stpLogPacket, sizeof( stLogPacket ), fpFile, 
			_stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

		fclose( fpFile );

		return	0;
	}

	// 8. Emergency 파일을 닫는다.
	fclose( fpFile );

	// 9. 성공!!
	return	0;
}


int OnReadGutsSmda( stSmdaThreadArgument* _stpThreadArg )
{
	int		iResult;
	int		iReadCount	= 0;

	char	caTimeBuffer[15];
	char	caReadSmdaGutsFile[512];

	char	caDeleteCommand[512];

	stContainer		stTempContainer;
	stContainer		*stpOffset;

	FILE*			fpReadFile;

	// 1. 로그 핸들을 초기화 한다(일반로그, 에러로그)
	CN_LOG_HANDLE	NLOG	= _stpThreadArg->NLOG;
	CN_LOG_HANDLE	ELOG	= _stpThreadArg->ELOG;

	CNLOG( NLOG, 3, "[T:%02d %02d][Guts File Read Start][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, __LINE__ );

	// 2. 읽어들일 Guts 파일명 셋팅(절대경로+파일이름)
	sprintf( caReadSmdaGutsFile, "%s/%s_%02d_%02d_%-.*s_%s.guts", 
		g_cpGutsSmdaFilePath, g_cpGutsSmdaFileName, _stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, 
		8, GetNowTimeStr(caTimeBuffer), g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	CNLOG( NLOG, 3, "[T:%02d %02d][Read Guts File Name: %s][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, caReadSmdaGutsFile, __LINE__ );

	// 3. 파일을 연다. 파일이 없을경우 최초기동으로 판단
	if( ( fpReadFile = fopen( caReadSmdaGutsFile, "r" ) ) == NULL )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][fopen( %s, r ) Error][최초기동][E:%d][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, caReadSmdaGutsFile, errno, __LINE__ );

		return	0;
	}

	// 4. 파일에서 데이터를 읽어서 메모리에 저장(최종 연동시간은 데이터 Insert 시간으로 갱신)
	while( fread( &stTempContainer, sizeof( stTempContainer ), 1, fpReadFile ) )
	{
		stpOffset	= CNPushList( &(_stpThreadArg->List), stTempContainer.data, sizeof( stLogPacket ), stTempContainer.iSequence );

		stpOffset->iRequestCount	= stTempContainer.iRequestCount;
		stpOffset->tLastActTime		= time( NULL );

		iReadCount++;
	}

	// 5. 다 읽어들이면 파일을 닫는다
	fclose( fpReadFile );

	// 6. 모든 데이터를 읽어들이면 Guts 파일 삭제
	sprintf( caDeleteCommand, "\\rm -f %s", caReadSmdaGutsFile );
	system( caDeleteCommand );

	CNLOG( NLOG, 3, "[T:%02d %02d][Read Guts Data: %d]][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, iReadCount, __LINE__ );

	CNLOG( NLOG, 3, "[T:%02d %02d][Guts File Read End][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, __LINE__ );

	return	1;
}


int OnWriteGutsSmda( stSmdaThreadArgument* _stpThreadArg )
{
	int		iWriteCount	= 0;

	char	caTimeBuffer[15];
	char	caWriteSmdaGutsFile[512];

	stContainer				*stpOffset;

	FILE*					fpWriteFile;

	// 1. 로그 핸들을 초기화 한다(일반로그, 에러로그)
	CN_LOG_HANDLE	NLOG	= _stpThreadArg->NLOG;
	CN_LOG_HANDLE	ELOG	= _stpThreadArg->ELOG;

	CNLOG( NLOG, 3, "[T:%02d %02d][Guts File Write Start][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, __LINE__ );

	if( _stpThreadArg->List.m_reference == 0 )
	{
		CNLOG( NLOG, 3, "[T:%02d %02d][Guts Data Not Exist]][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, __LINE__ );

		CNLOG( NLOG, 3, "[T:%02d %02d][Guts File Write End][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, __LINE__ );

		return	0;
	}

	// 2. 데이터를 출력 할 Guts 파일 셋팅(절대경로+파일이름, 시간단위로 생성)
	sprintf( caWriteSmdaGutsFile, "%s/%s_%02d_%02d_%-.*s_%s.guts", 
		g_cpGutsSmdaFilePath, g_cpGutsSmdaFileName, _stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, 
		8, GetNowTimeStr(caTimeBuffer), g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	CNLOG( NLOG, 3, "[T:%02d %02d][Write Guts File Name: %s][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, caWriteSmdaGutsFile, __LINE__ );

	// 3. 이미 파일이 존재한다면 ThreadStart 혹은 Guts Read 단에서 문제가 생겨서 파일을 지우지 않고
	//    읽어들이지 못했으므로 데이터를 쓰지않고 종료한다.
	if( ( fpWriteFile = fopen( caWriteSmdaGutsFile, "r" ) ) != NULL )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][Write Guts File Exist][Guts File: %s][L:%d]\n",
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, caWriteSmdaGutsFile, __LINE__ );

		fclose( fpWriteFile );

		return	-1;
	}

	fclose( fpWriteFile );

	// 4. 재처리 데이터를 기록할 파일(Guts 파일)을 오픈한다
	if( ( fpWriteFile = fopen( caWriteSmdaGutsFile, "w" ) ) == NULL )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][fopen( %s w ) Error][L:%d]\n",
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, caWriteSmdaGutsFile, __LINE__ );

		fclose( fpWriteFile );

		return	-1;
	}

	// 5. 메모리에 적재된 재처리 데이터를 Guts 파일에 출력
	for( stpOffset = _stpThreadArg->List.m_head.next; stpOffset->next; stpOffset = stpOffset->next )
	{
		if( fwrite( stpOffset, sizeof( stContainer ), 1, fpWriteFile ) != 1 )
		{
			fclose( fpWriteFile );

			CNLOG( ELOG, 1, "[T:%02d %02d][fwrite( %d %d 1 %d ) Error][E:%d][L:%d]\n", 
				_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, stpOffset, 
				sizeof( stContainer ), fpWriteFile, errno, __LINE__ );

			return	-2;
		}

		stpOffset	= CNDeleteList( &(_stpThreadArg->List), stpOffset );

		iWriteCount++;
	}

	// 6. Guts 파일 Close
	fclose( fpWriteFile );

	CNLOG( NLOG, 3, "[T:%02d %02d][Write Guts Data: %d]][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, iWriteCount, __LINE__ );

	CNLOG( NLOG, 3, "[T:%02d %02d][Guts File Write End][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, __LINE__ );

	// 7. 성공!!
	return	0;
}


int OnSmdaDataReprocess( stSmdaThreadArgument* _stpThreadArg, int* _ipSocketFD, time_t* _tpQueryTime )
{
	int		iResult;
	int		iForLoop;

	int		iSmdaIndex;
	int		iSessionIndex;

	stContainer		*stpOffset;
	stLogPacket		*stpLogPacket;

	CN_LOG_HANDLE	NLOG;
	CN_LOG_HANDLE	ELOG;

	// 1. 아규먼트 값으로 지역변수 초기화 및 셋팅
	iSmdaIndex		= _stpThreadArg->iSmdaIndex;
	iSessionIndex	= _stpThreadArg->iSessionIndex;

	NLOG			= _stpThreadArg->NLOG;
	ELOG			= _stpThreadArg->ELOG;

	CNLOG( NLOG, 3, "[T:%02d %02d][Reprocess Start][Reference: %d][L:%d]\n", 
		iSmdaIndex + 1, iSessionIndex + 1, _stpThreadArg->List.m_reference, __LINE__ );

	// 2. Heap Memory 공간을 검색하면서 데이터 재처리(재전송)
	for( stpOffset = _stpThreadArg->List.m_head.next; stpOffset->next; stpOffset = stpOffset->next )
	{
		// 2.1 Emergency Mode 인 경우 데이터를 재전송하지 않고 OnEmergencyMode() 함수로 처리
		if( *g_ipEmergencyMode != 0 )
		{
			OnEmergencyMode( iSmdaIndex, iSessionIndex, (stLogPacket*)stpOffset->data, _stpThreadArg );
			
			stpOffset	= CNDeleteList( &(_stpThreadArg->List), stpOffset );

			continue;
		}

		// 2.2 저장된 데이터의 마지막 처리 시간이 재처리 시간(g_iSmdaRetryTime)보다 크거나 같은경우
		//     해당 데이터를 재처리(재전송)한다.
		//     결론적으로 OnSmdaDataReprocess() 함수가 실행되면 메모리 공간을 돌면서 마지막 처리 시간이
		//     재처리 시간(g_iSmdaRetryTime) 이상 지난 데이터를 재처리한다.
		if( ( time( NULL ) - stpOffset->tLastActTime ) >= g_iSmdaRetryTime )
		{
			// 2.2.1 재처리 카운트 증가
			stpOffset->iRequestCount++;

			// 2.2.2 Smda와 연결되지 않은 상태면 전송하지 않고 바로 전송처리
			if( *_ipSocketFD < 0 )
			{
				// 2.2.2.1 Smda 서버와 연결되어 있지 않은경우 바로 전송실패 처리, 로그 기록
				stpLogPacket	= (stLogPacket*)stpOffset->data;

				CNLOG( ELOG, 1, "[T:%02d %02d][Reprocess SMDA Server Not Connected][Request Count: %d][Seq:%d][L:%d]\n", 
					iSmdaIndex + 1, iSessionIndex + 1, stpOffset->iRequestCount, stpLogPacket->stHeader.iSerialNumber, __LINE__ );

				// 2.2.2.2 데이터의 재처리 회수가 최대 재처리회수(g_iSmdaRetryCount)보다 클경우 
				//         OnWriteSmdaThreadError()를 실행하여 Error 파일로 출력
				if( stpOffset->iRequestCount >= g_iSmdaRetryCount )
				{
					OnWriteSmdaThreadError( (stLogPacket*)stpOffset->data, _stpThreadArg );

					stpOffset	= CNDeleteList( &(_stpThreadArg->List), stpOffset );
				}
				// 2.2.2.3 데이터의 재처리 회수가 최대 재처리회수(g_iSmdaRetryCount)보다 작은경우
				//         마지막 처리 시간만 업데이트 하여 다음 주기때 재처리
				else
				{
					stpOffset->tLastActTime	= time( NULL );
				}

				continue;
			}

			// 2.2.3 Smda 서버로 해당 데이터를 재전송
			#ifdef DEBUG
			stpLogPacket	= (stLogPacket*)stpOffset->data;
			CNLOG( NLOG, 1, "[T:%02d %02d][Re Sending][Request Count: %d][Routing: %s][Seq: %d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1,
			stpOffset->iRequestCount,
			stpLogPacket->caRoutingNumber,
			stpLogPacket->stHeader.iSerialNumber,
			__LINE__ );
			#endif

			if( OnSendDataToSmda( *_ipSocketFD, (stLogPacket*)stpOffset->data, _stpThreadArg ) < 0 )
			{
				// 2.2.3.1 Socket을 Close처리하고 소켓 값을 -1로 셋팅
				CNSocketClose( *_ipSocketFD );

				*_ipSocketFD	= -1;

				// 2.2.2.2 로그 기록
				stpLogPacket	= (stLogPacket*)stpOffset->data;

				CNLOG( ELOG, 1, "[T:%02d %02d][Reprocess Send Data Error][Request Count: %d][Seq:%d][L:%d]\n", 
					iSmdaIndex + 1, iSessionIndex + 1, stpOffset->iRequestCount, stpLogPacket->stHeader.iSerialNumber, __LINE__ );

				// 2.2.3.3 데이터의 재처리 회수가 최대 재처리회수(g_iSmdaRetryCount)보다 클경우 
				//         OnWriteSmdaThreadError()를 실행하여 Error 파일로 출력
				if( stpOffset->iRequestCount >= g_iSmdaRetryCount )
				{
					OnWriteSmdaThreadError( (stLogPacket*)stpOffset->data, _stpThreadArg );

					stpOffset	= CNDeleteList( &(_stpThreadArg->List), stpOffset );
				}
				// 2.2.3.4 데이터의 재처리 회수가 최대 재처리회수(g_iSmdaRetryCount)보다 작은경우
				//         마지막 처리 시간만 업데이트 하여 다음 주기때 재처리
				else
				{
					stpOffset->tLastActTime	= time( NULL );
				}

				// 2.2.3.5 마지막 Smda 통신 시간을 현재 시간으로 업데이트
				*_tpQueryTime	= time( NULL );

				continue;
			}

			// 2.2.4 마지막 Smda 통신 시간을 현재 시간으로 업데이트
			*_tpQueryTime	= time( NULL );

			// 2.2.5 데이터를 메모리에서 삭제
			stpOffset	= CNDeleteList( &(_stpThreadArg->List), stpOffset );
		}
	}

	CNLOG( NLOG, 3, "[T:%02d %02d][Reprocess End][Reference: %d][L:%d]\n", 
		iSmdaIndex + 1, iSessionIndex + 1, _stpThreadArg->List.m_reference, __LINE__ );

	// 3. 성공!!
	return	0;
}


int OnConnectToSmda( int _iSmdaIndex, int _iSessionIndex, stSmdaThreadArgument* stpLogArg, char* _cpAdress, int _iPort, char* _cpBindID, char* _cpBindPW )
{
	int	iResult;

	int	iSocketFD;

	stLogPacketHeader	stBindPacketHeader;
	stSmdaBind			stBindPacketBody;

	stLogPacketHeader	stBindResultPacketHeader;
	stSmdaBindRes		stBindResultPacketBody;

	stClientError		stConnectError;
	stUtilError			stRwError;

	CN_LOG_HANDLE	NLOG;
	CN_LOG_HANDLE	ELOG;

	// 1. 로그 핸들을 초기화 한다(일반로그, 에러로그)
	NLOG	= stpLogArg->NLOG;
	ELOG	= stpLogArg->ELOG;

	memset( &stConnectError, 0x00, sizeof( stClientError ) );
	memset( &stRwError, 0x00, sizeof( stUtilError ) );

	// 2. Smda 서버로 접속 시도
	if( ( iSocketFD = CNMakeConnectionTCP( _cpAdress, _iPort, &stConnectError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNMakeConnectionTCP( %s %d %d ) Error][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, 
			_cpAdress, _iPort, &stConnectError, stConnectError.iReturnErrCode, stConnectError.iErrnoCode, __LINE__ );

		return	-1;
	}

	CNLOG( NLOG, 1, "[T:%02d %02d][SMDA Connected][IP: %s][Port: %d][L:%d]\n", 
		_iSmdaIndex + 1, _iSessionIndex + 1, _cpAdress, _iPort, __LINE__ );

	// 3. Smda 서버로 접속이 성공한 경우 로그인 과정을 위해 로그인 데이터 셋팅
	memset( &stBindPacketHeader, 0x00, sizeof( stLogPacketHeader ) );
	memset( &stBindPacketBody, 0x00, sizeof( stSmdaBind ) );

	stBindPacketHeader.iSystem			= 0;
	stBindPacketHeader.iMsgType			= BIND;
	stBindPacketHeader.iVersion			= 1;
	stBindPacketHeader.iBodyLength		= sizeof( stSmdaBind );

	#ifdef OS508
	pthread_mutex_lock( &g_iMsgSerialLock );
	stBindPacketHeader.iSerialNumber	= ++g_iMsgSerial;
	pthread_mutex_unlock( &g_iMsgSerialLock );
	#else
	stBindPacketHeader.iSerialNumber	= atomic_inc_uint_nv( &g_iMsgSerial );
	#endif

	strlcpy( stBindPacketBody.caBindID, _cpBindID, sizeof(stBindPacketBody.caBindID) );
	strlcpy( stBindPacketBody.caBindPass, _cpBindPW, sizeof(stBindPacketBody.caBindPass) );

	// 4. 로그인 패킷 헤더 전송
	CNLOG( NLOG, 1, "[T:%02d %02d][Bind Packet Header Send][System Code: 0x00%x][Msg Type: 0x00%x][Version: %d][Body Length: %d][Seq:%d][L:%d]\n", 
		_iSmdaIndex + 1, _iSessionIndex + 1, stBindPacketHeader.iSystem, stBindPacketHeader.iMsgType, stBindPacketHeader.iVersion, 
		stBindPacketHeader.iBodyLength, stBindPacketHeader.iSerialNumber, __LINE__ );

	if( ( iResult = CNSocketWrite( iSocketFD, (char*)&stBindPacketHeader, sizeof(stLogPacketHeader), 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketWrite( %d %d %d 3000 %d) Error][Bind Packet Header Send Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, iSocketFD, &stBindPacketHeader, sizeof(stLogPacketHeader), &stRwError,
			stBindPacketHeader.iSerialNumber, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		CNSocketClose( iSocketFD );

		return	-1;
	}

	// 5. 로그인 패킷 바디 전송
	CNLOG( NLOG, 1, "[T:%02d %02d][Bind Packet Body Send][ID: %s][PW: %s][Seq:%d][L:%d]\n", 
		_iSmdaIndex + 1, _iSessionIndex + 1, stBindPacketBody.caBindID, stBindPacketBody.caBindPass, stBindPacketHeader.iSerialNumber, __LINE__ );

	if( ( iResult = CNSocketWrite( iSocketFD, (char*)&stBindPacketBody, stBindPacketHeader.iBodyLength, 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketWrite( %d %d %d 3000 %d) Error][Bind Packet Body Send Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, iSocketFD, &stBindPacketBody, stBindPacketHeader.iBodyLength, &stRwError,
			stBindPacketHeader.iSerialNumber, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		CNSocketClose( iSocketFD );

		return	-1;
	}

	memset( &stBindResultPacketHeader, 0x00, sizeof( stLogPacketHeader ) );
	memset( &stBindResultPacketBody, 0x00, sizeof( stSmdaBindRes ) );

	// 6. 로그인 결과 패킷 헤더 수신
	CNLOG( NLOG, 1, "[T:%02d %02d][Bind Result Packet Header Read][Seq:%d][L:%d]\n", 
		_iSmdaIndex + 1, _iSessionIndex + 1, stBindPacketHeader.iSerialNumber, __LINE__ );

	if( ( iResult = CNSocketRead( iSocketFD, (char*)&stBindResultPacketHeader, sizeof(stLogPacketHeader), 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketRead( %d %d %d 3000 %d) Error][Bind Result Packet Header Read Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, iSocketFD, &stBindResultPacketHeader, sizeof(stLogPacketHeader), &stRwError,
			stBindPacketHeader.iSerialNumber, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		CNSocketClose( iSocketFD );

		return	-1;
	}

	// 7. 로그인 결과 패킷이 맞는지 MsgType 확인
	if( stBindResultPacketHeader.iMsgType != BIND_REPONSE )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][Bind Result Packet Header MsgType Error][MsgType: 0x00%x][Seq:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, stBindResultPacketHeader.iMsgType, stBindPacketHeader.iSerialNumber, __LINE__ );

		CNSocketClose( iSocketFD );

		return	-1;
	}

	// 8. 보낸 메시지에 대한 결과가 맞는지 SerialNumber 확인
	if( stBindResultPacketHeader.iSerialNumber != stBindPacketHeader.iSerialNumber )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][Bind Result Packet Header Sequence Error][Send Sequence: %d][Recv Sequence: %d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, stBindPacketHeader.iSerialNumber, stBindResultPacketHeader.iSerialNumber, __LINE__ );

		CNSocketClose( iSocketFD );

		return	-1;
	}

	// 9. 로그인 결과 패킷 보디 수신
	CNLOG( NLOG, 1, "[T:%02d %02d][Bind Result Packet Body Read][Seq:%d][L:%d]\n", 
		_iSmdaIndex + 1, _iSessionIndex + 1, stBindPacketHeader.iSerialNumber, __LINE__ );

	if( ( iResult = CNSocketRead( iSocketFD, (char*)&stBindResultPacketBody, stBindResultPacketHeader.iBodyLength, 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketRead( %d %d %d 3000 %d ) Error][Bind Result Packet Body Read Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, iSocketFD, &stBindResultPacketBody, stBindResultPacketHeader.iBodyLength, &stRwError,
			stBindPacketHeader.iSerialNumber, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		CNSocketClose( iSocketFD );

		return	-1;
	}

	// 10. 로그인 결과 패킷 보디를 읽어서 로그인 과정 성공인지 확인
	if( stBindResultPacketBody.iResult != 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][Bind Result Packet Body Result Error][Bind Result: %d][Seq:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, stBindResultPacketBody.iResult, stBindPacketHeader.iSerialNumber, __LINE__ );

		CNSocketClose( iSocketFD );

		return	-1;
	}

	CNLOG( NLOG, 3, "[T:%02d %02d][SMDA Log In Success][Seq:%d][L:%d]\n", 
		_iSmdaIndex + 1, _iSessionIndex + 1, stBindPacketHeader.iSerialNumber, __LINE__ );

	// 11. Smda 서버 접속 및 로그인 성공!!
	return	iSocketFD;
}


int OnSysQueryToSmda( int _iSmdaIndex, int _iSessionIndex, stSmdaThreadArgument* stpLogArg, int _iSocketFD )
{
	int		iResult;

	stLogPacketHeader	stSysQueryPacketHeader;
	stSmdaSysQuery		stSysQueryPacketBody;

	stLogPacketHeader	stSysQueryResultPacketHeader;
	stSmdaSysQueryRes	stSysQueryResultPacketBody;

	stUtilError			stRwError;

	CN_LOG_HANDLE	NLOG;
	CN_LOG_HANDLE	ELOG;

	// 1. 로그 핸들을 초기화 한다(일반로그, 에러로그)
	NLOG	= stpLogArg->NLOG;
	ELOG	= stpLogArg->ELOG;

	// 2. Smda로 전송 할 SysQuery 데이터 패킷 초기화 및 셋팅
	memset( &stRwError, 0x00, sizeof(stUtilError) );
	memset( &stSysQueryPacketHeader, 0x00, sizeof( stLogPacketHeader ) );
	memset( &stSysQueryPacketBody, 0x00, sizeof( stSmdaSysQuery ) );

	stSysQueryPacketHeader.iSystem			= 0;
	stSysQueryPacketHeader.iMsgType			= SYS_QUERY;
	stSysQueryPacketHeader.iVersion			= 1;
	stSysQueryPacketHeader.iBodyLength		= sizeof( stSmdaSysQuery );

	#ifdef OS508
	pthread_mutex_lock( &g_iMsgSerialLock );
	stSysQueryPacketHeader.iSerialNumber	= ++g_iMsgSerial;
	pthread_mutex_unlock( &g_iMsgSerialLock );
	#else
	stSysQueryPacketHeader.iSerialNumber	= atomic_inc_uint_nv( &g_iMsgSerial );
	#endif

	stSysQueryPacketBody.iSequence			= stSysQueryPacketHeader.iSerialNumber;

	CNLOG( NLOG, 3, "[T:%02d %02d][SysQuery Packet Header Send][System Code: 0x000%x][Msg Type: 0x00%x][Version: %d][Body Length: %d][Seq:%d][L:%d]\n", 
		_iSmdaIndex + 1, _iSessionIndex + 1, stSysQueryPacketHeader.iSystem, stSysQueryPacketHeader.iMsgType, stSysQueryPacketHeader.iVersion, 
		stSysQueryPacketHeader.iBodyLength, stSysQueryPacketHeader.iSerialNumber, __LINE__ );

	// 3. SysQuery(접속유지 요청) 패킷 헤더 전송
	if( ( iResult = CNSocketWrite( _iSocketFD, (char*)&stSysQueryPacketHeader, sizeof(stLogPacketHeader), 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketWrite( %d %d %d 3000 %d ) Error][SysQuery Packet Header Send Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, _iSocketFD, &stSysQueryPacketHeader, sizeof(stLogPacketHeader), &stRwError,
			stSysQueryPacketBody.iSequence, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	-1;
	}

	CNLOG( NLOG, 3, "[T:%02d %02d][System Query Packet Body Send][Seq:%d][L:%d]\n", _iSmdaIndex + 1, _iSessionIndex + 1, stSysQueryPacketBody.iSequence, __LINE__ );

	// 4. SysQuery(접속유지 요청) 패킷 보디 전송
	if( ( iResult = CNSocketWrite( _iSocketFD, (char*)&stSysQueryPacketBody, stSysQueryPacketHeader.iBodyLength, 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketWrite( %d %d %d 3000 %d ) Error][SysQuery Packet Body Send Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, _iSocketFD, &stSysQueryPacketBody, stSysQueryPacketHeader.iBodyLength, &stRwError,
			stSysQueryPacketBody.iSequence, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	-1;
	}

	memset( &stSysQueryResultPacketHeader, 0x00, sizeof( stLogPacketHeader ) );
	memset( &stSysQueryResultPacketBody, 0x00, sizeof( stSmdaSysQueryRes ) );

	CNLOG( NLOG, 3, "[T:%02d %02d][System Query Result Packet Header Read][Seq:%d][L:%d]\n", 
		_iSmdaIndex + 1, _iSessionIndex + 1, stSysQueryPacketBody.iSequence, __LINE__ );

	// 5. SysQuery(접속유지 요청) 결과 패킷 헤더 수신
	if( ( iResult = CNSocketRead( _iSocketFD, (char*)&stSysQueryResultPacketHeader, sizeof(stLogPacketHeader), 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketRead( %d %d %d 3000 %d ) Error][SysQuery Result Packet Header Read Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, _iSocketFD, &stSysQueryResultPacketHeader, sizeof(stLogPacketHeader), &stRwError,
			stSysQueryPacketBody.iSequence, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	-1;
	}

	// 6. 수신된 패킷이 SysQuery 요청에 대한 응답패킷(SYS_QUERY_REPONSE)인지 확인
	if( stSysQueryResultPacketHeader.iMsgType != SYS_QUERY_REPONSE )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][SysQuery Result Packet Header MsgType Error][MsgType: 0x00%x][Seq:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, stSysQueryResultPacketHeader.iMsgType, stSysQueryPacketBody.iSequence, __LINE__ );

		return	-1;
	}

	// 7. SysQuery(접속유지 요청)에서 요청한 패킷에 대한 결과 패킷인지 확인
	if( stSysQueryResultPacketHeader.iSerialNumber != stSysQueryPacketHeader.iSerialNumber )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][SysQuery Result Packet Header Sequence Error][Send Sequence: %d][Recv Sequence: %d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, stSysQueryPacketHeader.iSerialNumber, stSysQueryResultPacketHeader.iSerialNumber, __LINE__ );

		return	-1;
	}

	CNLOG( NLOG, 3, "[T:%02d %02d][System Query Result Packet Body Read][Seq:%d][L:%d]\n", 
		_iSmdaIndex + 1, _iSessionIndex + 1, stSysQueryPacketBody.iSequence, __LINE__ );

	// 8. SysQuery(접속유지 요청) 결과 패킷 보디 수신
	if( ( iResult = CNSocketRead( _iSocketFD, (char*)&stSysQueryResultPacketBody, stSysQueryResultPacketHeader.iBodyLength, 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketRead( %d %d %d 3000 %d ) Error][SysQuery Result Packet Body Read Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, _iSocketFD, &stSysQueryResultPacketBody, stSysQueryResultPacketHeader.iBodyLength, &stRwError,
			stSysQueryPacketBody.iSequence, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	-1;
	}

	// 9. 결과 패킷 보디에서 SysQuery 요청에 대한 결과(결과값) 확인
	if( stSysQueryResultPacketBody.iResult != 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][Sysquery Result Packet Body Result Error][SysQuery Result: %d][Seq:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, stSysQueryResultPacketBody.iResult, stSysQueryPacketBody.iSequence, __LINE__ );

		return	-1;
	}

	// 10. 성공!!
	return	0;
}


int OnSendDataToSmda( int _iSocketFD, stLogPacket* _stpLogPacket, stSmdaThreadArgument* _stpThreadArg )
{
	int		iResult;

	int		iSmdaIndex;
	int		iSessionIndex;

	stLogPacketHeader	stDataResultPacketHeader;
	int					iDataResultPacketBody;

	stUtilError		stRwError;
	
	CN_LOG_HANDLE	NLOG;
	CN_LOG_HANDLE	ELOG;

	// 1. 아규먼트 값으로 지역변수 초기화 및 셋팅
	iSmdaIndex		= _stpThreadArg->iSmdaIndex;
	iSessionIndex	= _stpThreadArg->iSessionIndex;

	NLOG			= _stpThreadArg->NLOG;
	ELOG			= _stpThreadArg->ELOG;

	#ifdef DEBUG
	CNLOG( NLOG, 1, "[T:%02d %02d][SEND_DATA Header][System: %d][Msg Type: 0x00%x][Version: %d][Msg Length: %d][Serial: %d][L:%d]\n", 
		iSmdaIndex + 1, iSessionIndex + 1, _stpLogPacket->stHeader.iSystem, _stpLogPacket->stHeader.iMsgType, _stpLogPacket->stHeader.iVersion,
		_stpLogPacket->stHeader.iBodyLength, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );

	CNLOG( NLOG, 1, "[T:%02d %02d][SEND_DATA Body][Data: %s][L:%d]\n", iSmdaIndex + 1, iSessionIndex + 1, _stpLogPacket->caBody, __LINE__ );
	#endif

	// 2. 아규먼트로 넘겨받은 데이터를 전송
	if( ( iResult = CNSocketSend( _iSocketFD, (char*)&(_stpLogPacket->stHeader), sizeof(_stpLogPacket->stHeader) + _stpLogPacket->stHeader.iBodyLength, 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketSend( %d %d %d 3000 %d ) Error][DATA_SEND Packet Send Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, _iSocketFD, &(_stpLogPacket->stHeader), sizeof(_stpLogPacket->stHeader) + _stpLogPacket->stHeader.iBodyLength, &stRwError,
			_stpLogPacket->stHeader.iSerialNumber, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	-1;
	}

	// 3. 결과 패킷의 헤더 데이터를 수신
	if( ( iResult = CNSocketRecv( _iSocketFD, (char*)&stDataResultPacketHeader, sizeof(stLogPacketHeader), 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketRecv( %d %d %d 3000 %d ) Error][DATA_SEND_ACK Packet Header Read Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, _iSocketFD, &stDataResultPacketHeader, sizeof(stLogPacketHeader), &stRwError,
			_stpLogPacket->stHeader.iSerialNumber, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	-1;
	}

	// 4. 결과 패킷의 보디 데이터를 수신
	if( ( iResult = CNSocketRecv( _iSocketFD, (char*)&iDataResultPacketBody, sizeof(iDataResultPacketBody), 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketRecv( %d %d %d 3000 %d ) Error][DATA_SEND_ACK Packet Body Read Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, _iSocketFD, &iDataResultPacketBody, sizeof(iDataResultPacketBody), &stRwError,
			_stpLogPacket->stHeader.iSerialNumber, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	-1;
	}

	#ifdef DEBUG
	CNLOG( NLOG, 1, "[T:%02d %02d][SEND_DATA_ACK Header][System: %d][Msg Type: 0x00%x][Version: %d][Msg Length: %d][Serial: %d][L:%d]\n", 
		iSmdaIndex + 1, iSessionIndex + 1, stDataResultPacketHeader.iSystem, stDataResultPacketHeader.iMsgType, stDataResultPacketHeader.iVersion,
		stDataResultPacketHeader.iBodyLength, stDataResultPacketHeader.iSerialNumber, __LINE__ );

	CNLOG( NLOG, 1, "[T:%02d %02d][SEND_DATA_ACK Body][Result: %d][L:%d]\n", iSmdaIndex + 1, iSessionIndex + 1, iDataResultPacketBody, __LINE__ );
	#endif

	// 5. 결과 패킷의 보디데이터로 데이터 처리 결과를 확인
	if( iDataResultPacketBody != 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][DATA_SEND_ACK Packet Result Code Error][Result Code:%d][Seq:%d][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, iDataResultPacketBody, _stpLogPacket->stHeader.iSerialNumber, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	0;
	}

	// 6. 성공!!
	return	0;
}


static int GetLogThreadDone()
{
	#ifdef OS508
	int	iDone;

	pthread_mutex_lock( &g_iLogThreadDoneLock );
	iDone	= g_iLogThreadDone;
	pthread_mutex_unlock( &g_iLogThreadDoneLock );

	return	iDone;
	#else
	return	g_iLogThreadDone;
	#endif
}


static int GetSmdaThreadDone()
{
	#ifdef OS508
	int	iDone;

	pthread_mutex_lock( &g_iSmdaThreadDoneLock );
	iDone	= g_iSmdaThreadDone;
	pthread_mutex_unlock( &g_iSmdaThreadDoneLock );

	return	iDone;
	#else
	return	g_iSmdaThreadDone;
	#endif
}


static int GetLogThreadCount()
{
	#ifdef OS508
	int	iCount;

	pthread_mutex_lock( &g_iLogThreadCountLock );
	iCount	= g_iLogThreadCount;
	pthread_mutex_unlock( &g_iLogThreadCountLock );

	return	iCount;
	#else
	return	g_iLogThreadCount;
	#endif
}


static int GetSmdaThreadCount()
{
	#ifdef OS508
	int	iCount;

	pthread_mutex_lock( &g_iSmdaThreadCountLock );
	iCount	= g_iSmdaThreadCount;
	pthread_mutex_unlock( &g_iSmdaThreadCountLock );

	return	iCount;
	#else
	return	g_iSmdaThreadCount;
	#endif
}


static CN_LOG_HANDLE GetLogThreadLogHandle( int _iThreadIndex, char* _cpConfigFilePath, char* _cpFileName )
{
	int		iResult;

	int		iLogType;
	int		iLogLevel;

	char	caLogFilePath[256];
	char	caLogFileName[256];

	char	caConfigFile[512];

	CN_LOG_HANDLE	stpTempHandle;

	// 1. 읽어들일 컨피그 파일(파일경로+파일이름) 셋팅
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, LOG_MANAGER_CONFIG_FILE_NAME );

	// 2. 컨피그 파일에서 Thread Log Type 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"THREAD_LOG_FILE_TYPE", &iLogType ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[T:%d][CNGetConfigInt( %s COMMON THREAD_LOG_FILE_TYPE %d ) Error][R:%d][L:%d]\n", 
			_iThreadIndex + 1, caConfigFile, &iLogType, iResult, __LINE__ );

		return	NULL;
	}

	// 3. 컨피그 파일에서 Thread Log Level 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"THREAD_LOG_FILE_LEVEL", &iLogLevel ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[T:%d][CNGetConfigInt( %s COMMON THREAD_LOG_FILE_LEVEL %d ) Error][R:%d][L:%d]\n", 
			_iThreadIndex + 1, caConfigFile, &iLogLevel, iResult, __LINE__ );

		return	NULL;
	}

	memset( caLogFilePath, 0x00, sizeof( caLogFilePath ) );

	// 4. 컨피그 파일에서 Thread Log Path(로그파일 경로) 셋팅
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"THREAD_LOG_FILE_PATH", caLogFilePath, sizeof(caLogFilePath) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[T:%d][CNGetConfigStr( %s COMMON THREAD_LOG_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			_iThreadIndex + 1, caConfigFile, caLogFilePath, sizeof(caLogFilePath), iResult, __LINE__ );

		return	NULL;
	}

	// 5. 로그파일 이름 셋팅
	sprintf( caLogFileName, "%s_%02d", _cpFileName, _iThreadIndex + 1 );

	// 6. 셋팅된 데이터로 로거 초기화 및 등록(이때부터 Tread Log 쓰기 가능)
	if( ( stpTempHandle = CNGetLogHandle( iLogType, iLogLevel, caLogFilePath, caLogFileName, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[T:%d][CNGetLogHandle( %d %d %s %s %s ) Error][L:%d]\n", 
			_iThreadIndex + 1, iLogType, iLogLevel, caLogFilePath, caLogFileName, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A", __LINE__ );

		return	NULL;
	}

	// 7. 성공!!
	return	stpTempHandle;
}


static CN_LOG_HANDLE GetSmdaThreadLogHandle( int _iSmdaIndex, int _iSessionIndex, char* _cpConfigFilePath, char* _cpFileName )
{
	int		iResult;

	int		iLogType;
	int		iLogLevel;

	char	caLogFilePath[256];
	char	caLogFileName[256];

	char	caConfigFile[512];

	CN_LOG_HANDLE	stpTempHandle;

	// 1. 읽어들일 컨피그 파일(파일경로+파일이름) 셋팅
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, LOG_MANAGER_CONFIG_FILE_NAME );

	// 2. 컨피그 파일에서 Thread Log Type 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"THREAD_LOG_FILE_TYPE", &iLogType ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[T:%02d %02d][CNGetConfigInt( %s COMMON THREAD_LOG_FILE_TYPE %d ) Error][R:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, caConfigFile, &iLogType, iResult, __LINE__ );

		return	NULL;
	}

	// 3. 컨피그 파일에서 Thread Log Level 셋팅
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"THREAD_LOG_FILE_LEVEL", &iLogLevel ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[T:%02d %02d][CNGetConfigInt( %s COMMON THREAD_LOG_FILE_LEVEL %d ) Error][R:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, caConfigFile, &iLogLevel, iResult, __LINE__ );

		return	NULL;
	}

	memset( caLogFilePath, 0x00, sizeof( caLogFilePath ) );

	// 4. 컨피그 파일에서 Thread Log Path(로그파일 경로) 셋팅
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"THREAD_LOG_FILE_PATH", caLogFilePath, sizeof(caLogFilePath) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[T:%02d %02d][CNGetConfigStr( %s COMMON THREAD_LOG_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, caConfigFile, caLogFilePath, sizeof(caLogFilePath), iResult, __LINE__ );

		return	NULL;
	}

	// 5. 로그파일 이름 셋팅
	sprintf( caLogFileName, "%s_%02d_%02d", _cpFileName, _iSmdaIndex + 1, _iSessionIndex + 1 );

	// 6. 셋팅된 데이터로 로거 초기화 및 등록(이때부터 Tread Log 쓰기 가능)
	if( ( stpTempHandle = CNGetLogHandle( iLogType, iLogLevel, caLogFilePath, caLogFileName, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[T:%02d %02d][CNGetLogHandle( %d %d %s %s %s ) Error][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, iLogType, iLogLevel, caLogFilePath, caLogFileName, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A", __LINE__ );

		return	NULL;
	}

	// 7. 성공!!
	return	stpTempHandle;
}


static int GetDiskQuantity( char* _cpDiskName )
{
	int		iForLoop;
	int		iQuantity = -1;

	char	*cpToken[512];
	char	*cpSavePoint;

	char	caDiskPathBuf[512];	
	char	caBuf[513];

	FILE	*fpDisk;

	// 1. 사용량을 체크 할 디스크 경로 셋팅
	sprintf( caDiskPathBuf, "df -k %s", _cpDiskName );

	// 2. df -k 명령어의 디스크 사용량 부분을 추출해서 변수에 저장
	if( ( fpDisk = popen( caDiskPathBuf , "r") ) != NULL )
	{
		while( fgets( caBuf, 512, fpDisk ) != NULL && caBuf[0] != '\n' )
		{
			iForLoop = 1;

			// 2.1 공백 기준으로 Token 생성
			cpToken[iForLoop] = strtok_r( caBuf, " ", &cpSavePoint );

			// 2.2 df -k의 디스크 정보 각 필드를 변수에 차례대로 저장
			while( iForLoop < 6 )
			{
				++iForLoop;

				cpToken[iForLoop] = strtok_r( NULL, " ", &cpSavePoint );
			}
		}

		// 2.3 디스크 정보 닫기
		pclose( fpDisk );

		// 2.4 6번째 필드가 디스크 사용량 이므로 사용량 변수에 저장
		iQuantity = atoi( cpToken[5] );
	}
	else
	{
		pclose( fpDisk );
	}

	// 3. 성공!!
	return	iQuantity;
}


static void CNNanoSleep( unsigned long _iNano )
{
	struct timespec	tsStartTime;
	struct timespec	tsRemainTime;

	if( _iNano <= 0 )
		return;

	// 1. Unsigned long 의 사이즈를 초과하는 변수가 넘어올 경우 예외처리
	if( _iNano >= 4000000000 )
		_iNano	= 4000000000;

	// 2. nano sleep 에서 1000000000 는 1초이므로 나눠서 각 필드에 값을 저장
	if( _iNano >= 1000000000 )
	{
		tsStartTime.tv_sec	= _iNano / 1000000000;
		tsStartTime.tv_nsec	= _iNano % 1000000000;
	}
	else
	{
		tsStartTime.tv_sec	= 0;
		tsStartTime.tv_nsec	= _iNano;
	}

	// 3. 중간에 인터넙트가 발생 한 경우 남은 시간만큼 다시 Sleep로 들어간다
	while( ( nanosleep( &tsStartTime, &tsRemainTime ) == -1 ) && ( errno == EINTR ) )
	{
		// 3.1 인터럽트로 인해 Sleep하지 못한 시간을 다시 넣는다
		tsStartTime.tv_sec	= tsRemainTime.tv_sec;
		tsStartTime.tv_nsec	= tsRemainTime.tv_nsec;
		
		// 3.2 남은시간 저장 버퍼(변수)를 초기화
		tsRemainTime.tv_sec		= 0x00;
		tsRemainTime.tv_nsec	= 0x00;
	}

	// 4. 종료!!
	return;
}


static int MakeAndOpenQueue( int _iQueueKey, int _iQueueSize )
{
	int	iQueueID;

	// 1. 이미 해당 Key 값의 Queue가 존재 할 경우
	if( ( iQueueID = CNQueueCreate( _iQueueKey ) ) < 0 )
	{
		// 1.1 Key 값의 Queue를 Open(Attach)만 한다
		if( ( iQueueID = CNQueueOpen( _iQueueKey ) ) < 0 )
		{
			return	-1;
		}
	}
	// 2. Queue를 신규로 생성했을 경우
	else
	{
		// 2.1 새로 생성하였을 경우 Queue의 최대 사이즈를 조정한다
		if( CNQueueSetSize( iQueueID, _iQueueSize ) < 0 )
		{
			return	-2;
		}

	}

	// 3. 성공!!
	return	iQueueID;
}


static int DateCheck( time_t _tBrforeTime )
{
	time_t	tNowTime;

	struct tm	tmNowTime;
	struct tm	tmBrforeTime;

	tNowTime	= time( NULL );

	// 1. 이전 시간과 현재시간을 Struct tm 형으로 변경하여
	//    각각 저장한다(비교를 위해)
	localtime_r( &tNowTime, &tmNowTime );
	localtime_r( &_tBrforeTime, &tmBrforeTime );

	// 2. 날짜가 변경 된 경우
	if( tmNowTime.tm_mday != tmBrforeTime.tm_mday )
		return	1;

	// 3. 시간이 변경 된 경우
	if( tmNowTime.tm_hour != tmBrforeTime.tm_hour )
		return	2;

	// 4. 분이 변경 된 경우
	if( tmNowTime.tm_min != tmBrforeTime.tm_min )
		return	3;
	
	// 5. 초가 변경 된 경우
	if( tmNowTime.tm_sec != tmBrforeTime.tm_sec )
		return	4;

	// 6. 성공!!
	return	0;
}

