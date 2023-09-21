//*****************************************************************************
//*                                                                           *
//*                      Cho JuHyun's Development Kit                         *
//*                        Ver 1.5 / Bulid 2010.10.10                         *
//*                                                                           *
//*                              Thread Fuction                               *
//*                                                                           *
//*                                                                           *
//*                                                                           *
//*                                                                           *
//*  This Program is programmed by Cho JuHyun. mungmung80@gmail.com           *
//*  Best for Network Developement and Optimized for Network Developement.    *
//*                                                                           *
//*                                                                           *
//*****************************************************************************

#ifndef __CD_THREAD_DEFINITIONS_H__
#define __CD_THREAD_DEFINITIONS_H__


#ifdef  __cplusplus
extern "C"
{
#endif

#ifndef CD_THREAD_SUCCESS
#define CD_THREAD_SUCCESS       0
#endif

#ifndef CD_THREAD_ERROR
#define CD_THREAD_ERROR         -1
#endif


typedef int (*CD_THREAD_FUCTION_START)(void*);
typedef int (*CD_THREAD_FUCTION_RUN)(void*);
typedef int (*CD_THREAD_FUCTION_TERMINATE)(void*);


#ifdef  __cplusplus
}
#endif

#endif

