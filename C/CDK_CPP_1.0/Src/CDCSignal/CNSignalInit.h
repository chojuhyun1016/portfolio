#ifndef __CN_SIGNAL_INIT_H__
#define __CN_SIGNAL_INIT_H__

#include "CNSignalApi.h"
#include "CNSignalMask.h"
#include "CNSignalDefinitions.h"


#ifdef  __cplusplus
extern "C"
{
#endif

int	CNInitSignal( CN_SIG_HANDLER _fnHandler );
int	CNInitRtSignal( CN_RT_SIG_HANDLER _fnHandler );

#ifdef  __cplusplus
}
#endif

#endif

