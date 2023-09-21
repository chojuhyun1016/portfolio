#ifndef __LOG_MANAGER_H__
#define __LOG_MANAGER_H__

#include <errno.h>
#include <pthread.h>

#include "CNIpcMsgQueue.h"

#include "CNSocketApi.h"
#include "CNSocketClientTCP.h"
#include "CNSocketUtil.h"

// Log 관련
#include "CNLogWrite.h"

// Linked List 관련
#include "CNLinkedList.h"

#ifdef __cplusplus
extern "C" {
#endif

#define LOG_MANAGER_PROCESS_NAME		"LOG_MANAGER"
#define LOG_MANAGER_CONFIG_FILE_NAME	"LogManager.cfg"
#define LOG_QUEUE_INFO_FILE_NAME		"LogQueueInfo.cfg"
#define SMDA_QUEUE_INFO_FILE_NAME		"SmdaQueueInfo.cfg"
#define SMDA_ROUTING_INFO_FILE_NAME		"SmdaRoutingInfo.cfg"

#define BIND				0x0010
#define BIND_REPONSE		0x0011
#define SYS_QUERY			0x0020
#define SYS_QUERY_REPONSE	0x0021
#define DATA_SEND			0x0030
#define DATA_SEND_ACK		0x0031

#define LOG_QUEUE_LOG_FILE_NAME			"QUEUE"
#define LOG_QUEUE_ERROR_LOG_FILE_NAME	"ERR_QUEUE"
#define SMDA_QUEUE_LOG_FILE_NAME		"SMDA"
#define SMDA_QUEUE_ERROR_LOG_FILE_NAME	"ERR_SMDA"

#define SYSTEM_QUERY_TIME_CYCLE			600

#define MAX_SMDA_REPROCESS_DATA_SIZE	10000
#define MAX_LOG_REPROCESS_DATA_SIZE		10000

typedef struct
{
	int		iThreadIndex;
	int		iQueueKey;

} stLogThreadInfo;


typedef struct
{
	int	iUseNum;

	stLogThreadInfo	*Info;

} stLogThread;


typedef struct
{
	pthread_mutex_t	Lock;

	int		iQueueKey;
	int		iQueueID;

} stSmdaQueue;


typedef struct
{
	#ifdef OS508
	pthread_mutex_t	Lock;
	#endif

	volatile unsigned int	iMessageCount;

	int		iSmdaSessionNum;

	int		iPort;

	char	caAdress[16];
	char	caBindID[16];
	char	caBindPW[16];

	stSmdaQueue	*stpQueue;

} stSmdaThreadInfo;


typedef struct
{
	int	iSmdaServerNum;

	stSmdaThreadInfo	*Info;

} stSmdaThread;


typedef struct
{
	int	iUseNum;

	int	*Index;

} stSmdaRouting;


typedef struct
{
	int		iThreadIndex;

	char	*cpConfigPath;

	CN_LOG_HANDLE	NLOG;
	CN_LOG_HANDLE	ELOG;

} stLogThreadArgument;


typedef struct
{
	int		iSmdaIndex;
	int		iSessionIndex;
	int		iEmergencyFilingFlag;

	char	*cpConfigPath;
	char	*cpEmergencyTime;

	stNode	List;

	CN_LOG_HANDLE	NLOG;
	CN_LOG_HANDLE	ELOG;

} stSmdaThreadArgument;


typedef struct
{
	unsigned short	iSystem;
	unsigned short	iMsgType;

	unsigned short	iVersion;
	unsigned short	iBodyLength;

	unsigned int	iSerialNumber;

} stLogPacketHeader;


typedef struct
{
	long	iQueueMsgType;

	int		iLogFileType;

	char	caLogFileName[128];
	char	caRoutingNumber[32];

	stLogPacketHeader	stHeader;

	char	caBody[512];

} stLogPacket;


typedef struct
{
	char	caBindID[16];
	char	caBindPass[16];

} stSmdaBind;


typedef struct
{
	int	iResult;

} stSmdaBindRes;


typedef struct
{
	int	iSequence;

} stSmdaSysQuery;


typedef struct
{
	int	iResult;

} stSmdaSysQueryRes;


int main( int argc, char **argv );

void SignalHandler( int _iSignal );
void ProcessTerminate( int _iReturnCode );

int	ProcessCheck( char* _cpProcessName );

int ProcessDataInit( char* _cpConfigFilePath );
int	EmergencyTableInit( char* _cpConfigFilePath );
int LogQueueDataInit( char* _cpConfigFilePath );
int SmdaQueueDataInit( char* _cpConfigFilePath );
int SmdaRoutingDataInit( char* _cpConfigFilePath );

void LogQueueDataFree();
void SmdaQueueDataFree();
void SmdaRoutingDataFree();

int ReadSequenceFile();
int WriteSequenceFile();

int MakeSmdaThread( char* _cpConfigFilePath );
int MakeLogThread( char* _cpConfigFilePath );

int OnLogThreadStart( void* _vpArg );
int OnLogThreadRun( void* _vpArg );
int OnLogThreadTerminate( void* _vpArg );

int OnLogMsgRead( int _iThreadIndex, stLogPacket* _stpLogPacket, int _iPacketSize, stLogThreadArgument* stpLogArg );
int OnLogMsgEmpty();
int OnLogMsgError();
int OnLogMsgReadError( stLogPacket* _stpLogPacket, stLogThreadArgument* _stpThreadArg );
int OnWriteLogThreadError( stLogPacket* _stpLogPacket, stLogThreadArgument* _stpThreadArg );

int OnSmdaThreadStart( void* _vpArg );
int OnSmdaThreadRun( void* _vpArg );
int OnSmdaThreadTerminate( void* _vpArg );
int OnSmdaMsgRead( int _iSmdaIndex, int _iSessionIndex, int _iSocketFD, stLogPacket* _stpLogPacket, stSmdaThreadArgument* _stpThreadArg, time_t* _tpQueryTime );
int OnSmdaMsgEmpty();
int OnSmdaMsgError();
int OnSmdaMsgReadError( stLogPacket* _stpLogPacket, stSmdaThreadArgument* _stpThreadArg );
int OnSmdaQueueClear( stSmdaThreadArgument* _stpThreadArg );
int OnWriteSmdaThreadError( stLogPacket* _stpLogPacket, stSmdaThreadArgument* _stpThreadArg );
int OnEmergencyMode( int _iSmdaIndex, int _iSessionIndex, stLogPacket* _stpLogPacket, stSmdaThreadArgument* _stpThreadArg );

int OnReadGutsSmda( stSmdaThreadArgument* _stpThreadArg );
int OnWriteGutsSmda( stSmdaThreadArgument* _stpThreadArg );
int OnSmdaDataReprocess( stSmdaThreadArgument* _stpThreadArg, int* _ipSocketFD, time_t* _tpQueryTime );

int OnConnectToSmda( int _iSmdaIndex, int _iSessionIndex, stSmdaThreadArgument* stpLogArg, char* _cpAdress, int _iPort, char* _cpBindID, char* _cpBindPW );
int OnSysQueryToSmda( int _iSmdaIndex, int _iSessionIndex, stSmdaThreadArgument* stpLogArg, int _iSocketFD );
int OnSendDataToSmda( int _iSocketFD, stLogPacket* _stpLogPacket, stSmdaThreadArgument* _stpThreadArg );

static int GetLogThreadDone();
static int GetSmdaThreadDone();
static int GetLogThreadCount();
static int GetSmdaThreadCount();

static CN_LOG_HANDLE GetLogThreadLogHandle( int _iThreadIndex, char* _cpConfigPath, char* _cpFileName );
static CN_LOG_HANDLE GetSmdaThreadLogHandle( int _iSmdaIndex, int _iSessionIndex, char* _cpConfigFilePath, char* _cpFileName );
static int	GetDiskQuantity( char* _cpDiskName );
static void CNNanoSleep( unsigned long _iNano );
static int MakeAndOpenQueue( int _iQueueKey, int _iQueueSize );
static int DateCheck( time_t _tBrforeTime );

#ifdef __cplusplus
}
#endif

#endif

