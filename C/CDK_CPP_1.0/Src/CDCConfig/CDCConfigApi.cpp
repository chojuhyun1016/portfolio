#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "CDConfigDefinitions.h"
#include "CDConfigApi.h"


bool CDConfigOpen( const char* _cpFile, int _iMode )
{
	int	iResult;

	while( ( iResult = ::open( _cpFile, _iMode ) ) == -1 && errno == EINTR );

	return	iResult != -1;
}


bool CDConfigClose( int _iFd )
{
	int	iResult;

	while( ( iResult = ::close( _iFd ) ) == -1 && errno == EINTR );

	return	iResult != -1;
}

