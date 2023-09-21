#ifndef __CD_SIGNAL_TUTORIAL_H__
#define __CD_SIGNAL_TUTORIAL_H__

#include <time.h>

#include "CDSignal.h"

#ifdef  __cplusplus
extern "C"
{
#endif

#define PROCESS_NAME		"CD_SIGNAL_TUTORIAL"


int main( int argc, char **argv );

void SignalHandler1( int _iSignal );
void SignalHandler2( int _iSignal );
void RtSignalHandler1( int _iSignal, siginfo_t* _stpSigInfo, void* _iSigVal );
void RtSignalHandler2( int _iSignal, siginfo_t* _stpSigInfo, void* _iSigVal );
void RtsPrint( siginfo_t* _stpSigInfo );

#ifdef  __cplusplus
}
#endif

#endif

