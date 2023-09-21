#include <poll.h>
#include <fcntl.h>
#include <errno.h>
#include <netdb.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <sys/types.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>

#include "CNSocketApi.h"
#include "CNSocketUtil.h"

/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int                                                              *
*             _iSocket : 상태를 체크하고자 하는 소켓의 디스크립터              *
*                                                                              *
* Return    : int, 성공(1), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 해당 소켓(_iSocket) 의 상태(State)가 정상인지를 반환한다.        *
*             비정상일 경우 CN_SOCKET_STAT_ERROR를 반환한다.                   *
*******************************************************************************/
int CNGetSockStat( int _iSocket )
{
	int		iResult;

	struct  pollfd  stPoll;

	stPoll.fd = _iSocket;
	stPoll.events = 0x00;
	stPoll.revents = 0x00;

	if( _iSocket < 0 )
		return	CN_SOCKET_STAT_ERROR;

	iResult = CNSocketPoll( &stPoll, 1, NULL );

	if( iResult == CN_SOCKET_STAT_ERROR )
		return	CN_SOCKET_STAT_ERROR;

	if( stPoll.revents & ( POLLHUP | POLLERR | POLLNVAL ) )
		return	CN_SOCKET_STAT_ERROR;

	return	CN_SOCKET_STAT_NORMAL;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int                                                              *
*             _iSocket : 읽기 가능 상태인지를 체크하고자 하는 소켓의 디스크립터*
*                                                                              *
* Return    : int, 성공(2),실패(-1), 블럭(1)                                   *
* Stability : MT-Safe                                                          *
* Explain   : 해당 소켓(_iSocket)이 읽기 가능 상태(버퍼에 데이터가 있는지)     *
*             여부를 반환한다. 이상이 있을경우 CN_SOCKET_STAT_ERROR(-1)을      *
*             반환하고 소켓이 블럭 되어있거나 데이터가 없을 경우               *
*             CN_SOCKET_STAT_NORMAL(0)을                                       *
*             반환한다.                                                        *
*******************************************************************************/
int CNGetSockReadable( int _iSocket )
{
	int		iResult;

    struct  pollfd  stPoll;
	
	stPoll.fd = _iSocket;
	stPoll.events = ( POLLIN | POLLRDNORM );
	stPoll.revents = 0x00;

	if( _iSocket < 0 )
		return	CN_SOCKET_STAT_ERROR;

	iResult = CNSocketPoll( &stPoll, 1, NULL );

	if( iResult == CN_SOCKET_STAT_ERROR )
		return	CN_SOCKET_STAT_ERROR;

	if( stPoll.revents & ( POLLHUP | POLLERR | POLLNVAL ) )
		return	CN_SOCKET_STAT_ERROR;

	if( stPoll.revents & ( POLLIN | POLLRDNORM ) )
		return	CN_SOCKET_STAT_READABLE;

	return	CN_SOCKET_STAT_NORMAL;
}


/*******************************************************************************
* Update    : 2011/03/30                                                       *
* Argument  : int, int                                                         *
*             _iSocket : 읽기 가능 상태인지를 체크하고자 하는 소켓의 디스크립터*
*             _iTimeout: 읽기 가능 상태까지 기다리는 대기시간                  *
*                                                                              *
* Return    : int, 성공(2),실패(-1), 블럭(1)                                   *
* Stability : MT-Safe                                                          *
* Explain   : 해당 소켓(_iSocket)이 읽기 가능 상태(버퍼에 데이터가 있는지)     *
*             여부를 반환한다. 이상이 있을경우 CN_SOCKET_STAT_ERROR(-1)을      *
*             반환하고 소켓이 블럭 되어있거나 데이터가 없을 경우               *
*             CN_SOCKET_STAT_NORMAL(0)을 반환한다.                             *
*             일정시간(_iTimeout) 만큼 기다려도 읽기 가능상태가 아닐경우       *
*             읽을 데이터가 없음(CN_SOCKET_STAT_NORMAL)을 반환한다.            *
*******************************************************************************/
int CNGetTSockReadable( int _iSocket, int _iTimeout )
{
	int		iResult;

    struct  pollfd  stPoll;
	
	stPoll.fd = _iSocket;
	stPoll.events = ( POLLIN | POLLRDNORM );
	stPoll.revents = 0x00;

	if( _iSocket < 0 )
		return	CN_SOCKET_STAT_ERROR;

	iResult = CNSocketPoll( &stPoll, 1, _iTimeout );

	if( iResult == CN_SOCKET_STAT_ERROR )
		return	CN_SOCKET_STAT_ERROR;

	if( stPoll.revents & ( POLLHUP | POLLERR | POLLNVAL ) )
		return	CN_SOCKET_STAT_ERROR;

	if( stPoll.revents & ( POLLIN | POLLRDNORM ) )
		return	CN_SOCKET_STAT_READABLE;

	return	CN_SOCKET_STAT_NORMAL;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int                                                              *
*             _iSocket : 쓰기 가능 상태인지를 체크하고자 하는 소켓의 디스크립터*
*                                                                              *
* Return    : int, 성공(4),실패(-1), 블럭(3)                                   *
* Stability : Async-Signal-Safe                                                *
* Explain   : 해당 소켓(_iSocket)이 쓰기 가능 상태(버퍼에 데이터를 쓸수있는지) *
*             여부를 반환한다. 이상이 있을경우 CN_SOCKET_STAT_ERROR(-1)을      *
*             반환하고 소켓이 블럭 되어있거나 데이터가 없을 경우               *
*             CN_SOCKET_STAT_BLOCK(3)을 반환한다.                              *
*******************************************************************************/
int CNGetSockSendable( int _iSocket )
{
	int		iResult;

    struct  pollfd  stPoll;
	
	stPoll.fd = _iSocket;
	stPoll.events = ( POLLOUT );
	stPoll.revents = 0x00;

	if( _iSocket < 0 )
		return	CN_SOCKET_STAT_ERROR;

	iResult = CNSocketPoll( &stPoll, 1, NULL );

	if( iResult == CN_SOCKET_STAT_ERROR )
		return	CN_SOCKET_STAT_ERROR;

	if( stPoll.revents & ( POLLHUP | POLLERR | POLLNVAL ) )
		return	CN_SOCKET_STAT_ERROR;

	if( stPoll.revents & ( POLLOUT ) )
		return	CN_SOCKET_STAT_SENDABLE;

	return	CN_SOCKET_STAT_BLOCK;
}


/*******************************************************************************
* Update    : 2011/03/30                                                       *
* Argument  : int                                                              *
*             _iSocket : 쓰기 가능 상태인지를 체크하고자 하는 소켓의 디스크립터*
*             _iTimeout: 쓰기 가능 상태까지 기다리는 대기시간                  *
*                                                                              *
* Return    : int, 성공(4),실패(-1), 블럭(3)                                   *
* Stability : Async-Signal-Safe                                                *
* Explain   : 해당 소켓(_iSocket)이 쓰기 가능 상태(버퍼에 데이터를 쓸수있는지) *
*             여부를 반환한다. 이상이 있을경우 CN_SOCKET_STAT_ERROR(-1)을      *
*             반환하고 일정 시간이 지나도 쓰기가능 상태가 되지 않을경우        *
*             CN_SOCKET_STAT_BLOCK을 반환한다.                                 *
*******************************************************************************/
int CNGetTSockSendable( int _iSocket, int _iTimeout )
{
	int		iResult;

    struct  pollfd  stPoll;
	
	stPoll.fd = _iSocket;
	stPoll.events = ( POLLOUT );
	stPoll.revents = 0x00;

	if( _iSocket < 0 )
		return	CN_SOCKET_STAT_ERROR;

	iResult = CNSocketPoll( &stPoll, 1, _iTimeout );

	if( iResult == CN_SOCKET_STAT_ERROR )
		return	CN_SOCKET_STAT_ERROR;

	if( stPoll.revents & ( POLLHUP | POLLERR | POLLNVAL ) )
		return	CN_SOCKET_STAT_ERROR;

	if( stPoll.revents & ( POLLOUT ) )
		return	CN_SOCKET_STAT_SENDABLE;

	return	CN_SOCKET_STAT_BLOCK;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, char*, int, int, struct _stUtilErr*                         *
*             _iSocket    : 데이터를 읽어들일 소켓의 디스크립터                *
*             _cpReadBuff : 읽어들인 데이터를 저장 할 버퍼                     *
*             _iLength    : 소켓으로부터 읽어들일 데이터의 길이                *
*             _iMiliTime  : 소켓 읽기 타임아웃                                 *
*             _stpUtilErr : 읽기 실패시 실패 원인을 저장 할 구조체의 포인터    *
*                                                                              *
* Return    : int, 성공(받은 데이터 크기), 실패(-1)                            *
* Stability : Async-Signal-Safe                                                *
* Explain   : 소켓(_iSocket)에서 버퍼(_cpReadBuff)로 정해진 크기(_iLength) 만큼*
*             데이터를 읽어들인다. Timeout 시간은 _iMiliTime 만큼이다.         *
*             Timeout이 나거나 에러가 날 경우 _stpUtilErr 에 실패 원인을       *
*             기록해서 반환한다.                                               *
*             바쁜대기(스핀)이 걸림으로써 처리 속도가 빠른대신 데이터 지연이   *
*             생길 경우 CPU를 많이 잡아먹는다.                                 *
*******************************************************************************/
int CNSocketRead( int _iSocket, char* _cpReadBuff, int _iLength, int _iMiliTime, stUtilError* _stpUtilErr )
{
	int		iReadBytes = 0;
	int		iNewReadBytes = 0;

	int		iRTimer = (int)_iMiliTime;

	char	*cpPtr = (char *)_cpReadBuff;

	struct timespec tsStartTime;
	struct timespec	tsCurrentTime;

	clock_gettime( CLOCK_REALTIME, &tsStartTime );

	while( 1 )
	{
		iNewReadBytes = read( _iSocket, cpPtr, _iLength - iReadBytes );

		if( iNewReadBytes == 0 )
		{
			iNewReadBytes = -1;
			break;
		}

		if( iNewReadBytes == -1 )
		{
			if( errno != EAGAIN && errno != EINTR )
				break;

			iNewReadBytes = 0;
		}

		cpPtr = (char*)(_cpReadBuff + (iReadBytes += iNewReadBytes));
		
		if( iReadBytes >= _iLength )
			return iReadBytes;

		clock_gettime( CLOCK_REALTIME, &tsCurrentTime );

		if( iRTimer > CNGetElapseMiliTime( &tsStartTime, &tsCurrentTime ) )
			continue;

		break;
	}
		
	_stpUtilErr->iErrnoCode = errno;

	if( iNewReadBytes >= 0 )
		_stpUtilErr->iReturnErrCode = CN_SOCKET_TIME_OUT;
	else
		_stpUtilErr->iReturnErrCode = CN_SOCKET_RECV_ERROR;
		
	return CN_SOCKET_ERROR;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, char*, int, int, struct _stUtilErr*                         *
*             _iSocket     : 데이터를 읽어들일 소켓의 디스크립터               *
*             _cpWriteData : 읽어들인 데이터를 저장 할 버퍼                    *
*             _iLength     : 소켓으로부터 읽어들일 데이터의 길이               *
*             _iMiliTime   : 소켓 읽기 타임아웃                                *
*             _stpUtilErr  : 읽기 실패시 실패 원인을 저장 할 구조체의 포인터   *
*                                                                              *
* Return    : int, 성공(보낸 데이터 크기), 실패(-1)                            *
* Stability : Async-Signal-Safe                                                *
* Explain   : 소켓(_iSocket)에 버퍼(_cpReadBuff)의 데이터를                    *
*             정해진 크기(_iLength) 만큼 전송한다. Timeout 시간은 _iMiliTime   *
*             이다. Timeout이 나거나 에러가 날 경우 _stpUtilErr 에 실패 원인을 *
*             기록해서 반환한다.                                               *
*             바쁜대기(스핀)이 걸림으로써 처리 속도가 빠른대신 데이터 지연이   *
*             생길 경우 CPU를 많이 잡아먹는다.                                 *
*******************************************************************************/
int CNSocketWrite( int _iSocket, char* _cpWriteData, int _iLength, int _iMiliTime, stUtilError* _stpUtilErr )
{
	int		iSendBytes = 0;
	int		iNewSendBytes = 0;

	int		iRTimer = (int)_iMiliTime;

	char	*cpPtr = (char *)_cpWriteData;

	struct timespec tsStartTime;
	struct timespec tsCurrentTime; 	

	clock_gettime( CLOCK_REALTIME, &tsStartTime );

	while( 1 )
	{
		iNewSendBytes = write( _iSocket, cpPtr, _iLength - iSendBytes );

		if( iNewSendBytes == 0 )
		{
			iNewSendBytes = -1;
			break;
		}

		if( iNewSendBytes == -1 )
		{
			if( errno != EAGAIN && errno != EINTR )
				break;

			iNewSendBytes = 0;
		}

		cpPtr = (char *)(_cpWriteData + (iSendBytes += iNewSendBytes));

		if( iSendBytes >= _iLength )
			return iSendBytes;

		clock_gettime( CLOCK_REALTIME, &tsCurrentTime ); 

		if( iRTimer > CNGetElapseMiliTime( &tsStartTime, &tsCurrentTime ) )
			continue;

		break;
	}

	_stpUtilErr->iErrnoCode = errno;

	if( iNewSendBytes >= 0 )
		_stpUtilErr->iReturnErrCode = CN_SOCKET_TIME_OUT;
	else
		_stpUtilErr->iReturnErrCode = CN_SOCKET_SEND_ERROR;

	return CN_SOCKET_ERROR;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, char*, int, int, struct _stUtilErr*                         *
*             _iSocket    : 데이터를 읽어들일 소켓의 디스크립터                *
*             _cpRecvBuff : 읽어들인 데이터를 저장 할 버퍼                     *
*             _iLength    : 소켓으로부터 읽어들일 데이터의 길이                *
*             _iMiliTime  : 소켓 읽기 타임아웃                                 *
*             _stpUtilErr : 읽기 실패시 실패 원인을 저장 할 구조체의 포인터    *
*                                                                              *
* Return    : int, 성공(받은 데이터 크기), 실패(-1)                            *
* Stability : MT-Safe                                                          *
* Explain   : 소켓(_iSocket)에서 버퍼(_cpReadBuff)로 정해진 크기(_iLength) 만큼*
*             데이터를 읽어들인다. Timeout 시간은 _iMiliTime 만큼이다.         *
*             Timeout이 나거나 에러가 날 경우 _stpUtilErr 에 실패 원인을       *
*             기록해서 반환한다.                                               *
*             poll 을 사용해 바쁜대기(스핀)가 걸리지 않음 으로써 데이터 지연시 *
*             CPU 를 잡아먹지 않는다.                                          *
*******************************************************************************/
int CNSocketRecv( int _iSocket, char* _cpRecvBuff, int _iLength, int _iMiliTime, stUtilError* _stpUtilErr )
{
	int		iReadBytes = 0;
	int		iNewReadBytes = 0;
	int		iRTimer = (int)_iMiliTime;

	char	*cpPtr = (char *)_cpRecvBuff;

	struct timespec	tsStartTime;
	struct timespec	tsCurrentTime; 

	struct pollfd	stPoll;

	stPoll.fd 		= _iSocket;
	stPoll.events 	= ( POLLIN | POLLNORM );

	clock_gettime( CLOCK_REALTIME, &tsStartTime );

	while( 1 )
	{
		stPoll.revents = 0;
		iNewReadBytes = 0;

		if( poll( &stPoll, 1, iRTimer ) == -1 )
		{ 
			if( errno != EINTR && errno != EAGAIN )
			{
				iNewReadBytes = -1;
				break;
			}
		}

		if( stPoll.revents & ( POLLIN | POLLNORM ) )
		{
			iNewReadBytes = recv( _iSocket, cpPtr, _iLength - iReadBytes, O_NONBLOCK );

			if( iNewReadBytes == 0 )
			{
				iNewReadBytes = -1;
				break;
			}

			if( iNewReadBytes == -1 )
			{
				if( errno != EAGAIN && errno != EINTR  )
					break;

				iNewReadBytes = 0;
			}
		}

		cpPtr = (char *)(_cpRecvBuff + (iReadBytes += iNewReadBytes));
		
		if( iReadBytes >= _iLength )
			return iReadBytes;
			
		clock_gettime( CLOCK_REALTIME, &tsCurrentTime );
		
		if( ( iRTimer -= CNGetElapseMiliTime( &tsStartTime, &tsCurrentTime ) ) > 0 )
			continue;

		break;
	}

	_stpUtilErr->iErrnoCode = errno;

	if( iNewReadBytes >= 0 )
		_stpUtilErr->iReturnErrCode = CN_SOCKET_TIME_OUT;
	else
		_stpUtilErr->iReturnErrCode = CN_SOCKET_RECV_ERROR;
		
	return CN_SOCKET_ERROR;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, char*, int, int, struct _stUtilErr*                         *
*             _iSocket    : 데이터를 읽어들일 소켓의 디스크립터                *
*             _cpSendBuff : 읽어들인 데이터를 저장 할 버퍼                     *
*             _iLength    : 소켓으로부터 읽어들일 데이터의 길이                *
*             _iMiliTime  : 소켓 읽기 타임아웃                                 *
*             _stpUtilErr : 읽기 실패시 실패 원인을 저장 할 구조체의 포인터    *
*                                                                              *
* Return    : int, 성공(보낸 데이터 크기), 실패(-1)                            *
* Stability : MT-Safe                                                          *
* Explain   : 소켓(_iSocket)에 버퍼(_cpReadBuff)의 데이터를                    *
*             정해진 크기(_iLength) 만큼 전송한다. Timeout 시간은 _iMiliTime   *
*             이다. Timeout이 나거나 에러가 날 경우 _stpUtilErr 에 실패 원인을 *
*             기록해서 반환한다.                                               *
*             poll 을 사용해 바쁜대기(스핀)가 걸리지 않음으로써 데이터 지연시  *
*             CPU 를 잡아먹지 않는다.                                          *
*******************************************************************************/
int CNSocketSend( int _iSocket, char* _cpSendBuff, int _iLength, int _iMiliTime, stUtilError* _stpUtilErr )
{
	int		iSendBytes = 0;
	int		iNewSendBytes = 0;
	int		iRTimer = (int)_iMiliTime;

	char	*cpPtr = (char *)_cpSendBuff;

	struct timespec	tsStartTime;
	struct timespec	tsCurrentTime;

	struct pollfd   stPoll;

	stPoll.fd 		= _iSocket;
	stPoll.events 	= POLLOUT;

	clock_gettime( CLOCK_REALTIME, &tsStartTime );

	while( 1 )
	{
		stPoll.revents = 0;
		iNewSendBytes = 0;
		
		if( poll( &stPoll, 1, iRTimer ) == -1 )
		{ 
			if( errno != EINTR && errno != EAGAIN )
			{
				iNewSendBytes = -1;
				break;
			}
		}

		if( stPoll.revents & ( POLLOUT ) )
		{
			iNewSendBytes = send( _iSocket, cpPtr, _iLength - iSendBytes, O_NONBLOCK );

			if( iNewSendBytes == 0 )
			{
				iNewSendBytes = -1;
				break;
			}

			if( iNewSendBytes == -1 )
			{
				if( errno != EAGAIN && errno != EINTR  )
					break;

				iNewSendBytes = 0;
			}
		}

		cpPtr = (char *)(_cpSendBuff + (iSendBytes += iNewSendBytes));
		
		if( iSendBytes >= _iLength )
			return iSendBytes;
			
		clock_gettime( CLOCK_REALTIME, &tsCurrentTime );
		
		if( ( iRTimer -= CNGetElapseMiliTime( &tsStartTime, &tsCurrentTime ) ) > 0 )
			continue;

		break;
	}

	_stpUtilErr->iErrnoCode = errno;

	if( iNewSendBytes >= 0 )
		_stpUtilErr->iReturnErrCode = CN_SOCKET_TIME_OUT;
	else
		_stpUtilErr->iReturnErrCode = CN_SOCKET_SEND_ERROR;
		
	return CN_SOCKET_ERROR;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : char*, char*, int                                                *
*             _cpName  : IP 주소를 얻어내고자 하는 Host Name 문자열            *
*             _cpBuf   : Host Name을 통해 얻어 낸 IP 주소 문자열을 저장 할 버퍼*
*             _iBufLen : 버퍼(_cpBuf) 의 크기                                  *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 문자열로 이루어진 호스트 테임(_cpName)로부터 해당 호스트에       *
*             등록 된(/etc/hosts) IP 주소를 얻어낸다. 성공 할 경우 버퍼(_cpBuf)*
*             에 해당 IP 주소를 문자열(xxx.xxx.xxx.xxx)로 복사해 넣는다.       * 
*             실패 할 경우 CN_SOCKET_ERROR(-1)을 반환한다.                     *
*******************************************************************************/
int	CNGetAddrByHost( char* _cpName, char* _cpBuf, int _iBufLen )
{
	int		iError;

	char	szBuf[8192];

	struct hostent  stHost;
	struct hostent	*stpHost;

	if( _cpName == NULL )
		return	CN_SOCKET_ERROR;

	if( _cpBuf == NULL )
		return	CN_SOCKET_ERROR;

	if( _iBufLen < INET_ADDRSTRLEN )
		return	CN_SOCKET_ERROR;

	stpHost = gethostbyname_r( _cpName, &stHost, szBuf, sizeof( szBuf ), &iError );

	if( stpHost )
	{
		memset( _cpBuf, 0x00, _iBufLen );

		if( inet_ntop( AF_INET, (struct in_addr*)stHost.h_addr_list[0], _cpBuf, _iBufLen ) )
			return	CN_SOCKET_SUCCESS;
	}

	_cpBuf[0] = NULL;

	return	CN_SOCKET_ERROR;

}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, char*, int                                                  *
*             _iFd     : IP정보를 추출하고자 하는 소켓의 디스크립터            *
*             _cpBuf   : 소켓 디스크립터를 통해 얻어 낸 IP 주소 문자열을       *
*                        저장 할 버퍼                                          *
*             _iBufLen : 버퍼(_cpBuf) 의 크기                                  *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 해당 소켓(_iFd)로 부터 그 소켓에 접속되거나 Bind 되어있는        *
*             호스트의 IP 주소를 얻어온다. 주소를 성공 적으로 얻어 올 경우     *
*             주소를 버퍼(_cpBuf)에 문자열(xxx.xxx.xxx.xxx) 형식으로 저장한다. *
*             실패하거나 에러일 경우 CN_SOCKET_ERROR(-1)를 반환한다.           * 
*******************************************************************************/
int CNGetAddrBySock( int _iFd, char* _cpBuf, int _iBufLen )
{
	int		iLen;

	struct sockaddr_in	stAddr;

	if( _iFd < 0 )
		return	CN_SOCKET_ERROR;

	if( _cpBuf == NULL )
		return	CN_SOCKET_ERROR;

	if( _iBufLen < INET_ADDRSTRLEN )
		return	CN_SOCKET_ERROR;	

	iLen = sizeof( stAddr );

	if( getpeername( _iFd, (struct sockaddr*)&stAddr, &iLen ) < 0 )
		return	CN_SOCKET_ERROR;

	if( inet_ntop( AF_INET, (struct in_addr*)&stAddr.sin_addr, _cpBuf, _iBufLen ) )
		return	CN_SOCKET_ERROR;

	return CN_SOCKET_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : struct timespec*, struct timespec*                               *
*             _tvStrTime : 시간을 나노정밀도로 저장하는 구조체의 포인터        *
*             _tvCurTime : 시간을 나노정밀도로 저장하는 구조체의 포인터        *
*                                                                              *
* Return    : 성공(0), 실패(-1)                                                *
* Stability : MT-Safe, Async-Signal-Safe                                       *
* Explain   : 넘겨 받은 시간구조체의 연산 값을 반환한다.                       *
*             _tvCurTime 에서 _tvStrTime 을 뺀 값을 반환한다.                  *
*             struct timespec 의 시간 정밀도는 1/1000000000 이며 계산 후       *
*             반환하기 전에 1000000으로 나누어서 1/1000초 단위로 변형하여      *
*             반환한다. 결국 반환되는 값은 1/1000초 단위가 되는 것이다.        *
*******************************************************************************/
static int CNGetElapseMiliTime( struct timespec* _tvStrTime, struct timespec* _tvCurTime ) 
{ 
	unsigned long ulSec;
	unsigned long ulNano;

	if( _tvCurTime->tv_nsec >= _tvStrTime->tv_nsec )
	{ 
		ulSec = _tvCurTime->tv_sec - _tvStrTime->tv_sec; 
		ulNano = _tvCurTime->tv_nsec - _tvStrTime->tv_nsec; 
	}
	else
	{ 
		ulSec = _tvCurTime->tv_sec - (_tvStrTime->tv_sec - 1); 
		ulNano = (1000 * 1000 * 1000) + (_tvCurTime->tv_nsec - _tvStrTime->tv_nsec); 
	}

	return (int)((ulNano + ((1000 * 1000 * 1000) * ulSec)) / (1000 * 1000)); 
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : 없음                                                             *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : SIGPIFE 시그널을 받지 않도록 해당 시그널의 상태를 Ignore(무시)   *
*             로 셋팅한다. 이미 접속이 끊어지거나 깨지거나 Close 된 소켓에     *
*             쓰기나/읽기를 시도 할 경우 발생한다. Ignore 혹은 핸들링 해두지   *
*             않을 경우 프로세스 혹은 쓰레드가 종료된다.                       *
*******************************************************************************/
static int CNIgnoreSigPipe()
{
	struct sigaction stAction;

	if( sigaction( SIGPIPE, (struct sigaction*)NULL, &stAction ) == -1 )
		return	CN_SOCKET_ERROR;

	if( stAction.sa_handler == SIG_DFL )
	{
		stAction.sa_handler = SIG_IGN;
		
		if( sigaction( SIGPIPE, &stAction, (struct sigaction *)NULL ) == -1 )
			return	CN_SOCKET_ERROR;
	}

	return CN_SOCKET_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : unsigned long                                                    *
*             _iNano : sleep(나노 정밀도)을 수행할 시간                        *
*                                                                              *
* Return    : 없음                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : 인자로 넘겨 받은 시간(_iNano) 만큼 프로세스를 정지(sleep)        *
*             시킨다. unsigned long 자료형의 Data Overflow 때문에 최대         *
*             4초(4000000000)까지의 시간을 넘겨 받을 수 있으며                 *
*             4초 동안 정지(sleep) 시킬 수 있다.                               *
*             최대 시간 정밀도는 1/1000000000 초 이며 시그널(EINTR)에 방해     *
*             받을 시 지금까지 정지(sleep)한 시간과 남은 시간을 계산해서 다시  *
*             정지(sleep)에 들어간다.                                          *
*******************************************************************************/
static void CNSocketSleep( unsigned long _iNano )
{
	struct timespec	tsStartTime;
	struct timespec	tsRemainTime;

	if( _iNano <= 0 )
		return;

	if( _iNano >= 4000000000 )
		_iNano	= 4000000000;

	if( _iNano >= 1000000000 )
	{
		tsStartTime.tv_sec	= _iNano / 1000000000;
		tsStartTime.tv_nsec	= _iNano % 1000000000;
	}
	else
	{
		tsStartTime.tv_sec	= 0;
		tsStartTime.tv_nsec	= _iNano;
	}

	while( ( nanosleep( &tsStartTime, &tsRemainTime ) == -1 ) && ( errno == EINTR ) )
	{
		tsStartTime.tv_sec	= tsRemainTime.tv_sec;
		tsStartTime.tv_nsec	= tsRemainTime.tv_nsec;
		
		tsRemainTime.tv_sec		= 0x00;
		tsRemainTime.tv_nsec	= 0x00;
	}
}

