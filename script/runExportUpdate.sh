#!/bin/ksh
#############################################################################
#
# Starter script
#
#############################################################################

INSTALLDIR=`dirname $0`
test -z "$INSTALLDIR" && INSTALLDIR=.

LIBDIR=$INSTALLDIR/lib
CONFDIR=$INSTALLDIR/conf
export INSTALLDIR LIBDIR CONFDIR

JAVA_HOME=/usr/java
export JAVA_HOME

PROPSFILE=$CONFDIR/ExportUpdates.properties
LOGPROPS=$CONFDIR/ExportUpdates-logging.properties

while getopts i: argswitch
do
	case $argswitch in
	i)	INFILE=$OPTARG
		;;
	\?)	printf "Usage: %s: -i infile\n" `basename $0`
		exit 2
		;;
	esac
done

if [ -z "$INFILE" ]; then
	printf "Usage: %s: -i infile\n" `basename $0`
fi

JFLAGS="-Xms256m -Xmx3072m -Djavax.xml.transform.TransformerFactory=net.sf.saxon.TransformerFactoryImpl -Djava.util.logging.config.file=$LOGPROPS"
export JFLAGS

CLASSPATH=$INSTALLDIR
for i in `find $LIBDIR -type f -name '*'.jar -print`
do
	CLASSPATH=$CLASSPATH:$i
done
export CLASSPATH


exec $JAVA_HOME/bin/java $JFLAGS com.atex.h11.custom.sph.export.update.Main -p $PROPSFILE -i $INFILE

