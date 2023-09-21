
# General definitions
#

#  /usr/bin/rm
#  The following options are supported for /usr/bin/rm only:
#
#    -f    Remove all files (whether write-protected or not) in a
#          directory  without  prompting  the  user.  In a write-
#          protected directory, however, files are never  removed
#          (whatever  their permissions are), but no messages are
#          displayed. If the removal of a write-protected  direc-
#          tory  is  attempted,  this option will not suppress an
#          error message.

RM = /usr/bin/rm -f

#
# Definitions for compilation
#

CC = gcc
#CC = /opt/SUNWspro/bin/CC

INCLUDES = -I./

LIBS = #-lnsl -ldl -lrt

DEFS = -g

#OS = -D_CENT_OS_
OS = -D_SOLARIS_

CFLAGS = -D_POSIX_THREADS -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

COMPILE = $(CC) $(DEFS) $(OS) $(CFLAGS) $(INCLUDES) $(LIBS)

#
# Definitions for programs
#
TARGETS = libCDLog.a
OBJS = CDLogApi.o CDLogWrite.o

#
# Rules for making targets
#
all:: $(TARGETS)

CDLogApi.o: CDLogApi.c 
	$(COMPILE) -c $*.c 

CDLogWrite.o: CDLogWrite.c 
	$(COMPILE) -c $*.c 

$(TARGETS) : $(OBJS)
	ar curv $@ $(OBJS)
	ranlib $@
	cp *.h  ../../Inc
	cp $@ ../../Bin

clean:
	/bin/rm -f $(OBJS) $(TARGETS) *.o core ../$(TARGETS)

