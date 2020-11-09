#!/bin/bash

# for running in the ci-pipeline. to run locally, use: mvn -Dtest=RunAllUnitTests test

JAVA_HOME=${JDK_HOME} mvn -Dtest=RunAllUnitTests test
result=$?

exit $result
