#ifndef __CN_SOCKET_API_H__
#define __CN_SOCKET_API_H__

#include <poll.h>
#include <stdio.h>
#include <signal.h>
#include <unistd.h>
#include <sys/socket.h>

#ifdef  __cplusplus
extern "C"
{
#endif

int CNSocketClose( int _iFd );
int CNSocketAccept( int _iListen, struct sockaddr* _stpSockAddr, socklen_t* _ipLen );
int	CNSocketPoll( struct  pollfd* _stpPoll, nfds_t _iCnt, int _iTimeout );

#ifdef  __cplusplus
}
#endif

#endif
