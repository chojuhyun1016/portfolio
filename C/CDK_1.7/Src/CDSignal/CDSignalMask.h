#ifndef __CD_SIGNAL_MASK_H__
#define __CD_SIGNAL_MASK_H__

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

int CDSignalMask( int _iHow, sigset_t* _stpNewSig, sigset_t* _stpOldSig );

int CDSignalBlock( int _iSig );
int CDSignalUnBlock( int _iSig );

int CDSignalAllBlock( sigset_t* _stpSigTemp );
int CDSignalAllUnBlock();


#ifdef  __cplusplus
}
#endif

#endif

