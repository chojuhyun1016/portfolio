#include "CNNetIOSocketBase.h"

#include "CNExecuteCompletionport.h"

extern	CNExecute::CCompletionPort*		g_pExecutorCompletionPort;
extern	CNAPI::CIOCompletionPort*		g_pIOCP;

CNNetIO::Socket::NBase::~NBase()
{
	//-----------------------------------------------------------------
	// 1. Socket을 일단 닫는다.
	//-----------------------------------------------------------------
	// 1) Socket을 닫는다.
	ProcessCloseSocket();
}


//*****************************************************************************
// 
// 
// Operation함수 처리
// 
// 
//*****************************************************************************
void CNNetIO::Socket::NBase::ProcessPrepareSocket()
{
	//-----------------------------------------------------------------
	// 1) Socket Option설정하기.
	//-----------------------------------------------------------------
	//1. Socket을 새로 만든다.
	if( GetSocketHandle() == INVALID_SOCKET )
	{
		// - Socket을 새로 만든다.
		CreateNewSocketHandle();

		// - Setting한다.
		m_pConnectable	 = dynamic_cast<CNNetIO::IConnectable*>(this);
		m_pReceivable	 = dynamic_cast<CNNetIO::IReceivable*>(this);
	}

	//2. Socket을 State 를 Closed로 한다.
	SetSocketState( SOCKET_STATE_CLOSED );

	//-----------------------------------------------------------------
	// 2) Interface들 Reset
	//-----------------------------------------------------------------
	//1. Connectable을 Reset한다.
	if(m_pConnectable != NULL)
	{
		m_pConnectable->ResetConnectable();
	}

	//2. Receivable을 Reset한다.
	if(m_pReceivable != NULL)
	{
		m_pReceivable->ResetReceivable();
	}

	//-----------------------------------------------------------------
	// 3. Reset을 수행한다.
	//-----------------------------------------------------------------
	//1. Reset Socket을 수행한다.
	OnPrepareSocket();
}


bool CNNetIO::Socket::NBase::ProcessCloseSocket()
{
	//-----------------------------------------------------------------
	// 1. Socket을 얻는다.
	//    - 이 때 Interlocked함수를 사용하여 Socket을 변경하고 이전
	//      Socket Handle을 얻어낸다. 이 이전 Socket Handle이 
	//      INVALID_SOCKET이 아니면 Socketdmf 닫는다.
	//-----------------------------------------------------------------
	SOCKET	hSocket	 = SetSocketHandle( INVALID_SOCKET );

	// Check) p_pSocket이 NULL이어서는 안된다.
	RETURN_IF( hSocket == INVALID_SOCKET, FALSE );

	//-----------------------------------------------------------------
	// 2. SocketHandle을 INVALID_SOCKET으로 바꾼다.
	//-----------------------------------------------------------------
	// 1) Abortive disconnect로 옵션을 설정한다.
	CNNetAPI::Socket::GetInstance().SetLingerOption( hSocket, TRUE );

	// 2) Socket을 닫는다.
	CNNetAPI::Socket::GetInstance().CloseSocket( hSocket );

	//-----------------------------------------------------------------
	// 3. OnClose을 호출한다.
	//-----------------------------------------------------------------
	// 1) OnCloseSocket함수를 호출한다.
	OnCloseSocket();

	//-----------------------------------------------------------------
	// 4. IO가 완료될 때까지 기다린다.
	//-----------------------------------------------------------------
	// 1) Executor를 돌려 IOCP의 I/O를 처리한다.
	//while(Statistics_GetNowOverlappedIO()!=0)
	//{
	//	RunExecutor();
	//}

	// Return) 성공~
	return	TRUE;
}


void CNNetIO::Socket::NBase::CreateNewSocketHandle()
{
	//-----------------------------------------------------------------
	// 1. 새 Socket 생성하기.
	//-----------------------------------------------------------------
	// Declare) 
	SOCKET		hNewSocket;
	BOOL		bResult;

	// 1) Socket 생성
	hNewSocket	 = CNNetAPI::Socket::GetInstance().CreateSocket( m_iSocketType, m_iProtocolInfo );

	// Check) Socket Option의 수행에 실패했는가?
	CNNET_THROW_IF( hNewSocket == INVALID_SOCKET, CNException::NetIO::FailToCreateSocket(), ERROR_TRACE( "[Error : Socket의 생성에 실패했습니다. File:%s Line:%d]\n", __FILE__, __LINE__ ) );

	// 2) Send Buffer설정.
	bResult	 = CNNetAPI::Socket::GetInstance().SetSendBufferSize( hNewSocket, SIZE_OF_SOCKET_SEND_BUFFER );

	// Check) Socket Option의 수행에 실패했는가?
	CNNET_THROW_IF( bResult == FALSE, CNException::NetIO::FailToSetSocketOption(), ERROR_TRACE( "[Error: %dByte의 Socket Send Bufffer 설정 실패했습니다. File:%s Line:%d]\n", SIZE_OF_SOCKET_SEND_BUFFER, __FILE__, __LINE__ ) );

	// 3) Receive Buffuer을 설정한다.
	bResult	 = CNNetAPI::Socket::GetInstance().SetReceiveBufferSize( hNewSocket, SIZE_OF_SOCKET_RECEIVE_BUFFER );

	// Check) Socket Option의 수행에 실패했는가?
	CNNET_THROW_IF( bResult == FALSE, CNException::NetIO::FailToSetSocketOption(), ERROR_TRACE( "[Error : %dByte의 Socket Receive Bufffer 설정 실패했습니다. File:%s Line:%d]\n", SIZE_OF_SOCKET_RECEIVE_BUFFER, __FILE__, __LINE__ ) );

	//-----------------------------------------------------------------
	// 2. 새 Socket 설정한다.
	//-----------------------------------------------------------------
	// 1) 기존에 Socket이 있다면 먼저 Close한다.
	if( GetSocketHandle() != INVALID_SOCKET )
	{
		// - 기존 Socket을 닫는다.
		CNNetAPI::Socket::GetInstance().CloseSocket( GetSocketHandle() );
	}

	// 2) 설정한다.
	SetSocketHandle( hNewSocket );
}

