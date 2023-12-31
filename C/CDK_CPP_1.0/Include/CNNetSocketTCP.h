#ifndef	__CNNET_SOCKET_TCP__
#define	__CNNET_SOCKET_TCP__

#include "CNNetIOSocketBase.h"
#include "CNNetIOConnectableTCP.h"
#include "CNNetIOReceivableStream.h"

//-----------------------------------------------------------------------------
//
// CNNetSocket::CTCP
//
// 1. CNNetSocket::CTCP란!
//    1) TCP의 Send/Receive/Connect 등을 수행할 수 있는 Socket임. 
//    2) 기본적으로 재사용을 위한 Recycle처리가 들어 있음.
//    3) 소량의 연결처리를 전제로 하였으므로 Socket Manager와 Socket Pool 등은
//       지원하지 않는다.
//    4) 그래도 과거에 비해 반응속도가 크게 증가하였고 각종 Pending현상으로 인한
//       Failure를 거의 없앴다.
//
//
// 2. CNNetSocket::CTCP의 핵심! Function의 기능 설명
//    1) 처리함수들 기본적인 I/O처리를 위한 처리함수들이다.
//					내부적으로 각종 I/O를 처리하기 위해 필요한 함수들에 대해서 정의한 것이다.
//    2) 진행함수들	Socket의 각 동작을 정의하는 함수들이다.
//    3) User함수들	재정의를 위해 만들어진 함수이다. 프로그래머가 이 부분들을 재정의
//					함을써 Socket을 게임에 맞게 만들게 된다.
//
//
// 3. 사용 함수들.
//
//    1) Socket관련...
//	     - SOCKET_STATUS	GetSocketState();
//       - LPSOCKADDR_IN	GetSocketAddress();
//       - LPSOCKADDR_IN	GetPeerAddress();
//
//    2) Connectable.
//	     - BOOL	Connect(LPCSOCKADDR p_pSockAddr, int p_nSockAddrLen);
//       - BOOL	Disconnect();
//
//    3) Sendable관련...
// 	     - BOOL	Send(const CCNNetBuffer& p_rBuffer);
//
//    4) Socket속성 관련...
//
//
// 4. 재정의용 Hook함수들.
//
//    1) OnPrepareSocket()
//
//		virtual	void	OnPrepareSocket();
//
//       - Socket을 생성하거나 Pool등에서 할당받아서 사용할때 Socket을 Reset을
//         한다. 이때 불려지는 함수이다.
//       - 소속) CNNetIO::Socket::CTCP
//
//
//    2) OnClose()
//
//		virtual	void	OnClose();
//
//       - Socket Handle을 닫을 때 불려지는 함수이다. 접속종료와는 상관없다.
//       - 소속) CNNetIO::Socket::CTCP
//
//    3) OnFree()
//       - Socket이 접속종료를 한 이후 Socket에 대한 조작과 참조가 모두 완료
//         되었을 때 불려지는 함수이다.
//       - 일반적으로 Disconnect되더라도 Receive라든지 Send가 걸려있는 경우가
//         많다. Disconnect이후 이런 조작이 모두 완료되어 Socket사용이 모두
//         완료되면 불리는 함수이다
//       - 이 함수가 호출되었을 대 Socket을 지우거나 Pool로 되돌리면 된다.
//       - 소속) ICNReferenceCount (CNNetIO::Socket::CTCP)
//
//    4) OnConnect()
//
//		virtual void	OnConnect();
//
//       - 접속이 성립되었을 때 호출되는 함수이다.
//       - Accept나 혹은 Connect를 통해서 접속 모두에 해당한다.
//       - 소속) CNNetIO::Connectable::CTCP
//        
//    5) OnFailconnect()
//
//		virtual void	OnFailConnect(int p_ErrorCode);
//
//       - Connect함수를 호출하여 접속을 성공하지 못했을 때 호출된다.
//       - 소속) CNNetIO::Connectable::CTCP
// 
//    6) OnDisconnect()
//
//		virtual void	OnDisconnect();
//
//       - 접속이 종료되었을 때 호출되는 함수이다. (접속이 종료되었다고 할지라도
//         Socket의 사용이 종료된 것은 아니다.)
//       - 소속) CNNetIO::Connectable::CTCP
//
//    7) OnReceive()
//
//		virtual	void	OnReceive(const CCNWSABuffer& p_rWSABuffer, DWORD p_dwTransferred, LPCSOCKADDR_IN p_pSockAddr);
//
//       - 데이터를 전송받았을 때 호출된다.
//       - 이때 전송되어온 데이터를 전송되어온 데이터 그 자체로 Packet화되지
//         않은 상태이므로 이때 데이터를 처리하면 안된다.
//       - 소속) CNNetIO::Receivable::CTCP
//
//    8) OnFailReceive()
//
//		virtual void	OnFailReceive(int p_ErrorCode, const CCNWSABuffer& p_rWSABuffer, LPCSOCKADDR_IN p_pSockAddr);
//
//       - Receive의 완료를 실패했을 불려지는 함수이다. 일반적으로 접속이 종료
//         등으로 인해 Receive가 완료되지 못했을 때 불려진다.
//       - 소속) CNNetIO::Receivable::CStream
//  
///   9) OnMessage()
//
//		virtual BOOL	OnMessage(const CCNNetBuffer& p_pBuffer, LPCSOCKADDR_IN p_pSockAddr) PURE;
//
//       - 전송받아 Packet이 완성되었을 때 Packet마다 호출되는 함수이다.
//       - OnMessage()함수로 넘어오는 데이터는 하나의 완성된 Packet이다.
//       - 소속) CNNetIO::Messageable::CBase
//
//-----------------------------------------------------------------------------
namespace CNNetSocket
{

class CTCP :
// ****************************************************************************
// Inherited classes) 
// ----------------------------------------------------------------------------
	public						CNNetIO::Socket::NBase,					// (@) Socket TCP
	public						CNNetIO::Connectable::NTCP,				// (@) Connectable TCP
	public						CNNetIO::Receivable::NStream			// (@) Receivable TCP
{
// ****************************************************************************
// Constructor/Destructor)
// ----------------------------------------------------------------------------
public:
			CTCP() : CNNetIO::Socket::NBase( SOCK_STREAM, IPPROTO_TCP )	{}
	virtual	~CTCP()														{}
};


}

#endif

