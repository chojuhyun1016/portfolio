#include "CNAPIThread.h"

#include <stdio.h>
#include <strings.h>

CNAPI::CThread::CThread()
{
	SET_NULL( m_szName[0] );
	
	SET_NULL( m_hThread );
}


CNAPI::CThread::~CThread()
{
	// 1) Terminate한다.
	Terminate();

	// 2) Attr 구조체를 Dertroy 한다.
	AttrDestroy();
}


BOOL CNAPI::CThread::Begin( CNTHREAD_START_ROUTINE _pStartAddress, void* _vpParameter, char* _cpName )
{
	// 1) p_pStartAddress이 NULL이어서는 안된다.
	CNASSERT( _pStartAddress != NULL, FALSE );

	// 2) 이미 만들어져 있으면 일단 기존 Thread를 Terminate시킨다.
	if( m_hThread != NULL )
	{
		Terminate();
	}

	// 3) Attr 구조체 초기화
	RETURN_IF( AttrInit() == FALSE, FALSE );

	// 4) 쓰레드를 커널 모드로 셋팅
	RETURN_IF( SetScope() == FALSE, FALSE );

	// 5) 쓰레드를 비결합 속성으로 셋팅
	RETURN_IF( SetDetach() == FALSE, FALSE );

	// 6) Thread를 실행 시키고 실패일 경우 Return
	RETURN_IF( pthread_create( &m_hThread, &m_hAttr, _pStartAddress, _vpParameter ) != 0, FALSE );

	// 7) Thread이름을 정한다.
	SetThreadName( _cpName );

	return	TRUE;
}


void CNAPI::CThread::Terminate()
{
	// Check) Thread Handle이 없다면 그냥 끝낸다.
	RETURN_IF( m_hThread == NULL, );

	// 1) Terminate한다.
	pthread_cancel( m_hThread );

	// 2) Handle 초기화.
	SET_NULL( m_szName[0] );
	SET_ZERO( m_hThread );
}


void CNAPI::CThread::SetThreadName( char* _cpName )
{
	RETURN_IF( _cpName == NULL, );

	// 1) Thread Name을 설정한다.
	strlcpy( m_szName, _cpName, sizeof( m_szName ) );
}


BOOL CNAPI::CThread::AttrInit()
{
	if( pthread_attr_init( &m_hAttr ) != PTHREAD_SUCCESS )
	{
		fprintf( stderr, "pthread_attr_init() Error");

		return	FALSE;
	}
	
	return TRUE;
}


void CNAPI::CThread::AttrDestroy()
{
	if( pthread_attr_destroy( &m_hAttr ) != PTHREAD_SUCCESS )
		fprintf( stderr, "[pthread_attr_destroy() Error]" );
}


int CNAPI::CThread::GetScope()
{
	int iScope;

	RETURN_IF( pthread_attr_getscope( &m_hAttr, &iScope ) != PTHREAD_SUCCESS , PTHREAD_ERROR );	

	if( pthread_attr_getscope( &m_hAttr, &iScope ) != PTHREAD_SUCCESS )
	{
		fprintf( stderr, "[pthread_attr_getscope() Error]" );

		return PTHREAD_ERROR;
	}

	return	iScope;
}


BOOL CNAPI::CThread::SetScope()
{
	if( pthread_attr_setscope( &m_hAttr, PTHREAD_SCOPE_SYSTEM ) != PTHREAD_SUCCESS )
	{
		fprintf( stderr, "[pthread_attr_setscope() Error]" );

		return FALSE;
	}

	return	TRUE;
}


int CNAPI::CThread::GetDetach()
{
	int iDetach;

	if( pthread_attr_getdetachstate( &m_hAttr, &iDetach ) != PTHREAD_SUCCESS )
	{
		fprintf( stderr, "[pthread_attr_getdetachstate Error]" );

		return	PTHREAD_ERROR;
	}

	return	iDetach;
}


BOOL CNAPI::CThread::SetDetach()
{
	if( pthread_attr_setdetachstate( &m_hAttr, PTHREAD_CREATE_DETACHED ) != PTHREAD_SUCCESS )
	{
		fprintf( stderr, "[pthread_attr_setdetachstate Error]" );

		return	FALSE;
	}	

	return	TRUE;
}



