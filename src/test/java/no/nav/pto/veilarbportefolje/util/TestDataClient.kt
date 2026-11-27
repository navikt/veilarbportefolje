package no.nav.pto.veilarbportefolje.util

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.domene.Kjonn
import no.nav.pto.veilarbportefolje.domene.NavKontor
import no.nav.pto.veilarbportefolje.domene.VeilederId
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.tilfeldigDatoTilbakeITid
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class TestDataClient(
    private val jdbcTemplatePostgres: JdbcTemplate,
    private val oppfolgingsbrukerRepository: OppfolgingsbrukerRepositoryV3,
    private val opensearchTestClient: OpensearchTestClient,
    private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    private val pdlIdentRepository: PdlIdentRepository,
    private val pdlPersonRepository: PdlPersonRepository,
) {
    fun settBrukerUnderOppfolging(aktoerId: AktorId) {
        lagreBrukerUnderOppfolging(
            aktoerId,
            randomFnr(),
            randomNavKontor(),
            null,
            ZonedDateTime.now(),
            null,
            oppfolgingsperiodeId = UUID.randomUUID()
        )
    }

    fun lagreBrukerUnderOppfolging(aktoerId: AktorId, startDato: ZonedDateTime) {
        lagreBrukerUnderOppfolging(
            aktoerId,
            randomFnr(),
            randomNavKontor(),
            null,
            startDato,
            null,
            oppfolgingsperiodeId = UUID.randomUUID()
        )
    }

    fun lagreBrukerUnderOppfolging(
        aktoerId: AktorId,
        fnr: Fnr
    ) {
        lagreBrukerUnderOppfolging(
            aktoerId,
            fnr,
            randomNavKontor(),
            randomVeilederId(),
            tilfeldigDatoTilbakeITid(),
            null
        )
    }

    fun slettBrukerUnderOppfolging(aktoerId: AktorId) {
        oppfolgingRepositoryV2.slettOppfolgingData(aktoerId);
    }

    fun lagreBrukerUnderOppfolging(
        aktoerId: AktorId,
        navKontor: NavKontor,
        veilederId: VeilederId,
        startDato: ZonedDateTime, diskresjonKode: String?
    ) {
        val fnr = randomFnr()
        lagreBrukerUnderOppfolging(aktoerId, fnr, navKontor, veilederId, startDato, diskresjonKode)
    }

    fun lagreBrukerUnderOppfolging(aktoerId: AktorId, fnr: Fnr, navKontor: String?, diskresjonKode: String?) {
        val veilederId = randomVeilederId()
        lagreBrukerUnderOppfolging(
            aktoerId,
            fnr,
            NavKontor.of(navKontor),
            veilederId,
            ZonedDateTime.now(),
            diskresjonKode
        )
    }

    fun lagreBrukerUnderOppfolging(aktoerId: AktorId, fnr: Fnr, navKontor: NavKontor, veilederId: VeilederId) {
        lagreBrukerUnderOppfolging(aktoerId, fnr, navKontor, veilederId, ZonedDateTime.now(), null)
    }

    fun lagreBrukerUnderOppfolging(aktoerId: AktorId, fnr: Fnr, startDato: ZonedDateTime, oppfolgingsperiodeId: UUID) {
        lagreBrukerUnderOppfolging(
            aktoerId,
            fnr,
            randomNavKontor(),
            randomVeilederId(),
            startDato,
            null,
            oppfolgingsperiodeId
        )
    }

    private fun lagreBrukerUnderOppfolging(
        aktoerId: AktorId,
        fnr: Fnr?,
        navKontor: NavKontor?,
        veilederId: VeilederId?,
        startDato: ZonedDateTime,
        diskresjonKode: String?,
        oppfolgingsperiodeId: UUID = UUID.randomUUID(),
    ) {
        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(aktoerId.get(), false, Gruppe.AKTORID),
                PDLIdent(fnr?.get(), false, Gruppe.FOLKEREGISTERIDENT)
            )
        )
        pdlPersonRepository.upsertPerson(
            fnr,
            PDLPerson().setFoedsel(LocalDate.now()).setKjonn(Kjonn.K).setDiskresjonskode(diskresjonKode)
        )
        oppfolgingRepositoryV2.settUnderOppfolging(aktoerId, startDato, oppfolgingsperiodeId)
        if (veilederId != null) oppfolgingRepositoryV2.settVeileder(aktoerId, veilederId)
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(
            OppfolgingsbrukerEntity(
                fnr?.get(), null, null,
                "9999", null, null, null,
                ZonedDateTime.now()
            ),
            navKontor,
            aktoerId
        )
        opensearchTestClient.createUserInOpensearch(aktoerId)
    }

}
