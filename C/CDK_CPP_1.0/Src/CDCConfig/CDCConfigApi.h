#ifndef __CDC_CONFIG_API_H__
#define __CDC_CONFIG_API_H__

namespace CDCConfig
{
namespace API
{

bool CDConfigOpen( const char* _cpFile, int _iMode );
bool CDConfigClose( int _iFd );

}

}

#endif
