#ifndef __CN_CONFIG_API_H__
#define __CN_CONFIG_API_H__

#ifdef  __cplusplus
extern "C"
{
#endif

int CNConfigOpen( const char* _cpFile, int _iMode );
int CNConfigClose( int _iFd );

#ifdef  __cplusplus
}
#endif

#endif
