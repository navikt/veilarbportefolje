package no.nav.pto.veilarbportefolje.kafka

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.common.auth.context.AuthContextHolder
import no.nav.pto.veilarbportefolje.admin.KafkaAdminController
import no.nav.pto.veilarbportefolje.admin.dto.KafkaPartitionOffsetDto
import no.nav.pto.veilarbportefolje.admin.dto.KafkaResetOffsetRequest
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AlterConsumerGroupOffsetsResult
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.KafkaFuture
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.GroupNotEmptyException
import org.apache.kafka.common.internals.KafkaFutureImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

class KafkaAdminServiceTest {

    private lateinit var adminClient: AdminClient
    private lateinit var kafkaAdminService: KafkaAdminService

    private val periodeTopic = KafkaConfigCommon.Topic.ARBEIDSSOKERPERIODER_TOPIC.topicName
    private val annetTopic = KafkaConfigCommon.Topic.DIALOG_CONSUMER_TOPIC.topicName

    @BeforeEach
    fun setUp() {
        adminClient = mock(AdminClient::class.java)
        kafkaAdminService = KafkaAdminService { adminClient }
    }

    @Test
    fun `hentKonsumerteTopics skal returnere alle topics fra KafkaConfigCommon`() {
        val topics = kafkaAdminService.hentKonsumerteTopics()

        assertThat(topics)
            .containsExactlyElementsOf(KafkaConfigCommon.Topic.entries.map { it.topicName })
            .contains(periodeTopic, annetTopic)
    }

    @Test
    fun `Spring skal kunne opprette KafkaAdminService via parameterlos konstruktor`() {
        AnnotationConfigApplicationContext(KafkaAdminService::class.java).use { context ->
            assertThat(context.getBean(KafkaAdminService::class.java)).isNotNull
        }
    }

    @Test
    fun `hentCommitedeOffsets skal hente offsets pa tvers av consumer groups og filtrere pa topic`() {
        mockGroupOffsets(
            KafkaConfigCommon.CLIENT_ID_CONFIG,
            mapOf(
                TopicPartition(periodeTopic, 0) to OffsetAndMetadata(100L),
                TopicPartition(annetTopic, 0) to OffsetAndMetadata(999L)
            )
        )
        mockGroupOffsets(
            KafkaConfigCommon.CV_CLIENT_ID_CONFIG,
            emptyMap()
        )
        mockGroupOffsets(
            KafkaConfigCommon.ARBEIDSSOEKER_V3_CLIENT_ID_CONFIG,
            mapOf(
                TopicPartition(periodeTopic, 0) to OffsetAndMetadata(25L),
                TopicPartition(periodeTopic, 1) to OffsetAndMetadata(40L)
            )
        )

        val resultat = kafkaAdminService.hentCommitedeOffsets(periodeTopic)

        assertThat(resultat).containsExactly(
            KafkaPartitionOffsetDto(KafkaConfigCommon.CLIENT_ID_CONFIG, periodeTopic, 0, 100L),
            KafkaPartitionOffsetDto(KafkaConfigCommon.ARBEIDSSOEKER_V3_CLIENT_ID_CONFIG, periodeTopic, 0, 25L),
            KafkaPartitionOffsetDto(KafkaConfigCommon.ARBEIDSSOEKER_V3_CLIENT_ID_CONFIG, periodeTopic, 1, 40L)
        )
        verify(adminClient).close()
    }

    @Test
    fun `hentCommitedeOffsets skal kaste 400 Bad Request ved ukjent topic`() {
        assertThatThrownBy { kafkaAdminService.hentCommitedeOffsets("ukjent.topic-v1") }
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting { (it as ResponseStatusException).statusCode }
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `resetOffsets skal oppdatere offsets for gyldig consumer group og topic`() {
        val group = KafkaConfigCommon.ARBEIDSSOEKER_V3_CLIENT_ID_CONFIG
        mockGroupOffsets(
            group,
            mapOf(TopicPartition(periodeTopic, 0) to OffsetAndMetadata(50L))
        )
        val forventetNyeOffsets = mapOf(
            TopicPartition(periodeTopic, 0) to OffsetAndMetadata(10L),
            TopicPartition(periodeTopic, 1) to OffsetAndMetadata(0L)
        )
        val alterResult = mock(AlterConsumerGroupOffsetsResult::class.java)
        `when`(alterResult.all()).thenReturn(KafkaFuture.completedFuture(null))
        `when`(adminClient.alterConsumerGroupOffsets(group, forventetNyeOffsets)).thenReturn(alterResult)

        kafkaAdminService.resetOffsets(
            KafkaResetOffsetRequest(
                consumerGroup = group,
                topic = periodeTopic,
                partitionOffsets = mapOf(0 to 10L, 1 to 0L)
            ),
            utfortAv = "Z999999"
        )

        verify(adminClient).alterConsumerGroupOffsets(group, forventetNyeOffsets)
        verify(adminClient).close()
    }

    @Test
    fun `resetOffsets skal kaste 400 Bad Request ved ukjent topic, ukjent consumer group eller negativt offset`() {
        val gyldigGroup = KafkaConfigCommon.ARBEIDSSOEKER_V3_CLIENT_ID_CONFIG

        assertThatThrownBy {
            kafkaAdminService.resetOffsets(KafkaResetOffsetRequest(gyldigGroup, "ukjent.topic", mapOf(0 to 0L)))
        }.isInstanceOf(ResponseStatusException::class.java)
            .extracting { (it as ResponseStatusException).statusCode }
            .isEqualTo(HttpStatus.BAD_REQUEST)

        assertThatThrownBy {
            kafkaAdminService.resetOffsets(KafkaResetOffsetRequest("ukjent-group", periodeTopic, mapOf(0 to 0L)))
        }.isInstanceOf(ResponseStatusException::class.java)
            .extracting { (it as ResponseStatusException).statusCode }
            .isEqualTo(HttpStatus.BAD_REQUEST)

        assertThatThrownBy {
            kafkaAdminService.resetOffsets(KafkaResetOffsetRequest(gyldigGroup, periodeTopic, emptyMap()))
        }.isInstanceOf(ResponseStatusException::class.java)
            .extracting { (it as ResponseStatusException).statusCode }
            .isEqualTo(HttpStatus.BAD_REQUEST)

        assertThatThrownBy {
            kafkaAdminService.resetOffsets(KafkaResetOffsetRequest(gyldigGroup, periodeTopic, mapOf(0 to -1L)))
        }.isInstanceOf(ResponseStatusException::class.java)
            .extracting { (it as ResponseStatusException).statusCode }
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `resetOffsets skal kaste 409 Conflict nar konsumentgruppen har aktive medlemmer`() {
        val group = KafkaConfigCommon.ARBEIDSSOEKER_V3_CLIENT_ID_CONFIG
        mockGroupOffsets(group, emptyMap())

        val feilendeFuture = KafkaFutureImpl<Void>()
        feilendeFuture.completeExceptionally(GroupNotEmptyException("Group is not empty"))
        val alterResult = mock(AlterConsumerGroupOffsetsResult::class.java)
        `when`(alterResult.all()).thenReturn(feilendeFuture)
        `when`(
            adminClient.alterConsumerGroupOffsets(
                group,
                mapOf(TopicPartition(periodeTopic, 0) to OffsetAndMetadata(0L))
            )
        ).thenReturn(alterResult)

        assertThatThrownBy {
            kafkaAdminService.resetOffsets(
                KafkaResetOffsetRequest(
                    consumerGroup = group,
                    topic = periodeTopic,
                    partitionOffsets = mapOf(0 to 0L)
                )
            )
        }.isInstanceOf(ResponseStatusException::class.java)
            .satisfies({ ex ->
                val rse = ex as ResponseStatusException
                assertThat(rse.statusCode).isEqualTo(HttpStatus.CONFLICT)
                assertThat(rse.reason).contains("aktive konsumenter")
            })
    }

    @Test
    fun `sjekkTilgangTilKafkaAdmin skal tillate innlogget intern bruker med Team Obo AD-gruppe`() {
        val authContextHolder = mock(AuthContextHolder::class.java)
        `when`(authContextHolder.erInternBruker()).thenReturn(true)
        `when`(authContextHolder.idTokenClaims).thenReturn(
            Optional.of(
                JWTClaimsSet.Builder()
                    .claim("groups", listOf(KafkaAdminController.TEAM_OBO_PROD_GROUP_ID))
                    .claim("azp_name", "prod-gcp:obo:annen-app")
                    .build()
            )
        )
        `when`(authContextHolder.navIdent).thenReturn(Optional.empty())

        val controller = KafkaAdminController(authContextHolder, kafkaAdminService)

        controller.sjekkTilgangTilKafkaAdmin()
    }

    @Test
    fun `sjekkTilgangTilKafkaAdmin skal tillate kall fra poao-admin for intern bruker`() {
        val authContextHolder = mock(AuthContextHolder::class.java)
        `when`(authContextHolder.erInternBruker()).thenReturn(true)
        `when`(authContextHolder.idTokenClaims).thenReturn(
            Optional.of(
                JWTClaimsSet.Builder()
                    .claim("azp_name", "dev-gcp:poao:poao-admin")
                    .build()
            )
        )
        `when`(authContextHolder.navIdent).thenReturn(Optional.empty())

        val controller = KafkaAdminController(authContextHolder, kafkaAdminService)

        controller.sjekkTilgangTilKafkaAdmin()
    }

    @Test
    fun `sjekkTilgangTilKafkaAdmin skal avvise bruker uten Team Obo AD-gruppe og uten poao-admin`() {
        val authContextHolder = mock(AuthContextHolder::class.java)
        `when`(authContextHolder.erInternBruker()).thenReturn(true)
        `when`(authContextHolder.idTokenClaims).thenReturn(
            Optional.of(
                JWTClaimsSet.Builder()
                    .claim("groups", listOf("00000000-0000-0000-0000-000000000000"))
                    .claim("azp_name", "prod-gcp:obo:veilarbportefoljeflatefs")
                    .build()
            )
        )
        `when`(authContextHolder.navIdent).thenReturn(Optional.empty())

        val controller = KafkaAdminController(authContextHolder, kafkaAdminService)

        assertThatThrownBy { controller.sjekkTilgangTilKafkaAdmin() }
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting { (it as ResponseStatusException).statusCode }
            .isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `sjekkTilgangTilKafkaAdmin skal avvise azure-token-generator nar NAIS_CLUSTER_NAME ikke er dev-gcp`() {
        val authContextHolder = mock(AuthContextHolder::class.java)
        `when`(authContextHolder.erInternBruker()).thenReturn(true)
        `when`(authContextHolder.idTokenClaims).thenReturn(
            Optional.of(
                JWTClaimsSet.Builder()
                    .claim("azp_name", KafkaAdminController.AZURE_TOKEN_GENERATOR_DEV)
                    .build()
            )
        )
        `when`(authContextHolder.navIdent).thenReturn(Optional.empty())

        val controller = KafkaAdminController(authContextHolder, kafkaAdminService)

        assertThatThrownBy { controller.sjekkTilgangTilKafkaAdmin() }
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting { (it as ResponseStatusException).statusCode }
            .isEqualTo(HttpStatus.FORBIDDEN)
    }

    private fun mockGroupOffsets(group: String, offsets: Map<TopicPartition, OffsetAndMetadata>) {
        val result = mock(ListConsumerGroupOffsetsResult::class.java)
        `when`(result.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(offsets))
        `when`(adminClient.listConsumerGroupOffsets(group)).thenReturn(result)
    }
}
