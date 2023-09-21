#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <arpa/inet.h>

#include "CNSocketApi.h"
#include "CNSocketClientTCP.h"

/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : char*, int, struct _stClientErr*                                 *
*             _cpAddress    : 접속 할 서버의 주소(IP) 문자열                   *
*             _iPort        : 접속 할 서버의 포트(Port) 문자열                 *
*             _stpClientErr : 서버소켓 접속 실패 시 실패 원인을 저장 할 구조체 *
*                             포인터                                           *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 문자열 주소(_cpAddress, IP:xxx.xxx.xxx.xxx)와 Port(_nport)를     *
*             받아서 해당 주소의 서버로 접속을 시도한다. 접속 과정의 Timeout   *
*             은 3초이다. 3초 이내에 서버와 통신이 가능한 상태가 되지 않으면   *
*             실패를 반환한다. 실패 원인은 _stpClientErr 구조체에 기록된다.    *
*             기본적으로 Greaceful Disconnect가 아닌 Abortive Disconnect다.    *
*******************************************************************************/
int CNMakeConnectionTCP( char* _cpAddress, int _iPort, stClientError* _stpClientErr )
{
	int					iSock			=	0;
	int					iError			=	0;
	int					iResult			=	0;

	int					iReuseAddr		=	1;
	int                 iRecvBufSize	=	65535;
	int					iSendBufSize	=	65535;

	struct pollfd		stPoll;

	struct linger		stLinger;
	struct sockaddr_in	stSockAddr;

	stLinger.l_onoff			=	0;
	stLinger.l_linger			=	0;

	memset( ( char* )&stSockAddr, 0, sizeof( stSockAddr ) );
	stSockAddr.sin_family		=	AF_INET;
	stSockAddr.sin_port			=	htons( _iPort );
	stSockAddr.sin_addr.s_addr	=	htonl( inet_addr( _cpAddress ) );

	if( ( iSock = socket( AF_INET, SOCK_STREAM, 0 ) ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		_stpClientErr->iErrnoCode			=	iError;
		_stpClientErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpClientErr->szErrStr, "MakeConnection -> socket() Error", CN_ERR_CLIENT_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( setsockopt( iSock, SOL_SOCKET, SO_LINGER, ( char * )&stLinger, sizeof( stLinger ) ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( iSock );

		_stpClientErr->iErrnoCode			=	iError;
		_stpClientErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpClientErr->szErrStr, "MakeConnection : setsockopt() SO_LINGER Set Error", CN_ERR_CLIENT_BUF_SIZE );
		
		return	CN_SOCKET_ERROR;
	}

	if( setsockopt( iSock, SOL_SOCKET, SO_REUSEADDR, ( char * )&iReuseAddr, sizeof( iReuseAddr ) ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( iSock );

		_stpClientErr->iErrnoCode			=	iError;
		_stpClientErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpClientErr->szErrStr, "MakeConnection -> setsockopt() SO_REUSEADDR Set Error", CN_ERR_CLIENT_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( setsockopt( iSock, SOL_SOCKET, SO_RCVBUF, ( char* )&iRecvBufSize, sizeof( iRecvBufSize ) ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( iSock );

		_stpClientErr->iErrnoCode			=	iError;
		_stpClientErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpClientErr->szErrStr, "MakeConnection -> setsockopt() SO_RCVBUF Set Error", CN_ERR_CLIENT_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( setsockopt( iSock, SOL_SOCKET, SO_SNDBUF, ( char* )&iSendBufSize, sizeof( iSendBufSize ) ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( iSock );

		_stpClientErr->iErrnoCode			=	iError;
		_stpClientErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpClientErr->szErrStr, "MakeConnection -> setsockopt() SO_SNDBUF Set Error", CN_ERR_CLIENT_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( fcntl( iSock, F_SETFL, O_NDELAY | O_NONBLOCK ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( iSock );

		_stpClientErr->iErrnoCode			=	iError;
		_stpClientErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpClientErr->szErrStr, "MakeConnection -> fcntl() Socket Nonblocking Set Error", CN_ERR_CLIENT_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( connect( iSock, ( struct sockaddr* ) &stSockAddr, sizeof( stSockAddr ) ) == CN_SOCKET_ERROR )
	{
		if( errno == EINTR || errno == EALREADY || errno == EINPROGRESS )
		{
			stPoll.fd		= iSock;
			stPoll.events	= ( POLLOUT );
			stPoll.revents	= 0x00;

			iResult = CNSocketPoll( &stPoll, 1, CN_CONNECT_TIMEOUT );

			if( iResult == CN_SOCKET_ERROR )
			{
				iError = errno;

				CNSocketClose( iSock );

				_stpClientErr->iErrnoCode			=	iError;
				_stpClientErr->iReturnErrCode		=	CN_SOCKET_ERROR;
				strlcpy( _stpClientErr->szErrStr, "MakeConnection -> connect() Poll State Error", CN_ERR_CLIENT_BUF_SIZE );

				return	CN_SOCKET_ERROR;
			}

			if( stPoll.revents & ( POLLHUP | POLLERR | POLLNVAL ) )
			{
				iError = errno;

				CNSocketClose( iSock );

				_stpClientErr->iErrnoCode			=	iError;
				_stpClientErr->iReturnErrCode		=	CN_SOCKET_ERROR;
				strlcpy( _stpClientErr->szErrStr, "MakeConnection -> connect() Socket State Error", CN_ERR_CLIENT_BUF_SIZE );

				return	CN_SOCKET_ERROR;
			}

			if( !( stPoll.revents & ( POLLOUT ) ) )
			{
				iError = errno;

				CNSocketClose( iSock );

				_stpClientErr->iErrnoCode			=	iError;
				_stpClientErr->iReturnErrCode		=	CN_SOCKET_ERROR;
				strlcpy( _stpClientErr->szErrStr, "MakeConnection -> connect() Socket Usable Error", CN_ERR_CLIENT_BUF_SIZE );

				return	CN_SOCKET_ERROR;
			}
		}
		else
		{
			iError = errno;

			CNSocketClose( iSock );

			_stpClientErr->iErrnoCode			=	iError;
			_stpClientErr->iReturnErrCode		=	CN_SOCKET_ERROR;
			strlcpy( _stpClientErr->szErrStr, "MakeConnection -> connect() Socket Connect Error", CN_ERR_CLIENT_BUF_SIZE );

			return	CN_SOCKET_ERROR;
		}
	}

	return	iSock;
}

