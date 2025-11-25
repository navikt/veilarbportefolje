package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonKeyedConsumerService
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.DateUtils.fromZonedDateTimeToLocalDateOrNull
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull
import org.jetbrains.annotations.TestOnly
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Håndterer behandling av Kafka-meldinger fra [Topic.PORTEFOLJE_HENDELSESFILTER].
 *
 * Topic-et er et generisk topic som andre team kan produsere generelle oppfølgingshendelser på.
 * Bruksområdet for disse hendelsene er i hovedsak å populere statusfiltre i Oversikten (veilarbportefoljeflatefs).
 * Et eksempel på en hendelse er "Utgått varsel" som i skrivende stund produseres av "veilarbdialog"-applikasjonen. (2024-12-03)
 *
 * Denne klassen håndterer funksjonalitet knyttet til å starte (les: lagre), oppdatere og stoppe (les: slette)
 * hendelser.
 */
@Service
class HendelseService(
    @Autowired private val hendelseRepository: HendelseRepository,
    @Autowired private val pdlIdentRepository: PdlIdentRepository,
    @Autowired private val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt
) : KafkaCommonKeyedConsumerService<HendelseRecordValue>() {
    private val logger: Logger = LoggerFactory.getLogger(HendelseService::class.java)

    /**
     * Behandle en [HendelseRecordValue] og tilhørende `hendelseId`:
     *
     * * dersom brukeren gitt ved `hendelseRecordValue.personID` ikke er under oppfølging vil meldingen ignoreres, med unntak for stopp-meldinger
     * * dersom `hendelseRecordValue.operasjon` = [Operasjon.START] vil hendelsen kombineres med ID-en og lagres
     * * dersom `hendelseRecordValue.operasjon` = [Operasjon.OPPDATER] vil lagret hendelse identifisert med `hendelseId` oppdateres
     * * dersom `hendelseRecordValue.operasjon` = [Operasjon.STOPP] vil lagret hendelse identifisert med `hendelseId` slettes
     */
    @Transactional
    override fun behandleKafkaRecordLogikk(hendelseRecordValue: HendelseRecordValue, hendelseId: String) {
        val operasjon = hendelseRecordValue.operasjon
        val hendelse = toHendelse(hendelseRecordValue, hendelseId)

        val isUnderArbeidsrettetOppfolging = pdlIdentRepository.erBrukerUnderOppfolging(hendelse.personIdent.get())

        if (!isUnderArbeidsrettetOppfolging && operasjon != Operasjon.STOPP) {
            logger.info("Fikk melding/hendelse med hendelse ID $hendelseId for bruker som ikke er under oppfølging. Ignorerer melding.")
            return
        }

        when (operasjon) {
            Operasjon.START -> startHendelse(hendelse)
            Operasjon.OPPDATER -> oppdaterHendelse(hendelse)
            Operasjon.STOPP -> stoppHendelse(hendelse, isUnderArbeidsrettetOppfolging)
        }
    }

    @TestOnly
    fun hentHendelse(id: UUID): Hendelse? {
        return try {
            hendelseRepository.get(id)
        } catch (ex: IngenHendelseMedIdException) {
            null
        }
    }

    private fun startHendelse(hendelse: Hendelse) {
        val resultatAvInsertNyHendelse = try {
            hendelseRepository.insert(hendelse)
        } catch (ex: HendelseIdEksistererAlleredeException) {
            ex
        }

        if (resultatAvInsertNyHendelse is HendelseIdEksistererAlleredeException) {
            logger.info("Hendelse med ID ${hendelse.id} og kategori ${hendelse.kategori} allerede startet. Ignorerer melding.")
            return
        }

        val eldsteHendelseIKategorien = hendelseRepository.getEldste(hendelse.personIdent, hendelse.kategori)

        if (eldsteHendelseIKategorien.id == hendelse.id) {
            oppdaterHendelseForBrukerIOpenSearch(hendelse)
            logger.info("Hendelse med id ${hendelse.id} og kategori ${hendelse.kategori} ble lagret i DB og OpenSearch ble oppdatert med ny eldste utgåtte varsel for person.")
        } else {
            logger.info("Hendelse med id ${hendelse.id} og kategori ${hendelse.kategori} ble lagret i DB")
        }

    }

    private fun oppdaterHendelse(hendelse: Hendelse) {
        val resultatAvUpdateHendelse = try {
            hendelseRepository.update(hendelse)
        } catch (ex: IngenHendelseMedIdException) {
            ex
        }

        if (resultatAvUpdateHendelse is IngenHendelseMedIdException) {
            // 2024-12-02, Sondre:
            // Per no ignorer vi melding, då vi forventar å alltid få ei "START"-melding før ei eventuell "OPPDATER"- eller "STOPP"-melding.
            // Dette går fint så lenge vi ikkje har skrudd på "compaction" på topic-et. Dersom vi har "compaction" på er det ikkje gitt
            // at vi berre kan ignorere, sidan vi då potensielt går glipp av hendelsar ved ein eventuell rewind på topic-et.
            logger.warn("Fikk hendelse med operasjon ${Operasjon.OPPDATER}, ID ${hendelse.id} og kategori ${hendelse.kategori}, men ingen hendelse med denne ID-en finnes. Ignorerer melding.")
            return
        }

        val eldsteHendelseIKategorien = hendelseRepository.getEldste(hendelse.personIdent, hendelse.kategori)

        if (eldsteHendelseIKategorien.id == hendelse.id) {
            oppdaterHendelseForBrukerIOpenSearch(hendelse)
            logger.info("Hendelse med id ${hendelse.id} og kategori ${hendelse.kategori} ble oppdatert i DB og OpenSearch ble oppdatert med ny eldste utgåtte varsel for person.")
        } else {
            logger.info("Hendelse med id ${hendelse.id} og kategori ${hendelse.kategori} ble oppdatert i DB")
        }


    }

    private fun stoppHendelse(hendelse: Hendelse, isUnderArbeidsrettetOppfolging: Boolean = true) {
        val resultatAvDeleteHendelse = try {
            hendelseRepository.delete(hendelse.id)
        } catch (ex: IngenHendelseMedIdException) {
            ex
        }

        if (resultatAvDeleteHendelse is IngenHendelseMedIdException) {
            // 2024-12-02, Sondre:
            // Per no ignorer vi melding, då vi forventar å alltid få ei "START"-melding før ei eventuell "OPPDATER"- eller "STOPP"-melding.
            // Dette går fint så lenge vi ikkje har skrudd på "compaction" på topic-et. Dersom vi har "compaction" på er det ikkje gitt
            // at vi berre kan ignorere, sidan vi då potensielt går glipp av hendelsar ved ein eventuell rewind på topic-et.
            logger.warn("Fikk hendelse med operasjon ${Operasjon.STOPP}, ID ${hendelse.id} og kategori ${hendelse.kategori}, men ingen hendelse med denne ID-en finnes. Ignorerer melding.")
            return
        }
        if (!isUnderArbeidsrettetOppfolging) {
            logger.info("Hendelse med id ${hendelse.id} og kategori ${hendelse.kategori} for innbygger som ikke er i arbeidsrettet oppfølging, ble slettet")
        }

        val resultatAvGetEldsteHendelseIKategorien = try {
            hendelseRepository.getEldste(hendelse.personIdent, hendelse.kategori)
        } catch (ex: IngenHendelseForPersonException) {
            ex
        }

        if (resultatAvGetEldsteHendelseIKategorien is IngenHendelseForPersonException) {
            // All good - det var ingen flere hendelser for personen i kategorien etter at vi slettet den som kom inn som argument
            slettHendelseForBrukerIOpenSearch(hendelse)
            logger.info("Hendelse med id ${hendelse.id} og kategori ${hendelse.kategori} ble slettet i DB og fjernet for person i OpenSearch.")
            return
        }

        if (resultatAvGetEldsteHendelseIKategorien is Hendelse) {
            oppdaterHendelseForBrukerIOpenSearch(resultatAvGetEldsteHendelseIKategorien)
            logger.info("Hendelse med id ${hendelse.id}  og kategori ${hendelse.kategori} ble slettet i DB og OpenSearch ble oppdatert med ny eldste hendelse i kategorien for person, med id ${resultatAvGetEldsteHendelseIKategorien.id}")
        }
    }

    private fun oppdaterHendelseForBrukerIOpenSearch(hendelse: Hendelse) {
        val aktorId = pdlIdentRepository.hentAktorIdForAktivBruker(Fnr.of(hendelse.personIdent.get()))
        opensearchIndexerPaDatafelt.oppdaterHendelse(hendelse, aktorId)
    }

    private fun slettHendelseForBrukerIOpenSearch(hendelse: Hendelse) {
        val aktorId = pdlIdentRepository.hentAktorIdForAktivBruker(Fnr.of(hendelse.personIdent.get()))
        opensearchIndexerPaDatafelt.slettHendelse(hendelse.kategori, aktorId)
    }
}
