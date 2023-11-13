package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model

import java.util.UUID

data class AdjustmentEffectiveDays(
  val id: UUID,
  val effectiveDays: Int,
  val person: String,
)
