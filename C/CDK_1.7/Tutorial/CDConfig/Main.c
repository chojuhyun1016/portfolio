#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/atomic.h>

#include "Main.h"
#include "CDConfig.h"

int main( int argc, char **argv )
{
	int		iResult;
	int		iIntBuffer;

	char	caStrBuffer[512];

	iIntBuffer	= 0;
	::memset( caStrBuffer, 0x00, sizeof( caStrBuffer ) );

	// ������ ������ ȯ�� ���Ͽ��� Ư�� �з�([CONFIG1])�� Ư�� �׸�(CONFIG_INT)�� ������ �����͸� ȹ��
	if( ( iResult = CDConfigGetInt( (char*)CONFIG_FILE, (char*)"CONFIG1", (char*)"CONFIG_INT", &iIntBuffer ) ) != CD_CONFIG_SUCCESS )
	{
		::fprintf( stderr, "[CDConfigGetInt( %s CONFIG1 CONFIG_INT %d ) Error][L:%d]\n", 
			(char*)CONFIG_FILE, 
			&iIntBuffer, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[CONFIG1][CONFIG_INT][%d]\n", iIntBuffer );

	iIntBuffer	= 0;
	::memset( caStrBuffer, 0x00, sizeof( caStrBuffer ) );

	if( ( iResult = CDConfigGetStr( (char*)CONFIG_FILE, (char*)"CONFIG1", (char*)"CONFIG_STR", caStrBuffer, sizeof( caStrBuffer ) ) ) != CD_CONFIG_SUCCESS )
	{
		::fprintf( stderr, "[CDConfigGetInt( %s CONFIG1 CONFIG_STR %d %d ) Error][L:%d]\n", 
			(char*)CONFIG_FILE, 
			caStrBuffer, 
			sizeof( caStrBuffer ), 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[CONFIG1][CONFIG_STR][%s]\n", caStrBuffer );

	iIntBuffer	= 0;
	::memset( caStrBuffer, 0x00, sizeof( caStrBuffer ) );

	// ������ ������ ȯ�� ���Ͽ��� Ư�� �з�([CONFIG2])�� Ư�� �׸�(CONFIG_INT)�� ������ �����͸� ȹ��
	if( ( iResult = CDConfigGetInt( (char*)CONFIG_FILE, (char*)"CONFIG2", (char*)"CONFIG_INT", &iIntBuffer ) ) != CD_CONFIG_SUCCESS )
	{
		::fprintf( stderr, "[CDConfigGetInt( %s CONFIG2 CONFIG_INT %d ) Error][L:%d]\n", 
			(char*)CONFIG_FILE, 
			&iIntBuffer, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[CONFIG2][CONFIG_INT][%d]\n", iIntBuffer );

	iIntBuffer	= 0;
	::memset( caStrBuffer, 0x00, sizeof( caStrBuffer ) );

	// ������ ������ ȯ�� ���Ͽ��� Ư�� �з�([CONFIG2])�� Ư�� �׸�(CONFIG_STR)�� ���ڿ� �����͸� ȹ��
	if( ( iResult = CDConfigGetStr( (char*)CONFIG_FILE, (char*)"CONFIG2", (char*)"CONFIG_STR", caStrBuffer, sizeof( caStrBuffer ) ) ) != CD_CONFIG_SUCCESS )
	{
		::fprintf( stderr, "[CDConfigGetInt( %s CONFIG2 CONFIG_STR %d %d ) Error][L:%d]\n", 
			(char*)CONFIG_FILE, 
			caStrBuffer, 
			sizeof( caStrBuffer ), 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[CONFIG1][CONFIG_STR][%s]\n\n", caStrBuffer );

	::exit( 0 );
}
