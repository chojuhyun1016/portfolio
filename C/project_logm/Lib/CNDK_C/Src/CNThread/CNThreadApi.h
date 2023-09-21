#ifndef __CN_THREAD_API_H__
#define __CN_THREAD_API_H__

#include <pthread.h>

#include "CNThreadDefinitions.h"


#ifdef  __cplusplus
extern "C"
{
#endif

typedef void* (CN_THREAD_WORKER)(void*);


int CNThreadAttrInit( pthread_attr_t* _stpAttr );
int CNThreadAttrDestroy( pthread_attr_t* _stpAttr );
int CNThreadGetScope( pthread_attr_t* _stpAttr );
int CNThreadSetScope( pthread_attr_t* _stpAttr );
int CNThreadGetDetach( pthread_attr_t* _stpAttr );
int CNThreadSetDetach( pthread_attr_t* _stpAttr );

int CNThreadBegin( pthread_t* _pThreadID, CN_THREAD_WORKER _pStartAddress, void* _vpParameter );
int CNThreadTerminate( pthread_t _iThreadID );

#ifdef  __cplusplus
}
#endif

#endif
