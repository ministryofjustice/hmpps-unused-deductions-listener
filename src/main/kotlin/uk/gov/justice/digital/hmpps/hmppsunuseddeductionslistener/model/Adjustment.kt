package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model

import java.time.LocalDate
import java.util.UUID

data class Adjustment(
  val id: UUID?,
  val bookingId: Long,
  val sentenceSequence: Int?,
  val person: String,
  val adjustmentType: AdjustmentType,
  val toDate: LocalDate?,
  val fromDate: LocalDate?,
  val days: Int?,
  val daysBetween: Int?,
  val effectiveDays: Int? = null,
)
