package com.mesosphere.framework

import org.apache.mesos.Protos._
import org.apache.mesos.{MesosExecutorDriver, ExecutorDriver, Executor}

class AwesomeExecutor extends Executor {
  override def shutdown(driver: ExecutorDriver): Unit = ???

  override def disconnected(driver: ExecutorDriver): Unit = ???

  override def killTask(driver: ExecutorDriver, taskId: TaskID): Unit = ???

  override def reregistered(driver: ExecutorDriver, slaveInfo: SlaveInfo): Unit = ???

  override def error(driver: ExecutorDriver, message: String): Unit = ???

  override def frameworkMessage(driver: ExecutorDriver, data: Array[Byte]): Unit = ???

  override def registered(driver: ExecutorDriver, executorInfo: ExecutorInfo, frameworkInfo: FrameworkInfo, slaveInfo: SlaveInfo): Unit = ???

  override def launchTask(driver: ExecutorDriver, task: TaskInfo): Unit = ???

}
object AwesomeExecutor {
  def main(args: Array[String]) {
    val driver = new MesosExecutorDriver(new AwesomeExecutor)
    val status = driver.run() match {
      case Status.DRIVER_STOPPED => 0
      case Status.DRIVER_ABORTED => 1
      case Status.DRIVER_NOT_STARTED => 2
      case _ => 3
    }
    driver.stop()
    System.exit(status)
  }
}
