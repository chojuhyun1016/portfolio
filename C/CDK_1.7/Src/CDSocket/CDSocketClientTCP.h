#ifndef _CD_SOCKET_CLIENT_TCP_H_
#define _CD_SOCKET_CLIENT_TCP_H_

#include "CDSocketDefinitions.h"


#ifdef  __cplusplus
extern "C"
{
#endif

#ifndef CD_CONNECT_TIMEOUT
#define CD_CONNECT_TIMEOUT          3000    // 3 Second
#endif

#ifndef CD_ERR_CLIENT_BUF_SIZE
#define CD_ERR_CLIENT_BUF_SIZE      512     // Error String Buffer Size
#endif


typedef struct _stCDSocketCliErr
{
    int     iErrno;                         // errno
    int     iReturn;                        // 실행함수의 반환값

    char    caErrStr[CD_ERR_CLIENT_BUF_SIZE];   // 실패 메시지를 담을 버퍼
} stCDSocketCliErr;


int CDSocketMakeConnectTCP( char* _cpAddress, int _iPort, stCDSocketCliErr* _stpClientErr );


#ifdef  __cplusplus
}
#endif

#endif

