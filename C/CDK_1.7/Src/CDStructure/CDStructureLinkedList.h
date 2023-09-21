#ifndef __CD_STRUCTURE_LINKED_LIST_H__
#define __CD_STRUCTURE_LINKED_LIST_H__

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

#ifndef CD_STRUCTURE_LINKED_LIST_FULL
#define CD_STRUCTURE_LINKED_LIST_FULL			1
#endif

#ifndef CD_STRUCTURE_LINKED_LIST_EMPTY
#define CD_STRUCTURE_LINKED_LIST_EMPTY			2
#endif

#ifndef CD_STRUCTURE_LINKED_LIST_POP
#define CD_STRUCTURE_LINKED_LIST_POP			3
#endif

#ifndef CD_STRUCTURE_LINKED_LIST_PUSH
#define CD_STRUCTURE_LINKED_LIST_PUSH			4
#endif

#ifndef CD_STRUCTURE_LINKED_LIST_NOT_FOUND
#define CD_STRUCTURE_LINKED_LIST_NOT_FOUND		100
#endif

typedef struct _stCDStructLinkedListContainer
{
	int		key;
	int		type;

	void	*data;

	struct _stCDStructLinkedListContainer	*next;
	struct _stCDStructLinkedListContainer	*prev;

} stCDStructLinkedListContainer;


typedef struct _stCDStructureLinkedListNode
{
	int				m_reference;

	stCDStructLinkedListContainer		m_head;
	stCDStructLinkedListContainer		m_tail;

} stCDStructureLinkedListNode;


typedef struct _stCDStructureLinkedList
{
	pthread_mutex_t	m_iLock;

	stCDStructureLinkedListNode		m_stOccupiedList;
	stCDStructureLinkedListNode		m_stIdleList;

} stCDStructureLinkedList;


stCDStructureLinkedList* CDStructureLinkedListMake();

int CDStructureLinkedListInit( stCDStructureLinkedList* _stpLinkedList, int _iPrepare );
void CDStructureLinkedListFree( stCDStructureLinkedList** _stppLinkedList );

int CDStructureLinkedListAppend( stCDStructureLinkedList* _stpLinkedList, int _iAppendCount );
int CDStructureLinkedListDelete( stCDStructureLinkedList* _stpLinkedList, int _iDeleteCount );

int CDStructureLinkedListPush( stCDStructureLinkedList* _stpLinkedList, int _iKey, int _iType, void* _vpData );
int CDStructureLinkedListPop( stCDStructureLinkedList* _stpLinkedList, int _iKey, int* _ipType, void** _vppData );

int CDStructureLinkedListGetUseCount( stCDStructureLinkedList* _stpLinkedList );
int CDStructureLinkedListGetIdleCount( stCDStructureLinkedList* _stpLinkedList );

static void CDStructLinkedListInitLinkedList( stCDStructureLinkedListNode* _stpNode );

static int CDStructLinkedListAppendTailLinkedList( stCDStructureLinkedListNode* _stpNode );
static int CDStructLinkedListRemoveTailLinkedList( stCDStructureLinkedListNode* _stpNode );

static stCDStructLinkedListContainer* CDStructLinkedListPushLinkedList( stCDStructureLinkedListNode* _stpNode, stCDStructLinkedListContainer* _stpData );
static stCDStructLinkedListContainer* CDStructLinkedListPopLinkedList( stCDStructureLinkedListNode* _stpNode );
static stCDStructLinkedListContainer* CDStructLinkedListSearchAndPopLinkedList( stCDStructureLinkedListNode* _stpNode, int _iKey );

static void CDStructLinkedListDeleteAllLinkedList( stCDStructureLinkedListNode* _stpNode );
static stCDStructLinkedListContainer* CDStructLinkedListDeleteLinkedList( stCDStructureLinkedListNode* _stpNode, stCDStructLinkedListContainer* _stpDeleteNode );


#ifdef __cplusplus
}
#endif

#endif
