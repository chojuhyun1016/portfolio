#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <sys/socket.h>

#include "CNSocketApi.h"
#include "CNSocketServerTCP.h"

/*******************************************************************************
* Update    : 2010/08/04                                                       *
* Argument  : int, struct _stServerErr*                                        *
*             _iPort : ���� ������ ��Ʈ ��ȣ                                   *
*             _stpServerErr : �������� ���� ���� �� ���� ������ ���� �� ����ü *
*                             ������                                           *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : ��Ʈ��ȣ(_iPort) �� �޾Ƽ� �ش� ��Ʈ ��ȣ�� ������ �����ϰ�      *
*             �ش� ������ LISTEN ���·� �����. �⺻������ ���� �����̹Ƿ�     *
*             Timewait ���¸� ���ϱ� ���� LINGER �ɼ��� ���� REUSE�ɼ��� ���  *
*             �Ѵ�. ������ �� ��� ������ _stpServerErr ����ü�� ��ϵȴ�.     *
*             �⺻������ Greaceful Disconnect�� �ƴ� Abortive Disconnect��.    *
*******************************************************************************/
int CNMakeSvrSocketTCP( int _iPort, stServerError* _stpServerErr )
{
	int					iError				=	0;

	int					_iListeningSocket	=	0;
	
	int					iReuseAddr			=	1;
    int					iRecvBufSize		=	65535;
	int					iSendBufSize		=	65535;

	struct linger		stLinger;
	struct sockaddr_in	stSockAddr;

	memset( ( char* )&stSockAddr, 0, sizeof( stSockAddr ) );
	stSockAddr.sin_family		=	AF_INET;
	stSockAddr.sin_port			=	htons( _iPort );
	stSockAddr.sin_addr.s_addr	=	htonl( INADDR_ANY );

	stLinger.l_onoff			=	0;
	stLinger.l_linger			=	0;

	if( ( _iListeningSocket = socket( AF_INET, SOCK_STREAM, 0 ) ) == CN_SOCKET_ERROR )
    {
		iError = errno;

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	_iListeningSocket;
        strlcpy( _stpServerErr->szErrStr, "MakeSvrSocket : socket() Error", CN_ERR_SERVER_BUF_SIZE );

        return	CN_SOCKET_ERROR;
    }

	if( setsockopt( _iListeningSocket, SOL_SOCKET, SO_LINGER, ( char * )&stLinger, sizeof( stLinger ) ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( _iListeningSocket );

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpServerErr->szErrStr, "MakeSvrSocket : setsockopt() SO_LINGER Set Error", CN_ERR_SERVER_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( setsockopt( _iListeningSocket, SOL_SOCKET, SO_REUSEADDR, ( char * )&iReuseAddr, sizeof( iReuseAddr ) ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( _iListeningSocket );

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpServerErr->szErrStr, "MakeSvrSocket : setsockopt() SO_REUSEADDR Set Error", CN_ERR_SERVER_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( setsockopt( _iListeningSocket, SOL_SOCKET, SO_RCVBUF, ( char* )&iRecvBufSize, sizeof( iRecvBufSize ) ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( _iListeningSocket );

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpServerErr->szErrStr, "MakeSvrSocket : setsockopt() SO_RCVBUF Set Error", CN_ERR_SERVER_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( setsockopt( _iListeningSocket, SOL_SOCKET, SO_SNDBUF, ( char* )&iSendBufSize, sizeof( iSendBufSize ) ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( _iListeningSocket );

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpServerErr->szErrStr, "MakeSvrSocket : setsockopt() SO_SNDBUF Set Error", CN_ERR_SERVER_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( fcntl( _iListeningSocket, F_SETFL, O_NDELAY | O_NONBLOCK ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( _iListeningSocket );

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpServerErr->szErrStr, "MakeSvrSocket : fcntl() Listen Socket Nonblocking Set Error", CN_ERR_SERVER_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( bind( _iListeningSocket, ( struct sockaddr* )&stSockAddr, sizeof( stSockAddr ) ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	CN_SOCKET_ERROR;
        strlcpy( _stpServerErr->szErrStr, "MakeSvrSocket : bind() Error", CN_ERR_SERVER_BUF_SIZE );

		CNSocketClose( _iListeningSocket );

        return	CN_SOCKET_ERROR;
	}
	
	if( listen( _iListeningSocket, CN_LISTEN_WAIT_NUM ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( _iListeningSocket );

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	CN_SOCKET_ERROR;
        strlcpy( _stpServerErr->szErrStr, "MakeSvrSocket : listen() Error", CN_ERR_SERVER_BUF_SIZE );

        return	CN_SOCKET_ERROR;
	}

	return	_iListeningSocket;
}


/*******************************************************************************
* Update    : 2010/08/04                                                       *
* Argument  : int, struct _stServerErr*                                        *
*             _iListeningSocket : Accept�� ���� �� ���� ��ũ����             *
*             _stpServerErr     : Accept ���� �� ���� ������ ���� �� ����ü��  *
*                                 ������                                       *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : Listen ���� ����(_iListeningSocket)�� �޾Ƽ� Accept ó���� �Ѵ�. *
*             Accept �Ϸ� �� 3�� �̳��� ������ ���� ���� ���°� ���� ������    *
*             ���и� ��ȯ�Ѵ�. ���� ������ _stpServerErr ����ü�� ��ϵȴ�.    *
*******************************************************************************/
int CNGetConnectionTCP( int _iListeningSocket, stServerError* _stpServerErr )
{
	int						iError				=	0;
	int						iResult				=	0;
	int						iConnectedSocket	=	0;

	socklen_t				iAddrSize			=	0;

    struct pollfd			stPoll;
	
	struct sockaddr_in		stCliAddr;

	iAddrSize = sizeof( sockaddr_in );

	iConnectedSocket = CNSocketAccept( _iListeningSocket, (struct sockaddr*)&stCliAddr, &iAddrSize );

	if( iConnectedSocket == CN_SOCKET_ERROR )
	{
		iError = errno;

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	iConnectedSocket;
		strlcpy( _stpServerErr->szErrStr, "GetConnection : Socket Accept Error", CN_ERR_SERVER_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	stPoll.fd = iConnectedSocket;
	stPoll.events = ( POLLOUT );
	stPoll.revents = 0x00;

	iResult = CNSocketPoll( &stPoll, 1, CN_ACCEPT_TIMEOUT );

	if( iResult == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( iConnectedSocket );

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpServerErr->szErrStr, "GetConnection : Poll Stat Error", CN_ERR_SERVER_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( stPoll.revents & ( POLLHUP | POLLERR | POLLNVAL ) )
	{
		iError = errno;

		CNSocketClose( iConnectedSocket );

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpServerErr->szErrStr, "GetConnection : Socket Stat Error", CN_ERR_SERVER_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( !( stPoll.revents & ( POLLOUT ) ) )
	{
		iError = errno;

		CNSocketClose( iConnectedSocket );

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpServerErr->szErrStr, "GetConnection : Socket Usable Error", CN_ERR_SERVER_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	if( fcntl( iConnectedSocket, F_SETFL, O_NDELAY | O_NONBLOCK ) == CN_SOCKET_ERROR )
	{
		iError = errno;

		CNSocketClose( iConnectedSocket );

		_stpServerErr->iErrnoCode			=	iError;
		_stpServerErr->iReturnErrCode		=	CN_SOCKET_ERROR;
		strlcpy( _stpServerErr->szErrStr, "GetConnection : Connected Socket Nonblocking Set Error", CN_ERR_SERVER_BUF_SIZE );

		return	CN_SOCKET_ERROR;
	}

	return	iConnectedSocket;
}
