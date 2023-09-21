RM = /usr/bin/rm -f

CC = /opt/SUNWspro/bin/CC

INCLUDES = -I. -I../CND -I../Include -I../Include/Network/BaseClasses  -I../Include/Common/UtilCodeClasses -I../Include/Common/ExecuteClasses -I../Include/Common/CompletionPortClasses  -I../Include/Object

LIBS = -lsocket -lnsl -ldl
LDIR = -L./ -L../Lib

DEFS = -g
#DEFS += -DDEBUG

CFLAGS = -D_POSIX_THREADS -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

COMPILE = $(CC) $(DEFS) $(CFLAGS) $(INCLUDES) $(LDIR) 

#
# Definitions for programs
#
TARGETS = libcn_network.a
OBJS = CNCommonUtil.o CNCompletionPortBase.o CNCompletionPort.o CNAPIThread.o CNAPIThreadPool.o CNAPIIOCPCompletionPort.o CNExecuteCompletionPort.o CNExecuteCompletionPortThread.o CNNetAPISocket.o CNNetIOSocket.o CNNetIOConnectiveAcceptor.o CNNetIOConnectiveAcceptorTCP.o CNNetIOSocketBase.o CNNetIOConnectableTCP.o CNNetIOReceivableStream.o sock_util.o

#
# Rules for making targets
#
all:: $(TARGETS)

CNCommonUtil.o: CNCommonUtil.cpp
	$(COMPILE) -c $*.cpp

CNCompletionPortBase.o: CNCompletionPortBase.cpp
	$(COMPILE) -c $*.cpp

CNCompletionPort.o: CNCompletionPort.cpp
	$(COMPILE) -c $*.cpp

CNAPIThread.o: CNAPIThread.cpp
	$(COMPILE) -c $*.cpp

CNAPIThreadPool.o: CNAPIThreadPool.cpp
	$(COMPILE) -c $*.cpp

CNAPIIOCPCompletionPort.o: CNAPIIOCPCompletionPort.cpp
	$(COMPILE) -c $*.cpp

CNExecuteCompletionPort.o: CNExecuteCompletionPort.cpp
	$(COMPILE) -c $*.cpp

CNExecuteCompletionPortThread.o: CNExecuteCompletionPortThread.cpp
	$(COMPILE) -c $*.cpp

CNNetAPISocket.o: CNNetAPISocket.cpp
	$(COMPILE) -c $*.cpp

CNNetIOSocket.o: CNNetIOSocket.cpp
	$(COMPILE) -c $*.cpp

CNNetIOConnectiveAcceptor.o: CNNetIOConnectiveAcceptor.cpp
	$(COMPILE) -c $*.cpp

CNNetIOConnectiveAcceptorTCP.o: CNNetIOConnectiveAcceptorTCP.cpp
	$(COMPILE) -c $*.cpp

CNNetIOSocketBase.o: CNNetIOSocketBase.cpp
	$(COMPILE) -c $*.cpp

CNNetIOConnectableTCP.o: CNNetIOConnectableTCP.cpp
	$(COMPILE) -c $*.cpp

CNNetIOReceivableStream.o: CNNetIOReceivableStream.cpp
	$(COMPILE) -c $*.cpp

sock_util.o: sock_util.cpp
	$(COMPILE) -c $*.cpp

$(TARGETS) : $(OBJS)
	ar curv $@ $(OBJS)
	ranlib $@
	cp $@ ../Lib

clean:
	/bin/rm -f $(OBJS) $(TARGETS) *.o core
	/bin/rm -f ./libcn_network.a
