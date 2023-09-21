#include "CDBaseStrings.h"
#include "CDBaseDefinitions.h"

#ifdef  _SOLARIS_
    #include <stdlib.h>
    #include <strings.h>
#elif _CENT_OS_
    #include <stdlib.h>
    #include <string.h>
#else
    #include <stdlib.h>
    #include <strings.h>
#endif


/*******************************************************************************
* Update    : 2012/10/22                                                       *
* Argument  : char*                                                            *
*             _cpString  : 원본 문자열의 주소                                  *
*                                                                              *
* Return    : void                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : 인자 값으로 문자열(_cpString)의 주소값을 받아서 문자열 왼쪽      *
*             에 위치한 ' ', '\1', '\n' 문자를 떼어내고 떼어낸 문자열을 다시   *
*             주소값으로 받은 문자열(_cpString)에 저장한다.                    *
*******************************************************************************/
void CDBaseStrLeftTrim( char* _cpString )
{
    int iLength;
    int iStart;
    int iCurrent = 0;

    iLength = strlen( _cpString );

    for( iStart = 0; iStart < iLength; iStart++ )
    {
        if( ( _cpString[iStart] != ' ' ) && ( _cpString[iStart] != '\t' ) && ( _cpString[iStart] != '\n' ) )
        {
            iCurrent = iStart;

            break;
        }
    }

    if( iCurrent > 0 )
    {
        for( iStart = 0; iCurrent < iLength; iStart++, iCurrent++ )
        {
            _cpString[iStart] = _cpString[iCurrent];
        }

        _cpString[iStart] = NULL;
    }
}


/*******************************************************************************
* Update    : 2012/10/22                                                       *
* Argument  : char*, char*                                                     *
*             _cpBuffer  : 변환 된 문자열을 저장 할 버퍼의 주소                *
*             _cpString  : 원본 문자열의 주소                                  *
*                                                                              *
* Return    : void                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : 인자 값으로 받은 문자열(_cpString)의 왼쪽에 ' ', '\1', '\n'      *
*             문자를 떼어내고 떼어낸 문자열을 버퍼(_cpBuffer)에 저장한다.      *
*******************************************************************************/
void CDBaseStrLeftTrimToBuffer( char* _cpBuffer, char* _cpString )
{
    int iLength;
    int iStart;
    int iCurrent = 0;

    iLength = strlen( _cpString );

    for( iStart = 0; iStart < iLength; iStart++ )
    {
        if( ( _cpString[iStart] != ' ' ) && ( _cpString[iStart] != '\t' ) && ( _cpString[iStart] != '\n' ) )
        {
            iCurrent = iStart;

            break;
        }
    }

    if( iCurrent > 0 )
    {
        for( iStart = 0; iCurrent < iLength; iStart++, iCurrent++ )
        {
            _cpBuffer[iStart] = _cpString[iCurrent];
        }

        _cpBuffer[iStart] = NULL;
    }
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*                                                            *
*             _cpString  : 원본 문자열의 주소                                  *
*                                                                              *
* Return    : void                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : 인자 값으로 문자열(_cpString)의 주소값을 받아서 문자열 오른쪽    *
*             에 위치한 ' ', '\1', '\n' 문자를 떼어내고 떼어낸 문자열을 다시   *
*             주소값으로 받은 문자열(_cpString)에 저장한다.                    *
*******************************************************************************/
void CDBaseStrRightTrim( char* _cpString )
{
    int iStart;
    int iLength;

    iLength = strlen( _cpString );

    for( iStart = iLength - 1; iStart >= 0; iStart-- )
    {
        if( ( _cpString[iStart] == ' ' ) || ( _cpString[iStart] == '\t' ) || ( _cpString[iStart] == '\n' ) )
            _cpString[iStart] = NULL;
        else
            break;
    }
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*, char*                                                     *
*             _cpBuffer  : 변환 된 문자열을 저장 할 버퍼의 주소                *
*             _cpString  : 원본 문자열의 주소                                  *
*                                                                              *
* Return    : void                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : 인자 값으로 받은 문자열(_cpString)의 오른쪽에 ' ', '\1', '\n'    *
*             문자를 떼어내고 떼어낸 문자열을 버퍼(_cpBuffer)에 저장한다.      *
*******************************************************************************/
void CDBaseStrRightTrimToBuffer( char* _cpBuffer, char* _cpString )
{
    int iStart;
    int iLength;

    strcpy( _cpBuffer, _cpString );

    iLength = strlen( _cpBuffer );

    for( iStart = iLength - 1; iStart >= 0; iStart-- )
    {
        if( ( _cpBuffer[iStart] == ' ' ) || ( _cpBuffer[iStart] == '\t' ) || ( _cpBuffer[iStart] == '\n' ) )
            _cpBuffer[iStart] = NULL;
        else
            break;
    }
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*,                                                           *
*             _cpString  : 원본 문자열의 주소                                  *
*                                                                              *
* Return    : void                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : 인자 값으로 받은 문자열(_cpString)의 양쪽에 ' ', '\1', '\n'      *
*             문자를 떼어내고 떼어낸 문자열을 버퍼(_cpBuffer)에 저장한다.      *
*******************************************************************************/
void CDBaseStrTrim( char* _cpString )
{
    CDBaseStrLeftTrim( _cpString );
    CDBaseStrRightTrim( _cpString );
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*, char*                                                     *
*             _cpBuffer  : 변환 된 문자열을 저장 할 버퍼의 주소                *
*             _cpString  : 원본 문자열의 주소                                  *
*                                                                              *
* Return    : void                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : 인자 값으로 받은 문자열(_cpString)의 양쪽에 ' ', '\1', '\n'      *
*             문자를 떼어내고 떼어낸 문자열을 버퍼(_cpBuffer)에 저장한다.      *
*******************************************************************************/
void CDBaseStrTrimToBuffer( char* _cpBuffer, char* _cpString )
{
    CDBaseStrLeftTrimToBuffer( _cpBuffer, _cpString );

    CDBaseStrRightTrim( _cpBuffer );
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*, int, int                                                  *
*             _cpString  : 원본 문자열의 주소                                  *
*             _iStart    : 잘라낼 문자열 번지수의 시작 번지                    *
*             _iLength   : 잘라낼 문자열의 길이                                *
*                                                                              *
* Return    : char*, 변형 된 원본 문자열의 주소(_cpString)                     *
* Stability : Async-Signal-Safe                                                *
* Explain   : 문자열(_cpString)에서 _iStart번째 번지부터 _iLength 길이만큼     *
*             잘라낸 후 잘라낸 문자열을 다시 원본 문자열(_cpString)에 저장한다.*
*******************************************************************************/
char* CDBaseStrSub( char* _cpString, int _iStart, int _iLength )
{
    int iStringIndex;
    int iBufferIndex;

    int i1;
    int i2;

    for( iStringIndex = _iStart, iBufferIndex = 0; iBufferIndex < _iLength; iStringIndex++, iBufferIndex++ )
        _cpString[iBufferIndex] = _cpString[iStringIndex];

    _cpString[iBufferIndex] = NULL;

    return  _cpString;
}


/*******************************************************************************
* Update    : 2012/10/22                                                       *
* Argument  : char*, char*, int, int                                           *
*             _cpBuffer  : 잘라낸 문자열을 저장 할 버퍼의 주소                 *
*             _cpString  : 원본 문자열의 주소                                  *
*             _iStart    : 잘라낼 문자열 번지수의 시작 번지                    *
*             _iLength   : 잘라낼 문자열의 길이                                *
*                                                                              *
* Return    : char*, 잘라낸 문자열이 저장 된 버퍼의 주소(_cpString)            *
* Stability : Async-Signal-Safe                                                *
* Explain   : 문자열(_cpString)에서 _iStart번째 번지부터 _iLength 길이만큼     *
*             잘라낸 후 잘라낸 문자열을 버퍼(_cpBuffer)에 저장한다.            *
*******************************************************************************/
char* CDBaseStrSubToBuffer( char* _cpBuffer, char* _cpString, int _iStart, int _iLength )
{
    int iStringIndex;
    int iBufferIndex;

    for( iStringIndex = _iStart, iBufferIndex = 0; iStringIndex < _iStart + _iLength; iStringIndex++, iBufferIndex++ )
        _cpBuffer[iBufferIndex] = _cpString[iStringIndex];

    _cpBuffer[iBufferIndex] = NULL;

    return  _cpBuffer;
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*                                                            *
*             _cpString  : 원본 문자열의 주소                                  *
*                                                                              *
* Return    : char*, 변형 된 원본 문자열의 주소(_cpString)                     *
* Stability : Async-Signal-Safe                                                *
* Explain   : 문자열(_cpString)에서 소문자 알파뱃에 해당하는 문자를 대문자로   *
*             변경 후 변경 된 문자열을 다시 원본 문자열(_cpString)에 저장한다. *
*******************************************************************************/
char* CDBaseStrUp( char* _cpString )
{
    int iLoop;

    for( iLoop=0; _cpString[iLoop] != 0; iLoop++ )
    {
        if( _cpString[iLoop] >= 'a' && _cpString[iLoop] <= 'z' )
            _cpString[iLoop] -= 0x20;
    }

    return  _cpString;
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*, char*                                                     *
*             _cpBuffer  : 변경 된 문자열을 저장 할 버퍼의 주소                *
*             _cpString  : 원본 문자열의 주소                                  *
*                                                                              *
* Return    : char*, 변형 된 문자열이 저장 된 버퍼의 주소(_cpString)           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 문자열(_cpString)에서 소문자 알파뱃에 해당하는 문자를 대문자로   *
*             변경 후 변경 된 문자열을 버퍼(_cpBuffer)에 저장한다.             *
*******************************************************************************/
char* CDBaseStrUpToBuffer( char* _cpBuffer, char* _cpString )
{
    int iLoop;

    for( iLoop=0; _cpString[iLoop] != 0; iLoop++ )
    {
        if( _cpString[iLoop] >= 'a' && _cpString[iLoop] <= 'z' )
            _cpBuffer[iLoop] = _cpString[iLoop] - 0x20;
        else
            _cpBuffer[iLoop] = _cpString[iLoop];
    }

    _cpBuffer[iLoop] = NULL;

    return  _cpBuffer;
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*                                                            *
*             _cpString  : 원본 문자열의 주소                                  *
*                                                                              *
* Return    : char*, 변형 된 원본 문자열의 주소(_cpString)                     *
* Stability : Async-Signal-Safe                                                *
* Explain   : 문자열(_cpString)에서 대문자 알파뱃에 해당하는 문자를 소문자로   *
*             변경 후 변경 된 문자열을 다시 원본 문자열(_cpString)에 저장한다. *
*******************************************************************************/
char* CDBaseStrDown( char* _cpString )
{
    int iLoop;

    for( iLoop=0; _cpString[iLoop] != 0; iLoop++ )
    {
        if( _cpString[iLoop] >= 'A' && _cpString[iLoop] <= 'Z' )
            _cpString[iLoop] += 0x20;
    }

    return  _cpString;
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*, char*                                                     *
*             _cpBuffer  : 변경 된 문자열을 저장 할 버퍼의 주소                *
*             _cpString  : 원본 문자열의 주소                                  *
*                                                                              *
* Return    : char*, 변형 된 문자열이 저장 된 버퍼의 주소(_cpString)           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 문자열(_cpString)에서 대문자 알파뱃에 해당하는 문자를 소문자로   *
*             변경 후 변경 된 문자열을 버퍼(_cpBuffer)에 저장한다.             *
*******************************************************************************/
char* CDBaseStrDownToBuffer( char* _cpBuffer, char* _cpString )
{
    int iLoop;

    for( iLoop=0; _cpString[iLoop] != 0; iLoop++ )
    {
        if( _cpString[iLoop] >= 'A' && _cpString[iLoop] <= 'Z' )
            _cpBuffer[iLoop] = _cpString[iLoop] + 0x20;
        else
            _cpBuffer[iLoop] = _cpString[iLoop];
    }

    _cpBuffer[iLoop] = NULL;

    return  _cpBuffer;
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*                                                            *
*             _cpHex  : 원본 문자열의 주소                                     *
*                                                                              *
* Return    : long, 문자열에서 10진수로 변경 된 10진 정수값                    *
* Stability : MT-Safe                                                          *
* Explain   : 정수형 문자열(_cpHex)을 받아서 10진수로 변형하여 반환한다.       *
*             10진 문자열인 경우 atol을 통해서 바로 10진수로 반환되며          *
*             16진 문자열(문자열 시작이 "0x")인 경우 루프문에서 변환 과정을    *
*             거친 후 10진수로 반환된다.                                       *
*******************************************************************************/
long CDBaseStrToDec( char* _cpHex )
{
    int     iLoop;
    int     iLength;

    long    iResult = 0;
    long    iMultiple = 1;

    char    caBuffer[CD_BASE_STRING_BUFFER_SIZE];

    #ifdef  _SOLARIS_
        strlcpy( caBuffer, _cpHex, CD_BASE_STRING_BUFFER_SIZE );
    #elif _CENT_OS_
        memset( caBuffer, 0x00, CD_BASE_STRING_BUFFER_SIZE );
        strncpy( caBuffer, _cpHex, CD_BASE_STRING_BUFFER_SIZE - 1 );
    #else
        strlcpy( caBuffer, _cpHex, CD_BASE_STRING_BUFFER_SIZE );
    #endif

    if( strncmp( caBuffer, "0x", 2 ) == 0 )
    {
        #ifdef  _SOLARIS_
            strlcpy( caBuffer, caBuffer + 2, CD_BASE_STRING_BUFFER_SIZE - 2 );
        #elif _CENT_OS_
            memset( caBuffer, 0x00, CD_BASE_STRING_BUFFER_SIZE );
            strncpy( caBuffer, caBuffer + 2, CD_BASE_STRING_BUFFER_SIZE - 2 - 1 );
        #else
            strlcpy( caBuffer, caBuffer + 2, CD_BASE_STRING_BUFFER_SIZE - 2 );
        #endif

    }
    else
        return  atol( caBuffer );

    iLength = strlen( caBuffer );

    for( iLoop = iLength - 1; iLoop >= 0; iLoop-- )
    {
        if( caBuffer[ iLoop ] >= '0' && caBuffer[ iLoop ] <= '9' )
        {
            iResult += ( ( caBuffer[iLoop] - '0' ) * iMultiple );
        }
        else
        {
            if( caBuffer[iLoop] >= 'A' && caBuffer[iLoop] <= 'F' )
            {
                iResult += ( ( caBuffer[iLoop] - 'A' + 10 ) * iMultiple );
            }
            else
            {
                if( caBuffer[iLoop] >= 'a' && caBuffer[iLoop] <= 'f' )
                    iResult += ( ( caBuffer[iLoop] - 'a' + 10 ) * iMultiple );
            }
        }

        iMultiple *= 16;
    }

    return  iResult;
}

