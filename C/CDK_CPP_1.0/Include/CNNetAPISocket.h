#ifndef	__CNNET_API_SOCKET__
#define	__CNNET_API_SOCKET__

#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <stropts.h>
#include <arpa/inet.h>
#include <sys/socket.h>

#include "CND.h"
#include "CNCommonUtil.h"
#include "CNDefinitions.h"
#include "CNNetDefinitions.h"

namespace CNNetAPI
{

class Socket
{
	// Constructor/Destructor)
	private:
		Socket();
		~Socket();

	public:
		// 1) Get Instance
		static	Socket&		GetInstance()	{ static Socket socket; return socket; }

		// 2) Create Socket
		SOCKET				CreateSocket( int _iSocketType = SOCK_STREAM, int _iProtocolInfo = IPPROTO_TCP, int _iDomain = AF_INET );
	
		// 3) Basic Functions
		bool				GetPeerName( SOCKET _iSocket, LPSOCKADDR _pSockAddr, int* _pSockAddrLen )													{ return getpeername( _iSocket, _pSockAddr, _pSockAddrLen ) != SOCKET_ERROR; }
		bool				GetSockName( SOCKET _iSocket, LPSOCKADDR _pSockAddr, int* _pSockAddrLen )													{ return getsockname( _iSocket, _pSockAddr, _pSockAddrLen ) != SOCKET_ERROR; }

		bool				SetSockOpt( SOCKET _iSocket, int _iLevel, int _iOptionName, const void* _pOptionValue, int _iOptionLen )					{ return setsockopt( _iSocket, _iLevel, _iOptionName, _pOptionValue, _iOptionLen ) != SOCKET_ERROR; }
		bool				GetSockOpt( SOCKET _iSocket, int _iLevel, int _iOptionName, void* _pOptionValue, int _pOptionLen ) 							{ return getsockopt( _iSocket, _iLevel, _iOptionName, _pOptionValue, &_pOptionLen ) != SOCKET_ERROR; }

		int					GetMaxMsgSize( SOCKET _iSocket, int _iType = SO_RCVBUF )																	{ int iMaxMsgSize; if( GetSockOpt( _iSocket, SOL_SOCKET, _iType, &iMaxMsgSize, sizeof(int) ) == FALSE ) return SOCKET_ERROR; return iMaxMsgSize; }
		bool				SetLingerOption( SOCKET _iSocket, int _iEnable, int _iTimeOut = 0 )												 			{ linger tempLinger = { _iEnable, _iTimeOut }; return SetSockOpt( _iSocket, SOL_SOCKET, SO_LINGER, &tempLinger, sizeof(linger) ); }
		bool				SetReuseAddress( SOCKET _iSocket, int _iEnable )																			{ return SetSockOpt( _iSocket, SOL_SOCKET, SO_REUSEADDR, &_iEnable, sizeof(_iEnable) ); }
		bool				SetNonBlock( SOCKET _iSocket )																								{ return fcntl( _iSocket, F_SETFL, O_NDELAY | O_NONBLOCK ) > SOCKET_ERROR; }

		bool				SetSendBufferSize( SOCKET _iSocket, int _iSize )																			{ return SetSockOpt( _iSocket, SOL_SOCKET, SO_SNDBUF, &_iSize, sizeof(_iSize) ); }
		bool				SetReceiveBufferSize( SOCKET _iSocket, int _iSize )																			{ return SetSockOpt( _iSocket, SOL_SOCKET, SO_RCVBUF, &_iSize, sizeof(_iSize) ); }

		// 4) Bind & Connect & Accept & Close
		bool				Bind( SOCKET _iSocket, LPSOCKADDR _pSocketAddr, int _iSockAddrLen = sizeof( SOCKADDR_IN ) );
		bool				Listen( SOCKET _iSocket, int _iBacklog = SIZE_OF_LISTEN_BACKLOG ) 															{ return listen( _iSocket, _iBacklog ) != SOCKET_ERROR; }
		SOCKET				Connect( SOCKET _iSocket, LPSOCKADDR _pSockAddr, int _iSockAddrLen = sizeof( SOCKADDR_IN ) );
		bool				CloseSocket( SOCKET _iSocket );
		bool				Shutdown( SOCKET _iSocket, int _iHow = SHUT_WR ) 																			{ return shutdown( _iSocket, _iHow ) != SOCKET_ERROR; }
		SOCKET				Accept( SOCKET _iSocket, LPSOCKADDR _pSocketAddr, int _iSockAddrLen = sizeof( SOCKADDR_IN ) );
		bool				CloseSignalPipe();

	public:
		volatile unsigned long		m_nSocket;		// Socket¿« ºˆ¿”.
};

}

#endif

