#ifndef __CN_THREAD_H__
#define __CN_THREAD_H__

#include "CNThreadDefinitions.h"


#ifdef  __cplusplus
extern "C"
{
#endif

typedef struct _stThread
{
	void		*vpArgs;

	CN_THREAD_FUCTION_START			OnThreadStart;
	CN_THREAD_FUCTION_RUN			OnThreadRun;
	CN_THREAD_FUCTION_TERMINATE		OnThreadTerminate;

} stThread;

int CreateThread( stThread* _stpThreadInfo );

static void* RunThread( void* vpArgs );


#ifdef  __cplusplus
}
#endif

#endif

