#ifndef	__CNAPI_THREAD_POOL__
#define	__CNAPI_THREAD_POOL__

#include "CNAPIThread.h"
//-----------------------------------------------------------------------------
//
// CNAPI::CIOCompletionPort
//
// 1. CNAPI::CThreadPool�� Ư¡!
//    1) ���� �ϳ��� �Լ��� ���� Thread�� �����ϵ��� �ϴ� class�̴�.
//    2) Thread���� �����ؾ� �Ǵ� ������ ThreadRun()�Լ��� �����������ν� ����
//       �Ѵ�.
//    3) CreateThread�� ���� ������ Thread�� ThreadRon()�Լ��� ȣ���Ѵ�.
//    4) CreateThread�Լ��� ȣ��Ǹ� OnThreadStart()�Լ��� ȣ��ǰ� ����ɶ�
//       OnThreadTerminate()�Լ��� ȣ��ȴ�.
//    5) IOCP�� ����� Network I/O�� �̰����� ���۵Ǿ���.
//
// 2. CNNetSocket�� �ٽ�! Function�� ��� ����.
//    1) ThreadRun()		 �� �Լ��� �ٽ��Ҽ��� Thread���� ����Ǵ� �ٽ� �Լ��̴�.
//	  2) OnThreadStart()	 Tread�� �����ɶ� ThreadRun�� ����Ǳ� ���� ȣ��Ǵ� �Լ��̴�.
//	  3) OnThreadTerminate() Thread�� ����Ǳ� ���� ȣ��Ǵ� �Լ��̴�.
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
		// 1) Threa�� ����/��������
		BOOL				CreateThread( int _iThreadCount = 1, char* _cpName = NULL );

		void				Terminate();
		void				Suspend();

		// 2) Thread ����
		int					GetThreadCount() const						{ return m_nThread; }
		CThread&			Thread( int _iThread )						{ return m_Thread[_iThread]; }
		CThread*			GetThread( int _iThread )					{ return &m_Thread[_iThread]; }


	// ****************************************************************************
	// Framework)
	// ----------------------------------------------------------------------------
	private:
		// 1) Thread �����Լ�.
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

