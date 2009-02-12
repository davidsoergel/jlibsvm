#!/bin/bash

# adapted from http://www.mostscript.com/weblog/?p=43

MAX_INSTANCE_RESIDENT_SIZE=2048 #MB
USER_HOME=`(cd ~; pwd)`

MBVAL="0"

function gb_to_mb {
    MBVAL=`/usr/bin/python -c "print int(float('$1')*1000)"`
    return 0
}

function watchpid {
    #PID is $1

    ### get resident size from top or empty string if resident size doesn't
    ### have the 'm' (megabytes) suffix.
    RESIDENT=`top -b -n1 | grep $1 | sed -e 's/[ ]\+/ /g' | cut -d' ' -f6 | \\
    grep "[gm]" | tr -d '\n'`
    ### exit if no process w/ PID found, or if size doesn't have 'm' suffix;
    ### -- in either of these cases, $RESIDENT will be empty
    if [ -z "$RESIDENT" ]; then return 0; fi;

    if [ -n "`echo -n $RESIDENT | grep g`" ]; then
        ##gigabytes, not megabytes
        RESIDENT=`echo -n $RESIDENT | sed -e 's/g//'`
        gb_to_mb $RESIDENT
        RESIDENT=$MBVAL
    fi

    RESIDENT=`echo -n $RESIDENT | sed -e 's/m//'`
    if [ $RESIDENT -gt $MAX_INSTANCE_RESIDENT_SIZE ]; then
        echo "Process (PID $1) is too big: $RESIDENT MB"
        echo "Maximum allowed is $MAX_INSTANCE_RESIDENT_SIZE; sending SIGHUP"
        kill -HUP $1
    fi;

}

function main {
    # fork, run the remainder of the command line, and watch the resulting pid

     do watchpid $pid;

}

main

