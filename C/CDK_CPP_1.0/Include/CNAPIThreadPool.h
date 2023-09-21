#ifndef	__CNAPI_THREAD_POOL__
#define	__CNAPI_THREAD_POOL__

#include "CNAPIThread.h"
//-----------------------------------------------------------------------------
//
// CNAPI::CIOCompletionPort
//
// 1. CNAPI::CThreadPool의 특징!
//    1) 단지 하나의 함수를 여러 Thread로 수행하도록 하는 class이다.
//    2) Thread에서 실행해야 되는 내용은 ThreadRun()함수를 재정의함으로써 설정
//       한다.
//    3) CreateThread를 통해 생성된 Thread는 ThreadRon()함수를 호출한다.
//    4) CreateThread함수가 호출되면 OnThreadStart()함수가 호출되고 종료될때
//       OnThreadTerminate()함수가 호출된다.
//    5) IOCP를 사용한 Network I/O가 이것으로 제작되었다.
//
// 2. CNNetSocket의 핵심! Function의 기능 설명.
//    1) ThreadRun()		 이 함수가 핵심할수로 Thread에서 실행되는 핵심 함수이다.
//	  2) OnThreadStart()	 Tread가 생성될때 ThreadRun이 수행되기 전에 호출되는 함수이다.
//	  3) OnThreadTerminate() Thread가 종료되기 전에 호출되는 함수이다.
//
//
//-----------------------------------------------------------------------------

namespace CNAPI
{

extern "C" static void* fnThread( void* _vpParam );

class CThreadPool
{
	// ****************************************************************************
	// Constructor/Destructor)
	// ----------------------------------------------------------------------------
	public:
		CThreadPool();
		virtual	~CThreadPool();


	// ****************************************************************************
	// Publics)
	// ----------------------------------------------------------------------------
	public:
		// 1) Threa의 시작/중지관련
		BOOL				CreateThread( int _iThreadCount = 1, char* _cpName = NULL );

		void				Terminate();
		void				Suspend();

		// 2) Thread 상태
		int					GetThreadCount() const						{ return m_nThread; }
		CThread&			Thread( int _iThread )						{ return m_Thread[_iThread]; }
		CThread*			GetThread( int _iThread )					{ return &m_Thread[_iThread]; }


	// ****************************************************************************
	// Framework)
	// ----------------------------------------------------------------------------
	private:
		// 1) Thread 수행함수.
		virtual	void					ThreadRun() PURE;
	
		// 2) On Functions
		virtual	void					OnThreadStart() EMPTY
		virtual	void					OnThreadTerminate() EMPTY


	// ****************************************************************************
	// Implementation)
	// ----------------------------------------------------------------------------
	private:
		CThread*						m_Thread;

		volatile unsigned long			m_nThread;

		friend	void*					CNAPI::fnThread( void* _vpParam );
};

}

#endif


