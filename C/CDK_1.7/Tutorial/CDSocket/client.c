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

	// 접속 요청 결과 데이터가 저장되는 구조체
	stCDSocketCliErr	stClientError;

	// 데이터 송수신 결과 데이터가 저장되는 구조체
	stCDSocketUtilErr	stSendError;
	stCDSocketUtilErr	stRecvError;

	// 서버와 송수신 할 데이터(구조체)
	stTutorialPacket	stSendPacket;
	stTutorialPacket	stRecvPacket;

	::memset( &stClientError, 0x00, sizeof( stClientError ) );

	// 서버 접속 시도
	while( 1 )
	{
		::sleep( 1 );

		// 클라이언트 소켓 생성, 바인드, 접속 과정 실행 후 서버와 접속 된 디스크립터 획득
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

	// 서버로 요청 패킷 전송(Write: CPU 부하가 좀 더 큰 대신 속도가 빠름)
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

	// 서버로부터 결과 패킷 수신(Read: CPU 부하가 좀 더 큰 대신 속도가 빠름)
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

	// 서버로 요청 패킷 전송(Send: CPU 부하가 적은 대신 속도가 느림)
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

	// 서버로부터 결과 패킷 수신(Recv: CPU 부하가 적은 대신 속도가 느림)
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

