package com.mesosphere.framework

import java.util

import org.apache.mesos.Protos._
import org.apache.mesos.{MesosSchedulerDriver, SchedulerDriver, Scheduler}

class AwesomeScheduler extends Scheduler {

  override def offerRescinded(driver: SchedulerDriver, offerId: OfferID): Unit = ???

  override def disconnected(driver: SchedulerDriver): Unit = ???

  override def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo): Unit = ???

  override def slaveLost(driver: SchedulerDriver, slaveId: SlaveID): Unit = ???

  override def error(driver: SchedulerDriver, message: String): Unit = ???

  override def statusUpdate(driver: SchedulerDriver, status: TaskStatus): Unit = ???

  override def frameworkMessage(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, data: Array[Byte]): Unit = ???

  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]): Unit = ???

  override def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo): Unit = ???

  override def executorLost(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, status: Int): Unit = ???

}

object AwesomeScheduler {
  def main(args: Array[String]) {
    val frameworkInfo = FrameworkInfo.newBuilder()
      .build()
    val driver = new MesosSchedulerDriver(new AwesomeScheduler, frameworkInfo, "zk://localhost:2181/mesos")
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
