#ifndef	__CNNET_SOCKET_TCP__
#define	__CNNET_SOCKET_TCP__

#include "CNNetIOSocketBase.h"
#include "CNNetIOConnectableTCP.h"
#include "CNNetIOReceivableStream.h"

//-----------------------------------------------------------------------------
//
// CNNetSocket::CTCP
//
// 1. CNNetSocket::CTCP��!
//    1) TCP�� Send/Receive/Connect ���� ������ �� �ִ� Socket��. 
//    2) �⺻������ ������ ���� Recycleó���� ��� ����.
//    3) �ҷ��� ����ó���� ������ �Ͽ����Ƿ� Socket Manager�� Socket Pool ����
//       �������� �ʴ´�.
//    4) �׷��� ���ſ� ���� �����ӵ��� ũ�� �����Ͽ��� ���� Pending�������� ����
//       Failure�� ���� ���ݴ�.
//
//
// 2. CNNetSocket::CTCP�� �ٽ�! Function�� ��� ����
//    1) ó���Լ��� �⺻���� I/Oó���� ���� ó���Լ����̴�.
//					���������� ���� I/O�� ó���ϱ� ���� �ʿ��� �Լ��鿡 ���ؼ� ������ ���̴�.
//    2) �����Լ���	Socket�� �� ������ �����ϴ� �Լ����̴�.
//    3) User�Լ���	�����Ǹ� ���� ������� �Լ��̴�. ���α׷��Ӱ� �� �κе��� ������
//					������ Socket�� ���ӿ� �°� ����� �ȴ�.
//
//
// 3. ��� �Լ���.
//
//    1) Socket����...
//	     - SOCKET_STATUS	GetSocketState();
//       - LPSOCKADDR_IN	GetSocketAddress();
//       - LPSOCKADDR_IN	GetPeerAddress();
//
//    2) Connectable.
//	     - BOOL	Connect(LPCSOCKADDR p_pSockAddr, int p_nSockAddrLen);
//       - BOOL	Disconnect();
//
//    3) Sendable����...
// 	     - BOOL	Send(const CCNNetBuffer& p_rBuffer);
//
//    4) Socket�Ӽ� ����...
//
//
// 4. �����ǿ� Hook�Լ���.
//
//    1) OnPrepareSocket()
//
//		virtual	void	OnPrepareSocket();
//
//       - Socket�� �����ϰų� Pool��� �Ҵ�޾Ƽ� ����Ҷ� Socket�� Reset��
//         �Ѵ�. �̶� �ҷ����� �Լ��̴�.
//       - �Ҽ�) CNNetIO::Socket::CTCP
//
//
//    2) OnClose()
//
//		virtual	void	OnClose();
//
//       - Socket Handle�� ���� �� �ҷ����� �Լ��̴�. ��������ʹ� �������.
//       - �Ҽ�) CNNetIO::Socket::CTCP
//
//    3) OnFree()
//       - Socket�� �������Ḧ �� ���� Socket�� ���� ���۰� ������ ��� �Ϸ�
//         �Ǿ��� �� �ҷ����� �Լ��̴�.
//       - �Ϲ������� Disconnect�Ǵ��� Receive����� Send�� �ɷ��ִ� ��찡
//         ����. Disconnect���� �̷� ������ ��� �Ϸ�Ǿ� Socket����� ���
//         �Ϸ�Ǹ� �Ҹ��� �Լ��̴�
//       - �� �Լ��� ȣ��Ǿ��� �� Socket�� ����ų� Pool�� �ǵ����� �ȴ�.
//       - �Ҽ�) ICNReferenceCount (CNNetIO::Socket::CTCP)
//
//    4) OnConnect()
//
//		virtual void	OnConnect();
//
//       - ������ �����Ǿ��� �� ȣ��Ǵ� �Լ��̴�.
//       - Accept�� Ȥ�� Connect�� ���ؼ� ���� ��ο� �ش��Ѵ�.
//       - �Ҽ�) CNNetIO::Connectable::CTCP
//        
//    5) OnFailconnect()
//
//		virtual void	OnFailConnect(int p_ErrorCode);
//
//       - Connect�Լ��� ȣ���Ͽ� ������ �������� ������ �� ȣ��ȴ�.
//       - �Ҽ�) CNNetIO::Connectable::CTCP
// 
//    6) OnDisconnect()
//
//		virtual void	OnDisconnect();
//
//       - ������ ����Ǿ��� �� ȣ��Ǵ� �Լ��̴�. (������ ����Ǿ��ٰ� ������
//         Socket�� ����� ����� ���� �ƴϴ�.)
//       - �Ҽ�) CNNetIO::Connectable::CTCP
//
//    7) OnReceive()
//
//		virtual	void	OnReceive(const CCNWSABuffer& p_rWSABuffer, DWORD p_dwTransferred, LPCSOCKADDR_IN p_pSockAddr);
//
//       - �����͸� ���۹޾��� �� ȣ��ȴ�.
//       - �̶� ���۵Ǿ�� �����͸� ���۵Ǿ�� ������ �� ��ü�� Packetȭ����
//         ���� �����̹Ƿ� �̶� �����͸� ó���ϸ� �ȵȴ�.
//       - �Ҽ�) CNNetIO::Receivable::CTCP
//
//    8) OnFailReceive()
//
//		virtual void	OnFailReceive(int p_ErrorCode, const CCNWSABuffer& p_rWSABuffer, LPCSOCKADDR_IN p_pSockAddr);
//
//       - Receive�� �ϷḦ �������� �ҷ����� �Լ��̴�. �Ϲ������� ������ ����
//         ������ ���� Receive�� �Ϸ���� ������ �� �ҷ�����.
//       - �Ҽ�) CNNetIO::Receivable::CStream
//  
///   9) OnMessage()
//
//		virtual BOOL	OnMessage(const CCNNetBuffer& p_pBuffer, LPCSOCKADDR_IN p_pSockAddr) PURE;
//
//       - ���۹޾� Packet�� �ϼ��Ǿ��� �� Packet���� ȣ��Ǵ� �Լ��̴�.
//       - OnMessage()�Լ��� �Ѿ���� �����ʹ� �ϳ��� �ϼ��� Packet�̴�.
//       - �Ҽ�) CNNetIO::Messageable::CBase
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
