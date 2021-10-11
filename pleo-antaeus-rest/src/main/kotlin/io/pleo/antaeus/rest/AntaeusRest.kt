/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.Context
import io.pleo.antaeus.core.currentTime
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.formatDateTo
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import mu.KotlinLogging
import java.lang.Runnable
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.coroutines.EmptyCoroutineContext

private val logger = KotlinLogging.logger {}
private val thisFile: () -> Unit = {}

class AntaeusRest(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val billingService: BillingService
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
            get("/") {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            }
            path("rest") {
                // Route to check whether the app is running
                // URL: /rest/health
                get("health") {
                    it.json("ok")
                }

                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        get {
                            it.json(invoiceService.fetchAll())
                        }

                        // URL: /rest/v1/invoices/{:id}
                        get(":id") {
                            it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                        }
                    }

                    path("time") {
                        // URL: /rest/v1/time
                        get {
                            val formatter = DateTimeFormatter
                                .ofLocalizedDate(FormatStyle.LONG)
                                .withZone(TimeZone.getDefault().toZoneId())
                            it.json(formatter.format(Instant.now()))
                        }

                        // URL: /rest/v1/time/timezone
                        get("timezone") {
                            // Returns the default timezone
                            it.json(TimeZone.getDefault().toString())
                        }


                        // For testing purposes set the time in the future
                        // URL: /rest/v1/time/{timeInMillis}
                        post(":timeInMillis") {
                            startBillingServiceForTest(it)
                        }

                        // For testing purposes set the time in the future with infinite retries
                        // URL: /rest/v1/time/infiniteRetries/{timeInMillis}
                        path("infiniteRetries") {
                            post(":timeInMillis") {
                                startBillingServiceForTest(it, infiniteRetries = true)
                            }
                        }
                    }

                    path("customers") {
                        // URL: /rest/v1/customers
                        get {
                            it.json(customerService.fetchAll())
                        }

                        // URL: /rest/v1/customers/{:id}
                        get(":id") {
                            it.json(customerService.fetch(it.pathParam("id").toInt()))
                        }
                    }
                }
            }
        }
    }

    private fun startBillingServiceForTest(it: Context, infiniteRetries: Boolean = false) {
        val time = it.pathParam("timeInMillis").toLong()
        currentTime = time
        billingService.mockStart(infiniteRetries)
        if (infiniteRetries) it.json("Time was set successfully with infinite retries :)")
        else it.json("Time was set successfully :)")
    }

}
