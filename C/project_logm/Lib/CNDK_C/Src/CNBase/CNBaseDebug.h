#ifndef __CN_BASE_DEBUG_H__
#define __CN_BASE_DEBUG_H__


#ifdef __cplusplus
extern "C" {
#endif

#ifndef CN_BASE_PRINT_HEX_BUFFER_SIZE
#define CN_BASE_PRINT_HEX_BUFFER_SIZE	8192
#endif

char*	HexConvert( char* _cpBuffer, const char* _cpPoint, int _iSize, char* _cpTitle );
void	PrintHex( const char* _cpPoint, int _iSize, char* _cpTitle );

#ifdef __cplusplus
}
#endif

#endif

