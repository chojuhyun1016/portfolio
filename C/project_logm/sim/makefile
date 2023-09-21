#
CCX=        /opt/SUNWspro/bin/CC

SVCBASE     = $(TELESA_SRC_HOME)
DIR_SMSINC = $(SVCBASE)/comminc
DIR_SMSLIB = $(SVCBASE)/commlib
INCDIR      = -I$(SVCBASE)/comminc -I$(SVCBASE)/comminc/mwinc2
LIBDIR      = -L$(SVCBASE)/commlib
INCLUDES    = $(INCDIR)
LLOPTIONS   = $(KTFLIB)

LIBLINK     = 

CFLAGS		= -c -g -mt -compat=4 -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT
LDFLAGS	    = -compat=4  -g -L$(LIBHOME) $(LIBDIR) -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT
#LDFLAGS	    = -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

LIBTOT      = -lnsl -lpthread -lsocket
#LIBTOT      = -lnsl -lsocket -ltinyxml -lktfsmci5a -lthread -lpthread -lrwtool -ltelesastat

CLASSDIR	= . 

HDRS	= loggerwriter.h 

SRCS	= loggerwriter.cpp
OBJS = $(SRCS:.cpp=.o)

# Suffix rules

.SUFFIXES: .o .cpp

#TARGET = loggerwriter 
EXECS = libloggerwriter.a 

##########################    Targets    ########################

target: ${EXECS}

all: ${EXECS}

loggerwriter.o: 
	 $(CCX) $(CFLAGS) $(INCLUDES) loggerwriter.cpp logqueue.cpp logconfig.cpp


libloggerwriter.a: loggerwriter.o
	/bin/rm -f libloggerwriter.a
	ar curv libloggerwriter.a loggerwriter.o logqueue.o logconfig.o

install: libloggerwriter.a
	cp libloggerwriter.a ../libloggerwriter.a
	cat loggerwriter.h > $(DIR_SMSINC)/log/loggerwriter.h
	
clean:
	rm -rf $(EXECS) *.o *.a
	

