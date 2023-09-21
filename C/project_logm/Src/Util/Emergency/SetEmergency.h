#ifndef __SET_EMERGENCY_H__
#define __SET_EMERGENCY_H__

#ifdef __cplusplus
extern "C" {
#endif

#define SET_EMERGENCY_PROCESS_NAME	"SET_EMERGENCY"
#define SET_EMERGENCY_SHARED_KEY	0x12001

int main( int argc, char **argv );
int	ProcessCheck( char* _cpProcessName );

#ifdef __cplusplus
}
#endif

#endif


