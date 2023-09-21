#ifndef __MSG_RESENDER_H__
#define __MSG_RESENDER_H__

// Log ฐüทร
#include "CNLogWrite.h"

#ifdef __cplusplus
extern "C" {
#endif

#define MSG_RESENDER_PROCESS_NAME		"MSG_RESENDER"

#ifdef TELESA
	#define MSG_RESENDER_CONFIG_PATH	"/smsnfs1/sms/telesa/conf"
#elif IMP
	#define MSG_RESENDER_CONFIG_PATH	"/data/intelli/xml/env"
#elif MASS
	#define MSG_RESENDER_CONFIG_PATH	"/export/home/sms4/smaa/smaa/config"
#elif LSPW
	#define MSG_RESENDER_CONFIG_PATH	"/export/home/sms4/link/xml"
#else
	#define MSG_RESENDER_CONFIG_PATH	"/smsnfs1/sms/telesa/conf"
#endif

#define MSG_RESENDER_CONFIG_FILE_NAME	"MsgResender.cfg"

#define LOG_MANAGER_CONFIG_FILE_NAME	"LogManager.cfg"
#define SMDA_QUEUE_INFO_FILE			"SmdaQueueInfo.cfg"
#define SMDA_ROUTING_INFO_FILE			"SmdaRoutingInfo.cfg"

#define MSG_RESENDER_LOG_NAME	"REPORT_MSG_RESENDER"
#define MSG_RESENDER_LOG_TYPE	2
#define MSG_RESENDER_LOG_LEVEL	3

#define LIMIT_COUNT_TO_SEC		1000

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
	int		iQueueKey;
	int		iQueueID;

} stSmdaQueue;

typedef struct
{
	int		iSessionNum;
	int		iMsgCount;

	stSmdaQueue	*stpQueue;

} stSmdaQueueInfo;

typedef struct
{
	int	iSmdaServerNum;

	stSmdaQueueInfo	*Info;

} stSmdaQueueTable;

typedef struct
{
	int	iSmdaServerNum;

	int	*Index;

} stSmdaRouting;


int main( int argc, char **argv );

void ProcessTerminate( int _iReturnCode );
int PrecessDataInit( char* _cpConfigFilePath );
int SmdaQueueDataInit( char* _cpConfigFilePath );
void SmdaQueueDataFree();
int SmdaRoutingDataInit( char* _cpConfigFilePath );
void SmdaRoutingDataFree();
int ReInsertQueueData( char* _cpDataFilePath, char* _cpDataFileMovePath, char* _cpDataFileFinPath );
int OnDataFileMove( char* _cpDataFilePath, char* _cpDataFileMovePath );
int OnDataFileInsert( char* _cpDataFile );
int OnDataFileFinish( char* _cpDataFile, char* _cpDataFileFinPath );
int OnWriteErrorFile( char* _cpReportFile, char* _cpErrorFileName, stLogPacket* _stpErrorPacket );
void OnWriteReport( char* _cpLogFile, const char* fmt, ... );

void SignalHandler( int _iSignal );
int	ForkProcess( char* _cpProcessName );
int	ProcessCheck( char* _cpProcessName );

#ifdef __cplusplus
}
#endif

#endif

