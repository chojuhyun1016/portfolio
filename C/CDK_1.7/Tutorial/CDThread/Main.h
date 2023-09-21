#ifndef __CD_THREAD_TUTORIAL_H__
#define __CD_THREAD_TUTORIAL_H__

#include <time.h>


#ifdef  __cplusplus
extern "C"
{
#endif

#define PROCESS_NAME		"CD_THREAD_TUTORIAL"


int main( int argc, char **argv );

void* ThreadBegin( void* _vpArg );

int ThreadStart( void* _vpArg );
int ThreadRun( void* _vpArg );
int ThreadTerminate( void* _vpArg );

#ifdef  __cplusplus
}
#endif

#endif

