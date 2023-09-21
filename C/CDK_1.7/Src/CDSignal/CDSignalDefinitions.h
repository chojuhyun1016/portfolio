//*****************************************************************************
//*                                                                           *
//*                      Cho JuHyun's Development Kit                         *
//*                        Ver 1.5 / Bulid 2010.10.10                         *
//*                                                                           *
//*                              Signal Fuction                               *
//*                                                                           *
//*                                                                           *
//*                                                                           *
//*                                                                           *
//*  This Program is programmed by Cho JuHyun. mungmung80@gmail.com           *
//*  Best for Network Developement and Optimized for Network Developement.    *
//*                                                                           *
//*                                                                           *
//*****************************************************************************

#ifndef __CD_SIGNAL_DEFINITIONS_H__
#define __CD_SIGNAL_DEFINITIONS_H__

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

#ifndef CD_SIGNAL_SUCCESS
#define CD_SIGNAL_SUCCESS   0
#endif

#ifndef CD_SIGNAL_ERROR
#define CD_SIGNAL_ERROR     -1
#endif

#ifndef CD_SIGNAL_TIMEOUT
#define CD_SIGNAL_TIMEOUT   0
#endif


typedef void (*CD_SIG_HANDLER)(int);
typedef void (*CD_RT_SIG_HANDLER)(int, siginfo_t*, void*);


#ifdef  __cplusplus
}
#endif

#endif
