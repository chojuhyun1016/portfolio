//*****************************************************************************
//*                                                                           *
//*                      Cho JuHyun's Network Classes                         *
//*                        Ver 1.0 / Bulid 2010.10.01                         *
//*                                                                           *
//*                              Thread Classes                               *
//*                                                                           *
//*                                                                           *
//*                                                                           *
//*                                                                           *
//*  This Program is programmed by Cho JuHyun. mungmung80@gmail.com           *
//*  Best for Network Developement and Optimized for Network Developement.    *
//*                                                                           *
//*                                                                           *
//*****************************************************************************

#ifndef __CN_THREAD_DEFINITIONS__
#define __CN_THREAD_DEFINITIONS__


#ifdef  __cplusplus
extern "C"
{
#endif

#ifndef CN_THREAD_SUCCESS
#define CN_THREAD_SUCCESS			0
#endif

#ifndef CN_THREAD_ERROR
#define CN_THREAD_ERROR				-1
#endif

typedef int (*CN_THREAD_FUCTION_START)(void*);
typedef int (*CN_THREAD_FUCTION_RUN)(void*);
typedef int (*CN_THREAD_FUCTION_TERMINATE)(void*);

#ifdef  __cplusplus
}
#endif

#endif

