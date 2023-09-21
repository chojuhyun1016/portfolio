#ifndef _CN_CLIENT_SOCKET_TCP_H_
#define _CN_CLIENT_SOCKET_TCP_H_

#include "CNSocketDefinitions.h"

#ifdef  __cplusplus
extern "C"
{
#endif

#ifndef CN_CONNECT_TIMEOUT
#define CN_CONNECT_TIMEOUT			3000	// 3 Second
#endif

#ifndef CN_ERR_CLIENT_BUF_SIZE
#define CN_ERR_CLIENT_BUF_SIZE		512		// Error String Buffer Size
#endif

typedef struct _stClientError
{
	int		iErrnoCode;							// errno 
	int		iReturnErrCode;						// 실행함수의 반환값

	char	szErrStr[CN_ERR_CLIENT_BUF_SIZE];	// 실패 메시지를 담을 버퍼
} stClientError;


int CNMakeConnectionTCP( char* _cpAddress, int _iPort, stClientError* _stpClientErr );

#ifdef  __cplusplus
}
#endif

#endif 

