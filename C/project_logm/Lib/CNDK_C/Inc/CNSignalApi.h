#ifndef __CN_SIGNAL_API_H__
#define __CN_SIGNAL_API_H__

#include <signal.h>
#include <pthread.h>

#include "CNSignalDefinitions.h"


#ifdef  __cplusplus
extern "C"
{
#endif

int CNSigAction( int _iSig, CN_SIG_HANDLER _fnHandler, struct sigaction* _stpBeforeAction );
int CNRtSigAction( int _iSig, CN_RT_SIG_HANDLER _fnHandler, struct sigaction* _stpBeforeAction );

int CNSigIgnore( int _iSig, struct sigaction* _stpBeforeAction );

int CNRtsWait( sigset_t* _stpSigSet, siginfo_t* _stpSigInfo );
int CNRtsTimeWait( sigset_t* _stpSigSet, siginfo_t* _stpSigInfo, struct timespec* _stpTsTime );

int	CNSigWaitInfo( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo );
int	CNSigTimedWait( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo, const struct timespec* _stpTsTime );

#ifdef  __cplusplus
}
#endif

#endif
