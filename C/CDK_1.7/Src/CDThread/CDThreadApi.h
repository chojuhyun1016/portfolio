#ifndef __CD_THREAD_API_H__
#define __CD_THREAD_API_H__

#include "CDThreadDefinitions.h"

#ifdef _SOLARIS_
    #include <pthread.h>
#elif _CENT_OS_
    #include <pthread.h>
#else
    #include <pthread.h>
#endif


#ifdef  __cplusplus
extern "C"
{
#endif

typedef void* (CD_THREAD_WORKER)(void*);


int CDThreadAttrInit( pthread_attr_t* _stpAttr );
int CDThreadAttrDestroy( pthread_attr_t* _stpAttr );

int CDThreadGetScope( pthread_attr_t* _stpAttr );
int CDThreadSetScope( pthread_attr_t* _stpAttr, int _iScope );

int CDThreadGetDetach( pthread_attr_t* _stpAttr );
int CDThreadSetDetach( pthread_attr_t* _stpAttr, int _iDetach );

int CDThreadBegin( pthread_t* _pThreadID, CD_THREAD_WORKER _pStartAddress, void* _vpParameter );
int CDThreadTerminate( pthread_t _iThreadID );


#ifdef  __cplusplus
}
#endif

#endif
