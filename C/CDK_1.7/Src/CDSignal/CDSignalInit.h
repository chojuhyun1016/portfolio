#ifndef __CD_SIGNAL_INIT_H__
#define __CD_SIGNAL_INIT_H__

#include "CDSignalApi.h"
#include "CDSignalMask.h"
#include "CDSignalDefinitions.h"


#ifdef  __cplusplus
extern "C"
{
#endif

int CDSignalInit( CD_SIG_HANDLER _fnHandler );
int CDSignalInitRts( CD_RT_SIG_HANDLER _fnHandler );


#ifdef  __cplusplus
}
#endif

#endif

