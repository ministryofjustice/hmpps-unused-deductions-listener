package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.integration

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic

abstract class SqsIntegrationTestBase : IntegrationTestBase() {

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  internal val topic by lazy { hmppsQueueService.findByTopicId("domainevents") as HmppsTopic }

  internal val unusedDeductionsQueue by lazy { hmppsQueueService.findByQueueId("unuseddeductions") as HmppsQueue }

  internal val awsSqsUnusedDeductionsClient by lazy { unusedDeductionsQueue.sqsClient }
  internal val awsSqsUnusedDeductionsDlqClient by lazy { unusedDeductionsQueue.sqsDlqClient }
  internal val unusedDeductionsQueueUrl by lazy { unusedDeductionsQueue.queueUrl }
  internal val unusedDeductionsDlqUrl by lazy { unusedDeductionsQueue.dlqUrl }

  internal val awsSnsClient by lazy { topic.snsClient }
  internal val topicArn by lazy { topic.arn }

  @BeforeEach
  fun cleanQueue() {
    awsSqsUnusedDeductionsClient.purgeQueue(unusedDeductionsQueueUrl).get()
    awsSqsUnusedDeductionsDlqClient?.purgeQueue(unusedDeductionsDlqUrl)?.get()
  }
}

private fun SqsAsyncClient.purgeQueue(queueUrl: String?) = purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl!!).build())
