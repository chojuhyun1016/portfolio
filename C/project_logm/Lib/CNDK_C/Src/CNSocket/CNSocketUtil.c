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
*             _iSocket : ���¸� üũ�ϰ��� �ϴ� ������ ��ũ����              *
*                                                                              *
* Return    : int, ����(1), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : �ش� ����(_iSocket) �� ����(State)�� ���������� ��ȯ�Ѵ�.        *
*             �������� ��� CN_SOCKET_STAT_ERROR�� ��ȯ�Ѵ�.                   *
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
*             _iSocket : �б� ���� ���������� üũ�ϰ��� �ϴ� ������ ��ũ����*
*                                                                              *
* Return    : int, ����(2),����(-1), ����(1)                                   *
* Stability : MT-Safe                                                          *
* Explain   : �ش� ����(_iSocket)�� �б� ���� ����(���ۿ� �����Ͱ� �ִ���)     *
*             ���θ� ��ȯ�Ѵ�. �̻��� ������� CN_SOCKET_STAT_ERROR(-1)��      *
*             ��ȯ�ϰ� ������ ���� �Ǿ��ְų� �����Ͱ� ���� ���               *
*             CN_SOCKET_STAT_NORMAL(0)��                                       *
*             ��ȯ�Ѵ�.                                                        *
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
*             _iSocket : �б� ���� ���������� üũ�ϰ��� �ϴ� ������ ��ũ����*
*             _iTimeout: �б� ���� ���±��� ��ٸ��� ���ð�                  *
*                                                                              *
* Return    : int, ����(2),����(-1), ����(1)                                   *
* Stability : MT-Safe                                                          *
* Explain   : �ش� ����(_iSocket)�� �б� ���� ����(���ۿ� �����Ͱ� �ִ���)     *
*             ���θ� ��ȯ�Ѵ�. �̻��� ������� CN_SOCKET_STAT_ERROR(-1)��      *
*             ��ȯ�ϰ� ������ ���� �Ǿ��ְų� �����Ͱ� ���� ���               *
*             CN_SOCKET_STAT_NORMAL(0)�� ��ȯ�Ѵ�.                             *
*             �����ð�(_iTimeout) ��ŭ ��ٷ��� �б� ���ɻ��°� �ƴҰ��       *
*             ���� �����Ͱ� ����(CN_SOCKET_STAT_NORMAL)�� ��ȯ�Ѵ�.            *
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
*             _iSocket : ���� ���� ���������� üũ�ϰ��� �ϴ� ������ ��ũ����*
*                                                                              *
* Return    : int, ����(4),����(-1), ����(3)                                   *
* Stability : Async-Signal-Safe                                                *
* Explain   : �ش� ����(_iSocket)�� ���� ���� ����(���ۿ� �����͸� �����ִ���) *
*             ���θ� ��ȯ�Ѵ�. �̻��� ������� CN_SOCKET_STAT_ERROR(-1)��      *
*             ��ȯ�ϰ� ������ ���� �Ǿ��ְų� �����Ͱ� ���� ���               *
*             CN_SOCKET_STAT_BLOCK(3)�� ��ȯ�Ѵ�.                              *
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
*             _iSocket : ���� ���� ���������� üũ�ϰ��� �ϴ� ������ ��ũ����*
*             _iTimeout: ���� ���� ���±��� ��ٸ��� ���ð�                  *
*                                                                              *
* Return    : int, ����(4),����(-1), ����(3)                                   *
* Stability : Async-Signal-Safe                                                *
* Explain   : �ش� ����(_iSocket)�� ���� ���� ����(���ۿ� �����͸� �����ִ���) *
*             ���θ� ��ȯ�Ѵ�. �̻��� ������� CN_SOCKET_STAT_ERROR(-1)��      *
*             ��ȯ�ϰ� ���� �ð��� ������ ���Ⱑ�� ���°� ���� �������        *
*             CN_SOCKET_STAT_BLOCK�� ��ȯ�Ѵ�.                                 *
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
*             _iSocket    : �����͸� �о���� ������ ��ũ����                *
*             _cpReadBuff : �о���� �����͸� ���� �� ����                     *
*             _iLength    : �������κ��� �о���� �������� ����                *
*             _iMiliTime  : ���� �б� Ÿ�Ӿƿ�                                 *
*             _stpUtilErr : �б� ���н� ���� ������ ���� �� ����ü�� ������    *
*                                                                              *
* Return    : int, ����(���� ������ ũ��), ����(-1)                            *
* Stability : Async-Signal-Safe                                                *
* Explain   : ����(_iSocket)���� ����(_cpReadBuff)�� ������ ũ��(_iLength) ��ŭ*
*             �����͸� �о���δ�. Timeout �ð��� _iMiliTime ��ŭ�̴�.         *
*             Timeout�� ���ų� ������ �� ��� _stpUtilErr �� ���� ������       *
*             ����ؼ� ��ȯ�Ѵ�.                                               *
*             �ٻ۴��(����)�� �ɸ����ν� ó�� �ӵ��� ������� ������ ������   *
*             ���� ��� CPU�� ���� ��ƸԴ´�.                                 *
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
*             _iSocket     : �����͸� �о���� ������ ��ũ����               *
*             _cpWriteData : �о���� �����͸� ���� �� ����                    *
*             _iLength     : �������κ��� �о���� �������� ����               *
*             _iMiliTime   : ���� �б� Ÿ�Ӿƿ�                                *
*             _stpUtilErr  : �б� ���н� ���� ������ ���� �� ����ü�� ������   *
*                                                                              *
* Return    : int, ����(���� ������ ũ��), ����(-1)                            *
* Stability : Async-Signal-Safe                                                *
* Explain   : ����(_iSocket)�� ����(_cpReadBuff)�� �����͸�                    *
*             ������ ũ��(_iLength) ��ŭ �����Ѵ�. Timeout �ð��� _iMiliTime   *
*             �̴�. Timeout�� ���ų� ������ �� ��� _stpUtilErr �� ���� ������ *
*             ����ؼ� ��ȯ�Ѵ�.                                               *
*             �ٻ۴��(����)�� �ɸ����ν� ó�� �ӵ��� ������� ������ ������   *
*             ���� ��� CPU�� ���� ��ƸԴ´�.                                 *
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
*             _iSocket    : �����͸� �о���� ������ ��ũ����                *
*             _cpRecvBuff : �о���� �����͸� ���� �� ����                     *
*             _iLength    : �������κ��� �о���� �������� ����                *
*             _iMiliTime  : ���� �б� Ÿ�Ӿƿ�                                 *
*             _stpUtilErr : �б� ���н� ���� ������ ���� �� ����ü�� ������    *
*                                                                              *
* Return    : int, ����(���� ������ ũ��), ����(-1)                            *
* Stability : MT-Safe                                                          *
* Explain   : ����(_iSocket)���� ����(_cpReadBuff)�� ������ ũ��(_iLength) ��ŭ*
*             �����͸� �о���δ�. Timeout �ð��� _iMiliTime ��ŭ�̴�.         *
*             Timeout�� ���ų� ������ �� ��� _stpUtilErr �� ���� ������       *
*             ����ؼ� ��ȯ�Ѵ�.                                               *
*             poll �� ����� �ٻ۴��(����)�� �ɸ��� ���� ���ν� ������ ������ *
*             CPU �� ��Ƹ��� �ʴ´�.                                          *
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
*             _iSocket    : �����͸� �о���� ������ ��ũ����                *
*             _cpSendBuff : �о���� �����͸� ���� �� ����                     *
*             _iLength    : �������κ��� �о���� �������� ����                *
*             _iMiliTime  : ���� �б� Ÿ�Ӿƿ�                                 *
*             _stpUtilErr : �б� ���н� ���� ������ ���� �� ����ü�� ������    *
*                                                                              *
* Return    : int, ����(���� ������ ũ��), ����(-1)                            *
* Stability : MT-Safe                                                          *
* Explain   : ����(_iSocket)�� ����(_cpReadBuff)�� �����͸�                    *
*             ������ ũ��(_iLength) ��ŭ �����Ѵ�. Timeout �ð��� _iMiliTime   *
*             �̴�. Timeout�� ���ų� ������ �� ��� _stpUtilErr �� ���� ������ *
*             ����ؼ� ��ȯ�Ѵ�.                                               *
*             poll �� ����� �ٻ۴��(����)�� �ɸ��� �������ν� ������ ������  *
*             CPU �� ��Ƹ��� �ʴ´�.                                          *
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
*             _cpName  : IP �ּҸ� ������ �ϴ� Host Name ���ڿ�            *
*             _cpBuf   : Host Name�� ���� ��� �� IP �ּ� ���ڿ��� ���� �� ����*
*             _iBufLen : ����(_cpBuf) �� ũ��                                  *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : ���ڿ��� �̷���� ȣ��Ʈ ����(_cpName)�κ��� �ش� ȣ��Ʈ��       *
*             ��� ��(/etc/hosts) IP �ּҸ� ����. ���� �� ��� ����(_cpBuf)*
*             �� �ش� IP �ּҸ� ���ڿ�(xxx.xxx.xxx.xxx)�� ������ �ִ´�.       * 
*             ���� �� ��� CN_SOCKET_ERROR(-1)�� ��ȯ�Ѵ�.                     *
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
*             _iFd     : IP������ �����ϰ��� �ϴ� ������ ��ũ����            *
*             _cpBuf   : ���� ��ũ���͸� ���� ��� �� IP �ּ� ���ڿ���       *
*                        ���� �� ����                                          *
*             _iBufLen : ����(_cpBuf) �� ũ��                                  *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : �ش� ����(_iFd)�� ���� �� ���Ͽ� ���ӵǰų� Bind �Ǿ��ִ�        *
*             ȣ��Ʈ�� IP �ּҸ� ���´�. �ּҸ� ���� ������ ��� �� ���     *
*             �ּҸ� ����(_cpBuf)�� ���ڿ�(xxx.xxx.xxx.xxx) �������� �����Ѵ�. *
*             �����ϰų� ������ ��� CN_SOCKET_ERROR(-1)�� ��ȯ�Ѵ�.           * 
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
*             _tvStrTime : �ð��� �������е��� �����ϴ� ����ü�� ������        *
*             _tvCurTime : �ð��� �������е��� �����ϴ� ����ü�� ������        *
*                                                                              *
* Return    : ����(0), ����(-1)                                                *
* Stability : MT-Safe, Async-Signal-Safe                                       *
* Explain   : �Ѱ� ���� �ð�����ü�� ���� ���� ��ȯ�Ѵ�.                       *
*             _tvCurTime ���� _tvStrTime �� �� ���� ��ȯ�Ѵ�.                  *
*             struct timespec �� �ð� ���е��� 1/1000000000 �̸� ��� ��       *
*             ��ȯ�ϱ� ���� 1000000���� ����� 1/1000�� ������ �����Ͽ�      *
*             ��ȯ�Ѵ�. �ᱹ ��ȯ�Ǵ� ���� 1/1000�� ������ �Ǵ� ���̴�.        *
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
* Argument  : ����                                                             *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : SIGPIFE �ñ׳��� ���� �ʵ��� �ش� �ñ׳��� ���¸� Ignore(����)   *
*             �� �����Ѵ�. �̹� ������ �������ų� �����ų� Close �� ���Ͽ�     *
*             ���⳪/�б⸦ �õ� �� ��� �߻��Ѵ�. Ignore Ȥ�� �ڵ鸵 �ص���   *
*             ���� ��� ���μ��� Ȥ�� �����尡 ����ȴ�.                       *
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
*             _iNano : sleep(���� ���е�)�� ������ �ð�                        *
*                                                                              *
* Return    : ����                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���ڷ� �Ѱ� ���� �ð�(_iNano) ��ŭ ���μ����� ����(sleep)        *
*             ��Ų��. unsigned long �ڷ����� Data Overflow ������ �ִ�         *
*             4��(4000000000)������ �ð��� �Ѱ� ���� �� ������                 *
*             4�� ���� ����(sleep) ��ų �� �ִ�.                               *
*             �ִ� �ð� ���е��� 1/1000000000 �� �̸� �ñ׳�(EINTR)�� ����     *
*             ���� �� ���ݱ��� ����(sleep)�� �ð��� ���� �ð��� ����ؼ� �ٽ�  *
*             ����(sleep)�� ����.                                          *
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
