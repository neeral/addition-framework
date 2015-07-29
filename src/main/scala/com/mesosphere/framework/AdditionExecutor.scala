package com.mesosphere.framework

import com.google.protobuf.ByteString
import org.apache.mesos.Protos._
import org.apache.mesos.{Executor, ExecutorDriver, MesosExecutorDriver}

class AdditionExecutor extends Executor {

  override def shutdown(driver: ExecutorDriver): Unit = println(s"Received request to shutdown executor")

  override def disconnected(driver: ExecutorDriver): Unit = println(s"Received request to disconnect")

  override def killTask(driver: ExecutorDriver, taskId: TaskID): Unit = println(s"Recieved request to kill taskId=[${taskId.getValue}]")

  override def reregistered(driver: ExecutorDriver, slaveInfo: SlaveInfo): Unit = println(s"Slave reregistered")

  override def error(driver: ExecutorDriver, message: String): Unit = println(s"Error: [$message]")

  override def frameworkMessage(driver: ExecutorDriver, data: Array[Byte]): Unit = println(s"Received a framework message: [$data]")

  override def registered(driver: ExecutorDriver, executorInfo: ExecutorInfo, frameworkInfo: FrameworkInfo, slaveInfo: SlaveInfo): Unit =
    println(s"Executor [${executorInfo.getExecutorId.getValue}] registered on slave [${slaveInfo.getId.getValue}] for FrameworkID=[${frameworkInfo.getId.getValue})]")

  override def launchTask(driver: ExecutorDriver, task: TaskInfo): Unit = {
    val taskStatus = TaskStatus.newBuilder
      .setState(TaskState.TASK_RUNNING)
      .setTaskId(task.getTaskId)
      .setExecutorId(task.getExecutor.getExecutorId)
      .build
    driver.sendStatusUpdate(taskStatus)

    val data = task.getData.toByteArray
    val seq = Utils.deserialize[Seq[Int]](data)
    println(s"Task id [${task.getTaskId.getValue}] has these elements: [$seq]")
    val total = seq.sum
    println(s"Result of task id [${task.getTaskId.getValue}] is $total")

    driver.sendStatusUpdate(TaskStatus.newBuilder
      .setState(TaskState.TASK_FINISHED)
      .setTaskId(task.getTaskId)
      .setExecutorId(task.getExecutor.getExecutorId)
      .setData(ByteString.copyFromUtf8(total.toString))
      .setMessage(s"The sum for this task is $total")
      .build
    )
  }

}
object AdditionExecutor {
  def main(args: Array[String]) {
    val driver = new MesosExecutorDriver(new AdditionExecutor)
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
