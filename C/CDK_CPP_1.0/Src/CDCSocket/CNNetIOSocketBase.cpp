#include "CNNetIOSocketBase.h"

#include "CNExecuteCompletionport.h"

extern	CNExecute::CCompletionPort*		g_pExecutorCompletionPort;
extern	CNAPI::CIOCompletionPort*		g_pIOCP;

CNNetIO::Socket::NBase::~NBase()
{
	//-----------------------------------------------------------------
	// 1. Socket�� �ϴ� �ݴ´�.
	//-----------------------------------------------------------------
	// 1) Socket�� �ݴ´�.
	ProcessCloseSocket();
}


//*****************************************************************************
// 
// 
// Operation�Լ� ó��
// 
// 
//*****************************************************************************
void CNNetIO::Socket::NBase::ProcessPrepareSocket()
{
	//-----------------------------------------------------------------
	// 1) Socket Option�����ϱ�.
	//-----------------------------------------------------------------
	//1. Socket�� ���� �����.
	if( GetSocketHandle() == INVALID_SOCKET )
	{
		// - Socket�� ���� �����.
		CreateNewSocketHandle();

		// - Setting�Ѵ�.
		m_pConnectable	 = dynamic_cast<CNNetIO::IConnectable*>(this);
		m_pReceivable	 = dynamic_cast<CNNetIO::IReceivable*>(this);
	}

	//2. Socket�� State �� Closed�� �Ѵ�.
	SetSocketState( SOCKET_STATE_CLOSED );

	//-----------------------------------------------------------------
	// 2) Interface�� Reset
	//-----------------------------------------------------------------
	//1. Connectable�� Reset�Ѵ�.
	if(m_pConnectable != NULL)
	{
		m_pConnectable->ResetConnectable();
	}

	//2. Receivable�� Reset�Ѵ�.
	if(m_pReceivable != NULL)
	{
		m_pReceivable->ResetReceivable();
	}

	//-----------------------------------------------------------------
	// 3. Reset�� �����Ѵ�.
	//-----------------------------------------------------------------
	//1. Reset Socket�� �����Ѵ�.
	OnPrepareSocket();
}


bool CNNetIO::Socket::NBase::ProcessCloseSocket()
{
	//-----------------------------------------------------------------
	// 1. Socket�� ��´�.
	//    - �� �� Interlocked�Լ��� ����Ͽ� Socket�� �����ϰ� ����
	//      Socket Handle�� ����. �� ���� Socket Handle�� 
	//      INVALID_SOCKET�� �ƴϸ� Socketdmf �ݴ´�.
	//-----------------------------------------------------------------
	SOCKET	hSocket	 = SetSocketHandle( INVALID_SOCKET );

	// Check) p_pSocket�� NULL�̾�� �ȵȴ�.
	RETURN_IF( hSocket == INVALID_SOCKET, FALSE );

	//-----------------------------------------------------------------
	// 2. SocketHandle�� INVALID_SOCKET���� �ٲ۴�.
	//-----------------------------------------------------------------
	// 1) Abortive disconnect�� �ɼ��� �����Ѵ�.
	CNNetAPI::Socket::GetInstance().SetLingerOption( hSocket, TRUE );

	// 2) Socket�� �ݴ´�.
	CNNetAPI::Socket::GetInstance().CloseSocket( hSocket );

	//-----------------------------------------------------------------
	// 3. OnClose�� ȣ���Ѵ�.
	//-----------------------------------------------------------------
	// 1) OnCloseSocket�Լ��� ȣ���Ѵ�.
	OnCloseSocket();

	//-----------------------------------------------------------------
	// 4. IO�� �Ϸ�� ������ ��ٸ���.
	//-----------------------------------------------------------------
	// 1) Executor�� ���� IOCP�� I/O�� ó���Ѵ�.
	//while(Statistics_GetNowOverlappedIO()!=0)
	//{
	//	RunExecutor();
	//}

	// Return) ����~
	return	TRUE;
}


void CNNetIO::Socket::NBase::CreateNewSocketHandle()
{
	//-----------------------------------------------------------------
	// 1. �� Socket �����ϱ�.
	//-----------------------------------------------------------------
	// Declare) 
	SOCKET		hNewSocket;
	BOOL		bResult;

	// 1) Socket ����
	hNewSocket	 = CNNetAPI::Socket::GetInstance().CreateSocket( m_iSocketType, m_iProtocolInfo );

	// Check) Socket Option�� ���࿡ �����ߴ°�?
	CNNET_THROW_IF( hNewSocket == INVALID_SOCKET, CNException::NetIO::FailToCreateSocket(), ERROR_TRACE( "[Error : Socket�� ������ �����߽��ϴ�. File:%s Line:%d]\n", __FILE__, __LINE__ ) );

	// 2) Send Buffer����.
	bResult	 = CNNetAPI::Socket::GetInstance().SetSendBufferSize( hNewSocket, SIZE_OF_SOCKET_SEND_BUFFER );

	// Check) Socket Option�� ���࿡ �����ߴ°�?
	CNNET_THROW_IF( bResult == FALSE, CNException::NetIO::FailToSetSocketOption(), ERROR_TRACE( "[Error: %dByte�� Socket Send Bufffer ���� �����߽��ϴ�. File:%s Line:%d]\n", SIZE_OF_SOCKET_SEND_BUFFER, __FILE__, __LINE__ ) );

	// 3) Receive Buffuer�� �����Ѵ�.
	bResult	 = CNNetAPI::Socket::GetInstance().SetReceiveBufferSize( hNewSocket, SIZE_OF_SOCKET_RECEIVE_BUFFER );

	// Check) Socket Option�� ���࿡ �����ߴ°�?
	CNNET_THROW_IF( bResult == FALSE, CNException::NetIO::FailToSetSocketOption(), ERROR_TRACE( "[Error : %dByte�� Socket Receive Bufffer ���� �����߽��ϴ�. File:%s Line:%d]\n", SIZE_OF_SOCKET_RECEIVE_BUFFER, __FILE__, __LINE__ ) );

	//-----------------------------------------------------------------
	// 2. �� Socket �����Ѵ�.
	//-----------------------------------------------------------------
	// 1) ������ Socket�� �ִٸ� ���� Close�Ѵ�.
	if( GetSocketHandle() != INVALID_SOCKET )
	{
		// - ���� Socket�� �ݴ´�.
		CNNetAPI::Socket::GetInstance().CloseSocket( GetSocketHandle() );
	}

	// 2) �����Ѵ�.
	SetSocketHandle( hNewSocket );
}
