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
*             stThread* : Thread�� ������ �����ϰ� �ִ� ����ü�� ������        *
*                                                                              *
* Return    : int, ����(���� �� Thread�� ThreadID), ����(-1)                   *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Thread�� ���� ������ ������ ����ü�� ������(_stpThreadInfo)��    *
*             Argument�� ���޹޾� Thread�� �����ϸ鼭 ����ü�� �����͸�        *
*             Argrument�� �����Ѵ�.                                            *
*******************************************************************************/
int CreateThread( stThread* _stpThreadInfo )
{
	pthread_t	iThreadID;

	// 1. Thread ����!!
	if( CNThreadBegin( &iThreadID, RunThread, _stpThreadInfo ) == CN_THREAD_ERROR )
	{
		// 1.1 Error ����!!
		return	CN_THREAD_ERROR;
	}

	// 2. �ű� Thread ���� �Ϸ�!!
	return	iThreadID;
}


/*******************************************************************************
* Update    : 2011/08/01                                                       *
* Argument  : void*                                                            *
*             vpArgs : Thread���� �� ������ ó���Լ�(����,ó��,����)���� ����  *
*                      �� Ư�� �������� ������(void*)                          *
*                                                                              *
* Return    : void*, (�׻� NULL�� ��ȯ)                                        *
* Stability : MT-Safe                                                          *
* Explain   : Argument�� �Ѱܹ��� �����ͷ� ThreadInfo ����ü�� �����Ͽ� �ش�   *
*             ����ü�� ��� �� ����, ó��, ���� �Լ��� ���ʴ�� �����Ѵ�.      *
*             Thread Info ����ü(stThreadInfo*)�� �����ʹ� void*(vpArgs)���·� *
*             �Ѱܹ޴´�.(Thread �Լ��� �������̽��� void* �̹Ƿ�)             *
*                                                                              *
*             �����Լ�(OnThreadStart)�� Thread ����� �켱������ ó�� �Ǿ��   *
*             �� �۾�(������ �ʱ�ȭ, ���� ��)�� ��� �� �Լ��̴�.              *
*             �����Լ��� �ѹ��� ����Ǹ� ���н� ó�� �Լ��� �ǳʶٰ�           *
*             ���� �Լ��� ȣ���ϰ� �ȴ�.                                       *
*                                                                              *
*             ó���Լ�(OnThreadRun)�� ���� Thread�� ó��(����)�� ��� ��       *
*             �Լ��̴�.                                                        *
*             ó�� �Լ��� Thread ���� ���� ���޽ñ��� �ݺ��ؼ� ����ȴ�.       *
*             Thread ���� ������ Thread Info ����ü�� iState�� ���°�          *
*             CN_THREAD_NOT_RUN ���·� ������ �Ǵ°��� �ǹ��Ѵ�.               *
*                                                                              *
*             �����Լ�(OnThreadTerminate)�� Thread ���� �� ó���Ǿ�� ��       *
*             �۾�(�޸� ����, ������ �ʱ�ȭ ��)�� ��� �� �Լ��̴�.          *
*             �����Լ��� �ѹ��� ����ȴ�.                                      *
*******************************************************************************/
static void* RunThread( void* vpArgs )
{
	int				iResult	= 0;

	stThread*		stpThreadInfo;

	// 1. ���� �˻�
	if( vpArgs == NULL )
		return	NULL;

	// 2. void�� �����ʹ� ������ ������ �Ҽ������Ƿ� ĳ������ ����
	stpThreadInfo	= (stThread*)vpArgs;

	// 3. Thread Start(���۽� ����Ǵ� �Լ�) ����
	if( stpThreadInfo->OnThreadStart )
		iResult = stpThreadInfo->OnThreadStart( stpThreadInfo );

	// 4. Thread Run(Main Routine �Լ�)
	//    �����Լ�(OnThreadStart)�� ���� �� ��� �����Լ�(OnThreadRun)
	//    �� ������� �ʴ´�.
	if( stpThreadInfo->OnThreadRun && iResult >= 0 )
		iResult = stpThreadInfo->OnThreadRun( stpThreadInfo );

	// 5. Thread End(����� ����Ǵ� �Լ�)
	if( stpThreadInfo->OnThreadTerminate )
		iResult= stpThreadInfo->OnThreadTerminate( stpThreadInfo );

	// 6. Thread ����
	return	NULL;
}
