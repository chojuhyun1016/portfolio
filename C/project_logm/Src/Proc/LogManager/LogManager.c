// �⺻
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <strings.h>
#include <sys/atomic.h>

// ProcessCheck ����
#include <fcntl.h>
#include <dirent.h>
#include <procfs.h>

// Shared Memory ����
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/shm.h>

#include "LogManager.h"

// �ð��� ���ڿ� ����
#include "CNBaseTime.h"

// Config ����
#include "CNConfigRead.h"

// Signal ����
#include "CNSignalApi.h"
#include "CNSignalInit.h"

// Thread ����
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

	// 1. Agrument �˻�
	if( argc != 2 )
	{
		fprintf( stderr, "[Argument Error][L: %d]\n", __LINE__ );

		exit( -1 );
	}

	// 2. ������ �̸��� Process�� ����ǰ� �ִ��� üũ
	if( ( iResult = ProcessCheck( (char*)LOG_MANAGER_PROCESS_NAME ) ) != 1 )
	{
		fprintf( stderr, "[ProcessCheck( %s ) Error][R:%d][L:%d]\n", LOG_MANAGER_PROCESS_NAME, iResult, __LINE__  );

		exit( -1 );
	}

	// 3. ���� �� ������ �ʱ�ȭ
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

	// 4. �⺻���� ���μ����� �������� Config ���Ͽ��� �о ����
	if( ( iResult = ProcessDataInit( argv[1] ) ) < 0 )
	{
		fprintf( stderr, "[ProcessDataInit( %s ) Error][R:%d][L:%d]\n", argv[1], iResult, __LINE__  );

		ProcessTerminate( -1 );
	}

	// 5. Signal Handler ���(�ñ׳� ó��)
	if( ( iResult = CNInitSignal( SignalHandler ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNInitSignal( %d ) Error][R:%d][L:%d]\n", SignalHandler, iResult, __LINE__ );

		ProcessTerminate( -1 );
	}

	// 6. ���ݱ��� ó���� Sequence�� Guts ���Ͽ��� �о ����
	if( ( iResult = ReadSequenceFile() ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[ReadSequenceFile() Error][R:%d][L:%d]\n", iResult, __LINE__ );

		ProcessTerminate( -1 );
	}

	// 7. Emergency Mode �� ���� ������ ����
	if( ( iResult = EmergencyTableInit( argv[1] ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[EmergencyTableInit( %s ) Error][R:%d][L:%d]\n", argv[1], iResult, __LINE__ );

		ProcessTerminate( -1 );
	}

	// 8. LogQueue �� LogQueue Thread �� ���� �����͸� �о ����
	if( ( iResult = LogQueueDataInit( argv[1] ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[LogQueueDataInit( %s ) Error][R:%d][L:%d]\n", argv[1], iResult, __LINE__  );

		ProcessTerminate( -1 );
	}

	// 9.  SmdaQueue �� SmdaQueue Thread �� ���� �����͸� �о ����
	if( ( iResult = SmdaQueueDataInit( argv[1] ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SmdaQueueDataInit( %s ) Error][R:%d][L:%d]\n", argv[1], iResult, __LINE__  );

		ProcessTerminate( -1 );
	}

	// 10. SmdaRouting �����͸� �о ����
	if( ( iResult = SmdaRoutingDataInit( argv[1] ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SmdaRoutingDataInit( %s ) Error][R:%d][L:%d]\n", argv[1], iResult, __LINE__  );

		ProcessTerminate( -1 );
	}

	// 11. Smda Thread ����
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

	// 12. Log Thread ����
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

	// 13. Loop�� ���鼭 �ֱ������� �ؾ� �� �۾��� ó��
	tBeforeTime	= time( NULL );

	while( g_iDone )
	{
		// ��¥�� ���� üũ
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

	// 14. ���μ��� ����ó�� ���� ����(Thread�� ��� �����Ų �� ������ ����)
	{
		//14.1 LogThread ���� ���� ����
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

		//14.2 LogThread�� ������ 0�� �ɶ����� ��ٸ��� 
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

		//14.3 SmdaThread ���� ���� ����
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

		//14.4 SmdaThread�� ������ 0�� �ɶ����� ��ٸ��� 
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

	// 15. ������ ��ȯ, �����۾� ������(Heap �� ���� ������ ����)
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
	// 1. ���ݱ��� �߱��� Serial Number�� ���Ͽ� ���
	//    ��⵿�� �о���δ�.
	WriteSequenceFile();

	// 2. �α׿� ���õ� �޸� ����
	CNDeleteHandle( NOR_LOG );
	CNDeleteHandle( ERR_LOG );

	// 3. ������ ����(�ַ� ���ڿ�) �޸� ����
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

	// 4. LOG Thread ���� ���� �޸� ����
	LogQueueDataFree();

	// 5. SMDA Thread ���� ���� �޸� ����
	SmdaQueueDataFree();

	// 6. SMDA Routing ���� ���� �޸� ����
	SmdaRoutingDataFree();

	// 7. ����!!
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

	// 1. Solaris ���� ���μ��� ������ ����Ǵ� ���丮�� ����
	if( ( dpDirPoint = opendir( "/proc" ) ) == NULL )
	{
		fprintf( stderr, "[opendir( /proc) Error][E:%d][L:%d]\n", errno, __LINE__ );

		return	-1;
	}

	// 2. ���丮 ������ �������� ���� �̸�(_cpProcessName)�� ���μ����� �ִ��� �˻�
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

		// 2.1 ���μ��� ������ �����ϰ� �ִ� ���ϸ�
		sprintf( caProcFile, "/proc/%s/psinfo", stpDirData->d_name );
		
		// 2.2 ���μ��� ������ �����ϰ� �ִ� ������ ����
		if( ( iFileFd = open( caProcFile, O_RDONLY ) ) < 0 )
			continue;

		// 2.3 ���μ��� ������ ����ü�� �����Ѵ�
		if( read( iFileFd, (void *)&stPinfo, sizeof( psinfo_t ) ) <= 0 )
		{
			close( iFileFd );

			continue;
		}

		// 2.4 ���� ���μ����̸��� ������ ��� �ش� ���μ���
		//     ī��Ʈ�� ������Ų��.
		if( strcmp( _cpProcessName ,stPinfo.pr_fname ) == 0 )
			iProcessCount++;

		close( iFileFd );
	}

	// 3. ���丮 ������ �ݴ´�.
	closedir( dpDirPoint );

	// 4. ����!!(�������� ������ �̸��� ���μ��� ���� ��ȯ)
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

	// 1. ���μ��� ���Ǳ� ���� ����
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, LOG_MANAGER_CONFIG_FILE_NAME );

	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"CENTER_TYPE", &g_iCenterType ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON CENTER_TYPE %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &g_iCenterType, iResult, __LINE__ );

		return	-1;
	}

	// 2. Sequence �����͸� ����� ������ ��� ����(������)
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"SEQUENCE_FILE_PATH", caSequencePathBuffer, sizeof(caSequencePathBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON SEQUENCE_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caSequencePathBuffer, sizeof(caSequencePathBuffer), iResult, __LINE__ );

		return	-1;
	}

	iLength = strlen( caSequencePathBuffer ) + 1;
	g_cpSequenceFilePath = (char*)malloc( iLength );
	strlcpy( g_cpSequenceFilePath, caSequencePathBuffer, iLength );

	// 3. Sequence �����͸� ����� �����̸� ����
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"SEQUENCE_FILE_NAME", caSequenceFileBuffer, sizeof(caSequenceFileBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON SEQUENCE_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caSequenceFileBuffer, sizeof(caSequenceFileBuffer), iResult, __LINE__ );

		return	-1;
	}

	iLength = strlen( caSequenceFileBuffer ) + 1;
	g_cpSequenceFileName = (char*)malloc( iLength );
	strlcpy( g_cpSequenceFileName, caSequenceFileBuffer, iLength );

	// 5. EMERGENCY ���� ���Ͼ��� �Ѱ� �뷮 ����( ��ũ ��뷮�� g_iMaxDiskQuantity �� ������ �̸����� �����͸� ���� �ʴ´�.)
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"MAX_DISK_QUANTITY", &g_iMaxDiskQuantity ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON MAX_DISK_QUANTITY %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &g_iMaxDiskQuantity, iResult, __LINE__ );

		return	-1;
	}

	// 6. EMERGENCY ���� ��ũ ��뷮�� üũ �� ��ũ ��� ����(������)
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"CHECK_QUANTITY_PATH", caQuanTityBuffer, sizeof(caQuanTityBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON CHECK_QUANTITY_DISK %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caQuanTityBuffer, sizeof(caQuanTityBuffer), iResult, __LINE__ );

		return	-1;
	}

	iLength	= strlen( caQuanTityBuffer ) + 1;
	g_cpCheckQuantityPath = (char*)malloc( iLength );
	strlcpy( g_cpCheckQuantityPath, caQuanTityBuffer, iLength );

	// 7. EMERGENCY ���� ������ EMERGENCY ������ ������ ���(������)
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"EMERGENCY_FILE_PATH", caEmergencyPathBuffer, sizeof(caEmergencyPathBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON EMERGENCY_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caEmergencyPathBuffer, sizeof(caEmergencyPathBuffer), iResult, __LINE__ );

		return	-1;
	}

	iLength	= strlen( caEmergencyPathBuffer ) + 1;
	g_cpEmergencyFilePath = (char*)malloc( iLength );
	strlcpy( g_cpEmergencyFilePath, caEmergencyPathBuffer, iLength );

	// 8. EMERGENCY ���� ������ EMERGENCY ������ ������ �̸�
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"EMERGENCY_FILE_NAME", caEmergencyFileBuffer, sizeof(caEmergencyFileBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON EMERGENCY_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caEmergencyFileBuffer, sizeof(caEmergencyFileBuffer), iResult, __LINE__ );

		return	-1;
	}

	iLength	= strlen( caEmergencyFileBuffer ) + 1;
	g_cpEmergencyFileName = (char*)malloc( iLength );
	strlcpy( g_cpEmergencyFileName, caEmergencyFileBuffer, iLength );

	// 9. ���Ǳ� ���Ͽ��� �Ϲ� �α������� ����Ÿ��(��¥��,�ð���) ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"NORMAL_LOG_FILE_TYPE", &iLogType ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON NORMAL_LOG_FILE_TYPE %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iLogType, iResult, __LINE__ );

		return	-1;
	}

	// 10. ���Ǳ� ���Ͽ��� �Ϲ� �α������� �α׷���(������ ���� �αװ� ����ȭ �� ����ȭ �ȴ�) ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"NORMAL_LOG_FILE_LEVEL", &iLogLevel ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON NORMAL_LOG_FILE_LEVEL %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iLogLevel, iResult, __LINE__ );

		return	-1;
	}

	// 11. ���Ǳ� ���Ͽ��� �Ϲ� �α������� ���� �� ��θ�(������) ����
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"NORMAL_LOG_FILE_PATH", caBuffer1, sizeof(caBuffer1) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON NORMAL_LOG_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caBuffer1, sizeof(caBuffer1), iResult, __LINE__ );

		return	-1;
	}

	// 12. ���Ǳ� ���Ͽ��� �Ϲ� �α������� �����̸� ����
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"NORMAL_LOG_FILE_NAME", caBuffer2, sizeof(caBuffer2) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON NORMAL_LOG_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caBuffer2, sizeof(caBuffer2), iResult, __LINE__ );

		return	-1;
	}

	// 13. ���Ǳ׿��� �о���� �����ͷ� �Ϲݷα� �ΰ� �ʱ�ȭ �� ���(�̶����� �Ϲݷα� ���� ����)
	if( ( NOR_LOG = CNGetLogHandle( iLogType, iLogLevel, caBuffer1, caBuffer2, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" ) ) == NULL )
	{
		fprintf( stderr, "[CNGetLogHandle( %d %d %s %s %s ) Error][L:%d]\n", 
			iLogType, iLogLevel, caBuffer1, caBuffer2, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A", __LINE__ );

		return	-1;
	}

	// 14. ���Ǳ� ���Ͽ��� ���� �α������� ����Ÿ��(��¥��,�ð���) ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"ERROR_LOG_FILE_TYPE", &iLogType ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON ERROR_LOG_FILE_TYPE %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iLogType, iResult, __LINE__ );

		return	-1;
	}

	// 15. ���Ǳ� ���Ͽ��� ���� �α������� �α׷���(������ ���� �αװ� ����ȭ �� ����ȭ �ȴ�) ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"ERROR_LOG_FILE_LEVEL", &iLogLevel ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON ERROR_LOG_FILE_LEVEL %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iLogLevel, iResult, __LINE__ );

		return	-1;
	}

	// 16. ���Ǳ� ���Ͽ��� ���� �α������� ���� �� ��θ�(������) ����
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"ERROR_LOG_FILE_PATH", caBuffer1, sizeof(caBuffer1) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON ERROR_LOG_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caBuffer1, sizeof(caBuffer1), iResult, __LINE__ );

		return	-1;
	}

	// 17. ���Ǳ� ���Ͽ��� ���� �α������� �����̸� ����
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"ERROR_LOG_FILE_NAME", caBuffer2, sizeof(caBuffer2) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON ERROR_LOG_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caBuffer2, sizeof(caBuffer2), iResult, __LINE__ );

		return	-1;
	}

	// 18. ���Ǳ׿��� �о���� �����ͷ� �����α� �ΰ� �ʱ�ȭ �� ���(�̶����� �����α� ���� ����)
	if( ( ERR_LOG = CNGetLogHandle( iLogType, iLogLevel, caBuffer1, caBuffer2, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" ) ) == NULL )
	{
		fprintf( stderr, "[CNGetLogHandle( %d %d %s %s %s ) Error][L:%d]\n", 
			iLogType, iLogLevel, caBuffer1, caBuffer2, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A", __LINE__ );

		return	-1;
	}

	// 19. ����!!
	return	0;
}


int	EmergencyTableInit( char* _cpConfigFilePath )
{
	int		iResult;
	int		iSharedID;
	int		iEmergencyKey;

	char	caConfigFile[512];

	// 1. �о���� ���Ǳ� ����(���ϰ��+���ϸ�) ����
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, LOG_MANAGER_CONFIG_FILE_NAME );

	// 2. Emergency ��� Flag ���� Shared Memory Key���� ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"EMERGENCY_MODE_SHM_KEY", &iEmergencyKey ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s COMMON THREAD_LOG_FILE_TYPE %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iEmergencyKey, iResult, __LINE__ );

		return	NULL;
	}

	CNLOG( NOR_LOG, 3, "[Emergency Mode Shared Memory Key: 0x%x][L:%d]\n", iEmergencyKey, __LINE__ );

	// 3. Shared Memory Key������ Shared Memory �� SharedID ���� ����
	if( ( iSharedID = shmget( iEmergencyKey, 0, 0 ) ) < 0 )
	{
		CNLOG( NOR_LOG, 3, "[Emergency Mode Shared Memory Not Exist][R:%d][E:%d][L:%d]\n", iSharedID, errno, __LINE__ );

		// 3.1 SharedID ���ÿ� ������ ���(Shared Memory ������ �Ҵ�Ǿ����� �������) Shared Memory ���� ����
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

		// 3.3 Shared Memory�� Emergency Flag�� ó�� ������ ��� Emergency ���� 0(��)���� ����
		*g_ipEmergencyMode	= 0;
	}
	else
	{
		CNLOG( ERR_LOG, 1, "[Emergency Mode Shared Memory Exist][L:%d]\n", __LINE__ );

		// 3.4 SharedID ���ÿ� ������ ��� Shared Memory�� Attach �õ�
		if( ( g_ipEmergencyMode = (int*)shmat( iSharedID, (char*)0, 0 ) ) == NULL )
		{
			CNLOG( ERR_LOG, 1, "[shmat( %d 0 0 ) Error][E:%d][L:%d]\n", 
				iSharedID, errno, __LINE__ );

			return	-1;
		}
	}

	CNLOG( NOR_LOG, 3, "[Emergency Mode : %02d][L:%d]\n", *g_ipEmergencyMode, __LINE__ );

	// 4. ����!!
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

	// 1. LogQueue �� LogQueue Thread ����ü �ɹ� �ʱ�ȭ
	g_stLogThread.iUseNum	= 0;
	g_stLogThread.Info		= NULL;

	// 2. LogQueue�� LogQueue Thread�� ���� �����͸� �о���� Config �����̸�(������+�����̸�) ����
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, LOG_QUEUE_INFO_FILE_NAME );

	// 3. ���Ǳ� ���Ͽ��� Queue�� ������ Queue�� ����ϴ� Thread�� ����(LOG_QUEUE_NUM)�� �о ����(Queue�� Thread�� 1:1 ��Ī)
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"LOG_QUEUE_NUM", &iLogQueueNum ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s COMMON LOG_QUEUE_NUM %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iLogQueueNum, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[LOG_THREAD NUM: %d][L:%d]\n", iLogQueueNum, __LINE__ );

	// 4. ���Ǳ� ���Ͽ��� LogQueue �� �����Ҽ� �ִ� �ִ� �޽������� �о ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"MAX_QUEUE_COUNT", &g_iMaxLogQueueCount ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s COMMON MAX_QUEUE_COUNT %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &g_iMaxLogQueueCount, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[MAX_LOG_QUEUE_COUNT: %d][L:%d]\n", g_iMaxLogQueueCount, __LINE__ );

	// 5. LogQueue Thread �� ó������ �����Ͱ� ������ �� ��� �����͸� ����� ������ ���(������)
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"ERROR_FILE_PATH", caErrorLogFilePath, sizeof(caErrorLogFilePath) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s COMMON ERROR_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caErrorLogFilePath, sizeof(caErrorLogFilePath), iResult, __LINE__ );

		return	-1;
	}

	g_cpErrorLogThreadFilePath = (char*)malloc( strlen( caErrorLogFilePath ) + 1 );
	strlcpy( g_cpErrorLogThreadFilePath, caErrorLogFilePath, strlen( caErrorLogFilePath ) + 1 );

	CNLOG( NOR_LOG, 3, "[ERROR_LOG_FILE_PATH: %s][L:%d]\n", g_cpErrorLogThreadFilePath, __LINE__ );

	// 6. LogQueue Thread �� ó������ �����Ͱ� ������ �� ��� �����͸� ����� ������ �̸�
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"ERROR_FILE_NAME", caErrorLogFileName, sizeof(caErrorLogFileName) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s COMMON ERROR_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caErrorLogFileName, sizeof(caErrorLogFileName), iResult, __LINE__ );

		return	-1;
	}

	g_cpErrorLogThreadFileName = (char*)malloc( strlen( caErrorLogFileName ) + 1 );
	strlcpy( g_cpErrorLogThreadFileName, caErrorLogFileName, strlen( caErrorLogFileName ) + 1 );

	CNLOG( NOR_LOG, 3, "[ERROR_LOG_FILE_NAME: %s][L:%d]\n", g_cpErrorLogThreadFileName, __LINE__ );

	// 7. Queue ����(Queue�� ���� = Thread�� ����)��ŭ �޸𸮸� �Ҵ�(������ Queue �� Thread ������ ������ ����)
	g_stLogThread.iUseNum	= iLogQueueNum;
	g_stLogThread.Info		= (stLogThreadInfo*)malloc( sizeof(stLogThreadInfo) * g_stLogThread.iUseNum );

	memset( g_stLogThread.Info, 0x00, sizeof(stLogThreadInfo) * g_stLogThread.iUseNum );

	// 8. ���Ǳ� ���Ϸκ��� Queue �� Thread�� ���� ������ �ʱ�ȭ
	for( iForLoop = 0; iForLoop < iLogQueueNum; iForLoop++ )
	{
		sprintf( caPartBuffer, "QUQUE_%d", iForLoop + 1 );

		// 8.1 Queue Key ���� �о���δ�
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

	// 9. ����!!
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

	// 1. SmdaQueue �� SmdaQueue Thread ����ü �ɹ� �ʱ�ȭ
	g_stSmdaThread.iSmdaServerNum	= 0;
	g_stSmdaThread.Info		= NULL;

	// 2. �о���� Config ����(SmdaQueue Config File)�̸� ����
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, SMDA_QUEUE_INFO_FILE_NAME );

	// 3. ���Ǳ� ���Ͽ��� Queue�� ����(Queue ���� = Thread�� ����)�� �о ����	
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

	// 4. ���Ǳ� ���Ͽ��� Log Queue�� ���� �� �ִ� �޽����� ������ �о ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"MAX_QUEUE_COUNT", &g_iMaxSmdaQueueCount ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON MAX_QUEUE_COUNT %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &g_iMaxSmdaQueueCount, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[SMDA][MAX_QUEUE_COUNT: %d][L:%d]\n", g_iMaxSmdaQueueCount, __LINE__ );

	// 5. ���Ǳ� ���Ͽ��� Guts ����(������ ���� �� ����۽� ��ó�� �����͸� �����ϴ� ����)�� ��� ����(������)
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

	// 6. ���Ǳ� ���Ͽ��� Guts ����(������ ���� �� ����۽� ��ó�� �����͸� �����ϴ� ����)�� �����̸� ����
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"GUTS_FILE_NAME", caGutsSmdaFileName, sizeof(caGutsSmdaFileName) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON GUTS_FILE_NAME %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caGutsSmdaFileName, sizeof(caGutsSmdaFileName), iResult, __LINE__ );

		return	-1;
	}

	g_cpGutsSmdaFileName = (char*)malloc( strlen( caGutsSmdaFileName ) + 1 );
	strlcpy( g_cpGutsSmdaFileName, caGutsSmdaFileName, strlen( caGutsSmdaFileName ) + 1 );

	CNLOG( NOR_LOG, 3, "[SMDA][GUTS_FILE_NAME: %s][L:%d]\n", caGutsSmdaFileName, __LINE__ );

	// 7. SmdaQueue ������ ��ó�� ����� ������� ����(0:�̻�� 0�� �ƴѼ�:���)
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"RETRY_USE_FLAG", &g_iSmdaRetryUseFlag ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON RETRY_USE_FLAG %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &g_iSmdaRetryUseFlag, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[SMDA][RETRY_USE_FLAG: %d][L:%d]\n", g_iSmdaRetryUseFlag, __LINE__ );

	// 8. SmdaQueue ������ ��ó�� ����� ��� �� ��� ���� ������ ����
	if( g_iSmdaRetryUseFlag != 0 )
	{
		// 8.1 ��ó�� �����͸� ó���ϴ� �ֱ�(�ʴ���) ����
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

		// 8.2 ��ó���� ���� �� �������� �ð�(������ ó���ð��� RETRY_TIME �̻��� �����͸� ��ó��)
		if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"RETRY_TIME", &g_iSmdaRetryTime ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON RETRY_TIME %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, &g_iSmdaRetryTime, iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[SMDA][RETRY_TIME: %d][L:%d]\n", g_iSmdaRetryTime, __LINE__ );

		// 8.3 ��ó���� ���� �� �������� ��ó�� ȸ�� ����
		if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"RETRY_COUNT", &g_iSmdaRetryCount ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON RETRY_COUNT %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, &g_iSmdaRetryCount, iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[SMDA][RETRY_COUNT: %d][L:%d]\n", g_iSmdaRetryCount, __LINE__ );
	}

	// 9. SmdaQueue ������ ó������ �̻�(��ó�� ����, ���˿��� ���)�� ���� �����͸� ���� �� ������ ���(������)
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"ERROR_FILE_PATH", caErrorSmdaFilePath, sizeof(caErrorSmdaFilePath) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[SMDA][CNGetConfigInt( %s COMMON ERROR_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, caErrorSmdaFilePath, sizeof(caErrorSmdaFilePath), iResult, __LINE__ );

		return	-1;
	}

	g_cpErrorSmdaThreadFilePath	= (char*)malloc( strlen( caErrorSmdaFilePath ) + 1 );
	strlcpy( g_cpErrorSmdaThreadFilePath, caErrorSmdaFilePath, strlen( caErrorSmdaFilePath ) + 1 );

	CNLOG( NOR_LOG, 3, "[SMDA][ERROR_FILE_PATH: %s][L:%d]\n", g_cpErrorSmdaThreadFilePath, __LINE__ );

	// 10. SmdaQueue ������ ó������ �̻�(��ó�� ����, ���˿��� ���)�� ���� �����͸� ���� �� ������ �̸�
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

	// 11. ���Ǳ� ���Ͽ��� �����͸� �о���̰� Smda Queue �� Smda Queue Thread ������ �ʱ�ȭ
	for( iSmdaLoop = 0; iSmdaLoop < g_stSmdaThread.iSmdaServerNum; iSmdaLoop++ )
	{
		sprintf( caPartBuffer1, "SMDA_%d", iSmdaLoop + 1 );

		// 11.1 Smda Session ���� ���� �о���δ�
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

		// 11.2 Smda ������ IP �ּҸ� �о���δ�
		memset( caSmdaAdress, 0x00, sizeof( caSmdaAdress ) );

		if( ( iResult = CNGetConfigStr( caConfigFile, caPartBuffer1, (char*)"SMDA_IP", caSmdaAdress, sizeof(caSmdaAdress) ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[%s][CNGetConfigInt( %s %s SMDA_IP %d %d ) Error][R:%d][L:%d]\n", 
				caPartBuffer1, caConfigFile, caPartBuffer1, caSmdaAdress, sizeof(caSmdaAdress), iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[%s][SMDA_IP: %s][L:%d]\n", caPartBuffer1, caSmdaAdress, __LINE__ );

		// 11.3 Smda ������ Port�� �о���δ�
		if( ( iResult = CNGetConfigInt( caConfigFile, caPartBuffer1, (char*)"SMDA_PORT", &iSmdaPort ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[%s][CNGetConfigInt( %s %s SMDA_PORT %d ) Error][R:%d][L:%d]\n", 
				caPartBuffer1, caConfigFile, caPartBuffer1, &iSmdaPort, iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[%s][SMDA_PORT: %d][L:%d]\n", caPartBuffer1, iSmdaPort, __LINE__ );

		// 11.4 Smda ������ BIND ID �� �о���δ�
		memset( caSmdaBindID, 0x00, sizeof( caSmdaBindID ) );

		if( ( iResult = CNGetConfigStr( caConfigFile, caPartBuffer1, (char*)"SMDA_BIND_ID", caSmdaBindID, sizeof(caSmdaBindID) ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[%s][CNGetConfigInt( %s %s SMDA_BIND_ID %d %d ) Error][R:%d][L:%d]\n", 
				caPartBuffer1, caConfigFile, caPartBuffer1, caSmdaBindID, sizeof(caSmdaBindID), iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[%s][SMDA_BIND_ID: %s][L:%d]\n", caPartBuffer1, caSmdaBindID, __LINE__ );

		// 11.5 Smda ������ BIND PASSWORD �� �о���δ�
		memset( caSmdaBindPW, 0x00, sizeof( caSmdaBindPW ) );

		if( ( iResult = CNGetConfigStr( caConfigFile, caPartBuffer1, (char*)"SMDA_BIND_PW", caSmdaBindPW, sizeof(caSmdaBindPW) ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[%s][CNGetConfigInt( %s %s SMDA_BIND_PW %d %d ) Error][R:%d][L:%d]\n", 
				caPartBuffer1, caConfigFile, caPartBuffer1, caSmdaBindPW, sizeof(caSmdaBindPW), iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[%s][SMDA_BIND_PW: %s][L:%d]\n", caPartBuffer1, caSmdaBindPW, __LINE__ );

		// 11.6 Smda Queue Thread ����ü�� �����͸� ����
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

		// 11.7 Smda Queue Thread ����ü�� Session �����͸� ����
		for( iSessionLoop = 0; iSessionLoop < g_stSmdaThread.Info[iSmdaLoop].iSmdaSessionNum; iSessionLoop++ )
		{
			sprintf( caPartBuffer2, "SMDA_QUEUE_KEY_%d", iSessionLoop + 1 );

			// 11.7.1 Smda Queue Key ���� �о���δ�
			if( ( iResult = CNGetConfigInt( caConfigFile, caPartBuffer1, caPartBuffer2, &iSmdaQueueKey ) ) < 0 )
			{
				CNLOG( ERR_LOG, 1, "[%s][CNGetConfigInt( %s %s %s %d ) Error][R:%d][L:%d]\n", 
					caPartBuffer1, caConfigFile, caPartBuffer1, caPartBuffer2, &iSmdaQueueKey, iResult, __LINE__ );

				return	-1;
			}

			CNLOG( NOR_LOG, 3, "[%s][%s: 0x%x][L:%d]\n", caPartBuffer1, caPartBuffer2, iSmdaQueueKey, __LINE__ );

			// 11.7.2 Queue ���� �� Attach
			if( ( iSmdaQueueID = MakeAndOpenQueue( iSmdaQueueKey, g_iMaxSmdaQueueCount * sizeof( stLogPacket ) ) ) < 0 )
			{
				CNLOG( ERR_LOG, 1, "[%s][MakeAndOpenQueue( 0x%x %d ) Error][R:%d][E:%d][L:%d]\n", 
					caPartBuffer1, iSmdaQueueKey, g_iMaxSmdaQueueCount * sizeof( stLogPacket ), iSmdaQueueID, errno, __LINE__ );
			}

			CNLOG( NOR_LOG, 3, "[%s][SMDA_QUEUE_ID_%d: %d][L:%d]\n", caPartBuffer1, iSessionLoop + 1, iSmdaQueueID, __LINE__ );

			// 11.7.3 �о���� Queue ������ Smda Thread ����ü�� ����
			pthread_mutex_init( &(g_stSmdaThread.Info[iSmdaLoop].stpQueue[iSessionLoop].Lock), NULL );
			pthread_mutex_lock( &(g_stSmdaThread.Info[iSmdaLoop].stpQueue[iSessionLoop].Lock) );
			g_stSmdaThread.Info[iSmdaLoop].stpQueue[iSessionLoop].iQueueKey	= iSmdaQueueKey;
			g_stSmdaThread.Info[iSmdaLoop].stpQueue[iSessionLoop].iQueueID		= iSmdaQueueID;
			pthread_mutex_unlock( &(g_stSmdaThread.Info[iSmdaLoop].stpQueue[iSessionLoop].Lock) );
		}
	}

	// 12. ����!!
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

	// 1. SmdaRouting �����͸� �о���� Config ������ ���ϸ� ����
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, SMDA_ROUTING_INFO_FILE_NAME );

	// 2. ����ϴ� Routing Prefix ���� ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"SMDA_ROUTING", (char*)"SMDA_ROUTING_NUM", &iSmdaRoutingNum ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING SMDA_ROUTING_NUM %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iSmdaRoutingNum, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[SMDA_ROUTING_NUM: %d][L:%d]\n", iSmdaRoutingNum, __LINE__ );

	// 3. ETC Routing Queue Number(��ϵ��� ���� �뿪�� �����Ͱ� ���� ��� �����͸� ���� �� Queue�� Number) ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"SMDA_ROUTING", (char*)"SMDA_ETC_ROUTING", &g_iEtcRoutingQueue ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING SMDA_ETC_ROUTING %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &g_iEtcRoutingQueue, iResult, __LINE__ );

		return	-1;
	}

	CNLOG( NOR_LOG, 3, "[SMDA_ETC_ROUTING: %d][L:%d]\n", g_iEtcRoutingQueue, __LINE__ );

	g_iEtcRoutingQueue	= g_iEtcRoutingQueue - 1;

	// 4. �ӽ÷� Prefix �����͸� ���� �� ���� �Ҵ�(������ �������� ������ ��������)
	ipRoutingTable = (int*)malloc( sizeof(int) * 9999999 );

	for( iForLoop = 0; iForLoop < 9999999; iForLoop++ )
		ipRoutingTable[iForLoop]	= g_iEtcRoutingQueue;

	// 5. ���Ǳ� ���Ͽ��� Prefix ������ �о �ӽ� ��������� ���
	for( iForLoop = 1; iForLoop < iSmdaRoutingNum + 1;iForLoop++ )
	{
		// 5.1 ���� ����� ��ȣ �뿪 ����(������� 5�ڸ��� �����)
		sprintf( caPartBuffer, "SMDA_ROUTING_STR_%d", iForLoop );

		if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"SMDA_ROUTING", caPartBuffer, caStartPrefix, sizeof(caStartPrefix) ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING %s %d %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer, caStartPrefix, sizeof(caStartPrefix), iResult, __LINE__ );

			return	-1;
		}

		iStartPrefix = atoi( caStartPrefix );

		sprintf( caPartBuffer, "SMDA_ROUTING_END_%d", iForLoop );

		// 5.2 �� ����� ��ȣ �뿪 ����(������� 5�ڸ��� �����)
		if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"SMDA_ROUTING", caPartBuffer, caEndPrefix, sizeof(caEndPrefix) ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING %s %d %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer, caEndPrefix, sizeof(caEndPrefix), iResult, __LINE__ );

			return	-1;
		}

		iEndPrefix = atoi( caEndPrefix );

		sprintf( caPartBuffer, "SMDA_ROUTING_QUEUE_%d", iForLoop );

		// 5.3 ���ۿ��� �� ����� �뿪������ �����͸� ���� �� Queue�� Number(Queue Index)
		if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"SMDA_ROUTING", caPartBuffer, &iRoutingQueue ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[CNGetConfigInt( %s SMDA_QUEUE %s %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer, &iRoutingQueue, iResult, __LINE__ );

			return	-1;
		}

		CNLOG( NOR_LOG, 3, "[PREFIX: %s ~ %s][QUEUE: %02d][L:%d]\n", caStartPrefix, caEndPrefix, iRoutingQueue, __LINE__ );

		// 5.4 �о���� �����ͷ� ����� ���̺�(Smda Routing Table)�� ����
		for ( iPrefixLoop = iStartPrefix; iPrefixLoop < iEndPrefix + 1; iPrefixLoop++ )
			ipRoutingTable[iPrefixLoop] = iRoutingQueue - 1;
	}

	// 6. Smda Routing ������ ������ �������(���ʱ⵿�� ���) �����Ҵ�
	if( g_stSmdaRouting.Index == NULL )
		g_stSmdaRouting.Index = (int*)malloc( sizeof(int) * 99999 );
	
	// 7. �ӽð����� ����� Routing ������ ����
	memcpy( g_stSmdaRouting.Index, ipRoutingTable, sizeof(int) * 99999 );

	// 8. �ӽð��� ����
	free( ipRoutingTable );

	// 9. ����!!
	return	0;
}


void LogQueueDataFree()
{
	g_stLogThread.iUseNum	= 0;

	// 1. LOG Thread ������ �����ϴ� �޸� ���� ����
	if( g_stLogThread.Info != NULL )
		free( g_stLogThread.Info );

	g_stLogThread.Info	= NULL;

	// 2. LOG Thread Error �����̸� �����ϴ� �޸� ���� ����
	if( g_cpErrorLogThreadFileName != NULL )
		free( g_cpErrorLogThreadFileName );

	g_cpErrorLogThreadFileName	= NULL;

	// 3. LOG Thread Error ���ϰ�� �����ϴ� �޸� ���� ����
	if( g_cpErrorLogThreadFilePath != NULL )
		free( g_cpErrorLogThreadFilePath );

	g_cpErrorLogThreadFilePath	= NULL;

	// 4. ����!!
	return;

}


void SmdaQueueDataFree()
{
	int	iForLoop;

	// 1. SMDA Thread ������ �����ϴ� �޸� ������ �����ϴ� ���
	if( g_stSmdaThread.Info != NULL )
	{
		// 1.1 SMDA Thread ������ SMDA���� ���� ��ŭ �����ȴ�(���ڸ�ŭ ����)
		for( iForLoop = 0; iForLoop < g_stSmdaThread.iSmdaServerNum; iForLoop++ )
		{
			// 1.1.1 SMDA �������� �ȿ��� Session ���ڸ�ŭ Session ����
			//       (Queue)�� �����ȴ�. (Session ���ڸ�ŭ ����)
			if( g_stSmdaThread.Info[iForLoop].stpQueue != NULL )
				free( g_stSmdaThread.Info[iForLoop].stpQueue );
		}

		// 1.2 Session(Queue)������ ��� ������ �Ŀ�
		//     SMDA �������� �޸𸮸� ����
		free( g_stSmdaThread.Info );

		// 1.3 SMDA �������� ���� ���� �ּ� �ʱ�ȭ
		g_stSmdaThread.Info	= NULL;

		// 1.4 SMDA �������� �ʱ�ȭ
		g_stSmdaThread.iSmdaServerNum	= 0;
	}

	// 2. SMDA Thread �� Error �����̸��� �����ϴ� �޸� ������ ����
	if( g_cpErrorSmdaThreadFileName != NULL )
		free( g_cpErrorSmdaThreadFileName );

	g_cpErrorSmdaThreadFileName	= NULL;

	// 3. SMDA Thread �� Error ���ϰ�θ� �����ϴ� �޸� ������ ����
	if( g_cpErrorSmdaThreadFilePath != NULL )
		free( g_cpErrorSmdaThreadFilePath );

	g_cpErrorSmdaThreadFilePath	= NULL;

	// 4. ����!!
	return;
}


void SmdaRoutingDataFree()
{
	// 1. ����� ����(Config���� �о���� ����� �뿪�� ����) �ʱ�ȭ
	g_stSmdaRouting.iUseNum	= 0;

	// 2. ����� ���̺��� �����ϴ� ��� �޸� ���� ����
	if( g_stSmdaRouting.Index != NULL )
		free( g_stSmdaRouting.Index );

	// 3. ����� ���̺� �ּ� �ʱ�ȭ
	g_stSmdaRouting.Index	= NULL;

	// 4. ����!!
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

	// 1. Sequence������ �̸� ����(������/�����̸�_�ð�(YYYYMMDD)_�ֺμ���.guts)
	//    Sequence�� ������� �߱��� Serial Number�μ� ��⵿�� Serial�� �̾
	//    �߱��ϱ� ���ؼ� �����Ѵ�
	sprintf( caReadFile, "%s/%s_%-.*s_%s.guts", 
		g_cpSequenceFilePath, g_cpSequenceFileName, 8, GetNowTimeStr(caTimeBuffer), g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	// 2. Sequence ���� ���� ����ó��
	if( ( fpReadFile = fopen( caReadFile, "r" ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[fopen( %s, r ) Error][���ʱ⵿][E:%d][L:%d]\n", caReadFile, errno, __LINE__ );

		return	0;
	}

	// 3. ���Ͽ��� Sequence �����͸� �о���δ�
	memset( caBuffer, 0x00, sizeof(caBuffer) );
	fgets( caBuffer, sizeof(caBuffer), fpReadFile );

	fclose( fpReadFile );

	// 4. �о���� Sequence �����͸� ���������� ����
	#ifdef OS508
	pthread_mutex_lock( &g_iMsgSerialLock );
	g_iMsgSerial	= atoi(caBuffer);
	pthread_mutex_unlock( &g_iMsgSerialLock );	
	#else
	atomic_swap_uint( &g_iMsgSerial, atoi(caBuffer) );
	#endif

	CNLOG( NOR_LOG, 1, "[Read Sequence: %d][Set Sequence: %d][L:%d]\n", atoi(caBuffer), g_iMsgSerial, __LINE__ );

	// 5. ���������� �����͸� �о���̸� ���� ������ ����
	sprintf( caDeleteCommand, "\\rm -f %s", caReadFile );
	system( caDeleteCommand );

	// 6. ����!!
	return	0;
}


int WriteSequenceFile()
{
	int		iResult;

	char	caTimeBuffer[15];
	char	caWriteFile[512];

	FILE	*fpWriteFile;

	// 1. Sequence������ �̸� ����(������/�����̸�_�ð�(YYYYMMDD)_�ֺμ���.guts)
	//    Sequence�� ������� �߱��� Serial Number�μ� ��⵿�� Serial�� �̾
	//    �߱��ϱ� ���ؼ� �����Ѵ�
	sprintf( caWriteFile, "%s/%s_%-.*s_%s.guts", 
		g_cpSequenceFilePath, g_cpSequenceFileName, 8, GetNowTimeStr(caTimeBuffer), g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	// 2. Sqeuence ���� ���� ����ó��
	if( ( fpWriteFile = fopen( caWriteFile, "w+" ) ) == NULL )
	{
		if( ERR_LOG != 0 )
			CNLOG( ERR_LOG, 1, "[fopen( %s, w+ ) Error][Seq:%d][E:%d][L:%d]\n", caWriteFile, g_iMsgSerial, errno, __LINE__ );

		return	-1;
	}

	// 3. ���Ͽ� Sequence ������ ���
	fprintf( fpWriteFile, "%d", g_iMsgSerial );

	fclose( fpWriteFile );

	// 4. ������ Ȯ���� ���� �α� ����
	if( NOR_LOG != 0 )
		CNLOG( NOR_LOG, 1, "[Write Sequence: %d][L:%d]\n", g_iMsgSerial, __LINE__ );	

	// 5. ����!!
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

	// 1. ������ �÷��� ����(1�̸� Run, 0�̸� ����)
	//    ������ ����� 0���� �����ϸ� ��� SMDA �����尡
	//    ���� �۾��� �����Ѵ�
	#ifdef OS508
	pthread_mutex_lock( &g_iSmdaThreadDoneLock );
	g_iSmdaThreadDone	= 1;
	pthread_mutex_unlock( &g_iSmdaThreadDoneLock );	
	#else
	atomic_swap_uint( &g_iSmdaThreadDone, 1 );
	#endif

	// 2. SMDA Thread ���� ����(SMDA Thread ���� : SMDA ������ * �� SMDA ���Ǽ�)
	for( iSmdaLoop = 0; iSmdaLoop < g_stSmdaThread.iSmdaServerNum; iSmdaLoop++ )
	{
		// 2.1 �� SMDA ������ Session ���ڸ�ŭ ������ ����
		for( iSessionLoop = 0; iSessionLoop < g_stSmdaThread.Info[iSmdaLoop].iSmdaSessionNum; iSessionLoop++ )
		{
			// 2.1.1 Thread ���� �Լ��� ���ڷ� �Ѱ��� Thread ���� ����ü ���� ����(Lib �ҽ� �����Ұ�,CNThread.c)
			stpThreadInfo = (stThread*)malloc( sizeof(stThread) );

			// 2.1.2 ���� ���н� ����!!
			if( stpThreadInfo == NULL )
			{
				CNLOG( ERR_LOG, 1, "[malloc( %d ) Error][E:%d][L:%d]\n", sizeof(stThread), errno, __LINE__ );

				return	-1;
			}

			// 2.1.3 Thread ���� ����Ǵ� Run�Լ��鿡�� ���ڷ� �Ѱ��� Argument ������ ���� ����
			stpThreadArg = (stSmdaThreadArgument*)malloc( sizeof( stSmdaThreadArgument ) );

			// 2.1.4 ���� ���н� ����!!
			if( stpThreadArg == NULL )
			{
				CNLOG( ERR_LOG, 1, "[malloc( %d ) Error][E:%d][L:%d]\n", sizeof( stSmdaThreadArgument ), errno, __LINE__ );

				free( stpThreadInfo );

				return	-1;
			}

			// 2.1.5 ������ �޸� ���� �ʱ�ȭ
			memset( stpThreadInfo, 0x00, sizeof( stThread ) );
			memset( stpThreadArg, 0x00, sizeof( stSmdaThreadArgument ) );

			// 2.1.6 �� �ʵ��� �����͸� ä���ִ´�
			//       Run�Լ���(OnThreadStart,OnThreadRun,OnThreadTerminate)���� 
			//       ���޵Ǵ� �����͵��� stpThreadArg �̸�
			//       Thread ���̺귯���� ���޵Ǵ� �����ʹ� stpThreadInfo �̴�
			//       stpThreadInfo�� �ɹ������� stpThreadArg�� �����͸� �����Ѵ�
			//       �α� ������ �α� ���̺귯������ �Լ��� ��������� �� 
			//       Run�Լ� �鿡�� stpThreadArg �� �����Ѵ�

			// 2.1.7 �ش� �����尡 ���° SMDA������ ���° ������ ����ϴ��� ����
			stpThreadArg->iSmdaIndex			= iSmdaLoop;
			stpThreadArg->iSessionIndex			= iSessionLoop;

			// 2.1.8 ������ ���� �Լ��� ����ϴ� ������ ����
			//       vpArgs �� Run �Լ���(OnThreadStart, OnThreadRun, OnThreadTerminate)
			//       ���� �Ѱ��� Argument�̴�
			//       OnThreadStart, OnThreadRun, OnThreadTerminate��
			//       ������ ������ vpArgs�� �Ѱܹ޾� ���ʴ�� ����Ǵ�
			//       �Լ����� �Լ� �������̴�
			stpThreadInfo->vpArgs				= (void*)stpThreadArg;
			stpThreadInfo->OnThreadStart		= OnSmdaThreadStart;
			stpThreadInfo->OnThreadRun			= OnSmdaThreadRun;
			stpThreadInfo->OnThreadTerminate	= OnSmdaThreadTerminate;

			// 2.1.9 �ΰ� �ڵ�(������ ������ �� �����尡 ����)
			stpThreadArg->NLOG	= NULL;
			stpThreadArg->ELOG	= NULL;

			// 2.1.10 ��������� �о���� ���Ǳ� ���ϵ��� ��� ���� ���� �Ҵ� �� ����
			stpThreadArg->cpConfigPath	= (char*)malloc( strlen( _cpConfigFilePath ) + 1 );
			memset( stpThreadArg->cpConfigPath, 0x00, strlen( _cpConfigFilePath ) + 1 );
			strlcpy( stpThreadArg->cpConfigPath, _cpConfigFilePath, strlen( _cpConfigFilePath ) + 1 );

			// 2.1.11 ������ ���ڸ� �Ѱ� �ø���
			#ifdef OS508
			pthread_mutex_lock( &g_iSmdaThreadCountLock );
			g_iSmdaThreadCount++;
			pthread_mutex_unlock( &g_iSmdaThreadCountLock );
			#else
			atomic_inc_uint_nv( &g_iSmdaThreadCount );
			#endif

			// 2.1.12 ������ ����
			if( ( iResult = CreateThread( stpThreadInfo ) ) < 0 )
			{
				CNLOG( ERR_LOG, 1, "[CreateThread( %d ) Error][R:%d][E:%d][L:%d]\n", stpThreadInfo, iResult, errno, __LINE__ );

				// 2.1.12.1 ������ ���� ���н� �����忡 �Ѱ��ֱ� ���� �Ҵ� �ߴ� �޸� ���� ����
				free( stpThreadInfo );
				free( stpThreadArg );

				// 2.1.12.2 ������ ���� ���н� �������״� ������ ī��Ʈ�� �ٽ� ���ҽ�Ų��
				#ifdef OS508
				pthread_mutex_lock( &g_iSmdaThreadCountLock );
				g_iSmdaThreadCount--;
				pthread_mutex_unlock( &g_iSmdaThreadCountLock );
				#else
				atomic_dec_uint_nv( &g_iSmdaThreadCount );
				#endif

				// 2.1.12.3 ����!!
				return	-1;
			}
		}
	}

	// 3. ����!!
	return	0;
}


int MakeLogThread( char* _cpConfigFilePath )
{
	int	iResult;
	int	iForLoop;
	int	iThreadCount;

	stThread			*stpThreadInfo;
	stLogThreadArgument	*stpThreadArg;

	// 1. ������ �÷��� ����(1�̸� Run, 0�̸� ����)
	//    ������ ����� 0���� �����ϸ� ��� LOG �����尡
	//    ���� �۾��� �����Ѵ�	
	#ifdef OS508
	pthread_mutex_lock( &g_iLogThreadDoneLock );
	g_iLogThreadDone	= 1;
	pthread_mutex_unlock( &g_iLogThreadDoneLock );
	#else
	atomic_swap_uint( &g_iLogThreadDone, 1 );
	#endif

	// 2. LOG Thread ���� ����(LOG Thread ���� : Log Queue ����)
	for( iForLoop = 0; iForLoop < g_stLogThread.iUseNum; iForLoop++ )
	{
		// 2.1.1 Thread ���� �Լ��� ���ڷ� �Ѱ��� Thread ���� ����ü ���� ����(Lib �ҽ� �����Ұ�,CNThread.c)
		stpThreadInfo = (stThread*)malloc( sizeof(stThread) );

		// 2.1.2 ���� ���н� ����!!
		if( stpThreadInfo == NULL )
		{
			CNLOG( ERR_LOG, 1, "[malloc( %d ) Error][E:%d][L:%d]\n", sizeof(stThread), errno, __LINE__ );

			return	-1;
		}

		// 2.1.3 Thread ���� ����Ǵ� Run�Լ��鿡�� ���ڷ� �Ѱ��� Argument ������ ���� ����
		stpThreadArg = (stLogThreadArgument*)malloc( sizeof(stLogThreadArgument) );

		// 2.1.4 ���� ���н� ����!!
		if( stpThreadArg == NULL )
		{
			CNLOG( ERR_LOG, 1, "[malloc( %d ) Error][E:%d][L:%d]\n", sizeof(stLogThreadArgument), errno, __LINE__ );

			free( stpThreadInfo );

			return	-1;
		}

		// 2.1.5 ������ �޸� ���� �ʱ�ȭ
		memset( stpThreadInfo, 0x00, sizeof(stThread) );
		memset( stpThreadArg, 0x00, sizeof(stLogThreadArgument) );

		// 2.1.6 �� �ʵ��� �����͸� ä���ִ´�
		//       Run�Լ���(OnThreadStart,OnThreadRun,OnThreadTerminate)���� 
		//       ���޵Ǵ� �����͵��� stpThreadArg �̸�
		//       Thread ���̺귯���� ���޵Ǵ� �����ʹ� stpThreadInfo �̴�
		//       stpThreadInfo�� �ɹ������� stpThreadArg�� �����͸� �����Ѵ�
		//       �α� ������ �α� ���̺귯������ �Լ��� ��������� �� 
		//       Run�Լ� �鿡�� stpThreadArg �� �����Ѵ�

		// 2.1.7 �ش� �����尡 ���° Queue�� ����ϴ��� ����
		stpThreadArg->iThreadIndex			= iForLoop;

		// 2.1.8 ������ ���� �Լ��� ����ϴ� ������ ����
		//       vpArgs �� Run �Լ���(OnThreadStart, OnThreadRun, OnThreadTerminate)
		//       ���� �Ѱ��� Argument�̴�
		//       OnThreadStart, OnThreadRun, OnThreadTerminate��
		//       ������ ������ vpArgs�� �Ѱܹ޾� ���ʴ�� ����Ǵ�
		//       �Լ����� �Լ� �������̴�
		stpThreadInfo->vpArgs				= (void*)stpThreadArg;
		stpThreadInfo->OnThreadStart		= OnLogThreadStart;
		stpThreadInfo->OnThreadRun			= OnLogThreadRun;
		stpThreadInfo->OnThreadTerminate	= OnLogThreadTerminate;

		// 2.1.9 �ΰ� �ڵ�(������ ������ �� �����尡 ����)
		stpThreadArg->NLOG	= NULL;
		stpThreadArg->ELOG	= NULL;

		// 2.1.10 ��������� �о���� ���Ǳ� ���ϵ��� ��� ���� ���� �Ҵ� �� ����
		stpThreadArg->cpConfigPath	= (char*)malloc( strlen( _cpConfigFilePath ) + 1 );
		memset( stpThreadArg->cpConfigPath, 0x00, strlen( _cpConfigFilePath ) + 1 );
		strlcpy( stpThreadArg->cpConfigPath, _cpConfigFilePath, strlen( _cpConfigFilePath ) + 1 );

		// 2.1.11 ������ ���ڸ� �Ѱ� �ø���
		#ifdef OS508
		pthread_mutex_lock( &g_iLogThreadCountLock );
		g_iLogThreadCount++;
		pthread_mutex_unlock( &g_iLogThreadCountLock );
		#else
		atomic_inc_uint_nv( &g_iLogThreadCount );
		#endif

		// 2.1.12 ������ ����
		if( ( iResult = CreateThread( stpThreadInfo ) ) < 0 )
		{
			CNLOG( ERR_LOG, 1, "[CreateThread( %d ) Error][R:%d][E:%d][L:%d]\n", stpThreadInfo, iResult, errno, __LINE__ );

			// 2.1.12.1 ������ ���� ���н� �����忡 �Ѱ��ֱ� ���� �Ҵ� �ߴ� �޸� ���� ����
			free( stpThreadInfo );
			free( stpThreadArg );

			// 2.1.12.2 ������ ���� ���н� �������״� ������ ī��Ʈ�� �ٽ� ���ҽ�Ų��
			#ifdef OS508
			pthread_mutex_lock( &g_iLogThreadCountLock );
			g_iLogThreadCount--;
			pthread_mutex_unlock( &g_iLogThreadCountLock );
			#else
			atomic_dec_uint_nv( &g_iLogThreadCount );
			#endif

			// 2.1.12.3 ����!!
			return	-1;
		}
	}

	// 3. ����!!
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

	// 1. �ƱԸ�Ʈ �ʱ�ȭ, Thread ������ ���������� �޴´�.
	stpThreadInfo	= (stThread*)_vpArg;
	stpThreadArg	= (stLogThreadArgument*)stpThreadInfo->vpArgs;

	iThreadIndex	= stpThreadArg->iThreadIndex;

	// 2. �α� �����Ͱ��� ���� �ʱ�ȭ
	strlcpy( caLogFilePath, stpThreadArg->cpConfigPath, sizeof(caLogFilePath) );
	strlcpy( caLogFileName, LOG_QUEUE_LOG_FILE_NAME, sizeof(caLogFileName) );
	strlcpy( caErrLogFileName, LOG_QUEUE_ERROR_LOG_FILE_NAME, sizeof(caErrLogFileName) );

	// 3. �Ϲ� �α� ������ ���(�̶����� �Ϲݷα� ���� ����)
	if( ( stpThreadArg->NLOG = GetLogThreadLogHandle( iThreadIndex, caLogFilePath, caLogFileName ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[T:%d][GetLogThreadLogHandle( %d %s %s ) Error][E:%d][L:%d]\n", 
			iThreadIndex + 1, iThreadIndex, caLogFilePath, caLogFileName, errno, __LINE__ );

		return	-1;
	}

	// 4. ���� �α� ������ ���(�̶����� �����α� ���� ����)
	if( ( stpThreadArg->ELOG = GetLogThreadLogHandle( iThreadIndex, caLogFilePath, caErrLogFileName ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[T:%d][GetLogThreadLogHandle( %d %s %s ) Error][E:%d][L:%d]\n", 
			iThreadIndex + 1, iThreadIndex, caLogFilePath, caErrLogFileName, errno, __LINE__ );

		return	-1;
	}

	CNLOG( stpThreadArg->NLOG, 3, "[T:%d][LOG Thread Start][L:%d]\n", iThreadIndex + 1, __LINE__ );

	// 5. ��� �ñ׳� ����(Blocking), �ñ׳� ó���� Main Thread�� �����Ѵ�
	if( ( iResult = CNAllSignalBlock( &stSigTemp ) ) < 0 )
	{
		CNLOG( stpThreadArg->ELOG, 1, "[T:%d][CNAllSignalBlock( %d ) Error][R:%d][E:%d][L:%d]\n", 
			iThreadIndex + 1, &stSigTemp, iResult, errno, __LINE__ );

		return	-1;
	}

	// 6. ����!!
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

	// 1. �ƱԸ�Ʈ �ʱ�ȭ(Thread ������ ���������� �޴´�.
	stpThreadInfo	= (stThread*)_vpArg;
	stpThreadArg	= (stLogThreadArgument*)stpThreadInfo->vpArgs;

	iThreadIndex	= stpThreadArg->iThreadIndex;

	iQueueKey		= g_stLogThread.Info[iThreadIndex].iQueueKey;

	iReadPacketSize	= sizeof( stLogPacket ) - sizeof( long );

	NLOG			= stpThreadArg->NLOG;
	ELOG			= stpThreadArg->ELOG;

	CNLOG( NLOG, 3, "[T:%d][LOG Thread Run][L:%d]\n", iThreadIndex + 1, __LINE__ );

	// 2. ��������� �ö����� ������ ó��
	while( GetLogThreadDone() )
	{
		// 2.1 Queue�� Attach�Ǿ����� �������
		if( iQueueID < 0 )
		{
			// 2.1.2 Queue �� ������ ����, ������ Attach
			if( ( iQueueID = MakeAndOpenQueue( iQueueKey, g_iMaxLogQueueCount * sizeof( stLogPacket ) ) ) < 0 )
			{
				CNLOG( ELOG, 1, "[T:%d][MakeAndOpenQueue( 0x%x, %d ) Error][R:%d][E:%d][L:%d]\n", 
					iThreadIndex + 1, iQueueKey, g_iMaxLogQueueCount * sizeof( stLogPacket ), iQueueID, errno, __LINE__ );

				CNNanoSleep( 3000000000 );

				iLogQueueEmpty	= 0;

				continue;
			}
		}

		// 2.2 Qudud�� �����͸� üũ������ 300�� �̻� �����Ͱ� ����־��� ���
		//     CPU ���ϸ� ���̱� ���� ������ �ش�
		if( iLogQueueEmpty >= 300)
		{
			CNNanoSleep( 1000000000 );

			iLogQueueEmpty	= 0;

			continue;
		}

		// 2.3 Queue���� Packet ������ �б⸦ �õ��Ѵ�
		memset( &stTempPacket, 0x00, sizeof( stLogPacket ) );

		switch( CNQueueRead( iQueueID, &stTempPacket, iReadPacketSize, 0 ) )
		{
			// 2.3.1 Queue���� ���������� LogPacket �����͸� �о��� ���
			case CN_IPC_QUEUE_DATA_READ :

				// 2.3.1.1 �о���� Queue�����͸� OnLogMsgRead()�� ó��(���� ����ϰ� Smda Queue�� �����ϴ� ��Ȱ ����)
				if( OnLogMsgRead( iThreadIndex, &stTempPacket, iReadPacketSize, stpThreadArg ) < 0 )
				{
					CNLOG( ELOG, 1, "[T:%d][OnLogMsgRead( %d %d %d %d ) Error][Seq:%d][E:%d][L:%d]\n", 
						iThreadIndex + 1, iThreadIndex, &stTempPacket, iReadPacketSize, stpThreadArg, 
						stTempPacket.stHeader.iSerialNumber, errno, __LINE__ );

					// 2.3.1.1.1 OnLogMsgRead() ���н� OnLogMsgReadError()�� ����ó��(���� ������ ���� ���)
					if( OnLogMsgReadError( &stTempPacket, stpThreadArg ) < 0 )
					{
						CNLOG( ELOG, 1, "[T:%d][OnLogMsgReadError( %d %d ) Error][Seq:%d][E:%d][L:%d]\n", 
							iThreadIndex + 1, &stTempPacket, stpThreadArg, stTempPacket.stHeader.iSerialNumber, errno, __LINE__ );
					}
				}

				// 2.3.1.2 �����͸� �о���ΰ�� Queue������ ������� Count�� 0���� �ʱ�ȭ
				iLogQueueEmpty	= 0;

				break;
		
			// 2.3.2 Queue�� ����������(Packet �����Ͱ� �������)
			case CN_IPC_QUEUE_DATA_EMPTY :

				OnLogMsgEmpty();

				iLogQueueEmpty++;

				break;

			// 2.3.3 Queue�б⵵�� ������ �� ���
			case CN_IPC_QUEUE_DATA_ERROR :

				CNLOG( ELOG, 1, "[T:%d][CNMsgQueueRead( %d %d %d 0 ) Error][R:%d][E:%d][L:%d]\n", 
					iThreadIndex + 1, iQueueID, &stTempPacket, iReadPacketSize, CN_IPC_QUEUE_DATA_ERROR, errno, __LINE__ );

				OnLogMsgError();

				iLogQueueEmpty	= 0;

				iQueueID	= -1;

				break;

			// 2.3.4 Queue�б⵵�� �˼����� ������ �� ���
			default :

				CNLOG( ELOG, 1, "[T:%d][CNMsgQueueRead( %d %d %d 0 ) Error][E:%d][L:%d]\n", 
					iThreadIndex + 1, iQueueID, &stTempPacket, iReadPacketSize, errno, __LINE__ );

				OnLogMsgError();

				iLogQueueEmpty	= 0;

				iQueueID	= -1;

				break;
		}
	}

	// 3. ����!! OnLogThreadTerminate()�� �̾ ����(������� �� ������ ������ ���)
	return	0;
}


int OnLogThreadTerminate( void* _vpArg )
{
	int					iThreadIndex;

	stThread			*stpThreadInfo;
	stLogThreadArgument	*stpThreadArg;

	// 1. �ƱԸ�Ʈ �ʱ�ȭ(Thread ������ ���������� �޴´�.)
	stpThreadInfo	= (stThread*)_vpArg;
	stpThreadArg	= (stLogThreadArgument*)stpThreadInfo->vpArgs;

	iThreadIndex	= stpThreadArg->iThreadIndex;

	// 2. Heap ������ �ִ� ������ ����
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

	// 3. Thread�� ������ 1�� ���δ�
	#ifdef OS508
	pthread_mutex_lock( &g_iLogThreadCountLock );
	g_iLogThreadCount--;
	pthread_mutex_unlock( &g_iLogThreadCountLock );
	#else
	atomic_dec_uint_nv( &g_iLogThreadCount );
	#endif

	// 4. Thread ����!!
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

	// 1. �α� �ڵ��� �ʱ�ȭ �Ѵ�(�Ϲݷα�, �����α�)
	NLOG	= stpLogArg->NLOG;
	ELOG	= stpLogArg->ELOG;

	// 2. Serial �ѹ� �߱�
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

	// 3. �α����Ͽ� ������(Log Pakcet) ���
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

	// 4. ServiceType�� ServiceLog �� �ƴϸ� SmdaQueue�� �������� ����
	if( _stpLogPacket->iLogFileType != 1 )
		return	0;

	// 5. ����� SMDA Server(Group)�� ���Ѵ�
	{
		memset( caTempRouting, 0x00, sizeof( caTempRouting ) );
		memcpy( caTempRouting, _stpLogPacket->caRoutingNumber, 5 );

		// 5.1 �Ϲ� DST��ȣ�ΰ�� ����� ���̺��� ��ȸ�Ͽ� ���� Queue�� ���Ѵ�
		if( strstr( caTempRouting, "#" ) == NULL )
		{
			iPrefixIndex	= atoi( caTempRouting );

			// 5.1.1 Dest�� "0"�� ��� SMDA Queue�� �����ϴ°��� ���´�
			//      �ӽ������� �߰��Ѱ��̶� ���Ŀ� �����Ѵ�
			//      TELESA�� Ÿ�� REPORT ACK������ ���Ƶ�
			if( iPrefixIndex == 0 )
				return	0;

			iServerRoutingIndex	= g_stSmdaRouting.Index[iPrefixIndex];
		}
		// 5.2 Ư�� DST�� ��� ETC ROUTING �� �¿� ������
		else
		{
			iServerRoutingIndex	= g_iEtcRoutingQueue;
		}
	}

	// 6. ��ϵǾ����� ���� Server ����� �ε����� ������ ����ó��
	if( iServerRoutingIndex < 0 || iServerRoutingIndex >= g_stSmdaThread.iSmdaServerNum )
	{
		CNLOG( ELOG, 1, "[T:%d][Server Routing Index Error][Routing Index: %d][Seq:%d][L:%d]\n", 
			_iThreadIndex + 1, iServerRoutingIndex, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );

		return	-1;
	}

	// 7. ����� SMDA Session�� ���Ѵ�
	{
		#ifdef OS508
		pthread_mutex_lock( &g_stSmdaThread.Info[iServerRoutingIndex].Lock );
		iSessionRoutingIndex = (++g_stSmdaThread.Info[iServerRoutingIndex].iMessageCount) % g_stSmdaThread.Info[iServerRoutingIndex].iSmdaSessionNum;
		pthread_mutex_unlock( &g_stSmdaThread.Info[iServerRoutingIndex].Lock );
		#else
		iSessionRoutingIndex = atomic_inc_uint_nv( &g_stSmdaThread.Info[iServerRoutingIndex].iMessageCount ) % g_stSmdaThread.Info[iServerRoutingIndex].iSmdaSessionNum;
		#endif
	}

	// 8. ��ϵǾ����� ���� Session ����� �ε����� ������ ����ó��
	if( iSessionRoutingIndex < 0 || iSessionRoutingIndex >= g_stSmdaThread.Info[iServerRoutingIndex].iSmdaSessionNum )
	{
		CNLOG( ELOG, 1, "[T:%d][Session Routing Index Error][Routing Index : %d][Seq:%d][L:%d]\n", 
			_iThreadIndex + 1, iSessionRoutingIndex, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );

		return	-1;
	}

	// 9. ������ SmdaQueue�� Log Packet ����
	{
		pthread_mutex_lock( &(g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].Lock) );
		
		// 9.1 Smda Queue ���°� ������ �ƴϰų� Attach�� �ȵȰ�� ����ó��
		if( g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].iQueueID < 0 )
		{
			pthread_mutex_unlock( &(g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].Lock) );

			CNLOG( ELOG, 1, "[T:%d][Routing Smda Queue Not Attach][Routing Server: %d][Session Session: %d][Seq:%d][L:%d]\n", 
				_iThreadIndex + 1, iServerRoutingIndex, iSessionRoutingIndex, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );

			return	-1;
		}

		// 9.2 Smda Queue�� ������ ����
		iResult	= CNQueueWrite( g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].iQueueID, _stpLogPacket, _iPacketSize );

		// 9.3 ���� ������� ó��
		switch( iResult )
		{
			// 9.3.1 SmdaQueue�� ���������� LogPacket �����͸� ����
			case CN_IPC_QUEUE_DATA_WRITE :

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].Lock) );

				break;

			// 9.3.2 SmdaQueue�� ����á�����
			case CN_IPC_QUEUE_DATA_FULL :

				CNLOG( ELOG, 1, "[T:%d][CNQueueWrite( %d %d %d 0 ) Error][Queue Full][Seq:%d][R:%d][E:%d][L:%d]\n", 
					_iThreadIndex + 1, g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].iQueueID, 
					_stpLogPacket, _iPacketSize, _stpLogPacket->stHeader.iSerialNumber, iResult, errno, __LINE__ );

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].Lock) );

				return	-1;

				break;

			// 9.3.3 SmdaQueue ���۵��� ������ �� ���
			case CN_IPC_QUEUE_DATA_ERROR :

				CNLOG( ELOG, 1, "[T:%d][CNQueueWrite( %d %d %d 0 ) Error][IPC Queue Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
					_iThreadIndex + 1, g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].iQueueID, 
					_stpLogPacket, _iPacketSize, _stpLogPacket->stHeader.iSerialNumber, iResult, errno, __LINE__ );

				g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].iQueueID	= -1;

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iServerRoutingIndex].stpQueue[iSessionRoutingIndex].Lock) );

				return	-1;

				break;

			// 9.3.4 SmdaQueue ���۵��� �˼����� ������ �� ���
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

	// 10. ����!!
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

	// 1. �α� �ڵ��� �ʱ�ȭ �Ѵ�(�Ϲݷα�, �����α�)
	CN_LOG_HANDLE	NLOG	= _stpThreadArg->NLOG;
	CN_LOG_HANDLE	ELOG	= _stpThreadArg->ELOG;

	// 2. LogThread Error ���� ����(������+�����̸�)
	sprintf( caWriteFile, "%s/%s_%d_%-.*s_%s.dat", 
		g_cpErrorLogThreadFilePath, g_cpErrorLogThreadFileName, _stpThreadArg->iThreadIndex + 1, 
		10, GetNowTimeStr(caTimeBuffer), g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	// 3. Error ���� ����(���� �� ����, ���� �� �̾��)
	if( ( fpWriteFile = fopen( caWriteFile, "a+" ) ) == NULL )
	{
		CNLOG( ELOG, 1, "[T:%d][fopen( %s, a+ ) Error][Seq:%d][E:%d][L:%d]\n", 
			_stpThreadArg->iThreadIndex + 1, caWriteFile, _stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

		return	-1;
	}

	// 4. Error ������ ���� ���
	if( fwrite( _stpLogPacket, sizeof( stLogPacket ), 1, fpWriteFile ) != 1 )
	{
		fclose( fpWriteFile );

		CNLOG( ELOG, 1, "[T:%d][fwrite( %d %d 1 %d ) Error][Seq:%d][E:%d][L:%d]\n", 
			_stpThreadArg->iThreadIndex + 1, _stpLogPacket, sizeof( stLogPacket ), fpWriteFile, 
			_stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

		return	-1;
	}

	// 5. Error ���� Close
	fclose( fpWriteFile );

	// 6. ����!!
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

	// 1. �ƱԸ�Ʈ �ʱ�ȭ(Thread ������ ���������� �޴´�.
	stpThreadInfo	= (stThread*)_vpArg;
	stpThreadArg	= (stSmdaThreadArgument*)stpThreadInfo->vpArgs;

	iSmdaIndex		= stpThreadArg->iSmdaIndex;
	iSessionIndex	= stpThreadArg->iSessionIndex;

	// 2. �α� �����Ͱ��� ���� �ʱ�ȭ
	strlcpy( caLogFilePath, stpThreadArg->cpConfigPath, sizeof(caLogFilePath) );
	strlcpy( caLogFileName, SMDA_QUEUE_LOG_FILE_NAME, sizeof(caLogFileName) );
	strlcpy( caErrLogFileName, SMDA_QUEUE_ERROR_LOG_FILE_NAME, sizeof(caErrLogFileName) );

	CNInitList( &(stpThreadArg->List) );

	stpThreadArg->iEmergencyFilingFlag	= 0;
	stpThreadArg->cpEmergencyTime	= (char*)malloc( 15 );	// 11(YYYYMMDDhhmmss + 1)
	memset( stpThreadArg->cpEmergencyTime, 0x00, 15 );

	// 3. �Ϲ� �α� ������ ���(�̶����� �Ϲݷα� ���� ����)
	if( ( stpThreadArg->NLOG = GetSmdaThreadLogHandle( iSmdaIndex, iSessionIndex, caLogFilePath, caLogFileName ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[T:%02d %02d][GetLogThreadLogHandle( %d %d %s %s ) Error][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, iSmdaIndex, iSessionIndex, caLogFilePath, caLogFileName, errno, __LINE__ );

		return	-1;
	}

	// 4. ���� �α� ������ ���(�̶����� �����α� ���� ����)
	if( ( stpThreadArg->ELOG = GetSmdaThreadLogHandle( iSmdaIndex, iSessionIndex, caLogFilePath, caErrLogFileName ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[T:%02d %02d][GetLogThreadLogHandle( %d %d %s %s ) Error][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, iSmdaIndex, iSessionIndex, caLogFilePath, caErrLogFileName, errno, __LINE__ );

		return	-1;
	}

	CNLOG( stpThreadArg->NLOG, 3, "[T:%02d %02d][SMDA Thread Start][L:%d]\n", iSmdaIndex + 1, iSessionIndex + 1, __LINE__ );

	// 5. ��� �ñ׳� ����(Blocking), �ñ׳� ó���� Main Thread�� �����Ѵ�
	if( ( iResult = CNAllSignalBlock( &stSigTemp ) ) < 0 )
	{
		CNLOG( stpThreadArg->ELOG, 1, "[T:%02d %02d][CNAllSignalBlock( %d ) Error][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, &stSigTemp, iResult, errno, __LINE__ );

		return	-1;
	}

	// 6. ��ó�� ������ ������ �о �޸𸮿� �ø���(���μ��� ����� ��ó�� �����͸� ���Ͽ� ���� ��⵿��
	//    �ٽ� �о�鿩 ó���Ѵ�)
	if( ( iResult = OnReadGutsSmda( stpThreadArg ) ) < 0 )
	{
		CNLOG( stpThreadArg->ELOG, 1, "[T:%02d %02d][OnReadGutsSmda( %d ) Error][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, stpThreadArg, iResult, errno, __LINE__ );

		return	-1;
	}

	// 7. ����!!
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

	// 1. �ƱԸ�Ʈ �ʱ�ȭ(Thread ������ ���������� �޴´�.)
	stpThreadInfo	= (stThread*)_vpArg;
	stpThreadArg	= (stSmdaThreadArgument*)stpThreadInfo->vpArgs;

	iSmdaIndex		= stpThreadArg->iSmdaIndex;
	iSessionIndex	= stpThreadArg->iSessionIndex;

	iReadPacketSize	= sizeof( stLogPacket ) - sizeof( long );

	NLOG			= stpThreadArg->NLOG;
	ELOG			= stpThreadArg->ELOG;

	// 2. Smda ������ ���� ���� ������ Argument���� ����
	iSmdaPort	= g_stSmdaThread.Info[iSmdaIndex].iPort;

	strlcpy( caSmdaAdress, g_stSmdaThread.Info[iSmdaIndex].caAdress, sizeof( caSmdaAdress ) );
	strlcpy( caSmdaBindID, g_stSmdaThread.Info[iSmdaIndex].caBindID, sizeof( caSmdaBindID ) );
	strlcpy( caSmdaBindPW, g_stSmdaThread.Info[iSmdaIndex].caBindPW, sizeof( caSmdaBindPW ) );

	CNLOG( NLOG, 3, "[T:%02d %02d][SMDA Thread Run][L:%d]\n", iSmdaIndex + 1, iSessionIndex + 1, __LINE__ );

	// 3. Smda ������ ���� �õ�
	if( ( iSocketFD = OnConnectToSmda( iSmdaIndex, iSessionIndex, stpThreadArg, caSmdaAdress, iSmdaPort, caSmdaBindID, caSmdaBindPW ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][OnConnectToSmda( %d %d %d %s %d %s %s ) Error][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, 
			iSmdaIndex, iSessionIndex, stpThreadArg, caSmdaAdress, iSmdaPort, caSmdaBindID, caSmdaBindPW, 
			iSocketFD, errno, __LINE__ );
	}

	tLastQueryTime	= time( NULL );

	// 4. ��������� �ö����� ������ ó��
	while( GetSmdaThreadDone() )
	{
		// 4.1 Smda�� ������ �Ǿ����� ������� ������ �õ�
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

		// 4.2 Ư���� �ֱ�(g_iSmdaRetryCycle)�� ��ó�� �ʿ� ������ ��ó��
		if( ( g_iSmdaRetryUseFlag != 0 ) && ( time( NULL ) - tLastReprocessTime >= g_iSmdaRetryCycle ) )
		{
			OnSmdaDataReprocess( stpThreadArg, &iSocketFD, &tLastQueryTime );

			tLastReprocessTime	= time( NULL );
		}

		// 4.3 Ư���� �ð�(SYSTEM_QUERY_TIME_CYCLE)���� Smda������ ����� ������� System Query ����
		if( iSocketFD > 0 && ( time( NULL ) - tLastQueryTime >= SYSTEM_QUERY_TIME_CYCLE ) )
		{
			if( OnSysQueryToSmda( iSmdaIndex, iSessionIndex, stpThreadArg, iSocketFD ) < 0 )
			{
				CNSocketClose( iSocketFD );

				iSocketFD	= -1;
			}
			
			tLastQueryTime	= time( NULL );
		}

		// 4.4 Queue�� �����͸� üũ������ 300�� �̻� �����Ͱ� ����־��� ���
		//     CPU ���ϸ� ���̱� ���� ������ �ش�
		if( iSmdaQueueEmpty >= 300)
		{
			CNNanoSleep( 1000000000 );

			iSmdaQueueEmpty	= 0;

			continue;
		}

		pthread_mutex_lock( &(g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].Lock) );

		// 4.5 Queue�� Attach�Ǿ����� �������
		if( g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID < 0 )
		{
			// 4.5.1 Queue �� ������ ����, ������ Attach
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

		// 4.6 Queue���� Log Packet ������ �б⸦ �õ�
		iResult	= CNQueueRead( g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID, &stTempPacket, iReadPacketSize, 0 );

		switch( iResult )
		{
			// 4.6.1 Queue���� ���������� Packet �����͸� �о��� ���
			case CN_IPC_QUEUE_DATA_READ :

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].Lock) );

				// 4.6.1.1 �о���� Queue�����͸� OnSmdaMsgRead()�� ó��(Smda�� ������ ���� Ȥ�� ���Ϸ� ���)
				if( OnSmdaMsgRead( iSmdaIndex, iSessionIndex, iSocketFD, &stTempPacket, stpThreadArg, &tLastQueryTime ) < 0 )
				{
					CNLOG( ELOG, 1, "[T:%02d %02d][OnSmdaMsgRead( %d %d %d %d %d %d ) Error][Seq:%d][E:%d][L:%d]\n", 
						iSmdaIndex + 1, iSessionIndex + 1, 
						iSmdaIndex, iSessionIndex, iSocketFD, &stTempPacket, stpThreadArg, &tLastQueryTime, 
						stTempPacket.stHeader.iSerialNumber, errno, __LINE__ );

					// 4.6.1.1.1 OnLogMsgRead() ���н� OnSmdaMsgReadError()�� ����ó��(��ó�� �����ͷ� ���, ������ ���� ���)
					if( OnSmdaMsgReadError( &stTempPacket, stpThreadArg ) < 0 )
					{
						CNLOG( ELOG, 1, "[T:%02d %02d][OnSmdaMsgReadError( %d %d ) Error][Seq:%d][E:%d][L:%d]\n", 
							iSmdaIndex + 1, iSessionIndex + 1, 
							&stTempPacket, stpThreadArg, 
							stTempPacket.stHeader.iSerialNumber, errno, __LINE__ );
					}

					// 4.6.1.1.2 Smda ���� ���н� ���� ����ó��
					if( iSocketFD >= 0 )
					{
						CNSocketClose( iSocketFD );

						iSocketFD	= -1;
					}
				}

				iSmdaQueueEmpty	= 0;

				break;
			
			// 4.6.2 Queue�� ����������(Packet �����Ͱ� �������)
			case CN_IPC_QUEUE_DATA_EMPTY :

				pthread_mutex_unlock( &(g_stSmdaThread.Info[iSmdaIndex].stpQueue[iSessionIndex].Lock) );

				OnSmdaMsgEmpty();

				iSmdaQueueEmpty++;

				continue;

				break;

			// 4.6.3 Queue�б⵵�� ������ �� ���
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

			// 4.6.4 Queue�б⵵�� �˼����� ������ �� ���
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

	// 5. ����� Smda �������� ������ ����
	if( iSocketFD >= 0 )
	{
		CNSocketClose( iSocketFD );

		iSocketFD	= -1;
	}

	// 6. ����!! OnSmdaThreadTerminate()�� �̾ ����(������� �� ������ ������ ���)
	return	0;
}


int OnSmdaThreadTerminate( void* _vpArg )
{
	int	iResult;
	int	iSmdaIndex;
	int	iSessionIndex;

	stThread				*stpThreadInfo;
	stSmdaThreadArgument	*stpThreadArg;

	// 1. �ƱԸ�Ʈ �ʱ�ȭ(Thread ������ ���������� �޴´�.
	stpThreadInfo	= (stThread*)_vpArg;
	stpThreadArg	= (stSmdaThreadArgument*)stpThreadInfo->vpArgs;

	iSmdaIndex	= stpThreadArg->iSmdaIndex;

	// 2. Heap ���� ������ ����
	if( stpThreadArg->NLOG != NULL )
		CNLOG( stpThreadArg->NLOG, 3, "[T:%02d %02d][SMDA Thread Terminate][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, __LINE__ );

	// 3. ��ó�� �����͵��� ���Ͽ� ���(��⵿�� �о �̾ ó��)
	if( ( iResult = OnWriteGutsSmda( stpThreadArg ) ) < 0 )
	{
		// OnSmdaThreadStart���� ������ �� ��� �α׵����͸� �̴ϼȶ���¡ ���� ���Ҽ��� �����Ƿ�
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

	// 4. Thread ����!!
	return	0;
}


int OnSmdaMsgRead( int _iSmdaIndex, int _iSessionIndex, int _iSocketFD, stLogPacket* _stpLogPacket, stSmdaThreadArgument* _stpThreadArg, time_t* _tpQueryTime )
{
	int	iResult;

	CN_LOG_HANDLE	NLOG;
	CN_LOG_HANDLE	ELOG;

	// 1. �ƱԸ�Ʈ�� ���� �α��ڵ� �ʱ�ȭ
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

	// 2. Emergency ����� ��� �����͸� Smda�� �������� �ʰ� Emergency ó��
	if( *g_ipEmergencyMode != 0 )
		return	OnEmergencyMode( _iSmdaIndex, _iSessionIndex, _stpLogPacket, _stpThreadArg );

	// 3. Smda ������ ���ӵǾ����� ������� �����͸� �������� ����
	if( _iSocketFD < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][Smda Server Not Connected][Seq:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );

		return	-1;
	}

	// 4. Smda ������ ������ ����
	if( ( iResult = OnSendDataToSmda( _iSocketFD, _stpLogPacket, _stpThreadArg ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][OnSendDataToSmda( %d %d %d) Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, 
			_iSocketFD, _stpLogPacket, _stpThreadArg, _stpLogPacket->stHeader.iSerialNumber, 
			iResult, errno, __LINE__ );

		*_tpQueryTime	= time( NULL );

		return	-1;
	}

	// 5. ���� ���۽ð� ������Ʈ
	*_tpQueryTime	= time( NULL );

	// 6. ����!!
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

	// 1. �α� �ڵ��� �ʱ�ȭ �Ѵ�(�Ϲݷα�, �����α�)
	CN_LOG_HANDLE	NLOG	= _stpThreadArg->NLOG;
	CN_LOG_HANDLE	ELOG	= _stpThreadArg->ELOG;

	// 2. ������ ��ó�� ����� ������� �ʴ� ��� �����͸� Memory�� ���������ʰ� 
	//    Error ���Ϸ� �ٷ� ���
	if( g_iSmdaRetryUseFlag == 0 )
		return	OnWriteSmdaThreadError( _stpLogPacket, _stpThreadArg );

	// 3. ��ó�� ����� ����Ѵ� �ϴ��� ��ó�� �õ� Ƚ���� 0�̰ų� 0���� ������� 
	//    �����͸� Memory�� ���������ʰ� Error ���Ϸ� �ٷ� ���
	if( g_iSmdaRetryCount <= 0 )
		return	OnWriteSmdaThreadError( _stpLogPacket, _stpThreadArg );

	// 4. Queue�� ������� Queue���� ���� ���� �����͸� �����ϰ� �ִ´�.
	//    ������ �����ʹ� Error ������ ������Ͽ� ��ϵȴ�. ������ �Ǹ�
	//    ���Ŀ� Queue�� ���� ��������� �Լ�OnSmdaQueueClear()�� ��ü�Ѵ�.
	if( _stpThreadArg->List.m_reference >= MAX_SMDA_REPROCESS_DATA_SIZE )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][SMDA Reprocess Data Full][Delete Data Seq: %d][Seq:%d][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, 
			_stpThreadArg->List.m_head.next->iSequence, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );

		OnWriteSmdaThreadError( (stLogPacket*)_stpThreadArg->List.m_head.next->data, _stpThreadArg );

		CNDeleteList( &(_stpThreadArg->List), _stpThreadArg->List.m_head.next );
	}

	// 5. �����͸� ��ó��(������)�ϱ� ���� Memory ������ ����
	stpOffset	= CNPushList( &(_stpThreadArg->List), (char*)_stpLogPacket, sizeof( stLogPacket ), _stpLogPacket->stHeader.iSerialNumber );

	if( stpOffset == NULL )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNPushList( %d %d %d %d ) Error]][Seq:%d][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, 
			&(_stpThreadArg->List), _stpLogPacket, sizeof( stLogPacket ), _stpLogPacket->stHeader.iSerialNumber, 
			_stpLogPacket->stHeader.iSerialNumber, __LINE__ );

		return	-1;
	}

	// 6. ������ ������ Container ������ ����
	stpOffset->iRequestCount	= 0;
	stpOffset->tLastActTime		= time( NULL );

	// 7. ����!!
	return	0;
}


int OnSmdaQueueClear( stSmdaThreadArgument* _stpThreadArg )
{
	int		iDeleteCount	= 0;

	stContainer		*stpOffset;
	stLogPacket		*stpDeletePacket;

	// 1. �α� �ڵ��� �ʱ�ȭ �Ѵ�(�Ϲݷα�, �����α�)
	CN_LOG_HANDLE	NLOG	= _stpThreadArg->NLOG;
	CN_LOG_HANDLE	ELOG	= _stpThreadArg->ELOG;

	CNLOG( NLOG, 3, "[T:%02d %02d][SMDA Queue Clear Start][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, __LINE__ );

	// 2. �޸𸮿� ����� ��ó�� ������ Error���Ͽ� ����ϰ� �����
	for( stpOffset = _stpThreadArg->List.m_head.next; stpOffset->next; stpOffset = stpOffset->next )
	{
		stpDeletePacket	= (stLogPacket*)stpOffset->data;

		CNLOG( ELOG, 3, "[T:%02d %02d][Delete Queue Full Data][Seq:%d][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, stpDeletePacket->stHeader.iSerialNumber, __LINE__ );

		OnWriteSmdaThreadError( stpDeletePacket, _stpThreadArg );

		stpOffset	= CNDeleteList( &(_stpThreadArg->List), stpOffset );

		iDeleteCount++;
	}

	// 3. ������ ������ ������ ���
	CNLOG( NLOG, 3, "[T:%02d %02d][SMDA Queue Data Delete][Delete Count: %d][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, iDeleteCount, __LINE__ );

	CNLOG( NLOG, 3, "[T:%02d %02d][SMDA Queue Clear End][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, __LINE__ );

	// 4. ����!!
	return	0;
}


int OnWriteSmdaThreadError( stLogPacket* _stpLogPacket, stSmdaThreadArgument* _stpThreadArg )
{
	char	caWriteFile[512];
	char	caTimeBuffer[15];

	FILE	*fpWriteFile;

	// 1. �α� �ڵ��� �ʱ�ȭ �Ѵ�(�Ϲݷα�, �����α�)
	CN_LOG_HANDLE	NLOG	= _stpThreadArg->NLOG;
	CN_LOG_HANDLE	ELOG	= _stpThreadArg->ELOG;

	// 2. SmdaThread Error ���� ����(������+�����̸�)
	sprintf( caWriteFile, "%s/%s_%02d_%02d_%-.*s_%s.dat", 
		g_cpErrorSmdaThreadFilePath, g_cpErrorSmdaThreadFileName, _stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, 
		10, GetNowTimeStr(caTimeBuffer), g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	// 3. Error ���� ����(���� �� ����, ���� �� �̾��)
	if( ( fpWriteFile = fopen( caWriteFile, "a+" ) ) == NULL )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][fopen( %s, a+ ) Error][Seq:%d][E:%d][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, caWriteFile, _stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

		return	-1;
	}

	// 4. Error ������ ���� ���
	if( fwrite( _stpLogPacket, sizeof( stLogPacket ), 1, fpWriteFile ) != 1 )
	{
		fclose( fpWriteFile );

		CNLOG( ELOG, 1, "[T:%02d %02d][fwrite( %d %d 1 %d ) Error][Seq:%d][E:%d][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, _stpLogPacket, sizeof( stLogPacket ), fpWriteFile, 
			_stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

		return	-1;
	}

	// 5. Error ���� Close
	fclose( fpWriteFile );

	// 6. ����!!
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

	// 1. �α� �ڵ��� �ʱ�ȭ �Ѵ�(�Ϲݷα�, �����α�)
	NLOG	= _stpThreadArg->NLOG;
	ELOG	= _stpThreadArg->ELOG;

	// 2. Emergency ��尪�� 1�� �ƴѰ�� �̸����� ������ �������� �ʴ´�.
	// Emergency Mode 0: Emergency Mode�� �ƴ�, 0�� ��� �� ��Ʈ�� �ü�����
	// Emergency Mode 1: Smda�� �����͸� �������� �ʰ� Emergency ���Ͽ� �����͸� ���
	// Emergency Mode 2: Smda�� �����͸� �������� �ʰ� Emergency ���ϵ� ������� ����
	// Emergency Mode ���� 0, 1, 2�� �ƴѰ�� 2�� �Ǵ��Ͽ� �����Ѵ�
	if( *g_ipEmergencyMode != 1 )
		return	0;

	// 3. ������ �ð��� ���ؼ� ���ۿ� ����
	GetNowTimeStr( caTimeBuffer );

	// 4. ���� ��ũ ��뷮 üũ ������ 1���� �ȵ��� ���
	if( memcmp( _stpThreadArg->cpEmergencyTime, caTimeBuffer, 12 ) == 0 )
	{
		// 4.1 ���ϸ� ��尡 0�ϰ�� ���ϸ� ���� �ʴ´�.
		//     ���� ��뷮�� �ִ� ��� ��뷮�� �Ѿ ��� ���ϸ� ��尡 0�� �ȴ�.
		//     ���� ��뷮 üũ�� �Ʒ� 5���� 1�и��� �ѹ��� üũ�Ͽ� ��带 �����Ѵ�.
		if( _stpThreadArg->iEmergencyFilingFlag == 0 )
		{
			/*
			CNLOG( ELOG, 1, "[T:%02d %02d][DiskQuantity OverFlow Error][Seq:%d][L:%d]\n", 
				_iSmdaIndex + 1, _iSessionIndex + 1, _stpLogPacket->stHeader.iSerialNumber, __LINE__ );
			*/

			return	0;
		}
	}
	// 5. ���� ��ũ ��뷮 üũ ������ 1���̻��� ���
	else
	{
		// 5.1 ���� �ð��� ��ũ ��뷮 üũ�ð��� ������ �ִ´�.
		strcpy( _stpThreadArg->cpEmergencyTime, caTimeBuffer );

		// 5.2 ���� ��ũ ��뷮�� ��� ��뷮�� �Ѿ ���
		if( ( iResult = GetDiskQuantity( g_cpCheckQuantityPath ) ) >= g_iMaxDiskQuantity )
		{
			CNLOG( ELOG, 1, "[T:%02d %02d][DiskQuantity( %s ) Error][DiskQuantity: %d][Max DiskQuantity: %d][L:%d]\n", 
				_iSmdaIndex + 1, _iSessionIndex + 1, g_cpCheckQuantityPath, iResult, g_iMaxDiskQuantity, __LINE__ );

			// 5.2.1 �����ð�(��)���� ���ϸ� ���� �ʵ��� ���ϸ� �÷��׸� 0(���ϸ����� ����)���� ����
			_stpThreadArg->iEmergencyFilingFlag	= 0;

			return	0;
		}
		// 5.3 ���� ��ũ ��뷮�� ��� ��뷮 ���ϰ��
		else
		{
			// 5.3.1 ���� ��뷮 üũ�ð����� ���ϸ��� �����ϵ��� ����
			_stpThreadArg->iEmergencyFilingFlag	= 1;
		}
	}

	// 5. Emergency ���ϸ� ����(������+���ϸ�, �ð����� ����)
	sprintf( caEmergencyFile, "%s/%s_%02d_%02d_%-.*s_%s.dat", 
		g_cpEmergencyFilePath, g_cpEmergencyFileName, _iSmdaIndex + 1, _iSessionIndex + 1, 
		10, caTimeBuffer, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	// 6. ���� Open, ������ ����, ������ �̾��
	if( ( fpFile = fopen( caEmergencyFile, "a+" ) ) == NULL )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][fopen( %s, a+ ) Error][Seq:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, g_cpEmergencyFilePath, _stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

		return	0;
	}

	// 7. Emergency ������ ���
	if( fwrite( _stpLogPacket, sizeof(stLogPacket) , 1, fpFile ) != 1 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][fwrite( %d %d 1 %d ) Error][Seq:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, _stpLogPacket, sizeof( stLogPacket ), fpFile, 
			_stpLogPacket->stHeader.iSerialNumber, errno, __LINE__ );

		fclose( fpFile );

		return	0;
	}

	// 8. Emergency ������ �ݴ´�.
	fclose( fpFile );

	// 9. ����!!
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

	// 1. �α� �ڵ��� �ʱ�ȭ �Ѵ�(�Ϲݷα�, �����α�)
	CN_LOG_HANDLE	NLOG	= _stpThreadArg->NLOG;
	CN_LOG_HANDLE	ELOG	= _stpThreadArg->ELOG;

	CNLOG( NLOG, 3, "[T:%02d %02d][Guts File Read Start][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, __LINE__ );

	// 2. �о���� Guts ���ϸ� ����(������+�����̸�)
	sprintf( caReadSmdaGutsFile, "%s/%s_%02d_%02d_%-.*s_%s.guts", 
		g_cpGutsSmdaFilePath, g_cpGutsSmdaFileName, _stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, 
		8, GetNowTimeStr(caTimeBuffer), g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	CNLOG( NLOG, 3, "[T:%02d %02d][Read Guts File Name: %s][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, caReadSmdaGutsFile, __LINE__ );

	// 3. ������ ����. ������ ������� ���ʱ⵿���� �Ǵ�
	if( ( fpReadFile = fopen( caReadSmdaGutsFile, "r" ) ) == NULL )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][fopen( %s, r ) Error][���ʱ⵿][E:%d][L:%d]\n", 
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, caReadSmdaGutsFile, errno, __LINE__ );

		return	0;
	}

	// 4. ���Ͽ��� �����͸� �о �޸𸮿� ����(���� �����ð��� ������ Insert �ð����� ����)
	while( fread( &stTempContainer, sizeof( stTempContainer ), 1, fpReadFile ) )
	{
		stpOffset	= CNPushList( &(_stpThreadArg->List), stTempContainer.data, sizeof( stLogPacket ), stTempContainer.iSequence );

		stpOffset->iRequestCount	= stTempContainer.iRequestCount;
		stpOffset->tLastActTime		= time( NULL );

		iReadCount++;
	}

	// 5. �� �о���̸� ������ �ݴ´�
	fclose( fpReadFile );

	// 6. ��� �����͸� �о���̸� Guts ���� ����
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

	// 1. �α� �ڵ��� �ʱ�ȭ �Ѵ�(�Ϲݷα�, �����α�)
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

	// 2. �����͸� ��� �� Guts ���� ����(������+�����̸�, �ð������� ����)
	sprintf( caWriteSmdaGutsFile, "%s/%s_%02d_%02d_%-.*s_%s.guts", 
		g_cpGutsSmdaFilePath, g_cpGutsSmdaFileName, _stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, 
		8, GetNowTimeStr(caTimeBuffer), g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" );

	CNLOG( NLOG, 3, "[T:%02d %02d][Write Guts File Name: %s][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, caWriteSmdaGutsFile, __LINE__ );

	// 3. �̹� ������ �����Ѵٸ� ThreadStart Ȥ�� Guts Read �ܿ��� ������ ���ܼ� ������ ������ �ʰ�
	//    �о������ �������Ƿ� �����͸� �����ʰ� �����Ѵ�.
	if( ( fpWriteFile = fopen( caWriteSmdaGutsFile, "r" ) ) != NULL )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][Write Guts File Exist][Guts File: %s][L:%d]\n",
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, caWriteSmdaGutsFile, __LINE__ );

		fclose( fpWriteFile );

		return	-1;
	}

	fclose( fpWriteFile );

	// 4. ��ó�� �����͸� ����� ����(Guts ����)�� �����Ѵ�
	if( ( fpWriteFile = fopen( caWriteSmdaGutsFile, "w" ) ) == NULL )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][fopen( %s w ) Error][L:%d]\n",
			_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, caWriteSmdaGutsFile, __LINE__ );

		fclose( fpWriteFile );

		return	-1;
	}

	// 5. �޸𸮿� ����� ��ó�� �����͸� Guts ���Ͽ� ���
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

	// 6. Guts ���� Close
	fclose( fpWriteFile );

	CNLOG( NLOG, 3, "[T:%02d %02d][Write Guts Data: %d]][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, iWriteCount, __LINE__ );

	CNLOG( NLOG, 3, "[T:%02d %02d][Guts File Write End][L:%d]\n", 
		_stpThreadArg->iSmdaIndex + 1, _stpThreadArg->iSessionIndex + 1, __LINE__ );

	// 7. ����!!
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

	// 1. �ƱԸ�Ʈ ������ �������� �ʱ�ȭ �� ����
	iSmdaIndex		= _stpThreadArg->iSmdaIndex;
	iSessionIndex	= _stpThreadArg->iSessionIndex;

	NLOG			= _stpThreadArg->NLOG;
	ELOG			= _stpThreadArg->ELOG;

	CNLOG( NLOG, 3, "[T:%02d %02d][Reprocess Start][Reference: %d][L:%d]\n", 
		iSmdaIndex + 1, iSessionIndex + 1, _stpThreadArg->List.m_reference, __LINE__ );

	// 2. Heap Memory ������ �˻��ϸ鼭 ������ ��ó��(������)
	for( stpOffset = _stpThreadArg->List.m_head.next; stpOffset->next; stpOffset = stpOffset->next )
	{
		// 2.1 Emergency Mode �� ��� �����͸� ���������� �ʰ� OnEmergencyMode() �Լ��� ó��
		if( *g_ipEmergencyMode != 0 )
		{
			OnEmergencyMode( iSmdaIndex, iSessionIndex, (stLogPacket*)stpOffset->data, _stpThreadArg );
			
			stpOffset	= CNDeleteList( &(_stpThreadArg->List), stpOffset );

			continue;
		}

		// 2.2 ����� �������� ������ ó�� �ð��� ��ó�� �ð�(g_iSmdaRetryTime)���� ũ�ų� �������
		//     �ش� �����͸� ��ó��(������)�Ѵ�.
		//     ��������� OnSmdaDataReprocess() �Լ��� ����Ǹ� �޸� ������ ���鼭 ������ ó�� �ð���
		//     ��ó�� �ð�(g_iSmdaRetryTime) �̻� ���� �����͸� ��ó���Ѵ�.
		if( ( time( NULL ) - stpOffset->tLastActTime ) >= g_iSmdaRetryTime )
		{
			// 2.2.1 ��ó�� ī��Ʈ ����
			stpOffset->iRequestCount++;

			// 2.2.2 Smda�� ������� ���� ���¸� �������� �ʰ� �ٷ� ����ó��
			if( *_ipSocketFD < 0 )
			{
				// 2.2.2.1 Smda ������ ����Ǿ� ���� ������� �ٷ� ���۽��� ó��, �α� ���
				stpLogPacket	= (stLogPacket*)stpOffset->data;

				CNLOG( ELOG, 1, "[T:%02d %02d][Reprocess SMDA Server Not Connected][Request Count: %d][Seq:%d][L:%d]\n", 
					iSmdaIndex + 1, iSessionIndex + 1, stpOffset->iRequestCount, stpLogPacket->stHeader.iSerialNumber, __LINE__ );

				// 2.2.2.2 �������� ��ó�� ȸ���� �ִ� ��ó��ȸ��(g_iSmdaRetryCount)���� Ŭ��� 
				//         OnWriteSmdaThreadError()�� �����Ͽ� Error ���Ϸ� ���
				if( stpOffset->iRequestCount >= g_iSmdaRetryCount )
				{
					OnWriteSmdaThreadError( (stLogPacket*)stpOffset->data, _stpThreadArg );

					stpOffset	= CNDeleteList( &(_stpThreadArg->List), stpOffset );
				}
				// 2.2.2.3 �������� ��ó�� ȸ���� �ִ� ��ó��ȸ��(g_iSmdaRetryCount)���� �������
				//         ������ ó�� �ð��� ������Ʈ �Ͽ� ���� �ֱ⶧ ��ó��
				else
				{
					stpOffset->tLastActTime	= time( NULL );
				}

				continue;
			}

			// 2.2.3 Smda ������ �ش� �����͸� ������
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
				// 2.2.3.1 Socket�� Closeó���ϰ� ���� ���� -1�� ����
				CNSocketClose( *_ipSocketFD );

				*_ipSocketFD	= -1;

				// 2.2.2.2 �α� ���
				stpLogPacket	= (stLogPacket*)stpOffset->data;

				CNLOG( ELOG, 1, "[T:%02d %02d][Reprocess Send Data Error][Request Count: %d][Seq:%d][L:%d]\n", 
					iSmdaIndex + 1, iSessionIndex + 1, stpOffset->iRequestCount, stpLogPacket->stHeader.iSerialNumber, __LINE__ );

				// 2.2.3.3 �������� ��ó�� ȸ���� �ִ� ��ó��ȸ��(g_iSmdaRetryCount)���� Ŭ��� 
				//         OnWriteSmdaThreadError()�� �����Ͽ� Error ���Ϸ� ���
				if( stpOffset->iRequestCount >= g_iSmdaRetryCount )
				{
					OnWriteSmdaThreadError( (stLogPacket*)stpOffset->data, _stpThreadArg );

					stpOffset	= CNDeleteList( &(_stpThreadArg->List), stpOffset );
				}
				// 2.2.3.4 �������� ��ó�� ȸ���� �ִ� ��ó��ȸ��(g_iSmdaRetryCount)���� �������
				//         ������ ó�� �ð��� ������Ʈ �Ͽ� ���� �ֱ⶧ ��ó��
				else
				{
					stpOffset->tLastActTime	= time( NULL );
				}

				// 2.2.3.5 ������ Smda ��� �ð��� ���� �ð����� ������Ʈ
				*_tpQueryTime	= time( NULL );

				continue;
			}

			// 2.2.4 ������ Smda ��� �ð��� ���� �ð����� ������Ʈ
			*_tpQueryTime	= time( NULL );

			// 2.2.5 �����͸� �޸𸮿��� ����
			stpOffset	= CNDeleteList( &(_stpThreadArg->List), stpOffset );
		}
	}

	CNLOG( NLOG, 3, "[T:%02d %02d][Reprocess End][Reference: %d][L:%d]\n", 
		iSmdaIndex + 1, iSessionIndex + 1, _stpThreadArg->List.m_reference, __LINE__ );

	// 3. ����!!
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

	// 1. �α� �ڵ��� �ʱ�ȭ �Ѵ�(�Ϲݷα�, �����α�)
	NLOG	= stpLogArg->NLOG;
	ELOG	= stpLogArg->ELOG;

	memset( &stConnectError, 0x00, sizeof( stClientError ) );
	memset( &stRwError, 0x00, sizeof( stUtilError ) );

	// 2. Smda ������ ���� �õ�
	if( ( iSocketFD = CNMakeConnectionTCP( _cpAdress, _iPort, &stConnectError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNMakeConnectionTCP( %s %d %d ) Error][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, 
			_cpAdress, _iPort, &stConnectError, stConnectError.iReturnErrCode, stConnectError.iErrnoCode, __LINE__ );

		return	-1;
	}

	CNLOG( NLOG, 1, "[T:%02d %02d][SMDA Connected][IP: %s][Port: %d][L:%d]\n", 
		_iSmdaIndex + 1, _iSessionIndex + 1, _cpAdress, _iPort, __LINE__ );

	// 3. Smda ������ ������ ������ ��� �α��� ������ ���� �α��� ������ ����
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

	// 4. �α��� ��Ŷ ��� ����
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

	// 5. �α��� ��Ŷ �ٵ� ����
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

	// 6. �α��� ��� ��Ŷ ��� ����
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

	// 7. �α��� ��� ��Ŷ�� �´��� MsgType Ȯ��
	if( stBindResultPacketHeader.iMsgType != BIND_REPONSE )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][Bind Result Packet Header MsgType Error][MsgType: 0x00%x][Seq:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, stBindResultPacketHeader.iMsgType, stBindPacketHeader.iSerialNumber, __LINE__ );

		CNSocketClose( iSocketFD );

		return	-1;
	}

	// 8. ���� �޽����� ���� ����� �´��� SerialNumber Ȯ��
	if( stBindResultPacketHeader.iSerialNumber != stBindPacketHeader.iSerialNumber )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][Bind Result Packet Header Sequence Error][Send Sequence: %d][Recv Sequence: %d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, stBindPacketHeader.iSerialNumber, stBindResultPacketHeader.iSerialNumber, __LINE__ );

		CNSocketClose( iSocketFD );

		return	-1;
	}

	// 9. �α��� ��� ��Ŷ ���� ����
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

	// 10. �α��� ��� ��Ŷ ���� �о �α��� ���� �������� Ȯ��
	if( stBindResultPacketBody.iResult != 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][Bind Result Packet Body Result Error][Bind Result: %d][Seq:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, stBindResultPacketBody.iResult, stBindPacketHeader.iSerialNumber, __LINE__ );

		CNSocketClose( iSocketFD );

		return	-1;
	}

	CNLOG( NLOG, 3, "[T:%02d %02d][SMDA Log In Success][Seq:%d][L:%d]\n", 
		_iSmdaIndex + 1, _iSessionIndex + 1, stBindPacketHeader.iSerialNumber, __LINE__ );

	// 11. Smda ���� ���� �� �α��� ����!!
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

	// 1. �α� �ڵ��� �ʱ�ȭ �Ѵ�(�Ϲݷα�, �����α�)
	NLOG	= stpLogArg->NLOG;
	ELOG	= stpLogArg->ELOG;

	// 2. Smda�� ���� �� SysQuery ������ ��Ŷ �ʱ�ȭ �� ����
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

	// 3. SysQuery(�������� ��û) ��Ŷ ��� ����
	if( ( iResult = CNSocketWrite( _iSocketFD, (char*)&stSysQueryPacketHeader, sizeof(stLogPacketHeader), 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketWrite( %d %d %d 3000 %d ) Error][SysQuery Packet Header Send Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, _iSocketFD, &stSysQueryPacketHeader, sizeof(stLogPacketHeader), &stRwError,
			stSysQueryPacketBody.iSequence, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	-1;
	}

	CNLOG( NLOG, 3, "[T:%02d %02d][System Query Packet Body Send][Seq:%d][L:%d]\n", _iSmdaIndex + 1, _iSessionIndex + 1, stSysQueryPacketBody.iSequence, __LINE__ );

	// 4. SysQuery(�������� ��û) ��Ŷ ���� ����
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

	// 5. SysQuery(�������� ��û) ��� ��Ŷ ��� ����
	if( ( iResult = CNSocketRead( _iSocketFD, (char*)&stSysQueryResultPacketHeader, sizeof(stLogPacketHeader), 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketRead( %d %d %d 3000 %d ) Error][SysQuery Result Packet Header Read Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, _iSocketFD, &stSysQueryResultPacketHeader, sizeof(stLogPacketHeader), &stRwError,
			stSysQueryPacketBody.iSequence, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	-1;
	}

	// 6. ���ŵ� ��Ŷ�� SysQuery ��û�� ���� ������Ŷ(SYS_QUERY_REPONSE)���� Ȯ��
	if( stSysQueryResultPacketHeader.iMsgType != SYS_QUERY_REPONSE )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][SysQuery Result Packet Header MsgType Error][MsgType: 0x00%x][Seq:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, stSysQueryResultPacketHeader.iMsgType, stSysQueryPacketBody.iSequence, __LINE__ );

		return	-1;
	}

	// 7. SysQuery(�������� ��û)���� ��û�� ��Ŷ�� ���� ��� ��Ŷ���� Ȯ��
	if( stSysQueryResultPacketHeader.iSerialNumber != stSysQueryPacketHeader.iSerialNumber )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][SysQuery Result Packet Header Sequence Error][Send Sequence: %d][Recv Sequence: %d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, stSysQueryPacketHeader.iSerialNumber, stSysQueryResultPacketHeader.iSerialNumber, __LINE__ );

		return	-1;
	}

	CNLOG( NLOG, 3, "[T:%02d %02d][System Query Result Packet Body Read][Seq:%d][L:%d]\n", 
		_iSmdaIndex + 1, _iSessionIndex + 1, stSysQueryPacketBody.iSequence, __LINE__ );

	// 8. SysQuery(�������� ��û) ��� ��Ŷ ���� ����
	if( ( iResult = CNSocketRead( _iSocketFD, (char*)&stSysQueryResultPacketBody, stSysQueryResultPacketHeader.iBodyLength, 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketRead( %d %d %d 3000 %d ) Error][SysQuery Result Packet Body Read Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, _iSocketFD, &stSysQueryResultPacketBody, stSysQueryResultPacketHeader.iBodyLength, &stRwError,
			stSysQueryPacketBody.iSequence, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	-1;
	}

	// 9. ��� ��Ŷ ���𿡼� SysQuery ��û�� ���� ���(�����) Ȯ��
	if( stSysQueryResultPacketBody.iResult != 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][Sysquery Result Packet Body Result Error][SysQuery Result: %d][Seq:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, stSysQueryResultPacketBody.iResult, stSysQueryPacketBody.iSequence, __LINE__ );

		return	-1;
	}

	// 10. ����!!
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

	// 1. �ƱԸ�Ʈ ������ �������� �ʱ�ȭ �� ����
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

	// 2. �ƱԸ�Ʈ�� �Ѱܹ��� �����͸� ����
	if( ( iResult = CNSocketSend( _iSocketFD, (char*)&(_stpLogPacket->stHeader), sizeof(_stpLogPacket->stHeader) + _stpLogPacket->stHeader.iBodyLength, 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketSend( %d %d %d 3000 %d ) Error][DATA_SEND Packet Send Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, _iSocketFD, &(_stpLogPacket->stHeader), sizeof(_stpLogPacket->stHeader) + _stpLogPacket->stHeader.iBodyLength, &stRwError,
			_stpLogPacket->stHeader.iSerialNumber, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	-1;
	}

	// 3. ��� ��Ŷ�� ��� �����͸� ����
	if( ( iResult = CNSocketRecv( _iSocketFD, (char*)&stDataResultPacketHeader, sizeof(stLogPacketHeader), 3000, &stRwError ) ) < 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][CNSocketRecv( %d %d %d 3000 %d ) Error][DATA_SEND_ACK Packet Header Read Error][Seq:%d][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, _iSocketFD, &stDataResultPacketHeader, sizeof(stLogPacketHeader), &stRwError,
			_stpLogPacket->stHeader.iSerialNumber, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	-1;
	}

	// 4. ��� ��Ŷ�� ���� �����͸� ����
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

	// 5. ��� ��Ŷ�� �������ͷ� ������ ó�� ����� Ȯ��
	if( iDataResultPacketBody != 0 )
	{
		CNLOG( ELOG, 1, "[T:%02d %02d][DATA_SEND_ACK Packet Result Code Error][Result Code:%d][Seq:%d][R:%d][E:%d][L:%d]\n", 
			iSmdaIndex + 1, iSessionIndex + 1, iDataResultPacketBody, _stpLogPacket->stHeader.iSerialNumber, stRwError.iReturnErrCode, stRwError.iErrnoCode, __LINE__ );

		return	0;
	}

	// 6. ����!!
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

	// 1. �о���� ���Ǳ� ����(���ϰ��+�����̸�) ����
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, LOG_MANAGER_CONFIG_FILE_NAME );

	// 2. ���Ǳ� ���Ͽ��� Thread Log Type ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"THREAD_LOG_FILE_TYPE", &iLogType ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[T:%d][CNGetConfigInt( %s COMMON THREAD_LOG_FILE_TYPE %d ) Error][R:%d][L:%d]\n", 
			_iThreadIndex + 1, caConfigFile, &iLogType, iResult, __LINE__ );

		return	NULL;
	}

	// 3. ���Ǳ� ���Ͽ��� Thread Log Level ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"THREAD_LOG_FILE_LEVEL", &iLogLevel ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[T:%d][CNGetConfigInt( %s COMMON THREAD_LOG_FILE_LEVEL %d ) Error][R:%d][L:%d]\n", 
			_iThreadIndex + 1, caConfigFile, &iLogLevel, iResult, __LINE__ );

		return	NULL;
	}

	memset( caLogFilePath, 0x00, sizeof( caLogFilePath ) );

	// 4. ���Ǳ� ���Ͽ��� Thread Log Path(�α����� ���) ����
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"THREAD_LOG_FILE_PATH", caLogFilePath, sizeof(caLogFilePath) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[T:%d][CNGetConfigStr( %s COMMON THREAD_LOG_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			_iThreadIndex + 1, caConfigFile, caLogFilePath, sizeof(caLogFilePath), iResult, __LINE__ );

		return	NULL;
	}

	// 5. �α����� �̸� ����
	sprintf( caLogFileName, "%s_%02d", _cpFileName, _iThreadIndex + 1 );

	// 6. ���õ� �����ͷ� �ΰ� �ʱ�ȭ �� ���(�̶����� Tread Log ���� ����)
	if( ( stpTempHandle = CNGetLogHandle( iLogType, iLogLevel, caLogFilePath, caLogFileName, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[T:%d][CNGetLogHandle( %d %d %s %s %s ) Error][L:%d]\n", 
			_iThreadIndex + 1, iLogType, iLogLevel, caLogFilePath, caLogFileName, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A", __LINE__ );

		return	NULL;
	}

	// 7. ����!!
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

	// 1. �о���� ���Ǳ� ����(���ϰ��+�����̸�) ����
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, LOG_MANAGER_CONFIG_FILE_NAME );

	// 2. ���Ǳ� ���Ͽ��� Thread Log Type ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"THREAD_LOG_FILE_TYPE", &iLogType ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[T:%02d %02d][CNGetConfigInt( %s COMMON THREAD_LOG_FILE_TYPE %d ) Error][R:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, caConfigFile, &iLogType, iResult, __LINE__ );

		return	NULL;
	}

	// 3. ���Ǳ� ���Ͽ��� Thread Log Level ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"THREAD_LOG_FILE_LEVEL", &iLogLevel ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[T:%02d %02d][CNGetConfigInt( %s COMMON THREAD_LOG_FILE_LEVEL %d ) Error][R:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, caConfigFile, &iLogLevel, iResult, __LINE__ );

		return	NULL;
	}

	memset( caLogFilePath, 0x00, sizeof( caLogFilePath ) );

	// 4. ���Ǳ� ���Ͽ��� Thread Log Path(�α����� ���) ����
	if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"COMMON", (char*)"THREAD_LOG_FILE_PATH", caLogFilePath, sizeof(caLogFilePath) ) ) < 0 )
	{
		CNLOG( ERR_LOG, 1, "[T:%02d %02d][CNGetConfigStr( %s COMMON THREAD_LOG_FILE_PATH %d %d ) Error][R:%d][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, caConfigFile, caLogFilePath, sizeof(caLogFilePath), iResult, __LINE__ );

		return	NULL;
	}

	// 5. �α����� �̸� ����
	sprintf( caLogFileName, "%s_%02d_%02d", _cpFileName, _iSmdaIndex + 1, _iSessionIndex + 1 );

	// 6. ���õ� �����ͷ� �ΰ� �ʱ�ȭ �� ���(�̶����� Tread Log ���� ����)
	if( ( stpTempHandle = CNGetLogHandle( iLogType, iLogLevel, caLogFilePath, caLogFileName, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A" ) ) == NULL )
	{
		CNLOG( ERR_LOG, 1, "[T:%02d %02d][CNGetLogHandle( %d %d %s %s %s ) Error][L:%d]\n", 
			_iSmdaIndex + 1, _iSessionIndex + 1, iLogType, iLogLevel, caLogFilePath, caLogFileName, g_iCenterType == 0 ? (char*)"A_1A" : (char*)"B_1A", __LINE__ );

		return	NULL;
	}

	// 7. ����!!
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

	// 1. ��뷮�� üũ �� ��ũ ��� ����
	sprintf( caDiskPathBuf, "df -k %s", _cpDiskName );

	// 2. df -k ���ɾ��� ��ũ ��뷮 �κ��� �����ؼ� ������ ����
	if( ( fpDisk = popen( caDiskPathBuf , "r") ) != NULL )
	{
		while( fgets( caBuf, 512, fpDisk ) != NULL && caBuf[0] != '\n' )
		{
			iForLoop = 1;

			// 2.1 ���� �������� Token ����
			cpToken[iForLoop] = strtok_r( caBuf, " ", &cpSavePoint );

			// 2.2 df -k�� ��ũ ���� �� �ʵ带 ������ ���ʴ�� ����
			while( iForLoop < 6 )
			{
				++iForLoop;

				cpToken[iForLoop] = strtok_r( NULL, " ", &cpSavePoint );
			}
		}

		// 2.3 ��ũ ���� �ݱ�
		pclose( fpDisk );

		// 2.4 6��° �ʵ尡 ��ũ ��뷮 �̹Ƿ� ��뷮 ������ ����
		iQuantity = atoi( cpToken[5] );
	}
	else
	{
		pclose( fpDisk );
	}

	// 3. ����!!
	return	iQuantity;
}


static void CNNanoSleep( unsigned long _iNano )
{
	struct timespec	tsStartTime;
	struct timespec	tsRemainTime;

	if( _iNano <= 0 )
		return;

	// 1. Unsigned long �� ����� �ʰ��ϴ� ������ �Ѿ�� ��� ����ó��
	if( _iNano >= 4000000000 )
		_iNano	= 4000000000;

	// 2. nano sleep ���� 1000000000 �� 1���̹Ƿ� ������ �� �ʵ忡 ���� ����
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

	// 3. �߰��� ���ͳ�Ʈ�� �߻� �� ��� ���� �ð���ŭ �ٽ� Sleep�� ����
	while( ( nanosleep( &tsStartTime, &tsRemainTime ) == -1 ) && ( errno == EINTR ) )
	{
		// 3.1 ���ͷ�Ʈ�� ���� Sleep���� ���� �ð��� �ٽ� �ִ´�
		tsStartTime.tv_sec	= tsRemainTime.tv_sec;
		tsStartTime.tv_nsec	= tsRemainTime.tv_nsec;
		
		// 3.2 �����ð� ���� ����(����)�� �ʱ�ȭ
		tsRemainTime.tv_sec		= 0x00;
		tsRemainTime.tv_nsec	= 0x00;
	}

	// 4. ����!!
	return;
}


static int MakeAndOpenQueue( int _iQueueKey, int _iQueueSize )
{
	int	iQueueID;

	// 1. �̹� �ش� Key ���� Queue�� ���� �� ���
	if( ( iQueueID = CNQueueCreate( _iQueueKey ) ) < 0 )
	{
		// 1.1 Key ���� Queue�� Open(Attach)�� �Ѵ�
		if( ( iQueueID = CNQueueOpen( _iQueueKey ) ) < 0 )
		{
			return	-1;
		}
	}
	// 2. Queue�� �űԷ� �������� ���
	else
	{
		// 2.1 ���� �����Ͽ��� ��� Queue�� �ִ� ����� �����Ѵ�
		if( CNQueueSetSize( iQueueID, _iQueueSize ) < 0 )
		{
			return	-2;
		}

	}

	// 3. ����!!
	return	iQueueID;
}


static int DateCheck( time_t _tBrforeTime )
{
	time_t	tNowTime;

	struct tm	tmNowTime;
	struct tm	tmBrforeTime;

	tNowTime	= time( NULL );

	// 1. ���� �ð��� ����ð��� Struct tm ������ �����Ͽ�
	//    ���� �����Ѵ�(�񱳸� ����)
	localtime_r( &tNowTime, &tmNowTime );
	localtime_r( &_tBrforeTime, &tmBrforeTime );

	// 2. ��¥�� ���� �� ���
	if( tmNowTime.tm_mday != tmBrforeTime.tm_mday )
		return	1;

	// 3. �ð��� ���� �� ���
	if( tmNowTime.tm_hour != tmBrforeTime.tm_hour )
		return	2;

	// 4. ���� ���� �� ���
	if( tmNowTime.tm_min != tmBrforeTime.tm_min )
		return	3;
	
	// 5. �ʰ� ���� �� ���
	if( tmNowTime.tm_sec != tmBrforeTime.tm_sec )
		return	4;

	// 6. ����!!
	return	0;
}
