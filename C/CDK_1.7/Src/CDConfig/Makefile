
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

CC = gcc    #CENT_OS
# CC = /opt/SUNWspro/bin/CC    #SOLARIS

INCLUDES = -I./

LIBS = #-lnsl -ldl -lrt

DEFS = -g

#OS = -D_CENT_OS_
OS = -D_SOLARIS_

CFLAGS = -D_POSIX_THREADS -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

COMPILE = $(CC) $(OS) $(DEFS) $(CFLAGS) $(INCLUDES) $(LIBS)

#
# Definitions for programs
#
TARGETS = libCDConfig.a
OBJS = CDConfigApi.o CDConfigRead.o

#
# Rules for making targets
#
all:: $(TARGETS)

CDConfigApi.o: CDConfigApi.c 
	$(COMPILE) -c $*.c 

CDConfigRead.o: CDConfigRead.c 
	$(COMPILE) -c $*.c 

$(TARGETS) : $(OBJS)
	ar curv $@ $(OBJS)
	ranlib $@
	cp *.h  ../../Inc
	cp $@ ../../Bin

clean:
	/bin/rm -f $(OBJS) $(TARGETS) *.o core ../$(TARGETS)

