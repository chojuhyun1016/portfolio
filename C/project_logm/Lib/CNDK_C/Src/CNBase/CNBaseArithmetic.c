#include "CNBaseArithmetic.h"


/*******************************************************************************
* Update    : 2011/05/02                                                       *
* Argument  : char, char                                                       *
*             _cLeft    : ������ ������ ����(ū��) ����                        *
*             _cRight   : ������ ������ ������(������) ����                    *
*                                                                              *
* Return    : char, �� ���ڰ� ������ ���� ��                                   *
* Stability : Async-Signal-Safe                                                *
* Explain   : �ΰ��� ����(_cLeft, _cRight)�� �޾Ƽ� ���� ����(_cLeft)�� ����   *
*             4��Ʈ�� ������ ����(_cRight)�� ������ 4��Ʈ�� ���ļ� ��ģ ����   *
*             ��ȯ�Ѵ�.                                                        *
*******************************************************************************/
char AddTwoByte( char _cLeft, char _cRight )
{
	unsigned char	cLeft;
	unsigned char	cRight;

	cLeft = ( _cLeft & 0x0F ) << 4;
	cRight = ( _cRight & 0x0F );

	return	( cLeft | cRight );
}
