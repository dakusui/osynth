# The FloorPlan library
FloorPlan is a library to model a heterogeneous software system's specification
for testing.

A challenge in system level automated testing is in modeling SUT in a way where
you can

1. deploy your SUT automatically and repeatably
2. verify deployment variations.
3. deploy your SUT in various physical environments (dev, stg, prod, local) while
  a test scenario can work transparently.
4. reuse and run your tests by combinating your test scenario, environments, and 
  deployment from your repertoire.

at the same time.
  
This library gives a programmable way to achieve those. For more detail,
see our [our wiki][1].

## Installation and how to use it
Following is maven coordinate.

```xml
  <dependency>
    <groupId>com.github.dakusui</groupId>
    <artifactId>floorplan</artifactId>
    <version>[5.0.0)</version>
  </dependency>
```

## Building FloorPlan library.
This library is built and tested with following JDK and Maven.
```
// JDK
java version "1.8.0_172"
Java(TM) SE Runtime Environment (build 1.8.0_172-b11)
Java HotSpot(TM) 64-Bit Server VM (build 25.172-b11, mixed mode)
// Maven
Apache Maven 3.5.3 (3383c37e1f9e9b3bc3df5050c29c8aff9f295297; 2018-02-25T04:49:05+09:00)
```
 
To build this, clone this repo and run following maven command.

```
// clone
$ git clone https://github.com/dakusui/floorplan.git
// build and test
$ mvn clean compile test

```
# References
* [1]: FloorPlan Wiki

[1]: https://github.com/dakusui/floorplan/wiki
# osynth
