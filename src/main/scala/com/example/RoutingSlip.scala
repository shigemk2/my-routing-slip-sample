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
object RoutingSlipDriver extends CompletableApp(4) {
}
