package no.nav.pto.veilarbportefolje.util

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import lombok.SneakyThrows
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.domene.value.NavKontor
import no.nav.pto.veilarbportefolje.domene.value.PersonId
import no.nav.pto.veilarbportefolje.domene.value.VeilederId
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1
import org.apache.commons.lang3.RandomUtils
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.time.*
import java.util.*
import java.util.concurrent.ThreadLocalRandom

object TestDataUtils {
    private val random = Random()

    @JvmStatic
    fun randomFnr(): Fnr {
        return Fnr.ofValidFnr("010101" + randomDigits(5))
    }


    @JvmStatic
    fun randomAktorId(): AktorId {
        return AktorId.of(randomDigits(13))
    }

    @JvmStatic
    fun randomPersonId(): PersonId {
        return PersonId.of(ThreadLocalRandom.current().nextInt().toString())
    }

    @JvmStatic
    fun randomVeilederId(): VeilederId {
        val zIdent = "Z" + randomDigits(6)
        return VeilederId.of(zIdent)
    }

    @JvmStatic
    fun randomNavKontor(): NavKontor {
        return NavKontor.of(randomDigits(4))
    }

    private fun randomDigits(length: Int): String {
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(('0'.code + ThreadLocalRandom.current().nextInt(10)).toChar())
        }
        return sb.toString()
    }

    @JvmStatic
    fun tilfeldigDatoTilbakeITid(): ZonedDateTime {
        return ZonedDateTime.now().minus(Duration.ofDays(RandomUtils.nextLong(30, 1000))).withNano(0)
    }

    @JvmStatic
    fun tilfeldigSenereDato(zonedDateTime: ZonedDateTime): ZonedDateTime {
        return zonedDateTime.plus(Duration.ofDays(RandomUtils.nextLong(1, 10)))
    }

    @JvmStatic
    @JvmOverloads
    fun genererStartetOppfolgingsperiode(
        aktorId: AktorId,
        startDato: ZonedDateTime? = tilfeldigDatoTilbakeITid()
    ): SisteOppfolgingsperiodeV1 {
        return SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktorId.get(), startDato, null)
    }

    @JvmStatic
    fun genererAvsluttetOppfolgingsperiode(aktorId: AktorId): SisteOppfolgingsperiodeV1 {
        val periode = genererStartetOppfolgingsperiode(aktorId)
        return SisteOppfolgingsperiodeV1(
            periode.uuid,
            aktorId.get(),
            periode.startDato,
            tilfeldigSenereDato(periode.startDato)
        )
    }

    @JvmStatic
    @JvmOverloads
    fun genererSluttdatoForOppfolgingsperiode(
        periode: SisteOppfolgingsperiodeV1,
        sluttDato: ZonedDateTime? = tilfeldigSenereDato(periode.startDato)
    ): SisteOppfolgingsperiodeV1 {
        return SisteOppfolgingsperiodeV1(
            periode.uuid,
            periode.aktorId,
            periode.startDato,
            sluttDato
        )
    }

    @JvmStatic
    fun randomLocalDate(): LocalDate {
        val yearMonth = YearMonth.of(
            random.nextInt(2000, Year.now().value + 10),
            random.nextInt(Month.JANUARY.value, Month.DECEMBER.value + 1)
        )
        return LocalDate.of(
            yearMonth.year,
            yearMonth.month,
            random.nextInt(1, yearMonth.atEndOfMonth().dayOfMonth + 1)
        )
    }

    fun randomLocalTime(): LocalTime {
        return LocalTime.of(random.nextInt(0, 24), random.nextInt(0, 60))
    }

    @JvmStatic
    fun randomZonedDate(): ZonedDateTime {
        return ZonedDateTime.of(
            randomLocalDate(),
            randomLocalTime(),
            ZoneId.systemDefault()
        )
    }

    @JvmStatic
    fun randomInnsatsgruppe(): Innsatsgruppe {
        return Innsatsgruppe.entries[random.nextInt(Innsatsgruppe.entries.size)]
    }

    @JvmStatic
    fun randomHovedmal(): Hovedmal {
        return Hovedmal.entries[random.nextInt(Hovedmal.entries.size)]
    }

    @JvmStatic
    @SneakyThrows
    fun generateJWT(navIdent: String?): JWT {
        return generateJWT(navIdent, null)
    }

    @JvmStatic
    @SneakyThrows
    fun generateJWT(navIdent: String?, oid: String?): JWT {
        val claimsSet = JWTClaimsSet.Builder()
            .claim("NAVident", navIdent)
            .claim("oid", oid)
            .build()
        val header = JWSHeader.Builder(JWSAlgorithm("RS256"))
            .type(JOSEObjectType.JWT)
            .build()
        val jwt = SignedJWT(header, claimsSet)
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, SecureRandom("mock_key".toByteArray()))
        val keyPair = generator.generateKeyPair()
        jwt.sign(RSASSASigner(keyPair.private))

        return JWTParser.parse(jwt.serialize())
    }
}
