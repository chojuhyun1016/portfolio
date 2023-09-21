#ifndef __CD_CONFIG_READ_H__
#define __CD_CONFIG_READ_H__

#include "CDConfigDefinitions.h"


#ifdef  __cplusplus
extern "C"
{
#endif

#ifndef CD_CONFIG_BUF_SIZE
#define CD_CONFIG_BUF_SIZE          1024
#endif

#ifndef CD_CONFIG_TOKEN_SIZE
#define CD_CONFIG_TOKEN_SIZE        1024
#endif

#ifndef CD_CONFIG_NO_FORMAT
#define CD_CONFIG_NO_FORMAT         -1
#endif

#ifndef CD_CONFIG_NO_FILE
#define CD_CONFIG_NO_FILE           -2
#endif

#ifndef CD_CONFIG_NO_PART
#define CD_CONFIG_NO_PART           -3
#endif

#ifndef CD_CONFIG_NO_SPART
#define CD_CONFIG_NO_SPART          -4
#endif

#ifndef CD_CONFIG_NO_DECIMAL
#define CD_CONFIG_NO_DECIMAL        -5
#endif


int CDConfigGetInt( char* _cpFile, char* _cpPart, char* _cpSPart, int* _ipBuf );
int CDConfigGetStr( char* _cpFile, char* _cpPart, char* _cpSPart, char* _cpBuf, int _iSize );

static int  CDConfigRead( char* _cpFile, char* _cpPart, char* _cpSPart, char* _cpBuf, int _iSize );
static char* CDConfigMakeToken( char* _cpOffset, char* _cpBuf, int _iSize );

static int CDConfigStrToDec( char* _cpStr );
static int CDConfigHexConverter( char* _cpStr );
static int CDConfigDecConverter( char* _cpStr );

static int CDConfigReadLine( int _iFd, char* _cpBuf, int _iSize );


#ifdef  __cplusplus
}
#endif

#endif

