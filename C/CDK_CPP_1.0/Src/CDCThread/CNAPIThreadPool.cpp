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
	// 1) p_iThreadCount이 0이어서는 안된다.
	CNASSERT( _iThreadCount != 0, FALSE );

	// 2) Thread관련 설정
	m_Thread		 = new CNAPI::CThread[_iThreadCount];

	// 3) Thread를 숫자만큼 생성한다.
	for( int i=0; i < _iThreadCount; ++i )
	{
		char	tempString[256];

		// 이름을 작명
		snprintf( tempString, 255, "%s %d", ( _cpName != NULL ) ? _cpName : "UnNamed CNThread", i );
		// Thread를 만들고 시작함.
		m_Thread[i].Begin( fnThread, (void*)this, tempString );
	}

	return TRUE;
}


void CNAPI::CThreadPool::Terminate()
{
	// 1) Thread를 모두 Terminate시킨다.
	for( int i = 0; i < m_nThread; ++i )
	{
		// - Terminate시킨다.
		m_Thread[i].Terminate();
	}

	// 2) Thread정보를 모두 지운다.
	SAFE_DELETE_ARRAY( m_Thread );
}


void CNAPI::CThreadPool::Suspend()
{
	
}


void* CNAPI::fnThread( void* _vpParam )
{
	// 1) p_pParam이 NULL이어서는 안된다.
	CNASSERT( _vpParam != NULL, FALSE );

	// 2) Thread Pool을 얻는다.
	CNAPI::CThreadPool*	pThreadPool		 = (CNAPI::CThreadPool*)_vpParam;

	// 3) Thread수를 증가시킨다.
	CNInterlockedIncrement( &pThreadPool->m_nThread );

	// 4) Thread의 시작전 OnThreadStart()를 호출한다.
	pThreadPool->OnThreadStart();

	// 5) Thread를 수행한다.
	pThreadPool->ThreadRun();

	// 6) Thread를 닫기전 OnThreadTerminate()를 수행한다.
	pThreadPool->OnThreadTerminate();

	// 7) Thead 수를 줄인다.
	CNInterlockedDecrement( &pThreadPool->m_nThread );

	// 8) 값을 되돌린다.
	return	NULL;
}



