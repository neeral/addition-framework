package com.mesosphere.framework

import java.util
import java.util.UUID

import com.google.protobuf.ByteString
import org.apache.mesos.Protos._
import org.apache.mesos.{MesosSchedulerDriver, Scheduler, SchedulerDriver}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.Random

class AdditionScheduler extends Scheduler {
  val n: Int = 10000
  val chunkSize: Int = 1000
  val toProcess = Seq.fill(n)(Random.nextInt)

  val path = "file:///home/vagrant/hostfiles/target/scala-2.11/addition-framework-assembly-0.0.1-SNAPSHOT.jar" // this could also be a HTTP URL
  val uri = CommandInfo.URI.newBuilder.setValue(path).setExtract(false).build
  val cmd = CommandInfo.newBuilder.addUris(uri).setValue("java -cp addition-framework-assembly-0.0.1-SNAPSHOT.jar com.mesosphere.framework.AdditionExecutor")
  val additionExecutor = ExecutorInfo.newBuilder
    .setExecutorId(ExecutorID.newBuilder.setValue("additionExecutor-" + UUID.randomUUID())) // must be unique
    .setName("exe-templat")
    .setCommand(cmd)
    .addAllResources(
      Seq(
        Resource.newBuilder
          .setName("cpus")
          .setType(Value.Type.SCALAR)
          .setScalar(
            Value.Scalar.newBuilder.setValue(1).build
          )
          .build,
        Resource.newBuilder
          .setName("mem")
          .setType(Value.Type.SCALAR)
          .setScalar(
            Value.Scalar.newBuilder.setValue(128).build
          )
          .build
      ).asJava
    )
    .build

  val executors = scala.collection.mutable.Map[String, ExecutorInfo]()

  var total = 0
  var latestTaskNumber = 0
  
  override def offerRescinded(driver: SchedulerDriver, offerId: OfferID): Unit = println(s"Offer [${offerId.getValue}] has been rescinded")

  override def disconnected(driver: SchedulerDriver): Unit = println("Disconnected from the Mesos master...")

  override def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo): Unit = println("Re-registered with a Mesos master")

  override def slaveLost(driver: SchedulerDriver, slaveId: SlaveID): Unit = println(s"Slave [${slaveId.getValue}] lost")

  // this callback is not very useful as only has a String for information
  override def error(driver: SchedulerDriver, message: String): Unit = println(s"ERROR: [$message]")

  override def statusUpdate(driver: SchedulerDriver, status: TaskStatus): Unit = {
    println(s"""Task [${status.getTaskId.getValue}] is in state ${status.getState} and has message "${status.getMessage}" """)

    if (TaskState.TASK_FINISHED == status.getState) {
      val data: ByteString = status.getData
      val result = data.toStringUtf8.toInt
      total += result

      if (latestTaskNumber >= toProcess.size / chunkSize) {
        println("received all the results. stopping the SchedulerDriver.")
        val agree: Boolean = toProcess.sum == total
        println(s">>> calculations agree? $agree")
        driver.stop()
      }
    }
  }

  override def frameworkMessage(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, data: Array[Byte]): Unit = ???

  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]): Unit = {
    for (offer <- offers.asScala) {
      println(s"Received resource offer [${offer.getId.getValue}]")
      // just create a single task, let's not be too clever
      val taskNumber = latestTaskNumber
      if (toProcess.size <= taskNumber * chunkSize) {
        println("Exceeded toProcess input array size. No need to launch more tasks.")
        driver.declineOffer(offer.getId)
      } else {
        val chunk = toProcess.slice(taskNumber * chunkSize, (taskNumber + 1) * chunkSize)

        val task = TaskInfo.newBuilder
          .setTaskId(TaskID.newBuilder().setValue(s"$taskNumber"))
          .setName(s"merge_task_$taskNumber")
          .setSlaveId(offer.getSlaveId)
          .setData(ByteString.copyFrom(Utils.serialize(chunk)))
          .setExecutor(getExecutor(offer.getSlaveId.getValue)) // need an executor per slave
          .addAllResources(  // both executor and each task needs resources
            Seq(
              Resource.newBuilder
                .setName("cpus")
                .setType(Value.Type.SCALAR)
                .setScalar(
                  Value.Scalar.newBuilder.setValue(0.5).build
                )
                .build,
              Resource.newBuilder
                .setName("mem")
                .setType(Value.Type.SCALAR)
                .setScalar(
                  Value.Scalar.newBuilder.setValue(32).build
                )
                .build
            ).asJava
          )
          .build
        driver.launchTasks(Seq(offer.getId).asJava, Seq(task).asJava)
        incrementLatestTaskNumber()
      }
    }
  }

  def incrementLatestTaskNumber(): Unit = { latestTaskNumber += 1 }

  def getExecutor(slaveId: String): ExecutorInfo = {
    if (!executors.contains(slaveId)) {
      val executor = ExecutorInfo.newBuilder(additionExecutor)
        .setExecutorId(ExecutorID.newBuilder.setValue("additionExecutor-" + slaveId + "-" + UUID.randomUUID()).build)
        .setName("additionExecutor-" + slaveId)
        .build
      executors += (slaveId -> executor)
    }
    executors.get(slaveId).get
  }

  override def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo): Unit = {
    val host = masterInfo.getHostname
    val port = masterInfo.getPort
    println(s"Registered with Mesos master [$host:$port]. FrameworkID=${frameworkId.getValue}") // only time you get a framework id, useful if need to reregister
  }

  // this is not implemented in Mesos so I don't need to implement it either :)
  override def executorLost(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, status: Int): Unit = ???

}

object AdditionScheduler {
  def main(args: Array[String]) {
    val frameworkInfo = FrameworkInfo.newBuilder()
      .setName("distr addition Framework")
      .setFailoverTimeout((60 seconds) toMillis)
      .setCheckpoint(false)
      .setUser("") // Mesos can do this for us
      .build
    val driver = new MesosSchedulerDriver(new AdditionScheduler, frameworkInfo, "zk://localhost:2181/mesos")
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
