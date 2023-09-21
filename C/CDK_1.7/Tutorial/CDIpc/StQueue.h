#ifndef __CD_IPC_ST_QUEUE_TUTORIAL_H__
#define __CD_IPC_ST_QUEUE_TUTORIAL_H__

#include <time.h>


#ifdef  __cplusplus
extern "C"
{
#endif

#define PROCESS_NAME					"CD_IPC_ST_QUEUE_TUTORIAL"
#define CD_IPC_ST_QUEUE_KEY				0x9002
#define CD_IPC_ST_QUEUE_INSERT_NUM		5
#define CD_IPC_ST_QUEUE_MSG_TYPE		1

typedef struct _stPacket
{
	long	iType;

	int		iInt;
	char	caStr[16];

} stStPacket;

int main( int argc, char **argv );


#ifdef  __cplusplus
}
#endif

#endif

