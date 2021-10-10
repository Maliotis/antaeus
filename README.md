## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

## Thought Process

### Initial Setup
27th of September
* Familiarised myself with the codebase
* Set branch protection rules for branch `master`
* Created a project board in GitHub
  
### Analyzing requirements
The invoice needs to be charged on the first of the month. So we can assume that the invoice can be charged any time of the 
day during (from 00:00 to 23:59). In order to do that we will need the timezone for each customer is located, as that
doesn't exist at the moment we could the `timezone` attribute to the Customer entity.

We will need to execute a task that will charge each invoice at the correct time. In order to do that we will need to 
calculate the time until the first of the month minus the offset (including DST). This calculation is simple enough,<br>
[1] `(dateUntilFirstOfTheMonth - dateNow - offset)` an example of it would be:

Assume format of `yyyy-MM-dd HH:mm:ss` for illustrative purposes<br/>
`dateNow` = 2020-03-10 10:00:00 <br/>
`dateUntilFirstOfTheMonth` = 2020-04-01 00:00:00 <br/>
`offset` = 0 hours 

The execution will be in 21 days and 14 hours. <br/> 
Note: an offset of UTC+2 would make the execution to happen 2 hours earlier i.e. 21 days and 12 hours and vice 
versa for UTC-2

An interesting scenario is when `dateNow` is the first of the month. In that case we need to check on the hour.
At the extremes we have UTC+14 and UTC-12. To keep things simple we will execute all the tasks within 24 hours and not 
in separate months. So for example if the `dateNow` is 2020-04-01 18:00:00 tasks with `offset` > UTC+6 will need to be
executed next month (as it will officially be 2nd of the month). We will cap the hour to be less than 10 if it happens
to be the first of the month. Also, one more thing is to set the `dateUntilFirstOfTheMonth` to 0 hours, 0 min and 0 sec,
if the `dateNow.hour` > 10. That is to avoid unnecessary delays when charging an invoice, an example of the delay would be: <br/>
`dateNow` = 2020-04-01 04:00:00 <br/>
`dateUntilFirstOfTheMonth` = 2020-04-01 04:00:00<br/>
It's easy to see that any `offset` <= UTC-1 will be delayed using the above formula [1], an easy way to solve that is 
to set the hours, min and sec to 0.

We could also add `numberOfRetries` `dateCreated` and `datePaid` fields to Invoice entity

### Implementation decisions

`BillingService` will implement the logic for charging each invoice at the correct time and date.
We could organize the invoices to be charged based on the timezone of the customer/s.
A Map `timezoneToInvoices` will serve this purpose well. <br/>
I.e. `UTC+1 -> [Invoice1, Invoice2]` `UTC-4 -> [Invoice3, Invoice4]` <br/>

That way we can schedule a task to be executed with te formula at [Analyzing Requirements](#analyzing-requirements) [1] section.
Now that invoices are grouped based on timezone, we can pass the list of invoices to a background task and execute with a certain delay
or at a specific time. There are 24 different timezones, so we will have a maximum of 24 threads scheduled and is guaranteed that they will
never at the same time. 

The above is more performant than executing/scheduling each invoice separately in each own thread.

We will also need a delay when retrying a failed invoice charge. For the purpose of the exercise we will add 1 sec delay,
but in a more real world scenario it should be more and allow the customer some time to sort it out.
We will also limit the time of retries to 2.

We will use the [ExecutorService](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html) api to run tasks asynchronously as it provides and easy to use interface
for providing a pool of thread and assigning tasks to the different thread/s.

An alternative would be to use [Kotlin-Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) to handle asynchronous tasks

### Testing

I decided to test mostly the business logic. There was an attempt to test the BillingService end-to-end
but decided not to proceed with it in the end as it seemed unnecessary.




