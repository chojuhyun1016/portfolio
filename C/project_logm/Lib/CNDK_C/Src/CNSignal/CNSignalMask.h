#ifndef __CN_SIGNAL_MASK_H__
#define __CN_SIGNAL_MASK_H__

#include <signal.h>
#include <pthread.h>


#ifdef  __cplusplus
extern "C"
{
#endif

int CNSignalMask( int _iHow, sigset_t* _stpNewSig, sigset_t* _stpOldSig );
int CNAllSignalBlock( sigset_t* _stpSigTemp );
int CNAllSignalUnBlock();
int CNSignalBlock( int _iSig );
int CNSignalUnBlock( int _iSig );

#ifdef  __cplusplus
}
#endif

#endif

