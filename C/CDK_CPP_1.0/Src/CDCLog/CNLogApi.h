#ifndef __CN_LOG_API_H__
#define __CN_LOG_API_H__

#ifdef  __cplusplus
extern "C"
{
#endif

int CNLogFileOpen( const char* _cpFile, int _iMode );
int CNLogFileClose( int _iFd );

#ifdef  __cplusplus
}
#endif

#endif

