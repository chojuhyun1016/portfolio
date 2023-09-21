#ifndef __CD_LOG_API_H__
#define __CD_LOG_API_H__


#ifdef  __cplusplus
extern "C"
{
#endif

int CDLogFileOpen( const char* _cpFile, int _iMode );
int CDLogFileClose( int _iFd );


#ifdef  __cplusplus
}
#endif

#endif

