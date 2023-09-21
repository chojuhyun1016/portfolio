#include "CDBaseArithmetic.h"


/*******************************************************************************
* Update    : 2011/05/02                                                       *
* Argument  : char, char                                                       *
*             _cLeft    : 합쳐질 문자중 왼쪽(큰수) 문자                        *
*             _cRight   : 합쳐질 문자중 오른쪽(작은수) 문자                    *
*                                                                              *
* Return    : char, 두 문자가 더해진 문자 값                                   *
* Stability : Async-Signal-Safe                                                *
* Explain   : 두개의 문자(_cLeft, _cRight)를 받아서 왼쪽 문자(_cLeft)의 왼쪽   *
*             4비트와 오른쪽 문자(_cRight)의 오른쪽 4비트를 합쳐서 합친 값을   *
*             반환한다.                                                        *
*******************************************************************************/
char CDBaseAddTwoByte( char _cLeft, char _cRight )
{
    unsigned char   cLeft;
    unsigned char   cRight;

    cLeft = ( _cLeft & 0x0F ) << 4;
    cRight = ( _cRight & 0x0F );

    return  ( cLeft | cRight );
}

