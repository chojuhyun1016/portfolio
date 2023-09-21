//*****************************************************************************
//*                                                                           *
//*                      Cho JuHyun's Network Classes                         *
//*                        Ver 1.0 / Bulid 2010.10.01                         *
//*                                                                           *
//*                              Signal Classes                               *
//*                                                                           *
//*                                                                           *
//*                                                                           *
//*                                                                           *
//*  This Program is programmed by Cho JuHyun. mungmung80@gmail.com           *
//*  Best for Network Developement and Optimized for Network Developement.    *
//*                                                                           *
//*                                                                           *
//*****************************************************************************

#ifndef __CN_SIGNAL_DEFINITIONS__
#define __CN_SIGNAL_DEFINITIONS__


#ifdef  __cplusplus
extern "C"
{
#endif

#ifndef CN_SIGNAL_SUCCESS
#define	CN_SIGNAL_SUCCESS			0
#endif

#ifndef CN_SIGNAL_ERROR
#define	CN_SIGNAL_ERROR				-1
#endif

#ifndef CN_SIGNAL_TIMEOUT
#define	CN_SIGNAL_TIMEOUT			0
#endif

typedef void (CN_SIG_HANDLER)(int);
typedef void (CN_RT_SIG_HANDLER)(int, siginfo_t *, void *);

#ifdef  __cplusplus
}
#endif

#endif
