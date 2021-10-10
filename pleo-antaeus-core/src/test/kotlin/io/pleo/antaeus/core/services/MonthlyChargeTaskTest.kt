package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.pleo.antaeus.core.tasks.MonthlyChargeTask
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonthlyChargeTaskTest {

    lateinit var invoice: Invoice

    @BeforeEach
    fun setPendingInvoice() {
        // reset invoice
        invoice = createInvoiceWithPendingStatus()
    }

    @Test
    fun `charge invoice with charge returning true`() {
        val monthlyChargeTask = spyMonthlyCharge()

        monthlyChargeTask.call()
        assertEquals(InvoiceStatus.PAID.toString(), invoice.status.toString())
    }

    @Test
    fun `charge invoice with charge returning false then true`() {
        val maxTries = 1
        val monthlyChargeTask = spyMonthlyCharge(maxTries)

        monthlyChargeTask.call()
        Thread.sleep(100) // 100 millis
        assertEquals(InvoiceStatus.PAID.toString(), invoice.status.toString())
    }

    @Test
    fun `charge invoice with charge returning 2 false then true`() {
        val maxTries = 2
        val monthlyChargeTask = spyMonthlyCharge(maxTries)

        monthlyChargeTask.call()
        Thread.sleep(100) // 100 millis
        assertEquals(InvoiceStatus.PAID.toString(), invoice.status.toString())
    }

    @Test
    fun `charge invoice with charge 3 false then true`() {
        val maxTries = 3
        val monthlyChargeTask = spyMonthlyCharge(maxTries)

        monthlyChargeTask.call()
        Thread.sleep(100) // 100 millis
        assertEquals(InvoiceStatus.PENDING.toString(), invoice.status.toString())
    }

    private fun spyMonthlyCharge(
        maxTries: Int = 0
    ): MonthlyChargeTask {
        var tries = 0
        val listInvoice = mutableListOf(invoice)
        return spyk(
            MonthlyChargeTask(
                paymentProvider = mockk {
                    every { charge(any()) } answers {
                        if (tries < maxTries) {
                            tries += 1
                            false
                        } else true
                    }
                },
                invoiceService = mockk {},
                invoices = listInvoice,
                delay = 0
            )
        ) {
            every { updateInvoice(any()) } answers {
                invoice = copyInvoiceWithStatusPaid(invoice)
                invoice
            }
        }
    }

    private fun createInvoiceWithPendingStatus(): Invoice {
        return mockk {
            every { id } returns 1
            every { status } returns InvoiceStatus.PENDING
        }
    }

    private fun copyInvoiceWithStatusPaid(invoice: Invoice): Invoice {
        return mockk {
            every { id } returns invoice.id
            every { status } returns InvoiceStatus.PAID
        }
    }
}