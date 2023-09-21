#ifndef __SOCKET_UTIL_H__
#define __SOCKET_UTIL_H__

#include <strings.h>
#include <stdio.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>
#include <sys/time.h>
#include <errno.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <arpa/inet.h>

#ifdef  __cplusplus
extern "C"
{
#endif

/*	DEFAULT DEFINE DATA	*/
#define 	UTIL_SOCKET_TIMEOUT			5000
#define		UTIL_ERR_BUF_SIZE			512

/*	FUCTION RETURN RESULT CODE	*/
#define		UTIL_SOCKET_SUCCESS			0
#define     UTIL_SOCKET_FAIL			-1

/*	ERROR CODE	*/
#define		UTIL_SOCKET_TIME_OUT		0
#define		UTIL_SOCKET_RECV_ERROR		1
#define		UTIL_SOCKET_SEND_ERROR		2

/*	SOCKET STATUS RETURN CODE	*/
#define		UTIL_SOCKET_STAT_ERROR		-1
#define		UTIL_SOCKET_STAT_TIMEOUT	0
#define		UTIL_SOCKET_STAT_NORMAL		1
#define		UTIL_SOCKET_STAT_BLOCK		2


#define TIMEVAL_COPY( a, b )								\
{															\
	( a )->tv_sec = ( b )->tv_sec;							\
	( a )->tv_usec = ( b )->tv_usec;						\
}

#define TIMEVAL_ADD( a, b, result )							\
do{															\
	( result )->tv_sec = ( a )->tv_sec + ( b )->tv_sec;		\
	( result )->tv_usec = ( a )->tv_usec + ( b )->tv_usec;	\
	if( ( result )->tv_usec >= 1000000 )					\
	{														\
		++( result )->tv_sec;								\
		( result )->tv_usec -= 1000000;						\
	}														\
} while( 0 )

# define TIMEVAL_SUB( a, b, result )						\
do {														\
	(result)->tv_sec = (a)->tv_sec - (b)->tv_sec;			\
	(result)->tv_usec = (a)->tv_usec - (b)->tv_usec;		\
	if ((result)->tv_usec < 0)								\
	{														\
		--(result)->tv_sec;									\
		(result)->tv_usec += 1000000;						\
	}														\
} while (0)


typedef struct
{
	int		nResult;
	int		nErrnoCode;
	int		nReturnErrCode;

	char	caErrStr[UTIL_ERR_BUF_SIZE];
} stUtilErr;

void Sleep( unsigned long _iNano );
int GetSockStat( int _nSocket );
int GetSockTimeStat( int _nSocket, int msec );
int getElapseMicroTime( struct timeval *startTime, struct timeval *currentTime );
int getElapseMiliTime( struct timespec *startTime, struct timespec *currentTime );
int SocketRecv( int _nSocket, char* _cpRcvBuf, int _nLength, int _nMilliTime, stUtilErr* _stpUtilErr );
int SocketSend( int _nSocket, char* _cpSndData, int _nLength, int _nMilliTime, stUtilErr* _stpUtilErr );

#ifdef  __cplusplus
}
#endif

#endif

