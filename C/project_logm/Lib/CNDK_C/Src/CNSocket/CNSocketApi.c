#include <errno.h>

#include "CNSocketApi.h"
#include "CNSocketDefinitions.h"


/*******************************************************************************
* Update    : 2010/10/04                                                       *
* Argument  : int                                                              *
*             _iFd : 닫을려는 파일의 소켓 디스크립터                           *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : System Call close() 함수를 래핑한것이다.                         *
*             Interrupt의 영향을 받지 않도록 하기위해 close()함수의 리턴값이   *
*             CN_SOCKET_ERROR(-1) 이면서 errno가 EINTR(interrupt에 의한 중지)  *
*             일 경우 다시 close()를 수행한다.                                 *
*******************************************************************************/
int CNSocketClose( int _iFd )
{
	int	iResult;

	while( ( iResult = close( _iFd ) ) == CN_SOCKET_ERROR && errno == EINTR );

	return	iResult;
}


/*******************************************************************************
* Update    : 2010/10/04                                                       *
* Argument  : int, sockaddr*, socklen_t*                                       *
*             _iListen     : Accept를 진행 할 소켓 디스크립터                  *
*             _stpSockAddr : Accept 된 Client의 정보를 저장 할 구조체 포인터   *
*             _ipLen       : _stpSockAddr 구조체의 크기                        *
*                                                                              *
* Return    : int, 성공(Socket Descriptor), 실패(-1)                           *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : Sockets Library의 accept()함수를 래핑한것이다.                   *
*             Interrupt의 영향을 받지 않도록 하기위해 open()함수의 리턴값이    *
*             CN_SOCKET_ERROR(-1) 이면서 errno가 EINTR(interrupt에 의한 중지)  *
*             일 경우 다시 accept()을 수행한다.                                *
*******************************************************************************/
int CNSocketAccept( int _iListen, struct sockaddr* _stpSockAddr, socklen_t* _ipLen )
{
	int iResult;

	while( ( ( iResult = accept( _iListen, _stpSockAddr, _ipLen ) ) == CN_SOCKET_ERROR ) && (errno == EINTR) );

	return	iResult;
}


/*******************************************************************************
* Update    : 2010/10/04                                                       *
* Argument  : pollfd*, nfds_t, int                                             *
*             _stpPoll  : pollfd 구조체의 포인터                               *
*             _iCnt     : 이벤트 감지를 수행 할 pollfd 구조체의 갯수           *
*             _iTimeout : 이벤트 감지(poll) 타임아웃                           *
*                                                                              *
* Return    : int, 성공(이벤트가 일어난 소켓디스크립터의 갯수), 실패(-1)       *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : System Call poll()함수를 래핑한것이다.                           *
*             Interrupt의 영향을 받지 않도록 하기위해 open()함수의 리턴값이    *
*             CN_SOCKET_ERROR(-1) 이면서 errno가 EINTR(interrupt에 의한 중지)  *
*             일 경우 다시 poll()을 수행한다.                                  *
*******************************************************************************/
int	CNSocketPoll( struct  pollfd* _stpPoll, nfds_t _iCnt, int _iTimeout )
{
	int iResult;

	while( ( ( iResult = poll( _stpPoll, _iCnt, _iTimeout ) ) == CN_SOCKET_ERROR ) && ( errno == EINTR ) );

	return iResult;
}

