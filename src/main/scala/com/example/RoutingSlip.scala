package com.example

import akka.actor._

case class CustomerInformation(name: String, federalTaxId: String)
case class ContactInformation(postalAddress: PostalAddress, telephone: Telephone)
case class PostalAddress(
                        address1: String, address2: String,
                        city: String, state: String, zipCode: String
                        )
case class Telephone(number: String)
case class ServiceOption(id: String, description: String)
case class RegistrationData(customerInformation: CustomerInformation, contactInformation: ContactInformation, serviceOption: ServiceOption)
case class ProcessStep(name: String, processor: ActorRef)

case class RegistrationProcess(processId: String, processSteps: Seq[ProcessStep], currentStep: Int) {
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
    RegistrationProcess(processId, processSteps, currentStep + 1)
  }
}

case class RegisterCustomer(registrationData: RegistrationData, registrationProcess: RegistrationProcess) {
  def advance():Unit = {
    val advancedProcess = registrationProcess.stepCompleted()
    if (!advancedProcess.isCompleted) {
      advancedProcess.nextStep().processor ! RegisterCustomer(registrationData, advancedProcess)
    }
    RoutingSlipDriver.completedStep()
  }
}

object RoutingSlipDriver extends CompletableApp(4) {
  val processId = java.util.UUID.randomUUID().toString

  val step1 = ProcessStep("create_customer", ServiceRegistry.customerVault(system, processId))
  val step2 = ProcessStep("set_up_contact_info", ServiceRegistry.contactKeeper(system, processId))
  val step3 = ProcessStep("select_service_plan", ServiceRegistry.servicePlanner(system, processId))
  val step4 = ProcessStep("check_credit", ServiceRegistry.creditChecker(system, processId))

  val registrationProcess = new RegistrationProcess(processId, Vector(step1, step2, step3, step4))

  val registrationData =
    RegistrationData(
      CustomerInformation("ABC, Inc.", "123-45-6789"),
      ContactInformation(
        PostalAddress("123 Main Street", "suite 100", "Boulder", "CO", "80301"),
        Telephone("555-103-5532")),
      ServiceOption("99-1203", "A description of 315.")
    )

  val registerCustomer = RegisterCustomer(registrationData, registrationProcess)

  registrationProcess.nextStep().processor ! registerCustomer

  awaitCompletion
  println("RoutingSlip: is completed.")
}

class CreditChecker extends Actor {
  def receive: Receive = {
    case registerCustomer: RegisterCustomer =>
      val federalTaxId = registerCustomer.registrationData.customerInformation.federalTaxId
      println(s"CreditChecker: handling register customer to perform credit check: $federalTaxId")
      registerCustomer.advance()
      context.stop(self)
    case message: Any =>
      println(s"CreditChecker: received unexpected message: $message")
  }
}

class ContactKeeper extends Actor {
  def receive: Receive = {
    case registerCustomer: RegisterCustomer =>
      val contactInfo = registerCustomer.registrationData.contactInformation
      println(s"ContactKeeper: handling register customer to keep contact information: $contactInfo")
      registerCustomer.advance()
      context.stop(self)
    case message: Any =>
      println(s"ContactKeeper: received unexpected message: $message")
  }
}

class CustomerVault extends Actor {
  def receive: Receive = {
    case registerCustomer: RegisterCustomer =>
      val customerInformation = registerCustomer.registrationData.customerInformation
      println(s"CustomerVault: handling register customer to create a new customer: $customerInformation")
      registerCustomer.advance()
      context.stop(self)
    case message: Any =>
      println(s"CustomerVault: received unexpected message: $message")
  }
}

class ServicePlanner extends Actor {
  def receive: Receive = {
    case registerCustomer: RegisterCustomer =>
      val serviceOption = registerCustomer.registrationData.serviceOption
      println(s"ServicePlanner: handling register customer to plan a new customer service: $serviceOption")
      registerCustomer.advance()
      context.stop(self)
    case message: Any =>
      println(s"ServicePlanner: received unexpected message: $message")
  }
}

object ServiceRegistry {
  def contactKeeper(system: ActorSystem, id: String) = {
    system.actorOf(Props[ContactKeeper], "contactKeeper-" + id)
  }

  def creditChecker(system: ActorSystem, id: String) = {
    system.actorOf(Props[CreditChecker], "creditChecker-" + id)
  }

  def customerVault(system: ActorSystem, id: String) = {
    system.actorOf(Props[CustomerVault], "customerVault-" + id)
  }

  def servicePlanner(system: ActorSystem, id: String) = {
    system.actorOf(Props[ServicePlanner], "servicePlanner-" + id)
  }
}
