#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/atomic.h>

#include "Send.h"


int main( int argc, char **argv )
{
	int	iPid;
	int	iSig;
	int	iResult;

	sigval	stSigValue;

	if( argc != 3 )
	{
		::fprintf( stderr, "[Argument Num Error][Num:%d][L:%d]\n", 
			argc, 
			__LINE__ );

		::fprintf( stderr, "[Ex: CD_SIGNAL_SEND 1000(Pid) 15(Sig)]\n" );

		::exit( -1 );
	}

	// 첫번째 인자는 프로세스 ID
	iPid	= ::atoi( argv[1] );

	// 두번째 인자는 전송 할 시그널 번호
	iSig	= ::atoi( argv[2] );

	::memset( &stSigValue, 0x00, sizeof( stSigValue ) );

	// 특정 프로세스에 특정 시그널 전송
	// iPid : 프로세스 ID
	// iSig : 전송 할 시그널 번호
	if( ( iResult = sigqueue( iPid, iSig, stSigValue ) ) < 0 )
	{
		::fprintf( stderr, "[sigqueue( %d %d %d ) Error][E:%d][L:%d]\n", 
			iPid, 
			iSig, 
			stSigValue, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	return	0;
}

