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
//    1) Socket Interface�� Socket�� Handle�� ������ �ִ�.
//    2) Socket�� ���°��� ������ �ִ�.
//    3) Socket�� ������ �Ҹ��� ���� �Լ��� ������ �ִ�.
//    4) Socket�� ����� Reset�ϴ� �Լ��� ������ �ִ�.

// 2. CNNetSocket�� �ٽ�! Function�� ��� ����.
//    1) ProcessResetSocket()	Socket�� ���������� �����ϴ� �Լ��̴�.
//    2) ProcessCloseSocket()	Socket�� �ı������� �����ϴ� �Լ��̴�.
//    3) ResetSocket()			Socket�� ������ ���� Reset�ϴ� ������ �����ϴ� �Լ��̴�.
//    4) ��Ÿ�Լ���...
//       - Socket�� ���¸� Ȯ���ϴ� �Լ�.
//       - Socket�� Handle�� ���õ� �Լ�.
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
		// 1) Socket�� �غ��ϰ� ���� �� ȣ���ϴ� �Լ�.
		virtual	void		ProcessPrepareSocket() PURE;
		virtual	bool		ProcessCloseSocket() PURE;

	public:
		void				PrepareSocket()							{ ProcessPrepareSocket(); }
		bool				CloseSocket();

	// ****************************************************************************
	// Publics) 
	// ----------------------------------------------------------------------------
	public:
		// 1) Socket Handle����...
		SOCKET				SetSocketHandle( SOCKET _hSocket )		{ return (SOCKET)CNInterlockedExchange( (volatile unsigned long*)&m_hSocket, _hSocket ); }
		SOCKET				GetSocketHandle() const					{ return m_hSocket; }

		bool				IsInvalidSocket() const					{ return ( GetSocketHandle() == INVALID_SOCKET ); }
		bool				IsValidSocket() const					{ return ( GetSocketHandle() != INVALID_SOCKET ); }

		// 2) Socket State����.
		SOCKET_STATE		GetSocketState() const					{ return m_SocketStatus; }
		SOCKET_STATE		SetSocketState( SOCKET_STATE _Status )	{ return (SOCKET_STATE)CNInterlockedExchange( (volatile unsigned long*)&m_SocketStatus, _Status ); }
		SOCKET_STATE		SetSocketStateIf( SOCKET_STATE _StatusNew, SOCKET_STATE _StatusComperand )	{ return (SOCKET_STATE)CNInterlockedCompareExchange( (volatile unsigned long*)&m_SocketStatus, _StatusNew, _StatusComperand ); }

		bool				IsDisconnected() const					{ return ( m_SocketStatus == SOCKET_STATE_CLOSED ); }		// 0 : ������ ����� ����.
		bool				IsBinded() const						{ return ( m_SocketStatus == SOCKET_STATE_BINDED ); }		// 1 : Listen���ΰ�?
		bool				IsConnected() const						{ return ( m_SocketStatus >= SOCKET_STATE_ESTABLISHED ); }	// 2 : ���Ӹ� �� ����.
		bool				IsCertified() const						{ return ( m_SocketStatus >= SOCKET_STATE_CERTIFIED ); }	// 3 : ������ �ǰ� CNNet�� Client������ ���� ����.
		bool				IsLogined() const						{ return ( m_SocketStatus >= SOCKET_STATE_LOGINED ); }		// 4 : ID�� Password�� �ְ� Log-In�� �� ����.
		bool				IsListening() const						{ return ( m_SocketStatus == SOCKET_STATE_LISTEN ); }		// 9 : Listen���ΰ�?

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
