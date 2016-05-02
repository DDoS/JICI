# JICI #

[![Build status](https://travis-ci.org/DDoS/JICI.svg?branch=master)](https://travis-ci.org/DDoS/JICI)

Stands for <strong>J</strong>ava <strong>i</strong>nterpreter and <strong>c</strong>ode <strong>i</strong>nteraction.

Currently supported are expressions (all operators), including arrays, field accesses, method and constructor calls.
Generic types are fully supported, with the exception of inference.
Imports and variable declaration statements are also supported.

## Cloning
If you are using Git, use this command to clone the project: `git clone git@github.com:DDoS/JICI.git`

## Building
Install [Gradle](http://www.gradle.org/). Use the command `gradle` to build.

## Running
Use the command `gradle run` to run.  
Running JICI will start a read-evaluate-print loop. A proper environment has yet to be setup so the prompt is very basic.
