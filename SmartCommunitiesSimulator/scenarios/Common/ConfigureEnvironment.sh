#!/bin/bash
#
#   This script sets common environment variables for running a scenario.
#
JadeHome=external/jade-4.2
#
#   Define the jar files upon which JADE depends.  These environment variables
#   are used to create both the classpath and properties referenced in the
#   codebase section of the security policy files. 
#
export JadeJar=${JadeHome}/lib/jade.jar
export CommonsCodecJar=${JadeHome}/lib/commons-codec/commons-codec-1.3.jar
export JadeSecurityJar=${JadeHome}/add-ons/security/lib/jadeSecurity.jar
#
#   Define the JADE class path.
#
export JadeClasspath=${JadeJar}
export JadeClasspath=${JadeClasspath}:${CommonsCodecJar}
export JadeClasspath=${JadeClasspath}:${JadeSecurityJar}
