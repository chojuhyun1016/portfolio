#include "CNAPIThreadPool.h"

#include <stdio.h>

#include "CND.h"

CNAPI::CThreadPool::CThreadPool()
{
	SET_NULL( m_nThread );
	SET_NULL( m_Thread );
}


CNAPI::CThreadPool::~CThreadPool()
{
	SAFE_DELETE_ARRAY( m_Thread );
}


BOOL CNAPI::CThreadPool::CreateThread( int _iThreadCount, char* _cpName )
{
	// 1) p_iThreadCount�� 0�̾�� �ȵȴ�.
	CNASSERT( _iThreadCount != 0, FALSE );

	// 2) Thread���� ����
	m_Thread		 = new CNAPI::CThread[_iThreadCount];

	// 3) Thread�� ���ڸ�ŭ �����Ѵ�.
	for( int i=0; i < _iThreadCount; ++i )
	{
		char	tempString[256];

		// �̸��� �۸�
		snprintf( tempString, 255, "%s %d", ( _cpName != NULL ) ? _cpName : "UnNamed CNThread", i );
		// Thread�� ����� ������.
		m_Thread[i].Begin( fnThread, (void*)this, tempString );
	}

	return TRUE;
}


void CNAPI::CThreadPool::Terminate()
{
	// 1) Thread�� ��� Terminate��Ų��.
	for( int i = 0; i < m_nThread; ++i )
	{
		// - Terminate��Ų��.
		m_Thread[i].Terminate();
	}

	// 2) Thread������ ��� �����.
	SAFE_DELETE_ARRAY( m_Thread );
}


void CNAPI::CThreadPool::Suspend()
{
	
}


void* CNAPI::fnThread( void* _vpParam )
{
	// 1) p_pParam�� NULL�̾�� �ȵȴ�.
	CNASSERT( _vpParam != NULL, FALSE );

	// 2) Thread Pool�� ��´�.
	CNAPI::CThreadPool*	pThreadPool		 = (CNAPI::CThreadPool*)_vpParam;

	// 3) Thread���� ������Ų��.
	CNInterlockedIncrement( &pThreadPool->m_nThread );

	// 4) Thread�� ������ OnThreadStart()�� ȣ���Ѵ�.
	pThreadPool->OnThreadStart();

	// 5) Thread�� �����Ѵ�.
	pThreadPool->ThreadRun();

	// 6) Thread�� �ݱ��� OnThreadTerminate()�� �����Ѵ�.
	pThreadPool->OnThreadTerminate();

	// 7) Thead ���� ���δ�.
	CNInterlockedDecrement( &pThreadPool->m_nThread );

	// 8) ���� �ǵ�����.
	return	NULL;
}


