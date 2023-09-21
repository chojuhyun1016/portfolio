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

	// 주소(IP) 문자열이 저장 될 버퍼
	char	caAdressBuffer[256];

	// 호스트 이름이 저장 될 버퍼
	char	caHostNameBuffer[256];

	// 서버 소켓 생성 및 Accept 결과 데이터가 저장되는 구조체
	stCDSocketSvrErr	stServerError;

	// 클라이언트와의 데이터 송수신 결과 데이터가 저장되는 구조체
	stCDSocketUtilErr	stSendError;
	stCDSocketUtilErr	stRecvError;

	// 클라이언트와 송수신 할 데이터(구조체)
	stTutorialPacket	stSendPacket;
	stTutorialPacket	stRecvPacket;

	::memset( &stServerError, 0x00, sizeof( stServerError ) );

	// 서버 소켓 생성
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

	// 소켓의 상태를 획득
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

	// 클라이언트의 접속을 기다림
	while( 1 )
	{
		// 서버 소켓의 이벤트 검사
		iResult	= CDSocketGetReadable( iServerSocket );

		// 이벤트가 있을경우 Accept 수행
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

	// 시스템에서 서버의 호스트 이름을 획득
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

	// 호스트 이름으로 서버의 주소(IP)를 획득
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

	// 접속 된 클라이언트의 소켓에서 클라이언트의 주소(IP)를 획득
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


	// 클라이언트로 부터의 데이터를 송수신
	while( 1 )
	{
		// 클라이언트 소켓의 이벤트 검사
		iResult	= CDSocketGetReadable( iClientSocket );

		// 이벤트가 일어난 경우(전송 된 데이터가 있는 경우)
		if( iResult == CD_SOCKET_STAT_READABLE )
		{
			::memset( &stRecvPacket, 0x00, sizeof( stRecvPacket ) );

			// 클라이언트로 부터의 데이터를 수신
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

			// 클라이언트 소켓이 쓰기(전송) 가능 상태인지 검사
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

			// 클라이언트로 결과 패킷을 전송
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

	// 클라이언트로 부터의 데이터를 송수신
	while( 1 )
	{
		// 정해진 시간(SOCKET_TIME_OUT) 동안 클라이언트로 소켓의 이벤트를 검사(이벤트가 일어나면 반환)
		iResult	= CDSocketGetTimedReadable( iClientSocket, SOCKET_TIME_OUT );

		// 이벤트가 일어난 경우(전송 된 데이터가 있는 경우)
		if( iResult == CD_SOCKET_STAT_READABLE )
		{
			::memset( &stRecvPacket, 0x00, sizeof( stRecvPacket ) );

			// 클라이언트로 부터의 데이터를 수신
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

			// 정해진 시간(SOCKET_TIME_OUT) 동안 클라이언트 소켓이 쓰기(전송) 가능 상태인지 검사(이벤트가 일어나면 반환)
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

			// 클라이언트로 결과 패킷을 전송
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

