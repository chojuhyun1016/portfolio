
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

CC = /opt/SUNWspro/bin/CC

INCLUDES = -I./

LIBS = -lnsl -ldl -lrt

DEFS = -g

CFLAGS = -g -D_POSIX_THREADS -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

COMPILE = $(CC) $(DEFS) $(CFLAGS) $(INCLUDES) $(LIBS)

#
# Definitions for programs
#
TARGETS = libCNBase.a
OBJS = CNBaseReStart.o CNBaseArithmetic.o CNBaseStrings.o CNBaseTime.o CNBaseDebug.o

#
# Rules for making targets
#
all:: $(TARGETS)

CNBaseReStart.o: CNBaseReStart.c 
	$(COMPILE) -c $*.c 

CNBaseArithmetic.o: CNBaseArithmetic.c 
	$(COMPILE) -c $*.c 

CNBaseStrings.o: CNBaseStrings.c 
	$(COMPILE) -c $*.c 

CNBaseTime.o: CNBaseTime.c 
	$(COMPILE) -c $*.c 

CNBaseDebug.o: CNBaseDebug.c
	$(COMPILE) -c $*.c 

$(TARGETS) : $(OBJS)
	ar curv $@ $(OBJS)
	ranlib $@
	cp *.h  ../../Inc
	cp $@ ../../Bin

clean:
	/bin/rm -f $(OBJS) $(TARGETS) *.o core ../$(TARGETS)

