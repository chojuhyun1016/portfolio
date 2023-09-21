#include "CNNetAPISocket.h"

#include <fcntl.h>
#include <signal.h>
#include <string.h>
#include <sys/time.h>


CNNetAPI::Socket::Socket()
{
	SET_ZERO( m_nSocket );
}


CNNetAPI::Socket::~Socket()
{

}


SOCKET CNNetAPI::Socket::CreateSocket( int _iSocketType, int _iProtocolInfo, int _iDomain )
{
	// 1) Sig Pipe 대비해서 Signal Masking.
	if( CNNetAPI::Socket::CloseSignalPipe() == FALSE )
	{
		ERROR_TRACE( "[Error : CloseSignalPipe() 수행에 실패했습니다. File:%s Line:%d]\n", __FILE__, __LINE__ );

		// Return) 실패!!!!
		return	SOCKET_ERROR;
	}

	// 2) Socket을 생성한다.
	SOCKET	tempSocket	 = socket( _iDomain, _iSocketType, _iProtocolInfo );

	// 1. INVALID_SOCKET 이면 실패한 것이다. 그럼 끝낸다.
	if( tempSocket == SOCKET_ERROR )
	{
		ERROR_TRACE( "[Error : socket() 수행에 실패했습니다. File:%s Line:%d]\n", __FILE__, __LINE__ );

		return	SOCKET_ERROR;
	}

	// 3) Socket 갯수를 증가시킨다.
	CNInterlockedIncrement( &m_nSocket );

	// 4) 성공!!!!
	return	tempSocket;
}


bool CNNetAPI::Socket::Bind( SOCKET _iSocket, LPSOCKADDR _pSocketAddr, int _iSockAddrLen )
{
	int error;

	// 1) p_pSocketAddr가 NULL이어서는 안된다.
	CNASSERT( _pSocketAddr != NULL, false )

	// 2) p_iSockAddrLen가 0이어서는 안된다.
	CNASSERT( _iSockAddrLen != 0, false )

	// Decluare) 
	int	result;

	// 1) Sig Pipe 대비해서 Signal Masking.
	if( CNNetAPI::Socket::CloseSignalPipe() == FALSE )
	{
		ERROR_TRACE( "[Error : CloseSignalPipe() 수행에 실패했습니다. File:%s Line:%d]\n", __FILE__, __LINE__ );

		// Return) 실패!!!!
		return	FALSE;
	}

	// 2) Bind를 수행한다.
	result	= bind( _iSocket, _pSocketAddr, _iSockAddrLen );

	// 1. Bind의 실패했을 경우.
	if( result == SOCKET_ERROR )
	{
		error = errno;

		// Trace)
		ERROR_TRACE( "[Error : bind()에 실패했습니다.Error:%d File:%s Line:%d]\n", error, __FILE__, __LINE__ );

		// Return) 실패!!!
		return	FALSE;
	}

	// 3) 성공!!!
	return	TRUE;
}


SOCKET CNNetAPI::Socket::Connect( SOCKET _iSocket, LPSOCKADDR _pSocketAddr, int _iSockAddrLen )
{
	//-----------------------------------------------------------------
	// Check)
	//-----------------------------------------------------------------
	// 1) p_pSocketAddr가 NULL이어서는 안된다.
	CNASSERT( _pSocketAddr != NULL, false )

	// 2) p_iSockAddrLen가 0이어서는 안된다.
	CNASSERT( _iSockAddrLen != 0, false )

	//-----------------------------------------------------------------
	// 1. Socket 생성하기
	//-----------------------------------------------------------------
	// Decluare)
	int error;
	int	result;

	fd_set sockset;

	// 1) Sig Pipe 대비해서 Signal Masking.
	if( CNNetAPI::Socket::CloseSignalPipe() == FALSE )
	{
		ERROR_TRACE( "[Error : CloseSignalPipe() 수행에 실패했습니다. File:%s Line:%d]\n", __FILE__, __LINE__ );

		// Return) 실패!!!!
		return	SOCKET_ERROR;
	}

	// 2) Connect를 수행한다.
	result	= connect( _iSocket, _pSocketAddr, _iSockAddrLen );

	// 1. Connect의 Return Value 가 실패 일 경우
	if( result == SOCKET_ERROR )
	{
		error = errno;

		if( ( error != EINTR ) && ( error != EALREADY ) && ( error != EINPROGRESS ) )
		{
			// Trace)
			ERROR_TRACE( "[Error : connect()에 실패했습니다.Errno:%d File:%s Line:%d]\n", error, __FILE__, __LINE__ );

			// Return) 실패!!!
			return	SOCKET_ERROR;
		}
	}

	FD_ZERO( &sockset );
	FD_SET( _iSocket, &sockset );
	
	// 3) Socket 가 정상적인 소켓인지 확인한다.
	while( ( ( result = select( _iSocket + 1, NULL, &sockset, NULL, NULL ) ) == SOCKET_ERROR ) && ( errno == EINTR ) )
	{
		FD_ZERO( &sockset );
		FD_SET( _iSocket, &sockset );
	}

	// 1. Socket 의 상태가 비정상인 경우 Error(-1)을 리턴
	if( result == SOCKET_ERROR )
	{
		error = errno;

		ERROR_TRACE( "[Error : connect()에 실패했습니다.Errno:%d File:%s Line:%d]\n", error, __FILE__, __LINE__ );

		CNNetAPI::Socket::CloseSocket( _iSocket );
		
		return SOCKET_ERROR;
	}

	// 4) 성공!!!
	return	_iSocket;
}


bool CNNetAPI::Socket::CloseSocket( SOCKET _iSocket )
{
	int error;
	int	result;

	// 1) Sig Pipe 대비해서 Signal Masking.
	if( CNNetAPI::Socket::CloseSignalPipe() == FALSE )
	{
		ERROR_TRACE( "[Error : CloseSignalPipe() 수행에 실패했습니다. File:%s Line:%d]\n", __FILE__, __LINE__ );

		// Return) 실패!!!!
		return	FALSE;
	}

	// 2) Socket 닫기를 수행.
	while( ( ( result = close( _iSocket ) ) == SOCKET_ERROR ) && ( errno == EINTR ) )

	// 1. Close를 실패 할 경우 Error(-1) Return.
	if( result == SOCKET_ERROR )
	{
		error = errno;

		ERROR_TRACE( "[Error : Socket Close() 수행에 실패했습니다. Errorno : %d File:%s Line:%d]\n", error, __FILE__, __LINE__ );

		return FALSE;
	}

	// 3) 성공!!!
	return TRUE;
}


SOCKET CNNetAPI::Socket::Accept( SOCKET _iSocket, LPSOCKADDR _pSocketAddr, int _iSockAddrLen )
{
	int error;
	int iLoop;
	int result;

	fd_set sockset;

	SOCKET tempSocket;

	// 1) Sig Pipe 대비해서 Signal Masking.
	if( CNNetAPI::Socket::CloseSignalPipe() == FALSE )
	{
		ERROR_TRACE( "[Error : CloseSignalPipe() 수행에 실패했습니다. File:%s Line:%d]\n", __FILE__, __LINE__ );

		// Return) 실패!!!!
		return	SOCKET_ERROR;
	}

	// 2) Accept 를 수행한다.
	while( ( ( tempSocket = accept( _iSocket,  _pSocketAddr, &_iSockAddrLen ) ) == SOCKET_ERROR ) && (errno == EINTR) );

	if( tempSocket == SOCKET_ERROR )
	{
		error = errno;

		ERROR_TRACE( "[Error : accept() 수행에 실패했습니다. Errno:%d File:%s Line:%d]\n", error, __FILE__, __LINE__ );

		return SOCKET_ERROR;
	}

	FD_ZERO( &sockset );
	FD_SET( tempSocket, &sockset );
	
	// 3) Socket 가 정상적인 상태인지 체크한다.
	while( ( ( result = select( tempSocket + 1, NULL, &sockset, NULL, NULL ) ) == SOCKET_ERROR ) && ( errno == EINTR ) )
	{
		FD_ZERO( &sockset );
		FD_SET( tempSocket, &sockset );
	}

	// 1. 비정상일 경우 Error Return.
	if( result == SOCKET_ERROR )
	{
		error = errno;

		ERROR_TRACE( "[Error : accept() 수행에 실패했습니다. Errno:%d File:%s Line:%d]\n", error, __FILE__, __LINE__ );

		CNNetAPI::Socket::CloseSocket( tempSocket );

		return SOCKET_ERROR;
	}

	// 4) 성공!!
	return	tempSocket;
}


bool CNNetAPI::Socket::CloseSignalPipe()
{
	struct sigaction act;

	if( sigaction( SIGPIPE, (struct sigaction *)NULL, &act ) == -1 )
		return	FALSE;

	if( act.sa_handler == SIG_DFL )
	{
		act.sa_handler = SIG_IGN;
		
		if( sigaction( SIGPIPE, &act, (struct sigaction *)NULL ) == -1 )
			return FALSE;
	}

	return TRUE;
}
