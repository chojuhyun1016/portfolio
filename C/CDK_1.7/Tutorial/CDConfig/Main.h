#ifndef __CD_CONFIG_TUTORIAL_H__
#define __CD_CONFIG_TUTORIAL_H__

#include <time.h>


#ifdef  __cplusplus
extern "C"
{
#endif

#define PROCESS_NAME	"CD_CONFIG_TUTORIAL"

#define CONFIG_FILE		"./Config.cfg"

int main( int argc, char **argv );
void SignalHandler( int _iSignal );
int	ForkProcess( char* _cpProcessName );

#ifdef  __cplusplus
}
#endif

#endif

