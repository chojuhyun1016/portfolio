#ifndef _CD_SOCKET_SERVER_TCP_H_
#define _CD_SOCKET_SERVER_TCP_H_

#include "CDSocketDefinitions.h"


#ifdef  __cplusplus
extern "C"
{
#endif

#ifndef CD_LISTEN_WAIT_NUM
#define CD_LISTEN_WAIT_NUM          1024    // Listen Queue Size
#endif

#ifndef CD_ACCEPT_TIMEOUT
#define CD_ACCEPT_TIMEOUT           3000    // 3 Second
#endif

#ifndef CD_ERR_SERVER_BUF_SIZE
#define CD_ERR_SERVER_BUF_SIZE      512     // Error String Buffer Size
#endif


typedef struct _stCDSocketSvrErr
{
    int     iErrno;                         // errno
    int     iReturn;                        // 실행함수의 반환값

    char    caErrStr[CD_ERR_SERVER_BUF_SIZE];   // 실패 메시지를 담을 버퍼
} stCDSocketSvrErr;


int CDSocketMakeListenSockTCP( int _iPort, stCDSocketSvrErr* _stpServerErr );
int CDSocketMakeAcceptSockTCP( int _iListeningSocket, stCDSocketSvrErr* _stpServerErr );


#ifdef  __cplusplus
}
#endif

#endif

