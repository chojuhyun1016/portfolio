#ifndef	__CNAPI_THREAD__
#define	__CNAPI_THREAD__

#include "CNDefinitions.h"

#include <pthread.h>
//-----------------------------------------------------------------------------
//
// CNAPI::CThread
//
// 1. CNAPI::CThread�� Ư¡!
//    1) Thread�� Wrapping�� Class�̴�.
//
// 2. CNAPI::CThread�� �ٽ�! Function�� ��� ����.
//    1) Begin()			Thread�� �����ϴ� �Լ��̴�.
//    2) Terminate()		Thread�� ������ �����Ű�� �Լ��̴�.(�ظ��ϸ� ��������..)
//	  3) Suspend()/Resume()	�Լ��� ��� ����� �ٽ� ������ �Լ��̴�.
//
// 3. ���ǻ���
//    * Ư���� ������ ����.
//
//
//-----------------------------------------------------------------------------

extern "C" typedef void* ( CNTHREAD_START_ROUTINE )( void * );

namespace CNAPI
{

class CThread
{
	// ****************************************************************************
	// Constructor/Destructor)
	// ----------------------------------------------------------------------------
	public:
		CThread();
		~CThread();


	// ****************************************************************************
	// Publics)
	// ----------------------------------------------------------------------------
	public:
		// 1) Threa�� ����/��������
		BOOL				Begin( CNTHREAD_START_ROUTINE _pStartAddress, void* _vpParameter = NULL, char* _cpName = NULL );
		void				Terminate();

		// 2) Thread�̸�
		void				SetThreadName( char* _cpName );
		char*				GetThreadName() const						{ return (char*)m_szName; } 

		// 3) Thread Handle
		pthread_t			GetThreadHandle() const						{ return m_hThread; }

		// 4) Thread Setting 
		BOOL				AttrInit();
		void				AttrDestroy();

		int					GetScope();
		BOOL				SetScope();
		int					GetDetach();
		BOOL				SetDetach();

	// ****************************************************************************
	// Implementation) �����
	// ----------------------------------------------------------------------------
	private:
		pthread_t			m_hThread;

		pthread_attr_t		m_hAttr;

		char				m_szName[256];
};

}

#endif
