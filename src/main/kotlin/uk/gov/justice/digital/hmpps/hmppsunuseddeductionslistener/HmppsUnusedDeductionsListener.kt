package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsUnusedDeductionsListener

fun main(args: Array<String>) {
  runApplication<HmppsUnusedDeductionsListener>(*args)
}
