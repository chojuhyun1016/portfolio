#ifndef __GET_EMERGENCY_H__
#define __GET_EMERGENCY_H__

#ifdef __cplusplus
extern "C" {
#endif

#define GET_EMERGENCY_PROCESS_NAME	"GET_EMERGENCY"
#define GET_EMERGENCY_SHARED_KEY	0x12001

int main( int argc, char **argv );
int	ProcessCheck( char* _cpProcessName );

#ifdef __cplusplus
}
#endif

#endif


