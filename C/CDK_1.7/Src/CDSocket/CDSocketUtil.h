#ifndef __CD_SOCKET_UTIL_H__
#define __CD_SOCKET_UTIL_H__

#include "CDSocketDefinitions.h"

#ifdef  _SOLARIS_
    #include <time.h>
#elif _CENT_OS_
    #include <time.h>
#else
    #include <time.h>
#endif

#ifdef  __cplusplus
extern "C"
{
#endif

// ERROR CODE
#ifndef CD_SOCKET_TIME_OUT
#define CD_SOCKET_TIME_OUT          0
#endif

#ifndef CD_SOCKET_RECV_ERROR
#define CD_SOCKET_RECV_ERROR        1
#endif

#ifndef CD_SOCKET_SEND_ERROR
#define CD_SOCKET_SEND_ERROR        2
#endif

// SOCKET STATUS
#ifndef CD_SOCKET_STAT_ERROR
#define CD_SOCKET_STAT_ERROR        -1
#endif

#ifndef CD_SOCKET_STAT_TIMEOUT
#define CD_SOCKET_STAT_TIMEOUT      0
#endif

#ifndef CD_SOCKET_STAT_NORMAL
#define CD_SOCKET_STAT_NORMAL       1
#endif

#ifndef CD_SOCKET_STAT_READABLE
#define CD_SOCKET_STAT_READABLE     2
#endif

#ifndef CD_SOCKET_STAT_BLOCK
#define CD_SOCKET_STAT_BLOCK        3
#endif

#ifndef CD_SOCKET_STAT_SENDABLE
#define CD_SOCKET_STAT_SENDABLE     4
#endif

#ifndef CD_ERR_UTIL_BUF_SIZE
#define CD_ERR_UTIL_BUF_SIZE        512
#endif


typedef struct _stCDSocketUtilErr
{
    int     iErrno;                             // errno
    int     iReturn;                            // 실행함수의 반환값

    char    caErrStr[CD_ERR_UTIL_BUF_SIZE];     // 실패 메시지를 담을 버퍼
} stCDSocketUtilErr;


int CDSocketGetStat( int _nSocket );

int CDSocketGetReadable( int _iSocket );
int CDSocketGetTimedReadable( int _iSocket, int _iTimeout );

int CDSocketGetSendable( int _iSocket );
int CDSocketGetTimedSendable( int _iSocket, int _iTimeout );

int CDSocketRead( int _iSocket, char* _cpReadBuff, int _iLength, int _iMiliTime, stCDSocketUtilErr* _stpUtilErr );
int CDSocketWrite( int _iSocket, char* _cpWriteData, int _iLength, int _iMiliTime, stCDSocketUtilErr* _stpUtilErr );

int CDSocketRecv( int _iSocket, char* _cpBuff, int _iLength, int _iMiliTime, stCDSocketUtilErr* _stpUtilErr );
int CDSocketSend( int _iSocket, char* _cpSendBuff, int _iLength, int _iMiliTime, stCDSocketUtilErr* _stpUtilErr );

int CDSocketGetAddrByHost( char* _cpName, char* _cpBuf, int _iBufLen );
int CDSocketGetAddrBySock( int _iFd, char* _cpBuf, int _iBufLen );

static int CDSocketGetElapseMiliTime( struct timespec *startTime, struct timespec *currentTime );
static int CDSocketIgnoreSigPipe();
static void CDSocketNanoSleep( unsigned long _iNano );


#ifdef  __cplusplus
}
#endif

#endif

