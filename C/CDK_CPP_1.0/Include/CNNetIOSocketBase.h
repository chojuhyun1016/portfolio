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
// 1. CNNetIO::Socket::NBase의 특징!
//    1) Socket기능을 하는 Class로 UDP Socket으로 초기화를 수행하는 Socket이다. 
//    2) 대부분 내부적인 동작을 수행하는 class이므로 특별히 조작할 부분은 없다.
//
// 2. CNNetIO::Socket::NBase
//    1) ProcessResetSocket	Socket를 초기화하는 과정을 설정한 함수이다.
//    2) ProcessCloseSocket	Socket을 닫는 과정을 설정한 함수이다.
//
// 3. 설정하기.
//    1) TCP Socket으로 설정하기.
//		 TCP의 경우은 아래와 같이 설정하면 된다.
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
	// 1) ProcessResetSocket()함수가 수행될때 호출되는 On함수이다.
	virtual	void				OnPrepareSocket()						{}
	virtual	void				OnCloseSocket()							{}


// ****************************************************************************
// Implementation)
// ----------------------------------------------------------------------------
protected:
	// 1) Socket을 Reset하거나 Close할때의 과정을 정의한 함수.
	//    (필요할 경우 재정의할수 있다.)
	virtual	void				ProcessPrepareSocket();
	virtual	bool				ProcessCloseSocket();

	// 2) 새로운 SocketHandle함드는 함수.
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

