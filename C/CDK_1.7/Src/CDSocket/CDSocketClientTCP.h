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
    int     iReturn;                        // �����Լ��� ��ȯ��

    char    caErrStr[CD_ERR_CLIENT_BUF_SIZE];   // ���� �޽����� ���� ����
} stCDSocketCliErr;


int CDSocketMakeConnectTCP( char* _cpAddress, int _iPort, stCDSocketCliErr* _stpClientErr );


#ifdef  __cplusplus
}
#endif

#endif
