#ifndef __CD_BASE_RESTART_H__
#define __CD_BASE_RESTART_H__

#ifdef  _SOLARIS_
    #include <poll.h>
    #include <stdio.h>
    #include <signal.h>
    #include <sys/socket.h>
#elif _CENT_OS_
    #include <poll.h>
    #include <stdio.h>
    #include <signal.h>
    #include <sys/socket.h>
#else
    #include <poll.h>
    #include <stdio.h>
    #include <signal.h>
    #include <sys/socket.h>
#endif


#ifdef  __cplusplus
extern "C"
{
#endif

int     CDBaseOpen( const char* _cpFile, int _iMode );
int     CDBaseClose( int _iFd );

FILE*   CDBaseFopen( const char* _cpFile, char* _cpMode );
int     CDBaseFclose( FILE* _fpFile );

size_t  CDBaseFread( void* _vpBuf, size_t _iSize,  size_t _iCDt, FILE* _fpFile );
size_t  CDBaseFwrite( void* _vpBuf, size_t _iSize,  size_t _iCDt, FILE* _fpFile );
char*   CDBaseFgets( char* _cpBuf, int _iSize, FILE* _fpFile );

int     CDBaseDup( int _iFd );
int     CDBaseDup2( int _iFd1, int _iFd2 );

int     CDBasePoll( struct  pollfd* _stpPoll, nfds_t _iCDt, int _iTimeout );
int     CDBaseAccept( int _iListen, struct sockaddr* _stpSockAddr, socklen_t* _ipLen );

int     CDBaseSigwaitinfo( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo );
int     CDBaseSigtimedwait( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo, const struct timespec* _stpTsTime );

int     CDBaseMsgsnd( int _iQueueID, void* _vpPacket,  size_t _iPacketSize, int _iMsgFlag );
int     CDBaseMsgrcv( int _iQueueID, void* _vpPacket,  size_t _iPacketSize, long _iMsgType, int _iMsgFlag );


#ifdef  __cplusplus
}
#endif

#endif

