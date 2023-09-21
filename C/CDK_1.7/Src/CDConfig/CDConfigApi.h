#ifndef __CD_CONFIG_API_H__
#define __CD_CONFIG_API_H__


#ifdef  __cplusplus
extern "C"
{
#endif

int CDConfigOpen( const char* _cpFile, int _iMode );
int CDConfigClose( int _iFd );


#ifdef  __cplusplus
}
#endif

#endif
