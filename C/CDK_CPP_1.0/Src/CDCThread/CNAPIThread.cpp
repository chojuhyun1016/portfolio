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
	// 1) Terminate�Ѵ�.
	Terminate();

	// 2) Attr ����ü�� Dertroy �Ѵ�.
	AttrDestroy();
}


BOOL CNAPI::CThread::Begin( CNTHREAD_START_ROUTINE _pStartAddress, void* _vpParameter, char* _cpName )
{
	// 1) p_pStartAddress�� NULL�̾�� �ȵȴ�.
	CNASSERT( _pStartAddress != NULL, FALSE );

	// 2) �̹� ������� ������ �ϴ� ���� Thread�� Terminate��Ų��.
	if( m_hThread != NULL )
	{
		Terminate();
	}

	// 3) Attr ����ü �ʱ�ȭ
	RETURN_IF( AttrInit() == FALSE, FALSE );

	// 4) �����带 Ŀ�� ���� ����
	RETURN_IF( SetScope() == FALSE, FALSE );

	// 5) �����带 ����� �Ӽ����� ����
	RETURN_IF( SetDetach() == FALSE, FALSE );

	// 6) Thread�� ���� ��Ű�� ������ ��� Return
	RETURN_IF( pthread_create( &m_hThread, &m_hAttr, _pStartAddress, _vpParameter ) != 0, FALSE );

	// 7) Thread�̸��� ���Ѵ�.
	SetThreadName( _cpName );

	return	TRUE;
}


void CNAPI::CThread::Terminate()
{
	// Check) Thread Handle�� ���ٸ� �׳� ������.
	RETURN_IF( m_hThread == NULL, );

	// 1) Terminate�Ѵ�.
	pthread_cancel( m_hThread );

	// 2) Handle �ʱ�ȭ.
	SET_NULL( m_szName[0] );
	SET_ZERO( m_hThread );
}


void CNAPI::CThread::SetThreadName( char* _cpName )
{
	RETURN_IF( _cpName == NULL, );

	// 1) Thread Name�� �����Ѵ�.
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


