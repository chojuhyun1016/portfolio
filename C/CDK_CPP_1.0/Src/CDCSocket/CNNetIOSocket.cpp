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
	// 1. ProcessCloseSocket�� ȣ���Ѵ�.
	//-----------------------------------------------------------------
	// Trace) 
	TRACE("[Info : %d Socket�� ���� �����߽��ϴ�. File:%s Line:%d]\n", GetSocketHandle(), __FILE__, __LINE__ );

	// 1) ���� ���Ḧ �����Ѵ�.
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
	// 1) �̹� ������� ������ �׳� �ǵ�����.
	RETURN_IF( g_pIOCP != NULL, FALSE );

	//-----------------------------------------------------------------
	// 1. Executor�� �����Ѵ�.
	//-----------------------------------------------------------------
	// Declare)
	CNExecute::CCompletionPort*	pExecutor;

	// 1) new�Ѵ�.
	if( _iThread >= 0 )
	{
		CNExecute::CCompletionPortThread*	pExeucutorThread;

		// - Exeuctor�� �����Ѵ�.
		pExeucutorThread = new CNExecute::CCompletionPortThread( TRUE, _iThread );

		// - Exeuctor�� �����Ѵ�.
		pExecutor		 = pExeucutorThread;

		// - Thread�� ������ �����Ѵ�.
		g_iThreadCount	 = pExeucutorThread->GetThreadCount();
	}
	else
	{
		pExecutor		 = new CNExecute::CCompletionPort();

		// - Thread�� ������ �����Ѵ�.
		g_iThreadCount	 = 0;
	}

	// Check) 
	CNASSERT_ERROR( pExecutor != NULL );

	// 2) Executor�� �����Ѵ�.
	g_pExecutor		 = pExecutor;
	g_pIOCP			 = pExecutor;
	//g_autoIOCP.set_reference(pExecutor);

	TRACE("[Info : Excutor ���� ����. ThreadCount:%d File:%s Line:%d]\n", g_iThreadCount, __FILE__, __LINE__ );

	// Return) ����!!!
	return	TRUE;
}


BOOL CNNetIO::Socket::InitSocketExecutor( CNAPI::CIOCompletionPort* _pIOCP )
{
	//-----------------------------------------------------------------
	// Check) 
	//-----------------------------------------------------------------
	// 1) Executor�� NULL�� �ƴѰ�
	CNASSERT( _pIOCP!=NULL, FALSE );

	// 1) �̹� ������� �ִ°�?
	CNASSERT( g_pIOCP == NULL, FALSE );

	//-----------------------------------------------------------------
	// 1. Executor�� �����Ѵ�.
	//-----------------------------------------------------------------
	// 1) new�Ѵ�.
	g_pIOCP	 = _pIOCP;
	//g_autoIOCP.set_reference( g_pIOCP );


	//-----------------------------------------------------------------
	// 2. Thread���� �����Ѵ�.
	//-----------------------------------------------------------------
	// 1) Thread Pool�� ��´�.
	CNAPI::CThreadPool*	pThreadPool	 = dynamic_cast<CNAPI::CThreadPool*>(_pIOCP);

	// 2) Thread Pool�� NULL�̸� Thread������ 0���� �Ѵ�.
	g_iThreadCount	 = (pThreadPool != NULL) ? pThreadPool->GetThreadCount() : 0;

	// Return) ����!!!
	return	TRUE;
}


void CNNetIO::Socket::CloseSocketExecutor()
{
	//-----------------------------------------------------------------
	// Check) 
	//-----------------------------------------------------------------
	// 1) ���� Executor�� ������ �׳� ������ �ȴ�.
	RETURN_IF( g_pIOCP == NULL, );

	//-----------------------------------------------------------------
	// 1. Executor�� �ݴ´�.
	//-----------------------------------------------------------------
	// 1) Executor�� �����
	SAFE_DELETE( g_pIOCP );

	// 2) Executor�� Reset�Ѵ�.
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
	// 1. Executor�� ������ ó���Ѵ�.
	//-----------------------------------------------------------------
	// 1) Executor�� ���� IOCP�� I/O�� ó���Ѵ�.
	while( g_pIOCP->ExecuteCompletionPort() )
	{
	}
}


CNExecute::IBase* CNNetIO::Socket::GetDefaultExecutor()
{ 
	return g_pExecutor;
}
