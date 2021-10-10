package io.pleo.antaeus.core.tasks

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import java.util.*
import java.util.concurrent.Callable

class MonthlyChargeTask(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val invoices: List<Invoice>,
    private var infiniteRetries: Boolean = false,
    private val delay: Long = 1000

): Callable<Boolean> {

    private fun delay(invoice: Invoice, numberOfTries: Int, delay: Long = 1000) {
        Timer().schedule(DelayTimerTask {
            chargeInvoiceRec(invoice, numberOfTries = numberOfTries + 1)
        }, delay) // delay in millis
    }

    private fun chargeInvoiceRec(invoice: Invoice, numberOfTries: Int = 0) {
        if (!paymentProvider.charge(invoice)) {
            // Oops! :(
            // try again with a bit of delay
            if (numberOfTries < 2 || infiniteRetries) delay(invoice, numberOfTries, delay)
        } else {
            // Hurray! :)
            // Change the status to PAID
            val inv = updateInvoice(invoice)
            println("updated invoice = ${inv.status}")
        }

    }

    internal fun updateInvoice(invoice: Invoice): Invoice {
       return invoiceService.updateInvoice(invoice.id, InvoiceStatus.PAID)
    }

    override fun call(): Boolean {
        invoices.forEach {
            chargeInvoiceRec(it)
        }
        return true
    }
}