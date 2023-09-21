#ifndef __CD_THREAD_CREATE_H__
#define __CD_THREAD_CREATE_H__

#include "CDThreadDefinitions.h"


#ifdef  __cplusplus
extern "C"
{
#endif

typedef struct _stCDThreadCreate
{
    void        *vpArgs;

    CD_THREAD_FUCTION_START         OnThreadStart;
    CD_THREAD_FUCTION_RUN           OnThreadRun;
    CD_THREAD_FUCTION_TERMINATE     OnThreadTerminate;

} stCDThreadCreate;


int CDThreadCreate( stCDThreadCreate* _stpThreadInfo );

static void* CDThreadRun( void* vpArgs );


#ifdef  __cplusplus
}
#endif

#endif

