#ifndef	__CNNET_IO_SOCKET_BASE__
#define	__CNNET_IO_SOCKET_BASE__

#include "CNNetIOSocket.h"
#include "CNNetAPISocket.h"
#include "CNNetIOReceivable.h"
#include "CNNetIOConnectable.h"
#include "CNNetExceptions.h"

//-----------------------------------------------------------------------------
//
// CNNetIO::Socket::NBase
//
// 1. CNNetIO::Socket::NBase�� Ư¡!
//    1) Socket����� �ϴ� Class�� UDP Socket���� �ʱ�ȭ�� �����ϴ� Socket�̴�. 
//    2) ��κ� �������� ������ �����ϴ� class�̹Ƿ� Ư���� ������ �κ��� ����.
//
// 2. CNNetIO::Socket::NBase
//    1) ProcessResetSocket	Socket�� �ʱ�ȭ�ϴ� ������ ������ �Լ��̴�.
//    2) ProcessCloseSocket	Socket�� �ݴ� ������ ������ �Լ��̴�.
//
// 3. �����ϱ�.
//    1) TCP Socket���� �����ϱ�.
//		 TCP�� ����� �Ʒ��� ���� �����ϸ� �ȴ�.
//
//        NBase(SOCK_STREAM, IPPROTO_TCP);
//
//-----------------------------------------------------------------------------
namespace CNNetIO
{
namespace Socket
{

class NBase :
// ****************************************************************************
// Inherited classes)
// ----------------------------------------------------------------------------
	virtual public				ISocket							//     ISocket
{
// ****************************************************************************
// Constructor/Destructor)
// ----------------------------------------------------------------------------
protected:
			NBase(int _iSocketType, int _iProtocolInfo )	{ m_iSocketType = _iSocketType; m_iProtocolInfo = _iProtocolInfo; }
			~NBase();

// ****************************************************************************
// Frameworks)
// ----------------------------------------------------------------------------
protected:
	// 1) ProcessResetSocket()�Լ��� ����ɶ� ȣ��Ǵ� On�Լ��̴�.
	virtual	void				OnPrepareSocket()						{}
	virtual	void				OnCloseSocket()							{}


// ****************************************************************************
// Implementation)
// ----------------------------------------------------------------------------
protected:
	// 1) Socket�� Reset�ϰų� Close�Ҷ��� ������ ������ �Լ�.
	//    (�ʿ��� ��� �������Ҽ� �ִ�.)
	virtual	void				ProcessPrepareSocket();
	virtual	bool				ProcessCloseSocket();

	// 2) ���ο� SocketHandle�Ե�� �Լ�.
			void				CreateNewSocketHandle();

private:
	// 3) SocketType & Protocol
			int					m_iSocketType;
			int					m_iProtocolInfo;

private:
			CNNetIO::IConnectable*		m_pConnectable;
			CNNetIO::IReceivable*		m_pReceivable;
};

}

}

extern	CNAPI::CIOCompletionPort*	g_pIOCP;
extern	CNExecute::IBase*			g_pExecutor;

#endif
