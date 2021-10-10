package io.pleo.antaeus.core.services


import io.mockk.*
import io.pleo.antaeus.core.currentTime
import io.pleo.antaeus.core.tasks.MonthlyChargeTask
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.junit.jupiter.api.*
import java.math.BigDecimal
import java.util.*
import kotlin.random.Random

/**
 * Attempt to test the [BillingService]. This test take too long to complete
 * An alternative would to be mock the db and the dal
 * Disabled
 *
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BillingServiceTest {

    lateinit var customers: List<Customer>
    lateinit var invoices: MutableList<Invoice>

    @BeforeAll
    @Disabled
    internal fun setUp() {
        `setup mockk initial data`()
    }

    private fun `setup mockk initial data`() {
        customers = (1..100).mapNotNull {
            mockk<Customer> {
                every { currency } returns Currency.values()[Random.nextInt(0, Currency.values().size)]
                every { timezone } returns TimeZone.getAvailableIDs()[Random.nextInt(0, TimeZone.getAvailableIDs().size)]
                every { id } returns it
            }
        }

        val invoicesList = mutableListOf<List<Invoice>>()
        customers.forEach { customer ->
            val invList = (1..10).mapNotNull {
                mockk<Invoice> {
                    every { id } returns customer.id * 10 + it
                    every { amount } returns mockk {
                        every { value } returns BigDecimal(Random.nextDouble(10.0, 500.0))
                        every { currency } returns customer.currency
                    }
                    every { customerId } returns customer.id
                    every { status } returns if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
                }
            }
            invoicesList.add(invList)
        }

        invoices = invoicesList.flatten().toMutableList()
    }

    private fun copyInvoiceWithStatus(invoice: Invoice, newStatus: InvoiceStatus): Invoice {
        return mockk {
            every { id } returns invoice.id
            every { amount } returns invoice.amount
            every { customerId } returns invoice.customerId
            every { status } returns newStatus
        }
    }

    @Test
    @Disabled
    fun `test billingService charging invoice with infinite retries`() {
        val slotCustomerId = slot<Int>()
        val slotInvoicesForTaskCharge = slot<MutableList<Invoice>>()
        val slotInvoicesForScheduler = slot<MutableList<Invoice>>()
        val slotInvoiceUpdate = slot<Invoice>()
        currentTime = 32399000 // 1970-01-01 09:59:59
        val billingService = spyk(BillingService(paymentProvider = mockk {
            every { charge(any()) } returns Random.nextBoolean() },
            customerService = mockk {},
            invoiceService = mockk {}
        )) {
            every { getCustomer(capture(slotCustomerId)) } answers { customers.find { it.id == slotCustomerId.captured } }
            every { getPendingInvoices() } returns invoices.filter { it.status == InvoiceStatus.PENDING }
            every { monthlyChargeTask(capture(slotInvoicesForTaskCharge)) } answers {
                spyk(MonthlyChargeTask(paymentProvider, invoiceService, slotInvoicesForTaskCharge.captured, true, 0L)) {
                    every { updateInvoice(capture(slotInvoiceUpdate)) } answers {
                        val invo = copyInvoiceWithStatus(slotInvoiceUpdate.captured, InvoiceStatus.PAID)
                        val oldInv = invoices.find { it.id == slotInvoiceUpdate.captured.id }!!
                        invoices[invoices.indexOf(oldInv)] = invo
                        invo
                    }
                }
            }
            every { scheduleMonthlyChargeTask(any(), capture(slotInvoicesForScheduler), any()) } answers {
                monthlyChargeTask(slotInvoicesForTaskCharge.captured).call()
            }
        }

        billingService.retryChargeTaskInfinite = true
        billingService.delayChargeTask = 0L
        billingService.start()
        // and check invoices
        val anyInvoiceInPendingState = invoices.any { it.status == InvoiceStatus.PENDING }
        assert(!anyInvoiceInPendingState)
    }

    @AfterAll
    @Disabled
    fun clean() {
        unmockkAll()
    }

}