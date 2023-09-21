#ifndef __CD_SIGNAL_API_H__
#define __CD_SIGNAL_API_H__

#include "CDSignalDefinitions.h"

#ifdef  _SOLARIS_
    #include <signal.h>
#elif _CENT_OS_
    #include <signal.h>
#else
    #include <signal.h>
#endif


#ifdef  __cplusplus
extern "C"
{
#endif

int CDSignalAction( int _iSig, CD_SIG_HANDLER _fnHandler, struct sigaction* _stpBeforeAction );
int CDSignalRtsAction( int _iSig, CD_RT_SIG_HANDLER _fnHandler, struct sigaction* _stpBeforeAction );

int CDSignalIgnore( int _iSig, struct sigaction* _stpBeforeAction );

int CDSignalWait( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo );
int CDSignalTimedWait( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo, const struct timespec* _stpTsTime );

int CDSignalRtsWait( sigset_t* _stpSigSet, siginfo_t* _stpSigInfo );
int CDSignalRtsTimedWait( sigset_t* _stpSigSet, siginfo_t* _stpSigInfo, struct timespec* _stpTsTime );


#ifdef  __cplusplus
}
#endif

#endif
