#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "header.h"

#include "client.h"
#include "CDSocket.h"


int main( int argc, char **argv )
{
	int					iResult;
	int					iSocket;

	// ���� ��û ��� �����Ͱ� ����Ǵ� ����ü
	stCDSocketCliErr	stClientError;

	// ������ �ۼ��� ��� �����Ͱ� ����Ǵ� ����ü
	stCDSocketUtilErr	stSendError;
	stCDSocketUtilErr	stRecvError;

	// ������ �ۼ��� �� ������(����ü)
	stTutorialPacket	stSendPacket;
	stTutorialPacket	stRecvPacket;

	::memset( &stClientError, 0x00, sizeof( stClientError ) );

	// ���� ���� �õ�
	while( 1 )
	{
		::sleep( 1 );

		// Ŭ���̾�Ʈ ���� ����, ���ε�, ���� ���� ���� �� ������ ���� �� ��ũ���� ȹ��
		if( ( iSocket = CDSocketMakeConnectTCP( (char*)SERVER_IP, SERVER_PORT, &stClientError ) ) == CD_SOCKET_ERROR )
		{
			::fprintf( stderr, "[CDSocketMakeConnectTCP( %s %d %d ) Error][R:%d][E:%d][%s][L:%d]\n", 
				(char*)SERVER_IP, 
				SERVER_PORT, 
				&stClientError, 
				stClientError.iReturn, 
				stClientError.iErrno, 
				stClientError.caErrStr, 
				__LINE__ );

			continue;
		}

		break;
	}

	::fprintf( stderr, "[CDSocketMakeConnectTCP( %s %d %d ) Complete][Fd:%d][L:%d]\n", 
		(char*)SERVER_IP, 
		SERVER_PORT, 
		&stClientError, 
		iSocket, 
		__LINE__ );

	::memset( &stSendPacket, 0x00, sizeof( stSendPacket ) );

	stSendPacket.iInt	= 100;
	::strcpy( stSendPacket.caString, "Socket Write" );

	// ������ ��û ��Ŷ ����(Write: CPU ���ϰ� �� �� ū ��� �ӵ��� ����)
	if( ( iResult = CDSocketWrite( iSocket, (char*)&stSendPacket, sizeof(stSendPacket), SOCKET_TIME_OUT, &stSendError ) ) == CD_SOCKET_ERROR )
	{
		::fprintf( stderr, "[CDSocketWrite( %d %d %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
			iSocket, 
			(char*)&stSendPacket, 
			sizeof(stSendPacket), 
			SOCKET_TIME_OUT, 
			&stSendError, 
			stSendError.iReturn, 
			stSendError.iErrno, 
			__LINE__ );

		CDSocketClose( iSocket );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDSocketWrite( %d %d %d %d %d ) Complete][Int:%d][Str:%s][L:%d]\n", 
		iSocket, 
		(char*)&stSendPacket, 
		sizeof(stSendPacket), 
		SOCKET_TIME_OUT, 
		&stSendError, 
		stSendPacket.iInt, 
		stSendPacket.caString, 
		__LINE__ );

	::memset( &stRecvPacket, 0x00, sizeof( stRecvPacket ) );

	// �����κ��� ��� ��Ŷ ����(Read: CPU ���ϰ� �� �� ū ��� �ӵ��� ����)
	if( ( iResult = CDSocketRead( iSocket, (char*)&stRecvPacket, sizeof(stRecvPacket), SOCKET_TIME_OUT, &stRecvError ) ) == CD_SOCKET_ERROR )
	{
		::fprintf( stderr, "[CDSocketRead( %d %d %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
			iSocket, 
			(char*)&stRecvPacket, 
			sizeof(stRecvPacket), 
			SOCKET_TIME_OUT, 
			&stRecvError, 
			stRecvError.iReturn, 
			stRecvError.iErrno, 
			__LINE__ );

		CDSocketClose( iSocket );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDSocketRead( %d %d %d %d %d ) Complete][Int:%d][Str:%s][L:%d]\n", 
		iSocket, 
		(char*)&stRecvPacket, 
		sizeof(stRecvPacket), 
		SOCKET_TIME_OUT, 
		&stRecvError, 
		stRecvPacket.iInt, 
		stRecvPacket.caString, 
		__LINE__ );

	::memset( &stSendPacket, 0x00, sizeof( stSendPacket ) );

	stSendPacket.iInt	= 200;
	::strcpy( stSendPacket.caString, "Socket Send" );

	// ������ ��û ��Ŷ ����(Send: CPU ���ϰ� ���� ��� �ӵ��� ����)
	if( ( iResult = CDSocketSend( iSocket, (char*)&stSendPacket, sizeof(stSendPacket), SOCKET_TIME_OUT, &stSendError ) ) == CD_SOCKET_ERROR )
	{
		::fprintf( stderr, "[CDSocketSend( %d %d %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
			iSocket, 
			(char*)&stSendPacket, 
			sizeof(stSendPacket), 
			SOCKET_TIME_OUT, 
			&stSendError, 
			stSendError.iReturn, 
			stSendError.iErrno, 
			__LINE__ );

		CDSocketClose( iSocket );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDSocketSend( %d %d %d %d %d ) Complete][Int:%d][Str:%s][L:%d]\n", 
		iSocket, 
		(char*)&stSendPacket, 
		sizeof(stSendPacket), 
		SOCKET_TIME_OUT, 
		&stSendError, 
		stSendPacket.iInt, 
		stSendPacket.caString, 
		__LINE__ );

	::memset( &stRecvPacket, 0x00, sizeof( stRecvPacket ) );

	// �����κ��� ��� ��Ŷ ����(Recv: CPU ���ϰ� ���� ��� �ӵ��� ����)
	if( ( iResult = CDSocketRecv( iSocket, (char*)&stRecvPacket, sizeof(stRecvPacket), SOCKET_TIME_OUT, &stRecvError ) ) == CD_SOCKET_ERROR )
	{
		::fprintf( stderr, "[CDSocketRecv( %d %d %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
			iSocket, 
			(char*)&stRecvPacket, 
			sizeof(stRecvPacket), 
			SOCKET_TIME_OUT, 
			&stRecvError, 
			stRecvError.iReturn, 
			stRecvError.iErrno, 
			__LINE__ );

		CDSocketClose( iSocket );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDSocketRecv( %d %d %d %d %d ) Complete][Int:%d][Str:%s][L:%d]\n", 
		iSocket, 
		(char*)&stRecvPacket, 
		sizeof(stRecvPacket), 
		SOCKET_TIME_OUT, 
		&stRecvError, 
		stRecvPacket.iInt, 
		stRecvPacket.caString, 
		__LINE__ );

	CDSocketClose( iSocket );

	return 0;
}
