#ifndef __CN_SOCKET_UTIL_H__
#define __CN_SOCKET_UTIL_H__

#include <time.h>

#include "CNSocketDefinitions.h"

#ifdef  __cplusplus
extern "C"
{
#endif

// ERROR CODE
#ifndef CN_SOCKET_TIME_OUT
#define CN_SOCKET_TIME_OUT			0
#endif

#ifndef CN_SOCKET_RECV_ERROR
#define CN_SOCKET_RECV_ERROR		1
#endif

#ifndef CN_SOCKET_SEND_ERROR
#define CN_SOCKET_SEND_ERROR		2
#endif

// SOCKET STATUS
#ifndef CN_SOCKET_STAT_ERROR
#define CN_SOCKET_STAT_ERROR		-1
#endif

#ifndef CN_SOCKET_STAT_TIMEOUT
#define CN_SOCKET_STAT_TIMEOUT		0
#endif

#ifndef CN_SOCKET_STAT_NORMAL
#define CN_SOCKET_STAT_NORMAL		1
#endif

#ifndef CN_SOCKET_STAT_READABLE
#define CN_SOCKET_STAT_READABLE		2
#endif

#ifndef CN_SOCKET_STAT_BLOCK
#define CN_SOCKET_STAT_BLOCK		3
#endif

#ifndef CN_SOCKET_STAT_SENDABLE
#define CN_SOCKET_STAT_SENDABLE		4
#endif

#ifndef CN_ERR_UTIL_BUF_SIZE
#define CN_ERR_UTIL_BUF_SIZE		512
#endif

typedef struct _stUtilError
{
	int		iErrnoCode;							// errno
	int		iReturnErrCode;						// 실행함수의 반환값

	char	szErrStr[CN_ERR_UTIL_BUF_SIZE];		// 실패 메시지를 담을 버퍼
} stUtilError;


int CNGetSockStat( int _nSocket );
int CNGetSockReadable( int _iSocket );
int CNGetTSockReadable( int _iSocket, int _iTimeout );
int CNGetSockSendable( int _iSocket );
int CNGetSockTSendable( int _iSocket, int _iTimeout );
int CNSocketRead( int _iSocket, char* _cpReadBuff, int _iLength, int _iMiliTime, stUtilError* _stpUtilErr );
int CNSocketWrite( int _iSocket, char* _cpWriteData, int _iLength, int _iMiliTime, stUtilError* _stpUtilErr );
int CNSocketRecv( int _iSocket, char* _cpBuff, int _iLength, int _iMiliTime, stUtilError* _stpUtilErr );
int CNSocketSend( int _iSocket, char* _cpSendBuff, int _iLength, int _iMiliTime, stUtilError* _stpUtilErr );
int	CNGetAddrByHost( char* _cpName, char* _cpBuf, int _iBufLen );
int CNGetAddrBySock( int _iFd, char* _cpBuf, int _iBufLen );

static int CNGetElapseMiliTime( struct timespec *startTime, struct timespec *currentTime );
static int CNIgnoreSigPipe();
static void CNSocketSleep( unsigned long _iNano );

#ifdef  __cplusplus
}
#endif

#endif

