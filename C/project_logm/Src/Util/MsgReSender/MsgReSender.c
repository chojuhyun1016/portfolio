// �⺻
#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <unistd.h>
#include <strings.h>

// ProcessCheck ����
#include <fcntl.h>
#include <dirent.h>
#include <procfs.h>

// IPC ����
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/shm.h>

// �������� ���ϱ� ����
#include <sys/stat.h>

// �⺻ ��� ����
#include "MsgReSender.h"

// �ð��� ���ڿ� ����
#include "CNBaseTime.h"

// Config ����
#include "CNConfigRead.h"

// Signal ����
#include "CNSignalApi.h"
#include "CNSignalInit.h"

// IPC Queue ����
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

	// 1. ������ �̸��� Process�� ����ǰ� �ִ��� üũ
	if( ( iResult = ForkProcess( (char*)MSG_RESENDER_PROCESS_NAME ) ) < 0 )
	{
		fprintf( stderr, "[ForkProcess( %s ) Error][R:%d][L:%d]\n", MSG_RESENDER_PROCESS_NAME, iResult, __LINE__  );

		ProcessTerminate( iResult );
	}

	// 2. �⺻���� ���μ����� �������� Config ���Ͽ��� �о ����
	if( ( iResult = PrecessDataInit( (char*)MSG_RESENDER_CONFIG_PATH ) ) < 0 )
	{
		fprintf( stderr, "[ProcessDataInit( %s ) Error][R:%d][L:%d]\n", (char*)MSG_RESENDER_CONFIG_PATH, iResult, __LINE__  );

		ProcessTerminate( iResult );
	}

	CNLOG( H_LOG, 1, "[INSERT QUEUE START][L:%d]\n", __LINE__  );

	// 3. Signal Handler ���(�ñ׳� ó��)
	if( ( iResult = CNInitSignal( SignalHandler ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[CNInitSignal( %d ) Error][R:%d][L:%d]\n", SignalHandler, iResult, __LINE__ );

		ProcessTerminate( iResult );
	}

	// 4. SMDA Queue�� ���� �����͸� �о ����
	if( ( iResult = SmdaQueueDataInit( (char*)MSG_RESENDER_CONFIG_PATH ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[SmdaQueueDataInit( %s ) Error][R:%d][L:%d]\n", (char*)MSG_RESENDER_CONFIG_PATH, iResult, __LINE__  );

		ProcessTerminate( iResult );
	}

	// 5. SMDA Routing�� ���� �����͸� �о ����
	if( ( iResult = SmdaRoutingDataInit( (char*)MSG_RESENDER_CONFIG_PATH ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[SmdaRoutingDataInit( %s ) Error][R:%d][L:%d]\n", (char*)SMDA_ROUTING_INFO_FILE, iResult, __LINE__  );

		ProcessTerminate( iResult );
	}

	// 6. ��ó�� ���Ͽ��� �����͸� �о ��ó��
	if( ( iResult = ReInsertQueueData( g_cpDataPath, g_cpMovePath, g_cpFinPath ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[ReSendQueueData( %s %s %s ) Error][R:%d][L:%d]\n", 
			g_cpDataPath, g_cpMovePath, g_cpFinPath, iResult, __LINE__  );

		ProcessTerminate( iResult );
	}

	CNLOG( H_LOG, 1, "[INSERT QUEUE END][L:%d]\n", __LINE__  );

	// 7. ���μ��� ����!!
	ProcessTerminate( 0 );
}


void SignalHandler( int _iSignal )
{
	// 1. ���� �ñ׳� ������ ���
	CNLOG( H_LOG, 1, "[Signal Receive: %d][L:%d]\n", _iSignal, __LINE__ );

	// 2. ���μ��� ������� ����!!
	ProcessTerminate( _iSignal );
}


void ProcessTerminate( int _iReturnCode )
{
	// 1. Routing ���̺� ������ ����
	SmdaRoutingDataFree();

	// 2. Queue ���̺� ������ ����
	SmdaQueueDataFree();

	// 3. �α� ���̺� ����
	CNDeleteHandle( H_LOG );

	// 4. ���������� ��� ����
	if( g_cpDataPath != NULL )
		free( g_cpDataPath );

	// 5. ���������� �̵� ��� ����
	if( g_cpDataPath != NULL )
		free( g_cpDataPath );

	// 6. ���������� ó�� ���� ��� ����
	if( g_cpDataPath != NULL )
		free( g_cpDataPath );

	// 7. ���μ��� ����!!
	exit( _iReturnCode );
}


int	ForkProcess( char* _cpProcessName )
{
	int		iResult			=	0;
	int		iChildPid		=	0;

	// 1. ������ ���μ����� �̹� ���ִ��� üũ, �������� �׳� ����
	if( ( iResult = ProcessCheck( _cpProcessName ) ) != 1 )
	{
		fprintf( stderr, "[ProcessCheck() Error][R:%d][L:%d]\n", iResult, __LINE__ );

		return	-1;
	}

	// 2. ���μ����� ��׶���� �����ϵ��� ��ũ
	if( ( iChildPid = fork() ) < 0 )
	{
		fprintf( stderr, "[fork() Error][R:%d][L:%d]\n", iChildPid, __LINE__ );

		return	-1;
	}

	// 3. �θ� ���μ����� �״´�, �ڽ����μ����� �����͸� ó��
	if( iChildPid > 0 )
		exit( 0 );

	// 4. ����!!
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

	// 1. ���μ���(/proc) ���丮 ������ �о�´�
	if( ( dpDirPoint = opendir( "/proc" ) ) == NULL )
	{
		fprintf( stderr, "[opendir( /proc) Error][E:%d][L:%d]\n", errno, __LINE__ );

		return	-1;
	}

	// 2. ���μ��� ���丮�� ������ ���� �̸��� ���μ����� ���������� Ȯ��
	//    UNIX�� ��� ���μ����� ���� �ɰ�� (/proc)���丮 �ȿ� �ش� ���μ���
	//    �� PID(���μ���ID)�� �ش��ϴ� ���丮�� �����Ǹ� �׾ȿ��� �� ���μ���
	//    �� ���� ������ ����ȴ�. �� ������ �������ν� �������� ���μ�����
	//    ������ Ȯ���� �� �ִ�.
	while( 1 )
	{
		// 2.1 ���μ���(/proc) ���丮 ���� ����
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

		// 2.2 �ش� PID�� ������ ���μ����� ������ �����ϴ� ������ ����
		if( ( iFileFd = open( caProcFile, O_RDONLY ) ) < 0 )
			continue;

		// 2.4 �ش� PID�� ���μ����� ������ ����
		if( read( iFileFd, (void *)&stPinfo, sizeof( psinfo_t ) ) <= 0 )
		{
			close( iFileFd );

			continue;
		}

		// 2.5 ������ ���μ����� �̸��� �������� ���μ��� �̸��� �������� ��
		if( strcmp( _cpProcessName ,stPinfo.pr_fname ) == 0 )
			iProcessCount++;

		close( iFileFd );
	}

	closedir( dpDirPoint );

	// 3. �������� ���� �̸��� ���μ��� �� ����!!
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

	// 1. �ּ��� �μ��� ���� ����
	if( ( iResult = CNGetConfigInt( caCenterTypeFile, (char*)"COMMON", (char*)"CENTER_TYPE", &iCenterType ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigInt( %s COMMON CENTER_TYPE %d ) Error][R:%d][L:%d]\n", 
			caCenterTypeFile, &iCenterType, iResult, __LINE__ );

		return	-1;
	}

	// 2. ��ó�� ������ ���� ��� ����
	memset( caTempBuffer, 0x00, sizeof( caTempBuffer ) );

	if( ( iResult = CNGetConfigStr( caMsgResenderConfigFile, (char*)"COMMON", (char*)"DATA_DIRECTORY", caTempBuffer, sizeof(caTempBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON DATA_DIRECTORY %d %d ) Error][R:%d][L:%d]\n", 
			caMsgResenderConfigFile, caTempBuffer, sizeof( caTempBuffer ), iResult, __LINE__ );

		return	-1;
	}

	g_cpDataPath	= (char*)malloc( strlen( caTempBuffer ) + 1 );
	memcpy( g_cpDataPath, caTempBuffer, strlen( caTempBuffer ) );

	// 3. ��ó�� ������ ���� ��� ����
	memset( caTempBuffer, 0x00, sizeof( caTempBuffer ) );

	if( ( iResult = CNGetConfigStr( caMsgResenderConfigFile, (char*)"COMMON", (char*)"MOVE_DIRECTORY", caTempBuffer, sizeof(caTempBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON MOVE_DIRECTORY %d %d ) Error][R:%d][L:%d]\n", 
			caMsgResenderConfigFile, caTempBuffer, sizeof( caTempBuffer ), iResult, __LINE__ );

		return	-1;
	}

	g_cpMovePath	= (char*)malloc( strlen( caTempBuffer ) + 1 );
	memcpy( g_cpMovePath, caTempBuffer, strlen( caTempBuffer ) );

	// 4. ��ó�� ������ ���� ��� ����
	memset( caTempBuffer, 0x00, sizeof( caTempBuffer ) );

	if( ( iResult = CNGetConfigStr( caMsgResenderConfigFile, (char*)"COMMON", (char*)"FIN_DIRECTORY", caTempBuffer, sizeof(caTempBuffer) ) ) < 0 )
	{
		fprintf( stderr, "[CNGetConfigStr( %s COMMON FIN_DIRECTORY %d %d ) Error][R:%d][L:%d]\n", 
			caMsgResenderConfigFile, caTempBuffer, sizeof( caTempBuffer ), iResult, __LINE__ );

		return	-1;
	}

	g_cpFinPath	= (char*)malloc( strlen( caTempBuffer ) + 1 );
	memcpy( g_cpFinPath, caTempBuffer, strlen( caTempBuffer ) );

	// 6. �ΰ� �ʱ�ȭ �� ���
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

	// 7. ����!!
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

	// 1. SmdaQueue ����ü �ɹ� �ʱ�ȭ
	g_stSmdaQueueTable.iSmdaServerNum	= 0;
	g_stSmdaQueueTable.Info		= NULL;

	// 2. �о���� Config ����(SmdaQueue Config File)�̸� ����
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, SMDA_QUEUE_INFO_FILE );

	// 3. ���Ǳ� ���Ͽ��� SMDA ���� ������ �о ����	
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"COMMON", (char*)"SMDA_SERVER_NUM", &iSmdaServerNum ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[CNGetConfigInt( %s SMDA_QUEUE SMDA_QUEUE_NUM %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iSmdaServerNum, iResult, __LINE__ );

		return	-1;
	}

	// 4. SMDA �������� �б� ����ó��
	if( iSmdaServerNum <= 0 )
	{
		CNLOG( H_LOG, 1, "[Read Format Error][SMDA_QUEUE_NUM: %d][L:%d]\n", iSmdaServerNum, __LINE__ );

		return	-1;
	}

	CNLOG( H_LOG, 3, "[SMDA_SERVER_NUM: %d][L:%d]\n", iSmdaServerNum, __LINE__ );

	// 5. SMDA ���� ���ڸ�ŭ SMDA ����� ���� ���̺� ������ �Ҵ�
	g_stSmdaQueueTable.iSmdaServerNum	= iSmdaServerNum;
	g_stSmdaQueueTable.Info	= (stSmdaQueueInfo*)malloc( sizeof(stSmdaQueueInfo) * g_stSmdaQueueTable.iSmdaServerNum );

	// 6. SMDA ���� ����� ���̺� �޸��Ҵ� ����ó��
	if( g_stSmdaQueueTable.Info == NULL )
	{
		CNLOG( H_LOG, 1, "[malloc( %d ) Error][E:%d][L:%d]\n", 
			sizeof(stSmdaQueueInfo) * g_stSmdaQueueTable.iSmdaServerNum, errno, __LINE__ );

		return	-1;
	}

	memset( g_stSmdaQueueTable.Info, 0x00, sizeof(stSmdaQueueInfo) * g_stSmdaQueueTable.iSmdaServerNum );

	// 7. ���Ǳ� ���Ͽ��� SMDA ����� ���̺� ������ �о���̰� ������ �ʱ�ȭ
	for( iSmdaLoop = 0; iSmdaLoop < g_stSmdaQueueTable.iSmdaServerNum; iSmdaLoop++ )
	{
		sprintf( caPartBuffer1, "SMDA_%d", iSmdaLoop + 1 );

		// 7.1 SMDA Session ���� ���� �о���δ�
		if( ( iResult = CNGetConfigInt( caConfigFile, caPartBuffer1, (char*)"SESSION_NUM", &iSmdaSessionNum ) ) < 0 )
		{
			CNLOG( H_LOG, 1, "[CNGetConfigInt( %s %s SESSION_NUM %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer1, &iSmdaSessionNum, iResult, __LINE__ );

			return	-1;
		}

		// 7.2 SMDA Session ���� �б� ����ó��
		if( iSmdaSessionNum <= 0 )
		{
			CNLOG( H_LOG, 1, "[Read Format Error][SESSION_NUM: %d][L:%d]\n", iSmdaSessionNum, __LINE__ );

			return	-1;
		}

		// 7.3 ���� ������ŭ �������� ���� �޸� ���� �Ҵ�
		g_stSmdaQueueTable.Info[iSmdaLoop].iSessionNum	= iSmdaSessionNum;
		g_stSmdaQueueTable.Info[iSmdaLoop].stpQueue	= (stSmdaQueue*)malloc( sizeof(stSmdaQueue) * g_stSmdaQueueTable.Info[iSmdaLoop].iSessionNum );

		// 7.4 �������� ���� ���� �޸� �Ҵ� ����ó��
		if( g_stSmdaQueueTable.Info[iSmdaLoop].stpQueue == NULL )
		{
			CNLOG( H_LOG, 1, "[malloc( %d ) Error][E:%d][L:%d]\n", 
				sizeof( stSmdaQueue ) * g_stSmdaQueueTable.Info[iSmdaLoop].iSessionNum, errno, __LINE__ );

			return	-1;
		}

		memset( g_stSmdaQueueTable.Info[iSmdaLoop].stpQueue, 0x00, sizeof(stSmdaQueue) * g_stSmdaQueueTable.Info[iSmdaLoop].iSessionNum );

		// 7.5 ���� ������ŭ �Ҵ�� SMDA ����� ���̺� �޸� ������ ���Ǳ׿��� �����͸� �о ���� �� Queue ����
		for( iSessionLoop = 0; iSessionLoop < g_stSmdaQueueTable.Info[iSmdaLoop].iSessionNum; iSessionLoop++ )
		{
			sprintf( caPartBuffer2, "SMDA_QUEUE_KEY_%d", iSessionLoop + 1 );

			// 7.5.1 Smda Queue Key ���� �о���δ�
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

			// 7.5.3 ����� ���̺� ���� ����
			g_stSmdaQueueTable.Info[iSmdaLoop].stpQueue[iSessionLoop].iQueueKey	= iSmdaQueueKey;
			g_stSmdaQueueTable.Info[iSmdaLoop].stpQueue[iSessionLoop].iQueueID	= iSmdaQueueID;
		}
	}

	// 8. ����!!
	return	0;
}


void SmdaQueueDataFree()
{
	int	iForLoop;

	// 1. Heap ������ �Ҵ��� SMDA Queue ���� ����
	if( g_stSmdaQueueTable.Info != NULL )
	{
		for( iForLoop = 0; iForLoop < g_stSmdaQueueTable.iSmdaServerNum; iForLoop++ )
		{
			if( g_stSmdaQueueTable.Info[iForLoop].stpQueue != NULL )
				free( g_stSmdaQueueTable.Info[iForLoop].stpQueue );
		}

		free( g_stSmdaQueueTable.Info );
	}

	// 2. ���� �ʱ�ȭ
	g_stSmdaQueueTable.Info				= NULL;
	g_stSmdaQueueTable.iSmdaServerNum	= 0;

	// 3. ����!!
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

	// 1. Smart Routing ���̺� �ʱ�ȭ
	g_stSmdaRouting.iSmdaServerNum	= 0;
	g_stSmdaRouting.Index			= NULL;

	// 2. SMDA Routing �����͸� �о���� Config ������ ���ϸ� ����
	sprintf( caConfigFile, "%s/%s", _cpConfigFilePath, SMDA_ROUTING_INFO_FILE );

	// 3. ����ϴ� Routing Prefix ���� ����
	if( ( iResult = CNGetConfigInt( caConfigFile, (char*)"SMDA_ROUTING", (char*)"SMDA_ROUTING_NUM", &iSmartRoutingNum ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING SMDA_ROUTING_NUM %d ) Error][R:%d][L:%d]\n", 
			caConfigFile, &iSmartRoutingNum, iResult, __LINE__ );

		return	-1;
	}

	g_stSmdaRouting.iSmdaServerNum	= iSmartRoutingNum;

	CNLOG( H_LOG, 3, "[SMDA_ROUTING_NUM: %d][L:%d]\n", g_stSmdaRouting.iSmdaServerNum, __LINE__ );

	// 3. ETC Routing Queue Number(��ϵ��� ���� �뿪�� �����Ͱ� ���� ��� �����͸� ���� �� Queue�� Number) ����
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

	// 4. �ӽ÷� Prefix �����͸� ���� �� ���� �Ҵ�(������ �������� ������ �����)
	g_stSmdaRouting.Index = (int*)malloc( sizeof(int) * 100000 );

	for( iForLoop = 0; iForLoop < 100000; iForLoop++ )
		g_stSmdaRouting.Index[iForLoop]	= iEtcRoutingQueue - 1;

	// 5. ���Ǳ� ���Ͽ��� Prefix ������ �о �ӽ� ��������� ���
	for( iForLoop = 1; iForLoop < g_stSmdaRouting.iSmdaServerNum + 1;iForLoop++ )
	{
		// 5.1 ���� ����� ��ȣ �뿪 ����(������� 5�ڸ��� �����)
		sprintf( caPartBuffer, "SMDA_ROUTING_STR_%d", iForLoop );

		if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"SMDA_ROUTING", caPartBuffer, caStartPrefix, sizeof(caStartPrefix) ) ) < 0 )
		{
			CNLOG( H_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING %s %d %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer, caStartPrefix, sizeof(caStartPrefix), iResult, __LINE__ );

			return	-1;
		}

		iStartPrefix = atoi( caStartPrefix );

		sprintf( caPartBuffer, "SMDA_ROUTING_END_%d", iForLoop );

		// 5.2 �� ����� ��ȣ �뿪 ����(������� 5�ڸ��� �����)
		if( ( iResult = CNGetConfigStr( caConfigFile, (char*)"SMDA_ROUTING", caPartBuffer, caEndPrefix, sizeof(caEndPrefix) ) ) < 0 )
		{
			CNLOG( H_LOG, 1, "[CNGetConfigInt( %s SMDA_ROUTING %s %d %d ) Error][R:%d][L:%d]\n", 
				caConfigFile, caPartBuffer, caEndPrefix, sizeof(caEndPrefix), iResult, __LINE__ );

			return	-1;
		}

		iEndPrefix = atoi( caEndPrefix );

		sprintf( caPartBuffer, "SMDA_ROUTING_QUEUE_%d", iForLoop );

		// 5.3 ���ۿ��� �� ����� �뿪������ �����͸� ���� �� Queue�� Number(Queue Index)
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

		// 5.4 �о���� �����ͷ� ����� ���̺�(Smart Routing Table)�� ����
		for ( iPrefixLoop = iStartPrefix; iPrefixLoop < iEndPrefix + 1; iPrefixLoop++ )
			g_stSmdaRouting.Index[iPrefixLoop] = iRoutingQueue - 1;
	}

	// 9. ����!!
	return	0;
}


void SmdaRoutingDataFree()
{
	// 1. Heap ������ �Ҵ� �� Routing ���� ����!!
	if( g_stSmdaRouting.Index != NULL )
		free( g_stSmdaRouting.Index );

	// 2. ����ü ���� �ʱ�ȭ
	g_stSmdaRouting.Index			= NULL;
	g_stSmdaRouting.iSmdaServerNum	= 0;

	// 3. ����!!
	return;
}


int ReInsertQueueData( char* _cpDataFilePath, char* _cpDataFileMovePath, char* _cpDataFileFinPath )
{
	int	iResult;

	char	caDataFileName[1024];

	struct dirent*	stpDirData		=	NULL;
	DIR*			dpDirPoint		=	NULL;

	// 1. ��ó�� Data ������ ã�Ƽ� ó�� ���丮�� ������ �̵�
	if( ( iResult = OnDataFileMove( _cpDataFilePath, _cpDataFileMovePath ) ) < 0 )
	{
		CNLOG( H_LOG, 1, "[OnDataFileMove( %s %s ) Error][R:%d][L:%d]\n", 
			_cpDataFilePath, _cpDataFileMovePath, iResult, __LINE__ );

		return	-1;
	}

	sleep( 5 );

	// 2. ó�� ���丮�� ���� ������ �о���δ�
	if( ( dpDirPoint = opendir( _cpDataFileMovePath ) ) == NULL )
	{
		CNLOG( H_LOG, 1, "[opendir( %s) Error][E:%d][L:%d]\n", _cpDataFileMovePath, errno, __LINE__ );

		return	-1;
	}

	// 2. �ش� ��ο� ��ó�� ����(xxx.dat)�� ������� ��ó�� ����
	while( 1 )
	{
		// 2.1 �Ѷ��ξ� �ش� ����� ���� ������ �о����
		stpDirData = readdir( dpDirPoint );

		// 2.2 �� �о����� ����
		if( !stpDirData )
			break;

		// 2.3 ���ϵ��� �ƴѰ�� continue
		if( strcmp( stpDirData->d_name, "." ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, ".." ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, "0" ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, "1" ) == 0 )
			continue;

		// 2.5 ���̰� 4���� ������� �Ѿ(xxx.dat�̹Ƿ� .dat�� �ּ� 4�ڸ�)
		if( strlen( stpDirData->d_name ) < 4 )
			continue;

		// 2.6 ���̰� 4�ڸ� �̻��̰� ��ó�� ����(.dat�� ������ ����)�� ��� ��ó�� ����
		if( strncmp( stpDirData->d_name + ( strlen( stpDirData->d_name ) ) - 4, ".dat", 4 ) == 0 )
		{
			// 2.6.1 �����̸� �տ� ���� ��α��� �ٿ��� ������ ���Ϸ� �����
			sprintf( caDataFileName, "%s/%s", _cpDataFileMovePath, stpDirData->d_name );

			CNLOG( H_LOG, 1, "[%s File Start][L:%d]\n", caDataFileName, __LINE__ );

			// 2.6.2 OnFindDataFile()�� �����ؼ� ��ó���� ����
			if( ( iResult = OnDataFileInsert( caDataFileName ) ) < 0 )
			{
				CNLOG( H_LOG, 1, "[OnDataFileInsert( %s ) Error][R:%d][L:%d]\n", 
					caDataFileName, iResult, __LINE__ );
			}

			// 2.6.3 ó���� ���� Data ������ Fin ���丮�� �̵�
			if( ( iResult = OnDataFileFinish( caDataFileName, _cpDataFileFinPath ) ) < 0 )
			{
				CNLOG( H_LOG, 1, "[OnDataFileFinish( %s %s ) Error][R:%d][L:%d]\n", 
					caDataFileName, _cpDataFileFinPath, iResult, __LINE__ );
			}

			CNLOG( H_LOG, 1, "[%s File End][L:%d]\n", caDataFileName, __LINE__ );		
		}
	}

	closedir( dpDirPoint );

	// 3. ����!!
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

	// 1. ���μ��� Argument�� ���� ��ο� �ִ� ���� ������ �о���δ�
	if( ( dpDirPoint = opendir( _cpDataFilePath ) ) == NULL )
	{
		CNLOG( H_LOG, 1, "[opendir( %s) Error][E:%d][L:%d]\n", _cpDataFilePath, errno, __LINE__ );

		return	-1;
	}

	// 2. �ش� ��ο� ��ó�� ����(xxx.dat)�� ������� �����̵� ����
	while( 1 )
	{
		// 2.1 �Ѷ��ξ� �ش� ����� ���� ������ �о����
		stpDirData = readdir( dpDirPoint );

		// 2.2 �� �о����� ����
		if( !stpDirData )
			break;

		// 2.3 ���ϵ��� �ƴѰ�� continue
		if( strcmp( stpDirData->d_name, "." ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, ".." ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, "0" ) == 0 )
			continue;

		if( strcmp( stpDirData->d_name, "1" ) == 0 )
			continue;

		// 2.4 ���̰� 4���� ������� �Ѿ(xxx.dat�̹Ƿ� .dat�� �ּ� 4�ڸ�)
		if( strlen( stpDirData->d_name ) < 4 )
			continue;

		// 2.5 ���̰� 4�ڸ� �̻��̰� ��ó�� ����(.dat�� ������ ����)�� ��� �����̵�
		if( strncmp( stpDirData->d_name + ( strlen( stpDirData->d_name ) - 4 ), ".dat", 4 ) == 0 )
		{
			// 2.6.1 �����̸� �տ� ���� ��α��� �ٿ��� ������ ���Ϸ� �����
			sprintf( caMoveFileName, "%s/%s", _cpDataFilePath, stpDirData->d_name );

			// 2.6.2 Data������ �̵���Ű�� ���ɾ� ����
			sprintf( caDataFileMoveCommand, "mv %s %s", caMoveFileName, _cpDataFileMovePath );

			// 2.6.3 Data������ ó�� ���丮�� �̵�
			system( caDataFileMoveCommand );

			CNLOG( H_LOG, 1, "[%s][L:%d]\n", caDataFileMoveCommand, __LINE__ );
		}
	}

	closedir( dpDirPoint );

	// 3. ����!!
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

	// 1. ������ �ʱ�ȭ
	iPacketSize	= sizeof( stLogPacket ) - sizeof( long );

	// 2. Data���� Error���� Report ���� �̸� ����
	strlcpy( caDataFile, _cpDataFile, 1024 );
	sprintf( caReportFile, "%s.log", caDataFile );
	sprintf( caErrorDataFile, "%s.err", caDataFile );

	// 3. Data ������ ����
	if( ( fpDataFile = fopen( caDataFile, "r" ) ) == NULL )
	{
		OnWriteReport( caReportFile , "[fopen( %s, r ) Error][E:%d][L:%d]\n", caDataFile, errno, __LINE__ );

		return	-1;
	}

	iSecToCount	= 0;
	tCheckTime	= time( NULL );

	// 4. Data ���� ������ �����ؼ� Queue�� Insert �۾��� ����
	while( 1 )
	{
		// 4.1 Queue �����ϸ� ���̱� ���� �ʴ� ó���Ǽ� ������ �д�(�ʴ�250)
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

		// 4.2 Data���Ͽ��� Packet �����͸� �ѰǾ� �о���δ�
		if( fread( &stTempPacket, sizeof( stLogPacket ), 1, fpDataFile ) != 1 )
			break;

		iReadCount++;
		iSecToCount++;

		// 4.3 Routing �뿪(���� Queue Index)�� ���Ѵ�
		memset( caTempRouting, 0x00, sizeof( caTempRouting ) );
		memcpy( caTempRouting, stTempPacket.caRoutingNumber, 5 );

		iPrefixIndex	= atoi( caTempRouting );
		iSmdaIndex		= g_stSmdaRouting.Index[iPrefixIndex];
		iSessionIndex	= (++g_stSmdaQueueTable.Info[iSmdaIndex].iMsgCount) % g_stSmdaQueueTable.Info[iSmdaIndex].iSessionNum;

		// 4.4 LOG FILE TYPE(����Ʈ ��������)�� 1�� �ƴѰ�� ���۸޽����� �ƴϹǷ� �Ѿ��
		if( stTempPacket.iLogFileType != 1 || iPrefixIndex == 0 )
		{
			iNotInsertCount++;

			continue;
		}

		// 4.5 Routing �� �ش��ϴ� Queue�� Data Insert
		iResult	= CNQueueWrite( g_stSmdaQueueTable.Info[iSmdaIndex].stpQueue[iSessionIndex].iQueueID, &stTempPacket, iPacketSize );

		switch( iResult )
		{
			// 4.5.1 ���������� ���� �� ���
			case CN_IPC_QUEUE_DATA_WRITE :
				
				iInsertCount++;

				break;

			// 4.5.2 Queue�� �����Ͱ� ������ ���
			case CN_IPC_QUEUE_DATA_FULL :

				// 4.5.2.1 �����α� ���
				OnWriteReport( caReportFile , "[CN_IPC_QUEUE_DATA_FULL][Seq: %d][R:%d][E:%d][L:%d]\n", 
					stTempPacket.stHeader.iSerialNumber, iResult, errno, __LINE__ );

				// 4.5.2.2 �������� ���
				OnWriteErrorFile( caReportFile, caErrorDataFile, &stTempPacket );

				// 4.5.2.3 �������
				iErrorCount++;

				break;

			// 4.5.2 ��Ÿ ������ Queue Insert�� ���� �� ���
			default :

				// 4.5.2.1 �����α� ���
				OnWriteReport( caReportFile , "[CN_IPC_QUEUE_DATA_ERROR][Seq: %d][R:%d][E:%d][L:%d]\n", 
					stTempPacket.stHeader.iSerialNumber, iResult, errno, __LINE__ );

				// 4.5.2.2 �������� ���
				OnWriteErrorFile( caReportFile, caErrorDataFile, &stTempPacket );

				// 4.5.2.3 �������
				iErrorCount++;

				break;
		}
	}

	fclose( fpDataFile );

	// 5. �� ���Ͽ� ���ؼ� ó���� �����ٸ� �ش������� ����Ʈ ���ϵ����͸� ����
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

	// 6. ����!!
	return	0;
}


int OnDataFileFinish( char* _cpDataFile, char* _cpDataFileFinPath )
{
	char	caDataFile[512];
	char	caReportFile[512];
	char	caErrorDataFile[512];

	char	caMoveCommand[1024];

	// 1. Data���� Error���� Report���� �̸� ����
	strlcpy( caDataFile, _cpDataFile, 512 );
	sprintf( caReportFile, "%s.log", caDataFile );
	sprintf( caErrorDataFile, "%s.err", caDataFile );

	// 2. Data���� Fin ���丮�� �̵�
	sprintf( caMoveCommand, "mv %s %s", caDataFile, _cpDataFileFinPath );

	CNLOG( H_LOG, 1, "[%s][L:%d]\n", caMoveCommand, __LINE__ );
	//OnWriteReport( caReportFile ,"[%s][L:%d]\n", caMoveCommand, __LINE__ );

	system( caMoveCommand );

	// 3. Error���� Fin ���丮�� �̵�
	sprintf( caMoveCommand, "mv %s %s", caErrorDataFile, _cpDataFileFinPath );

	CNLOG( H_LOG, 1, "[%s][L:%d]\n", caMoveCommand, __LINE__ );
	//OnWriteReport( caReportFile ,"[%s][L:%d]\n", caMoveCommand, __LINE__ );

	system( caMoveCommand );

	// 4. Report���� Fin ���丮�� �̵�
	sprintf( caMoveCommand, "mv %s %s", caReportFile, _cpDataFileFinPath );

	CNLOG( H_LOG, 1, "[%s][L:%d]\n", caMoveCommand, __LINE__ );
	//OnWriteReport( caReportFile ,"[%s][L:%d]\n", caMoveCommand, __LINE__ );

	system( caMoveCommand );

	// 5. ����!!
	return	0;
}


int OnWriteErrorFile( char* _cpReportFile, char* _cpErrorFileName, stLogPacket* _stpErrorPacket )
{
	int		iResult;

	FILE*	fpErrorFile;

	// 1. Error �����͸� ��� �� ������ ����
	if( ( fpErrorFile = fopen( _cpErrorFileName, "a+" ) ) == NULL )
	{
		OnWriteReport( _cpReportFile , "[fopen( %s, a+ ) Error][E:%d][L:%d]\n", _cpErrorFileName, errno, __LINE__ );

		return	-1;
	}

	// 2. Error ���� ���Ͽ� ���
	if( fwrite( _stpErrorPacket, sizeof( stLogPacket ), 1, fpErrorFile ) != 1 )
	{
		OnWriteReport( _cpReportFile , "[fwrite( %s %d 1 %d ) Error][Seq:%d][E:%d][L:%d]\n", 
			_stpErrorPacket, sizeof( stLogPacket ), fpErrorFile, _stpErrorPacket->stHeader.iSerialNumber, errno, __LINE__ );

		fclose( fpErrorFile );

		return	-1;
	}

	// 3. ������ �ݴ´�
	fclose( fpErrorFile );

	// 4. ����!!
	return	0;
}


void OnWriteReport( char* _cpLogFile, const char* fmt, ... )
{
	time_t		tTime;

	char		cpDate[32];

	FILE		*fpLog;

	va_list		vaArgs;

	va_start( vaArgs, fmt );

	// 1. ���� �ð��� ���ڿ��� �����ؼ� ������ �����Ѵ�
	tTime = time( ( time_t ) NULL );
	memset( cpDate, 0x00, sizeof( cpDate ) );
	GetNowTimeFormatStr( cpDate, (char*)"[YYYY/MM/DD:hh:mm:ss]" );
	
	// 2. Report ���� ���� ������ ���
	if( ( fpLog = fopen( _cpLogFile, "a+" ) ) == NULL )
	{
		// 2.1 ȭ�鿡 �ð�+�α׵����� ���
		fprintf( stderr, "%s ", cpDate );
		vfprintf( stderr, fmt, vaArgs );
		fflush( stderr );
	}
	// 3. Report ���� ���� ������ ���
	else
	{
		// 3.1 ���Ͽ� �ð�+�α׵����� ���
		//     �̱� ������ �����̹Ƿ� ���� ����ص�
		//     �������
		fprintf( fpLog, "%s ", cpDate );
		vfprintf( fpLog, fmt, vaArgs );
		fclose( fpLog );
	}

	// 4. �������� ��� ����
	va_end( vaArgs );

	// 5. ����!!
	return;
}
