#ifndef	__CNAPI_THREAD__
#define	__CNAPI_THREAD__

#include "CNDefinitions.h"

#include <pthread.h>
//-----------------------------------------------------------------------------
//
// CNAPI::CThread
//
// 1. CNAPI::CThread의 특징!
//    1) Thread를 Wrapping한 Class이다.
//
// 2. CNAPI::CThread의 핵심! Function의 기능 설명.
//    1) Begin()			Thread를 시작하는 함수이다.
//    2) Terminate()		Thread를 강제로 종료시키는 함수이다.(왠만하면 쓰지마라..)
//	  3) Suspend()/Resume()	함수를 잠시 세우고 다시 돌리는 함수이다.
//
// 3. 주의사항
//    * 특별한 사항은 없음.
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
		// 1) Threa의 시작/중지관련
		BOOL				Begin( CNTHREAD_START_ROUTINE _pStartAddress, void* _vpParameter = NULL, char* _cpName = NULL );
		void				Terminate();

		// 2) Thread이름
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
	// Implementation) 선언부
	// ----------------------------------------------------------------------------
	private:
		pthread_t			m_hThread;

		pthread_attr_t		m_hAttr;

		char				m_szName[256];
};

}

#endif

