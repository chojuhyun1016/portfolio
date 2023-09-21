#include "CNNetIOSocket.h"

#include "CNCommonUtil.h"
#include "CNAPIThreadPool.h"
#include "CNExecuteCompletionPortThread.h"

CNAPI::CIOCompletionPort*				g_pIOCP			 = NULL;
CNExecute::IBase*						g_pExecutor		 = NULL;
int										g_iThreadCount;

bool CNNetIO::ISocket::CloseSocket()
{
	//-----------------------------------------------------------------
	// 1. ProcessCloseSocket을 호출한다.
	//-----------------------------------------------------------------
	// Trace) 
	TRACE("[Info : %d Socket를 강제 종료했습니다. File:%s Line:%d]\n", GetSocketHandle(), __FILE__, __LINE__ );

	// 1) 강제 종료를 진행한다.
	return ProcessCloseSocket();
}


int	CNNetIO::Socket::GetExecutorThreadCount()
{
	return	g_iThreadCount;
}


BOOL CNNetIO::Socket::InitSocketExecutor( int _iThread )
{
	//-----------------------------------------------------------------
	// Check) 
	//-----------------------------------------------------------------
	// 1) 이미 만들어져 있으면 그냥 되돌린다.
	RETURN_IF( g_pIOCP != NULL, FALSE );

	//-----------------------------------------------------------------
	// 1. Executor를 설정한다.
	//-----------------------------------------------------------------
	// Declare)
	CNExecute::CCompletionPort*	pExecutor;

	// 1) new한다.
	if( _iThread >= 0 )
	{
		CNExecute::CCompletionPortThread*	pExeucutorThread;

		// - Exeuctor를 생성한다.
		pExeucutorThread = new CNExecute::CCompletionPortThread( TRUE, _iThread );

		// - Exeuctor를 설정한다.
		pExecutor		 = pExeucutorThread;

		// - Thread의 갯수를 설정한다.
		g_iThreadCount	 = pExeucutorThread->GetThreadCount();
	}
	else
	{
		pExecutor		 = new CNExecute::CCompletionPort();

		// - Thread의 갯수를 설정한다.
		g_iThreadCount	 = 0;
	}

	// Check) 
	CNASSERT_ERROR( pExecutor != NULL );

	// 2) Executor를 설정한다.
	g_pExecutor		 = pExecutor;
	g_pIOCP			 = pExecutor;
	//g_autoIOCP.set_reference(pExecutor);

	TRACE("[Info : Excutor 생성 성공. ThreadCount:%d File:%s Line:%d]\n", g_iThreadCount, __FILE__, __LINE__ );

	// Return) 성공!!!
	return	TRUE;
}


BOOL CNNetIO::Socket::InitSocketExecutor( CNAPI::CIOCompletionPort* _pIOCP )
{
	//-----------------------------------------------------------------
	// Check) 
	//-----------------------------------------------------------------
	// 1) Executor가 NULL이 아닌가
	CNASSERT( _pIOCP!=NULL, FALSE );

	// 1) 이미 만들어져 있는가?
	CNASSERT( g_pIOCP == NULL, FALSE );

	//-----------------------------------------------------------------
	// 1. Executor를 설정한다.
	//-----------------------------------------------------------------
	// 1) new한다.
	g_pIOCP	 = _pIOCP;
	//g_autoIOCP.set_reference( g_pIOCP );


	//-----------------------------------------------------------------
	// 2. Thread수를 설정한다.
	//-----------------------------------------------------------------
	// 1) Thread Pool을 얻는다.
	CNAPI::CThreadPool*	pThreadPool	 = dynamic_cast<CNAPI::CThreadPool*>(_pIOCP);

	// 2) Thread Pool이 NULL이면 Thread갯수를 0으로 한다.
	g_iThreadCount	 = (pThreadPool != NULL) ? pThreadPool->GetThreadCount() : 0;

	// Return) 성공!!!
	return	TRUE;
}


void CNNetIO::Socket::CloseSocketExecutor()
{
	//-----------------------------------------------------------------
	// Check) 
	//-----------------------------------------------------------------
	// 1) 만약 Executor가 없으면 그냥 끝내면 된다.
	RETURN_IF( g_pIOCP == NULL, );

	//-----------------------------------------------------------------
	// 1. Executor를 닫는다.
	//-----------------------------------------------------------------
	// 1) Executor를 지운다
	SAFE_DELETE( g_pIOCP );

	// 2) Executor를 Reset한다.
	//g_autoIOCP.reset();
}


void CNNetIO::Socket::RunExecutor()
{
	//-----------------------------------------------------------------
	// Check) 
	//-----------------------------------------------------------------
	// 1) 
	RETURN_IF( g_pIOCP == NULL, );

	//-----------------------------------------------------------------
	// 1. Executor를 돌리며 처리한다.
	//-----------------------------------------------------------------
	// 1) Executor를 돌려 IOCP의 I/O를 처리한다.
	while( g_pIOCP->ExecuteCompletionPort() )
	{
	}
}


CNExecute::IBase* CNNetIO::Socket::GetDefaultExecutor()
{ 
	return g_pExecutor;
}

