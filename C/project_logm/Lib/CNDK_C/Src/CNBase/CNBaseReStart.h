#ifndef __CN_BASE_RESTART_H__
#define __CN_BASE_RESTART_H__

#include <poll.h>
#include <stdio.h>
#include <signal.h>
#include <unistd.h>
#include <sys/socket.h>

#ifdef  __cplusplus
extern "C"
{
#endif

int		r_open( const char* _cpFile, int _iMode );
int		r_close( int _iFd );

FILE*	r_fopen( const char* _cpFile, char* _cpMode );
int		r_fclose( FILE* _fpFile );

size_t	r_fread( void* _vpBuf, size_t _iSize,  size_t _iCnt, FILE* _fpFile );
size_t	r_fwrite( void* _vpBuf, size_t _iSize,  size_t _iCnt, FILE* _fpFile );
char*	r_fgets( char* _cpBuf, int _iSize, FILE* _fpFile );

int		r_dup( int _iFd );
int		r_dup2( int _iFd1, int _iFd2 );

int		r_poll( struct  pollfd* _stpPoll, nfds_t _iCnt, int _iTimeout );
int		r_accept( int _iListen, struct sockaddr* _stpSockAddr, socklen_t* _ipLen );

int		r_sigwaitinfo( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo );
int		r_sigtimedwait( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo, const struct timespec* _stpTsTime );

int		r_msgsnd( int _iQueueID, void* _vpPacket,  size_t _iPacketSize, int _iMsgFlag );
int		r_msgrcv( int _iQueueID, void* _vpPacket,  size_t _iPacketSize, long _iMsgType, int _iMsgFlag );

#ifdef  __cplusplus
}
#endif

#endif

