#ifndef _CN_SERVER_SOCKET_TCP_H_
#define _CN_SERVER_SOCKET_TCP_H_

#include "CNSocketDefinitions.h"

#ifdef  __cplusplus
extern "C"
{
#endif

#ifndef CN_LISTEN_WAIT_NUM
#define CN_LISTEN_WAIT_NUM			1024	// Listen Queue Size
#endif

#ifndef CN_ACCEPT_TIMEOUT
#define CN_ACCEPT_TIMEOUT			3000	// 3 Second
#endif

#ifndef CN_ERR_SERVER_BUF_SIZE
#define CN_ERR_SERVER_BUF_SIZE		512		// Error String Buffer Size
#endif

typedef struct _stServerError
{
	int		iErrnoCode;							// errno 
	int		iReturnErrCode;						// 실행함수의 반환값

	char	szErrStr[CN_ERR_SERVER_BUF_SIZE];	// 실패 메시지를 담을 버퍼
} stServerError;


int CNMakeSvrSocketTCP( int _iPort, stServerError* _stpServerErr );
int CNGetConnectionTCP( int _iListeningSocket, stServerError* _stpServerErr );

#ifdef  __cplusplus
}
#endif

#endif

