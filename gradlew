#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAVA_EXE="java"
[ -n "$JAVA_HOME" ] && JAVA_EXE="$JAVA_HOME/bin/java"
exec "$JAVA_EXE" -DprojectDir="$APP_HOME" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
