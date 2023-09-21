#ifndef	__CNNET_IO_SOCKET__
#define	__CNNET_IO_SOCKET__

#include "CND.h"
#include "CNDefinitions.h"
#include "CNNetDefinitions.h"

#include "CNExecuteBase.h"
#include "CNAPIIOCPCompletionPort.h"

//-----------------------------------------------------------------------------
//
// CNNetIO::ISocket
//
// 1. CNNetIO::ISocket
//    1) Socket Interface로 Socket의 Handle을 가지고 있다.
//    2) Socket의 상태값을 가지고 있다.
//    3) Socket의 생성과 소멸을 위한 함수를 가지고 있다.
//    4) Socket을 재사용시 Reset하는 함수를 가지고 있다.

// 2. CNNetSocket의 핵심! Function의 기능 설명.
//    1) ProcessResetSocket()	Socket의 생성과정을 정의하는 함수이다.
//    2) ProcessCloseSocket()	Socket의 파괴과정을 정의하는 함수이다.
//    3) ResetSocket()			Socket의 재사용을 위해 Reset하는 과정을 정의하는 함수이다.
//    4) 기타함수들...
//       - Socket의 상태를 확인하는 함수.
//       - Socket의 Handle에 관련된 함수.
//
//
//-----------------------------------------------------------------------------

namespace CNNetIO
{

class ISocket
{
	// ****************************************************************************
	// Constructor/Destructor)
	// ----------------------------------------------------------------------------
	public:
		ISocket()				{ m_hSocket = INVALID_SOCKET; SetSocketState( SOCKET_STATE_CLOSED ); }
		virtual	~ISocket()		{}


	// ****************************************************************************
	// Frameworks) 
	// ----------------------------------------------------------------------------
	protected:
		// 1) Socket을 준비하고 닫을 때 호출하는 함수.
		virtual	void		ProcessPrepareSocket() PURE;
		virtual	bool		ProcessCloseSocket() PURE;

	public:
		void				PrepareSocket()							{ ProcessPrepareSocket(); }
		bool				CloseSocket();

	// ****************************************************************************
	// Publics) 
	// ----------------------------------------------------------------------------
	public:
		// 1) Socket Handle관련...
		SOCKET				SetSocketHandle( SOCKET _hSocket )		{ return (SOCKET)CNInterlockedExchange( (volatile unsigned long*)&m_hSocket, _hSocket ); }
		SOCKET				GetSocketHandle() const					{ return m_hSocket; }

		bool				IsInvalidSocket() const					{ return ( GetSocketHandle() == INVALID_SOCKET ); }
		bool				IsValidSocket() const					{ return ( GetSocketHandle() != INVALID_SOCKET ); }

		// 2) Socket State관련.
		SOCKET_STATE		GetSocketState() const					{ return m_SocketStatus; }
		SOCKET_STATE		SetSocketState( SOCKET_STATE _Status )	{ return (SOCKET_STATE)CNInterlockedExchange( (volatile unsigned long*)&m_SocketStatus, _Status ); }
		SOCKET_STATE		SetSocketStateIf( SOCKET_STATE _StatusNew, SOCKET_STATE _StatusComperand )	{ return (SOCKET_STATE)CNInterlockedCompareExchange( (volatile unsigned long*)&m_SocketStatus, _StatusNew, _StatusComperand ); }

		bool				IsDisconnected() const					{ return ( m_SocketStatus == SOCKET_STATE_CLOSED ); }		// 0 : 접속이 종료된 상태.
		bool				IsBinded() const						{ return ( m_SocketStatus == SOCKET_STATE_BINDED ); }		// 1 : Listen중인가?
		bool				IsConnected() const						{ return ( m_SocketStatus >= SOCKET_STATE_ESTABLISHED ); }	// 2 : 접속만 된 상태.
		bool				IsCertified() const						{ return ( m_SocketStatus >= SOCKET_STATE_CERTIFIED ); }	// 3 : 접속이 되고 CNNet의 Client검증이 끝난 상태.
		bool				IsLogined() const						{ return ( m_SocketStatus >= SOCKET_STATE_LOGINED ); }		// 4 : ID와 Password를 넣고 Log-In이 된 상태.
		bool				IsListening() const						{ return ( m_SocketStatus == SOCKET_STATE_LISTEN ); }		// 9 : Listen중인가?

	// ****************************************************************************
	// Implementation)
	// ----------------------------------------------------------------------------
	private:
		volatile SOCKET				m_hSocket;
		volatile SOCKET_STATE		m_SocketStatus;
};


namespace Socket
{
	BOOL				InitSocketExecutor( int _iThread = 0 );
	BOOL				InitSocketExecutor( CNAPI::CIOCompletionPort* _pIOCP );
	void				CloseSocketExecutor();
	void				RunExecutor();
	int					GetExecutorThreadCount();

	CNExecute::IBase*	GetDefaultExecutor();
}

}

#endif

