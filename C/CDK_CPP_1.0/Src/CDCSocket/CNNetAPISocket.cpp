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
	// 1) Sig Pipe ����ؼ� Signal Masking.
	if( CNNetAPI::Socket::CloseSignalPipe() == FALSE )
	{
		ERROR_TRACE( "[Error : CloseSignalPipe() ���࿡ �����߽��ϴ�. File:%s Line:%d]\n", __FILE__, __LINE__ );

		// Return) ����!!!!
		return	SOCKET_ERROR;
	}

	// 2) Socket�� �����Ѵ�.
	SOCKET	tempSocket	 = socket( _iDomain, _iSocketType, _iProtocolInfo );

	// 1. INVALID_SOCKET �̸� ������ ���̴�. �׷� ������.
	if( tempSocket == SOCKET_ERROR )
	{
		ERROR_TRACE( "[Error : socket() ���࿡ �����߽��ϴ�. File:%s Line:%d]\n", __FILE__, __LINE__ );

		return	SOCKET_ERROR;
	}

	// 3) Socket ������ ������Ų��.
	CNInterlockedIncrement( &m_nSocket );

	// 4) ����!!!!
	return	tempSocket;
}


bool CNNetAPI::Socket::Bind( SOCKET _iSocket, LPSOCKADDR _pSocketAddr, int _iSockAddrLen )
{
	int error;

	// 1) p_pSocketAddr�� NULL�̾�� �ȵȴ�.
	CNASSERT( _pSocketAddr != NULL, false )

	// 2) p_iSockAddrLen�� 0�̾�� �ȵȴ�.
	CNASSERT( _iSockAddrLen != 0, false )

	// Decluare) 
	int	result;

	// 1) Sig Pipe ����ؼ� Signal Masking.
	if( CNNetAPI::Socket::CloseSignalPipe() == FALSE )
	{
		ERROR_TRACE( "[Error : CloseSignalPipe() ���࿡ �����߽��ϴ�. File:%s Line:%d]\n", __FILE__, __LINE__ );

		// Return) ����!!!!
		return	FALSE;
	}

	// 2) Bind�� �����Ѵ�.
	result	= bind( _iSocket, _pSocketAddr, _iSockAddrLen );

	// 1. Bind�� �������� ���.
	if( result == SOCKET_ERROR )
	{
		error = errno;

		// Trace)
		ERROR_TRACE( "[Error : bind()�� �����߽��ϴ�.Error:%d File:%s Line:%d]\n", error, __FILE__, __LINE__ );

		// Return) ����!!!
		return	FALSE;
	}

	// 3) ����!!!
	return	TRUE;
}


SOCKET CNNetAPI::Socket::Connect( SOCKET _iSocket, LPSOCKADDR _pSocketAddr, int _iSockAddrLen )
{
	//-----------------------------------------------------------------
	// Check)
	//-----------------------------------------------------------------
	// 1) p_pSocketAddr�� NULL�̾�� �ȵȴ�.
	CNASSERT( _pSocketAddr != NULL, false )

	// 2) p_iSockAddrLen�� 0�̾�� �ȵȴ�.
	CNASSERT( _iSockAddrLen != 0, false )

	//-----------------------------------------------------------------
	// 1. Socket �����ϱ�
	//-----------------------------------------------------------------
	// Decluare)
	int error;
	int	result;

	fd_set sockset;

	// 1) Sig Pipe ����ؼ� Signal Masking.
	if( CNNetAPI::Socket::CloseSignalPipe() == FALSE )
	{
		ERROR_TRACE( "[Error : CloseSignalPipe() ���࿡ �����߽��ϴ�. File:%s Line:%d]\n", __FILE__, __LINE__ );

		// Return) ����!!!!
		return	SOCKET_ERROR;
	}

	// 2) Connect�� �����Ѵ�.
	result	= connect( _iSocket, _pSocketAddr, _iSockAddrLen );

	// 1. Connect�� Return Value �� ���� �� ���
	if( result == SOCKET_ERROR )
	{
		error = errno;

		if( ( error != EINTR ) && ( error != EALREADY ) && ( error != EINPROGRESS ) )
		{
			// Trace)
			ERROR_TRACE( "[Error : connect()�� �����߽��ϴ�.Errno:%d File:%s Line:%d]\n", error, __FILE__, __LINE__ );

			// Return) ����!!!
			return	SOCKET_ERROR;
		}
	}

	FD_ZERO( &sockset );
	FD_SET( _iSocket, &sockset );
	
	// 3) Socket �� �������� �������� Ȯ���Ѵ�.
	while( ( ( result = select( _iSocket + 1, NULL, &sockset, NULL, NULL ) ) == SOCKET_ERROR ) && ( errno == EINTR ) )
	{
		FD_ZERO( &sockset );
		FD_SET( _iSocket, &sockset );
	}

	// 1. Socket �� ���°� �������� ��� Error(-1)�� ����
	if( result == SOCKET_ERROR )
	{
		error = errno;

		ERROR_TRACE( "[Error : connect()�� �����߽��ϴ�.Errno:%d File:%s Line:%d]\n", error, __FILE__, __LINE__ );

		CNNetAPI::Socket::CloseSocket( _iSocket );
		
		return SOCKET_ERROR;
	}

	// 4) ����!!!
	return	_iSocket;
}


bool CNNetAPI::Socket::CloseSocket( SOCKET _iSocket )
{
	int error;
	int	result;

	// 1) Sig Pipe ����ؼ� Signal Masking.
	if( CNNetAPI::Socket::CloseSignalPipe() == FALSE )
	{
		ERROR_TRACE( "[Error : CloseSignalPipe() ���࿡ �����߽��ϴ�. File:%s Line:%d]\n", __FILE__, __LINE__ );

		// Return) ����!!!!
		return	FALSE;
	}

	// 2) Socket �ݱ⸦ ����.
	while( ( ( result = close( _iSocket ) ) == SOCKET_ERROR ) && ( errno == EINTR ) )

	// 1. Close�� ���� �� ��� Error(-1) Return.
	if( result == SOCKET_ERROR )
	{
		error = errno;

		ERROR_TRACE( "[Error : Socket Close() ���࿡ �����߽��ϴ�. Errorno : %d File:%s Line:%d]\n", error, __FILE__, __LINE__ );

		return FALSE;
	}

	// 3) ����!!!
	return TRUE;
}


SOCKET CNNetAPI::Socket::Accept( SOCKET _iSocket, LPSOCKADDR _pSocketAddr, int _iSockAddrLen )
{
	int error;
	int iLoop;
	int result;

	fd_set sockset;

	SOCKET tempSocket;

	// 1) Sig Pipe ����ؼ� Signal Masking.
	if( CNNetAPI::Socket::CloseSignalPipe() == FALSE )
	{
		ERROR_TRACE( "[Error : CloseSignalPipe() ���࿡ �����߽��ϴ�. File:%s Line:%d]\n", __FILE__, __LINE__ );

		// Return) ����!!!!
		return	SOCKET_ERROR;
	}

	// 2) Accept �� �����Ѵ�.
	while( ( ( tempSocket = accept( _iSocket,  _pSocketAddr, &_iSockAddrLen ) ) == SOCKET_ERROR ) && (errno == EINTR) );

	if( tempSocket == SOCKET_ERROR )
	{
		error = errno;

		ERROR_TRACE( "[Error : accept() ���࿡ �����߽��ϴ�. Errno:%d File:%s Line:%d]\n", error, __FILE__, __LINE__ );

		return SOCKET_ERROR;
	}

	FD_ZERO( &sockset );
	FD_SET( tempSocket, &sockset );
	
	// 3) Socket �� �������� �������� üũ�Ѵ�.
	while( ( ( result = select( tempSocket + 1, NULL, &sockset, NULL, NULL ) ) == SOCKET_ERROR ) && ( errno == EINTR ) )
	{
		FD_ZERO( &sockset );
		FD_SET( tempSocket, &sockset );
	}

	// 1. �������� ��� Error Return.
	if( result == SOCKET_ERROR )
	{
		error = errno;

		ERROR_TRACE( "[Error : accept() ���࿡ �����߽��ϴ�. Errno:%d File:%s Line:%d]\n", error, __FILE__, __LINE__ );

		CNNetAPI::Socket::CloseSocket( tempSocket );

		return SOCKET_ERROR;
	}

	// 4) ����!!
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