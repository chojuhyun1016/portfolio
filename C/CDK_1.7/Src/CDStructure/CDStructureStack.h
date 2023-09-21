#ifndef __CD_STRUCTURE_STACK_H__
#define __CD_STRUCTURE_STACK_H__

#include "CDStructureDefinitions.h"

#ifdef  _SOLARIS_
    #include <pthread.h>
#elif _CENT_OS_
    #include <pthread.h>
#else
    #include <pthread.h>
#endif


#ifdef __cplusplus
extern "C" {
#endif

#ifndef CD_STRUCTURE_STACK_FULL
#define CD_STRUCTURE_STACK_FULL     1
#endif

#ifndef CD_STRUCTURE_STACK_EMPTY
#define CD_STRUCTURE_STACK_EMPTY    2
#endif

#ifndef CD_STRUCTURE_STACK_POP
#define CD_STRUCTURE_STACK_POP      3
#endif

#ifndef CD_STRUCTURE_STACK_PUSH
#define CD_STRUCTURE_STACK_PUSH     4
#endif


typedef struct _stCDStructStackContainer
{
    int     type;

    void    *data;

    struct _stCDStructStackContainer    *next;
    struct _stCDStructStackContainer    *prev;

} stCDStructStackContainer;


typedef struct _stCDStructureStackNode
{
    int             m_reference;

    stCDStructStackContainer        m_head;
    stCDStructStackContainer        m_tail;

} stCDStructureStackNode;


typedef struct _stCDStructureStack
{
    pthread_mutex_t m_iLock;

    stCDStructureStackNode      m_stOccupiedList;
    stCDStructureStackNode      m_stIdleList;

} stCDStructureStack;


stCDStructureStack* CDStructureStackMake();

int CDStructureStackInit( stCDStructureStack* _stpStack, int _iPrepare );
void CDStructureStackFree( stCDStructureStack** _stppStack );

int CDStructureStackAppend( stCDStructureStack* _stpStack, int _iAppendCount );
int CDStructureStackDelete( stCDStructureStack* _stpStack, int _iDeleteCount );

int CDStructureStackPush( stCDStructureStack* _stpStack, int _iType, void* _vpData );
int CDStructureStackPop( stCDStructureStack* _stpStack, int* _ipType, void** _vppData );

int CDStructureStackGetUseCount( stCDStructureStack* _stpStack );
int CDStructureStackGetIdleCount( stCDStructureStack* _stpStack );

static void CDStructStackInitLinkedList( stCDStructureStackNode* _stpNode );

static int CDStructStackAppendTailLinkedList( stCDStructureStackNode* _stpNode );
static int CDStructStackRemoveTailLinkedList( stCDStructureStackNode* _stpNode );

static stCDStructStackContainer* CDStructStackPushLinkedList( stCDStructureStackNode* _stpNode, stCDStructStackContainer* _stpData );
static stCDStructStackContainer* CDStructStackPopLinkedList( stCDStructureStackNode* _stpNode );

static void CDStructStackDeleteAllLinkedList( stCDStructureStackNode* _stpNode );
static stCDStructStackContainer* CDStructStackDeleteLinkedList( stCDStructureStackNode* _stpNode, stCDStructStackContainer* _stpDeleteNode );


#ifdef __cplusplus
}
#endif

#endif

