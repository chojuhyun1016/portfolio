#ifndef __CD_BASE_DEBUG_H__
#define __CD_BASE_DEBUG_H__


#ifdef __cplusplus
extern "C" {
#endif

#ifndef CD_BASE_PRINT_HEX_BUFFER_SIZE
#define CD_BASE_PRINT_HEX_BUFFER_SIZE   8192
#endif


char*   CDBaseHexConvert( char* _cpBuffer, const char* _cpPoint, int _iSize, char* _cpTitle );
void    CDBaseHexPrint( const char* _cpPoint, int _iSize, char* _cpTitle );


#ifdef __cplusplus
}
#endif

#endif

