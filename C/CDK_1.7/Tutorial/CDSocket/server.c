#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "header.h"

#include "server.h"
#include "CDSocket.h"


int main( int argc, char **argv )
{
	int		iResult;

	int		iServerSocket;
	int		iClientSocket;

	// �ּ�(IP) ���ڿ��� ���� �� ����
	char	caAdressBuffer[256];

	// ȣ��Ʈ �̸��� ���� �� ����
	char	caHostNameBuffer[256];

	// ���� ���� ���� �� Accept ��� �����Ͱ� ����Ǵ� ����ü
	stCDSocketSvrErr	stServerError;

	// Ŭ���̾�Ʈ���� ������ �ۼ��� ��� �����Ͱ� ����Ǵ� ����ü
	stCDSocketUtilErr	stSendError;
	stCDSocketUtilErr	stRecvError;

	// Ŭ���̾�Ʈ�� �ۼ��� �� ������(����ü)
	stTutorialPacket	stSendPacket;
	stTutorialPacket	stRecvPacket;

	::memset( &stServerError, 0x00, sizeof( stServerError ) );

	// ���� ���� ����
	if( ( iServerSocket = CDSocketMakeListenSockTCP( SERVER_PORT, &stServerError ) ) == CD_SOCKET_ERROR )
	{
		::fprintf( stderr, "[CDSocketMakeListenSockTCP( %d %d ) Error][R:%d][E:%d][S:%s][L:%d]\n", 
			SERVER_PORT, 
			&stServerError, 
			stServerError.iReturn, 
			stServerError.iErrno, 
			stServerError.caErrStr, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDSocketMakeListenSockTCP( %d %d ) Complete][Fd:%d][L:%d]\n", 
		SERVER_PORT, 
		&stServerError, 
		iServerSocket, 
		__LINE__ );

	// ������ ���¸� ȹ��
	if ( ( iResult = CDSocketGetStat( iServerSocket ) ) != CD_SOCKET_STAT_NORMAL )
	{
		::fprintf( stderr, "[CDSocketGetStat( %d ) Error][R:%d][E:%d][L:%d]\n", 
			iServerSocket, 
			iResult, 
			errno, 
			__LINE__ );

		CDSocketClose( iServerSocket );

		::exit( -1 );
	}

	// Ŭ���̾�Ʈ�� ������ ��ٸ�
	while( 1 )
	{
		// ���� ������ �̺�Ʈ �˻�
		iResult	= CDSocketGetReadable( iServerSocket );

		// �̺�Ʈ�� ������� Accept ����
		if( iResult == CD_SOCKET_STAT_READABLE )
		{
			if( ( iClientSocket = CDSocketMakeAcceptSockTCP( iServerSocket, &stServerError ) ) == CD_SOCKET_ERROR )
			{
				::fprintf( stderr, "[CDSocketMakeAcceptSockTCP( %d %d ) Error][R:%d][E:%d][S:%s][L:%d]\n", 
					iServerSocket, 
					&stServerError, 
					stServerError.iReturn, 
					stServerError.iErrno, 
					stServerError.caErrStr, 
					__LINE__ );

				CDSocketClose( iServerSocket );

				::exit( -1 );
			}

			::fprintf( stderr, "[CDSocketMakeAcceptSockTCP( %d %d ) Complete][Fd:%d][L:%d]\n", 
				iServerSocket, 
				&stServerError,
				iClientSocket, 
				__LINE__ );

			break;
		}

		if( iResult == CD_SOCKET_STAT_ERROR )
		{
			::fprintf( stderr, "[CDSocketGetReadable( %d ) Error][R:%d][E:%d][L:%d]\n", 
				iServerSocket, 
				iResult, 
				errno, 
				__LINE__ );

			CDSocketClose( iServerSocket );

			::exit( -1 );
		}

		::sleep( 1 );
	}

	::memset( caAdressBuffer, 0x00, sizeof( caAdressBuffer ) );
	::memset( caHostNameBuffer, 0x00, sizeof( caHostNameBuffer ) );

	// �ý��ۿ��� ������ ȣ��Ʈ �̸��� ȹ��
	if ( ( iResult = ::gethostname( caHostNameBuffer, sizeof(caHostNameBuffer) ) ) < 0 )
	{
		::fprintf( stderr, "[gethostname( %d %d ) Error][R:%d][E:%d][L:%d]\n", 
			caHostNameBuffer, 
			sizeof(caHostNameBuffer), 
			iResult, 
			errno, 
			__LINE__ );

		CDSocketClose( iServerSocket );
		CDSocketClose( iClientSocket );

		::exit( -1 );
	}

	::fprintf( stderr, "[gethostname( %d %d ) Complete][Host:%s][L:%d]\n", 
		caHostNameBuffer, 
		sizeof(caHostNameBuffer), 
		caHostNameBuffer, 
		__LINE__ );

	// ȣ��Ʈ �̸����� ������ �ּ�(IP)�� ȹ��
	if ( ( iResult = CDSocketGetAddrByHost( caHostNameBuffer, caAdressBuffer, sizeof(caAdressBuffer) ) ) != CD_SOCKET_SUCCESS )
	{
		::fprintf( stderr, "[CDSocketGetAddrByHost( %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
			caHostNameBuffer, 
			caAdressBuffer, 
			sizeof(caAdressBuffer), 
			iResult, 
			errno, 
			__LINE__ );

		CDSocketClose( iServerSocket );
		CDSocketClose( iClientSocket );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDSocketGetAddrByHost( %d %d %d ) Complete][IP:%s][L:%d]\n", 
		caHostNameBuffer, 
		caAdressBuffer, 
		sizeof(caAdressBuffer), 
		caAdressBuffer, 
		__LINE__ );

	::memset( caAdressBuffer, 0x00, sizeof( caAdressBuffer ) );

	// ���� �� Ŭ���̾�Ʈ�� ���Ͽ��� Ŭ���̾�Ʈ�� �ּ�(IP)�� ȹ��
	if ( ( iResult = CDSocketGetAddrBySock( iClientSocket, caAdressBuffer, sizeof(caAdressBuffer) ) ) != CD_SOCKET_SUCCESS )
	{
		::fprintf( stderr, "[CDSocketGetAddrBySock( %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
			iClientSocket, 
			caAdressBuffer, 
			sizeof(caAdressBuffer), 
			iResult, 
			errno, 
			__LINE__ );

		CDSocketClose( iServerSocket );
		CDSocketClose( iClientSocket );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDSocketGetAddrBySock( %d %d %d ) Complete][IP:%s][L:%d]\n", 
		iClientSocket, 
		caAdressBuffer, 
		sizeof(caAdressBuffer), 
		caAdressBuffer, 
		__LINE__ );


	// Ŭ���̾�Ʈ�� ������ �����͸� �ۼ���
	while( 1 )
	{
		// Ŭ���̾�Ʈ ������ �̺�Ʈ �˻�
		iResult	= CDSocketGetReadable( iClientSocket );

		// �̺�Ʈ�� �Ͼ ���(���� �� �����Ͱ� �ִ� ���)
		if( iResult == CD_SOCKET_STAT_READABLE )
		{
			::memset( &stRecvPacket, 0x00, sizeof( stRecvPacket ) );

			// Ŭ���̾�Ʈ�� ������ �����͸� ����
			if( ( iResult = CDSocketRead( iClientSocket, (char*)&stRecvPacket, sizeof(stRecvPacket), SOCKET_TIME_OUT, &stRecvError ) ) == CD_SOCKET_ERROR )
			{
				::fprintf( stderr, "[CDSocketRead( %d %d %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
					iClientSocket, 
					(char*)&stRecvPacket, 
					sizeof(stRecvPacket), 
					SOCKET_TIME_OUT, 
					&stRecvError, 
					stRecvError.iReturn, 
					stRecvError.iErrno, 
					__LINE__ );

				CDSocketClose( iServerSocket );
				CDSocketClose( iClientSocket );

				::exit( -1 );
			}

			::fprintf( stderr, "[CDSocketRead( %d %d %d %d %d ) Complete][Int:%d][Str:%s][L:%d]\n", 
				iClientSocket, 
				(char*)&stRecvPacket, 
				sizeof(stRecvPacket), 
				SOCKET_TIME_OUT, 
				&stRecvError, 
				stRecvPacket.iInt, 
				stRecvPacket.caString, 
				__LINE__ );

			::memset( &stSendPacket, 0x00, sizeof( stSendPacket ) );

			stSendPacket.iInt	= stRecvPacket.iInt + 1;
			::strcpy( stSendPacket.caString, "Socket Read" );

			// Ŭ���̾�Ʈ ������ ����(����) ���� �������� �˻�
			if( iResult = CDSocketGetSendable( iClientSocket ) != CD_SOCKET_STAT_SENDABLE )
			{
				::fprintf( stderr, "[CDSocketGetSendable( %d ) Error][R:%d][E:%d][L:%d]\n", 
					iClientSocket, 
					iResult, 
					errno, 
					__LINE__ );

				CDSocketClose( iServerSocket );
				CDSocketClose( iClientSocket );

				::exit( -1 );
			}

			// Ŭ���̾�Ʈ�� ��� ��Ŷ�� ����
			if( ( iResult = CDSocketWrite( iClientSocket, (char*)&stSendPacket, sizeof(stSendPacket), SOCKET_TIME_OUT, &stSendError ) ) == CD_SOCKET_ERROR )
			{
				::fprintf( stderr, "[CDSocketWrite( %d %d %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
					iClientSocket, 
					(char*)&stSendPacket, 
					sizeof(stSendPacket), 
					SOCKET_TIME_OUT, 
					&stSendError, 
					stSendError.iReturn, 
					stSendError.iErrno, 
					__LINE__ );

				CDSocketClose( iServerSocket );
				CDSocketClose( iClientSocket );

				::exit( -1 );
			}

			::fprintf( stderr, "[CDSocketWrite( %d %d %d %d %d ) Complete][Int:%d][Str:%s][L:%d]\n", 
				iClientSocket, 
				(char*)&stSendPacket, 
				sizeof(stSendPacket), 
				SOCKET_TIME_OUT, 
				&stSendError, 
				stSendPacket.iInt, 
				stSendPacket.caString, 
				__LINE__ );

			break;
		}

		::sleep( 1 );

		if( iResult == CD_SOCKET_STAT_ERROR )
		{
			::fprintf( stderr, "[CDSocketGetTimedReadable( %d %d ) Error][R:%d][E:%d][L:%d]\n", 
				iClientSocket, 
				SOCKET_TIME_OUT, 
				iResult, 
				errno, 
				__LINE__ );

			CDSocketClose( iServerSocket );
			CDSocketClose( iClientSocket );

			::exit( -1 );
		}
	}

	// Ŭ���̾�Ʈ�� ������ �����͸� �ۼ���
	while( 1 )
	{
		// ������ �ð�(SOCKET_TIME_OUT) ���� Ŭ���̾�Ʈ�� ������ �̺�Ʈ�� �˻�(�̺�Ʈ�� �Ͼ�� ��ȯ)
		iResult	= CDSocketGetTimedReadable( iClientSocket, SOCKET_TIME_OUT );

		// �̺�Ʈ�� �Ͼ ���(���� �� �����Ͱ� �ִ� ���)
		if( iResult == CD_SOCKET_STAT_READABLE )
		{
			::memset( &stRecvPacket, 0x00, sizeof( stRecvPacket ) );

			// Ŭ���̾�Ʈ�� ������ �����͸� ����
			if( ( iResult = CDSocketRecv( iClientSocket, (char*)&stRecvPacket, sizeof(stRecvPacket), SOCKET_TIME_OUT, &stRecvError ) ) == CD_SOCKET_ERROR )
			{
				::fprintf( stderr, "[CDSocketRecv( %d %d %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
					iClientSocket, 
					(char*)&stRecvPacket, 
					sizeof(stRecvPacket), 
					SOCKET_TIME_OUT, 
					&stRecvError, 
					stRecvError.iReturn, 
					stRecvError.iErrno, 
					__LINE__ );

				CDSocketClose( iServerSocket );
				CDSocketClose( iClientSocket );

				::exit( -1 );
			}

			::fprintf( stderr, "[CDSocketRecv( %d %d %d %d %d ) Complete][Int:%d][Str:%s][L:%d]\n", 
				iClientSocket, 
				(char*)&stRecvPacket, 
				sizeof(stRecvPacket), 
				SOCKET_TIME_OUT, 
				&stRecvError, 
				stRecvPacket.iInt, 
				stRecvPacket.caString, 
				__LINE__ );

			::memset( &stSendPacket, 0x00, sizeof( stSendPacket ) );

			stSendPacket.iInt	= stRecvPacket.iInt + 1;
			::strcpy( stSendPacket.caString, "Socket Recv" );

			// ������ �ð�(SOCKET_TIME_OUT) ���� Ŭ���̾�Ʈ ������ ����(����) ���� �������� �˻�(�̺�Ʈ�� �Ͼ�� ��ȯ)
			if( iResult = CDSocketGetTimedSendable( iClientSocket, SOCKET_TIME_OUT ) != CD_SOCKET_STAT_SENDABLE )
			{
				::fprintf( stderr, "[CDSocketGetTimedSendable( %d %d ) Error][R:%d][E:%d][L:%d]\n", 
					iClientSocket, 
					SOCKET_TIME_OUT, 
					iResult, 
					errno, 
					__LINE__ );

				CDSocketClose( iServerSocket );
				CDSocketClose( iClientSocket );

				::exit( -1 );
			}

			// Ŭ���̾�Ʈ�� ��� ��Ŷ�� ����
			if( ( iResult = CDSocketSend( iClientSocket, (char*)&stSendPacket, sizeof(stSendPacket), SOCKET_TIME_OUT, &stSendError ) ) == CD_SOCKET_ERROR )
			{
				::fprintf( stderr, "[CDSocketSend( %d %d %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
					iClientSocket, 
					(char*)&stSendPacket, 
					sizeof(stSendPacket), 
					SOCKET_TIME_OUT, 
					&stSendError, 
					stSendError.iReturn, 
					stSendError.iErrno, 
					__LINE__ );

				CDSocketClose( iServerSocket );
				CDSocketClose( iClientSocket );

				::exit( -1 );
			}

			::fprintf( stderr, "[CDSocketSend( %d %d %d %d %d ) Complete][Int:%d][Str:%s][L:%d]\n", 
				iClientSocket, 
				(char*)&stSendPacket, 
				sizeof(stSendPacket), 
				SOCKET_TIME_OUT, 
				&stSendError, 
				stSendPacket.iInt, 
				stSendPacket.caString, 
				__LINE__ );

			break;
		}

		::sleep( 1 );

		if( iResult == CD_SOCKET_STAT_ERROR )
		{
			::fprintf( stderr, "[CDSocketGetTimedReadable( %d %d ) Error][R:%d][E:%d][L:%d]\n", 
				iClientSocket, 
				SOCKET_TIME_OUT, 
				iResult, 
				errno, 
				__LINE__ );

			CDSocketClose( iServerSocket );
			CDSocketClose( iClientSocket );

			::exit( -1 );
		}
	}

	return 0;
}
