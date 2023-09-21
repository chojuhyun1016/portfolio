#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <strings.h>
#include <sys/atomic.h>

#include "CNThread.h"
#include "CNThreadApi.h"
#include "CNThreadDefinitions.h"


/*******************************************************************************
* Update    : 2011/08/01                                                       *
* Argument  : stThread*                                                        *
*             stThread* : Thread의 정보를 저장하고 있는 구조체의 포인터        *
*                                                                              *
* Return    : int, 성공(생성 된 Thread의 ThreadID), 실패(-1)                   *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Thread에 대한 정보를 저장한 구조체의 포인터(_stpThreadInfo)를    *
*             Argument로 전달받아 Thread를 생성하면서 구조체의 포인터를        *
*             Argrument로 전달한다.                                            *
*******************************************************************************/
int CreateThread( stThread* _stpThreadInfo )
{
	pthread_t	iThreadID;

	// 1. Thread 실행!!
	if( CNThreadBegin( &iThreadID, RunThread, _stpThreadInfo ) == CN_THREAD_ERROR )
	{
		// 1.1 Error 리턴!!
		return	CN_THREAD_ERROR;
	}

	// 2. 신규 Thread 실행 완료!!
	return	iThreadID;
}


/*******************************************************************************
* Update    : 2011/08/01                                                       *
* Argument  : void*                                                            *
*             vpArgs : Thread에서 각 구간별 처리함수(시작,처리,종료)에게 전달  *
*                      할 특정 데이터의 포인터(void*)                          *
*                                                                              *
* Return    : void*, (항상 NULL을 반환)                                        *
* Stability : MT-Safe                                                          *
* Explain   : Argument로 넘겨받은 포인터로 ThreadInfo 구조체에 접근하여 해당   *
*             구조체에 등록 된 시작, 처리, 종료 함수를 차례대로 실행한다.      *
*             Thread Info 구조체(stThreadInfo*)의 포인터는 void*(vpArgs)형태로 *
*             넘겨받는다.(Thread 함수의 인터페이스가 void* 이므로)             *
*                                                                              *
*             시작함수(OnThreadStart)는 Thread 실행시 우선적으로 처리 되어야   *
*             할 작업(데이터 초기화, 인증 등)이 등록 된 함수이다.              *
*             시작함수는 한번만 실행되며 실패시 처리 함수를 건너뛰고           *
*             종료 함수를 호출하게 된다.                                       *
*                                                                              *
*             처리함수(OnThreadRun)는 실제 Thread의 처리(동작)이 등록 된       *
*             함수이다.                                                        *
*             처리 함수는 Thread 종료 명령 전달시까지 반복해서 실행된다.       *
*             Thread 종료 명령은 Thread Info 구조체의 iState의 상태가          *
*             CN_THREAD_NOT_RUN 상태로 셋팅이 되는것을 의미한다.               *
*                                                                              *
*             종료함수(OnThreadTerminate)는 Thread 종료 전 처리되어야 할       *
*             작업(메모리 해제, 데이터 초기화 등)이 등록 된 함수이다.          *
*             종료함수는 한번만 실행된다.                                      *
*******************************************************************************/
static void* RunThread( void* vpArgs )
{
	int				iResult	= 0;

	stThread*		stpThreadInfo;

	// 1. 예외 검사
	if( vpArgs == NULL )
		return	NULL;

	// 2. void형 포인터는 직간접 접근을 할수없으므로 캐스팅후 복사
	stpThreadInfo	= (stThread*)vpArgs;

	// 3. Thread Start(시작시 실행되는 함수) 실행
	if( stpThreadInfo->OnThreadStart )
		iResult = stpThreadInfo->OnThreadStart( stpThreadInfo );

	// 4. Thread Run(Main Routine 함수)
	//    시작함수(OnThreadStart)가 실패 할 경우 실행함수(OnThreadRun)
	//    는 실행되지 않는다.
	if( stpThreadInfo->OnThreadRun && iResult >= 0 )
		iResult = stpThreadInfo->OnThreadRun( stpThreadInfo );

	// 5. Thread End(종료시 실행되는 함수)
	if( stpThreadInfo->OnThreadTerminate )
		iResult= stpThreadInfo->OnThreadTerminate( stpThreadInfo );

	// 6. Thread 종료
	return	NULL;
}

