package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.currentTime
import io.pleo.antaeus.core.tasks.MonthlyChargeTask
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.getExecutionTime
import io.pleo.antaeus.core.setCalendarToFirstDayOfTheMonth
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import java.util.*
import java.util.concurrent.*

class BillingService(
    val paymentProvider: PaymentProvider,
    val customerService: CustomerService,
    val invoiceService: InvoiceService
) {
    private val customerIdToCustomer = mutableMapOf<Int, Customer>()
    lateinit var executor: ScheduledExecutorService
    // Change to true for testing purposes
    internal var retryChargeTaskInfinite = false
    internal var delayChargeTask = 1000L

    init {
        // set timezone to be GMT
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    }

    /**
     * Returns the customer based on the customer id.
     * The customer and customer.id get saved into a map so that when we encounter the same customer again
     * we are not going to do a fetch from the db, rather retrieve it from the MutableMap [customerIdToCustomer]
     * This sacrifices memory for the sake of speed, as DB operations as slower than memory lookups.
     *
     * Gets used quite often in [assignTimezoneToInvoices]
     * @see [Customer]
     */
    internal fun getCustomer(id: Int): Customer? {
        // If we encounter the same customer don't fetch again
        return if (customerIdToCustomer.containsKey(id)) customerIdToCustomer[id]
        else {
            val customer = customerService.fetch(id)
            customerIdToCustomer[id] = customer
            customer
        }
    }

    internal fun getPendingInvoices(): List<Invoice> {
        return invoiceService.fetchPendingInvoices()
    }

    fun start() {
        val nowDate = Date(currentTime)
        val firstDayOfMonth = setCalendarToFirstDayOfTheMonth(date = nowDate)
        val pendingInvoices = getPendingInvoices()
        val timezoneToInvoices = assignTimezoneToInvoices(pendingInvoices)
        executor = scheduleMonthlyCharges(timezoneToInvoices, nowDate, firstDayOfMonth)
    }

    internal fun scheduleMonthlyCharges(
        timezoneToInvoices: MutableMap<TimeZone, MutableList<Invoice>>,
        nowDate: Date,
        firstDayOfMonth: Calendar
    ): ScheduledExecutorService {
        val threadPoolSize = timezoneToInvoices.keys.size
        val executor = Executors.newScheduledThreadPool(threadPoolSize)

        timezoneToInvoices.forEach { (timeZone, invoices) ->
            // e.g
            // now is - 2020/01/05 10:00
            // cal is - 2020/02/01 00:00
            // calculate time from now to cal
            // if offset is 2 hours i.e UTC+2
            // execute on time = now to cal - 2
            // if offset is -3 hours
            // execute on time = now to cal - (-3)
            val executionTime = getExecutionTime(timeZone, nowDate, firstDayOfMonth)
            // executing a batch of invoices that have the same timezone
            scheduleMonthlyChargeTask(executor, invoices, executionTime)

        }
        return executor
    }

    internal fun scheduleMonthlyChargeTask(
        executor: ScheduledExecutorService,
        invoices: MutableList<Invoice>,
        executionTime: Long
    ) {
        executor.schedule(
            monthlyChargeTask(invoices),
            executionTime,
            TimeUnit.MILLISECONDS
        )
    }

    internal fun monthlyChargeTask(invoices: MutableList<Invoice>) =
        MonthlyChargeTask(paymentProvider, invoiceService, invoices, retryChargeTaskInfinite, delayChargeTask)



    private fun assignTimezoneToInvoices(pendingInvoices: List<Invoice>): MutableMap<TimeZone, MutableList<Invoice>> {
        val timezoneToInvoices = mutableMapOf<TimeZone, MutableList<Invoice>>()
        pendingInvoices.forEach {
            val customer = getCustomer(it.customerId)
            val timezone = TimeZone.getTimeZone(customer?.timezone)
            var invoiceList = timezoneToInvoices[timezone]
            if (invoiceList == null) invoiceList = mutableListOf()
            invoiceList.add(it)
            timezoneToInvoices[timezone] = invoiceList
        }

        return timezoneToInvoices
    }

    /**
     * To be used for testing. It will stop all other tasks that are scheduled to happen in the future
     */
    fun mockStart(infiniteRetries: Boolean) {
        clearAndShutdown()
        if (infiniteRetries) {
            retryChargeTaskInfinite = true
            delayChargeTask = 0L
        }
        start()
    }

    private fun clearAndShutdown() {
        customerIdToCustomer.clear()
        executor.shutdown()
    }
}
