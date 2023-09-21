#ifndef __CD_SOCKET_API_H__
#define __CD_SOCKET_API_H__

#ifdef  _SOLARIS_
    #include <poll.h>
    #include <sys/socket.h>
#elif _CENT_OS_
    #include <poll.h>
    #include <sys/socket.h>
#else
    #include <poll.h>
    #include <sys/socket.h>
#endif


#ifdef  __cplusplus
extern "C"
{
#endif

int CDSocketClose( int _iFd );
int CDSocketAccept( int _iListen, struct sockaddr* _stpSockAddr, socklen_t* _ipLen );
int	CDSocketPoll( struct  pollfd* _stpPoll, nfds_t _iCnt, int _iTimeout );


#ifdef  __cplusplus
}
#endif

#endif
