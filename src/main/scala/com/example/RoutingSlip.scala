package com.example

import akka.actor._

case class CustomerInformation(val name: String, val federalTaxId: String)
case class ContactInformation(val postalAddress: PostalAddress, val telephone: Telephone)
case class PostalAddress(
                        val address1: String, val address2: String,
                        val city: String, val state: String, val zipCode: String
                        )
case class Telephone(val number: String)
case class ServiceOption(val id: String, val description: String)
case class RegistrationData(val customerInformation: CustomerInformation, val contactInformation: ContactInformation, val serviceOption: ServiceOption)
case class ProcessStep(val name: String, val processor: ActorRef)

case class RegistrationProcess(val processId: String, val processSteps: Seq[ProcessStep], val currentStep: Int) {
  def this(processId: String, processSteps: Seq[ProcessStep]) {
    this(processId, processSteps, 0)
  }

  def isCompleted: Boolean = {
    currentStep >= processSteps.size
  }

  def nextStep(): ProcessStep = {
    if (isCompleted) {
      throw new IllegalStateException("Process had already completed.")
    }
    processSteps(currentStep)
  }

  def stepCompleted(): RegistrationProcess = {
    new RegistrationProcess(processId, processSteps, currentStep + 1)
  }
}

object RoutingSlipDriver extends CompletableApp(4) {
}
