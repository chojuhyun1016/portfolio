#!/bin/sh

NOW=`date +%Y%m%d_%H%M%S`

PROJECT_NAME=$1
JENKINS_HOME=$2
JENKINS_PROJECT_NAME=$3
GRADLE_PATH=/data1/mps/.gradle

echo NOW : ${NOW}
echo PROJECT_NAME  : ${PROJECT_NAME}
echo JENKINS_HOME  : ${JENKINS_HOME}
echo JENKINS_PROJECT_NAME  : ${JENKINS_PROJECT_NAME}
echo GRADLE_PATH : ${GRADLE_PATH}

cd ${GRADLE_PATH}

tar -cvzf ${PROJECT_NAME}.tar.gz.${NOW} ${PROJECT_NAME}

rm -rf ${PROJECT_NAME}

mv ${PROJECT_NAME}.tar.gz.${NOW} backup/

cd ${JENKINS_HOME}/workspace/${JENKINS_PROJECT_NAME}/gradle

cp ${PROJECT_NAME}.tar.gz ${GRADLE_PATH}

cd ${GRADLE_PATH}

tar -xvzf ${PROJECT_NAME}.tar.gz

rm -rf ${PROJECT_NAME}.tar.gz
