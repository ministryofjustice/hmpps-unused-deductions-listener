package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model

import java.time.LocalDate
import java.util.UUID

data class EditableAdjustmentDto(
  val id: UUID?,
  val bookingId: Long,
  val person: String,
  val adjustmentType: AdjustmentType,
  val toDate: LocalDate?,
  val fromDate: LocalDate?,
  val days: Int?,
  val prisonId: String,
  val sentenceSequence: Int? = null,
)
