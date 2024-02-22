package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Adjustment(
  val id: UUID?,
  val bookingId: Long,
  val sentenceSequence: Int?,
  val person: String,
  val adjustmentType: AdjustmentType,
  val toDate: LocalDate?,
  val fromDate: LocalDate?,
  val createdDate: LocalDateTime = LocalDateTime.now(),
  val days: Int?,
  val effectiveDays: Int? = null,
  val taggedBail: TaggedBailDto? = null,
  val remand: RemandDto? = null,
)
