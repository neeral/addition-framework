Addition-Calculator Scala Mesos Framework
=============================


# addition-calculator

A framework for [Apache Mesos](http://mesos.apache.org/) that generates a list of random integers and distributes the task of calculating their sum. 

Based on [Rendler](https://github.com/mesosphere/RENDLER) implementation.

Addition-Calculator consists of two main components:
* `AdditionExecutor` extends `mesos.Executor`
* `AdditionScheduler` extends `mesos.Scheduler`

## Quick Start with Vagrant

### Requirements

- [VirtualBox](http://www.virtualbox.org/) 4.1.18+
- [Vagrant](http://www.vagrantup.com/) 1.3+
- [git](http://git-scm.com/downloads) (command line tool)
- [Scala](www.scala-lang.org/download)
- [Sbt](http://www.scala-sbt.org/) (build tool)

### Start the `mesos-demo` VM

```bash
$ wget http://downloads.mesosphere.io/demo/mesos.box -O /tmp/mesos.box
$ vagrant box add --name mesos-demo /tmp/mesos.box
$ git clone https://github.com/neeral/addition-framework
$ vagrant up
```

### Build 
```bash
$ sbt clean assembly
```

### Execution:

```bash
$ vagrant ssh
vagrant@mesos:~ $ cd hostfiles

# Start the scheduler
vagrant@mesos:~ $ java -cp /home/vagrant/hostfiles/target/scala-2.11/addition-framework-assembly-0.0.1-SNAPSHOT.jar com.mesosphere.framework.AdditionSchedulervagrant@mesos

# once completed, it will stop on its own
# example output at the end
Task [9] is in state TASK_FINISHED and has message "The sum for this task is 1732163649" 
received all the results. stopping the SchedulerDriver.
>>> calculations agree? true
```

### Shutting down the `mesos-demo` VM

```bash
# Exit out of the VM
vagrant@mesos:hostfiles $ exit
# Stop the VM
$ vagrant halt
# To delete all traces of the vagrant machine
$ vagrant destroy
```


## Addition-Framework Architecture

### Addition-Framework Scheduler

Creates a list of _n_ random integers. It carves out the list of random integers into chunks of size _chunkSize_. Upon receiving resource offers, the scheduler launches a task and assigns it a chunk. When the task finishes the scheduler keeps track of a running total. When all the tasks complete, it prints out the total sum of all the numbers and compares it to the result if done serially, with `list.sum`  

#### Default values

* _n_ = 10000
* _chunkSize_ = 1000

### Addition-Framework Executor

Receives a _chunkSize_ list of numbers, adds them all together and returns the sum in the data field of the TASK_FINISHED status update message.