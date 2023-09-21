#ifndef __CN_GET_CONFIG_H__
#define __CN_GET_CONFIG_H__


#ifdef  __cplusplus
extern "C"
{
#endif

#ifndef CN_CONFIG_BUF_SIZE
#define CN_CONFIG_BUF_SIZE			1024
#endif

#ifndef CN_CONFIG_TOKEN_SIZE
#define CN_CONFIG_TOKEN_SIZE		1024
#endif

#ifndef CN_CONFIG_NO_FORMAT
#define	CN_CONFIG_NO_FORMAT			-1
#endif

#ifndef CN_CONFIG_NO_FILE
#define CN_CONFIG_NO_FILE			-2
#endif

#ifndef CN_CONFIG_NO_PART
#define CN_CONFIG_NO_PART			-3
#endif

#ifndef CN_CONFIG_NO_SPART
#define CN_CONFIG_NO_SPART			-4
#endif

#ifndef CN_CONFIG_NO_DECIMAL
#define CN_CONFIG_NO_DECIMAL		-5
#endif


int CNGetConfigInt( char* _cpFile, char* _cpPart, char* _cpSPart, int* _ipBuf );
int CNGetConfigStr( char* _cpFile, char* _cpPart, char* _cpSPart, char* _cpBuf, int _iSize );

static int	CNReadConfig( char* _cpFile, char* _cpPart, char* _cpSPart, char* _cpBuf, int _iSize );
static char* CNConfigMakeToken( char* _cpOffset, char* _cpBuf, int _iSize );

static int CNConfigStrToDec( char* _cpStr );
static int CNConfigHexConverter( char* _cpStr );
static int CNConfigDecConverter( char* _cpStr );

static int CNConfigReadLine( int _iFd, char* _cpBuf, int _iSize );

#ifdef  __cplusplus
}
#endif

#endif

