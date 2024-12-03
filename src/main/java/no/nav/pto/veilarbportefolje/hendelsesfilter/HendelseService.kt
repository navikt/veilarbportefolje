package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonKeyedConsumerService
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import org.jetbrains.annotations.TestOnly
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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
    @Autowired private val opensearchIndexerV2: OpensearchIndexerV2
) : KafkaCommonKeyedConsumerService<HendelseRecordValue>() {
    private val logger: Logger = LoggerFactory.getLogger(HendelseService::class.java)

    /**
     * Behandle en [HendelseRecordValue] og tilhørende `hendelseId`:
     *
     * * dersom brukeren gitt ved `hendelseRecordValue.personID` ikke er under oppfølging vil meldingen ignoreres
     * * dersom `hendelseRecordValue.operasjon` = [Operasjon.START] vil hendelsen kombineres med ID-en og lagres
     * * dersom `hendelseRecordValue.operasjon` = [Operasjon.OPPDATER] vil lagret hendelse identifisert med `hendelseId` oppdateres
     * * dersom `hendelseRecordValue.operasjon` = [Operasjon.STOPP] vil lagret hendelse identifisert med `hendelseId` slettes
     */
    override fun behandleKafkaRecordLogikk(hendelseRecordValue: HendelseRecordValue, hendelseId: String) {
        val operasjon = hendelseRecordValue.operasjon
        val hendelse = toHendelse(hendelseRecordValue, hendelseId)

        val isUnderArbeidsrettetOppfolging = pdlIdentRepository.erBrukerUnderOppfolging(hendelse.personIdent.get())

        if (!isUnderArbeidsrettetOppfolging) {
            logger.info("Fikk melding/hendelse med hendelse ID $hendelseId for bruker som ikke er under oppfølging. Ignorerer melding.")
            return
        }

        when (operasjon) {
            Operasjon.START -> startHendelse(hendelse)
            Operasjon.OPPDATER -> oppdaterHendelse(hendelse)
            Operasjon.STOPP -> stoppHendelse(hendelse)
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
        try {
            hendelseRepository.insert(hendelse)

            val eldsteHendelse = hendelseRepository.getEldste(hendelse.personIdent)
            if (eldsteHendelse.id == hendelse.id) {
                oppdaterUgattVarselForBrukerIOpenSearch(hendelse)
            }

            logger.info("Hendelse med id ${hendelse.id} ble startet")
        } catch (ex: HendelseIdEksistererAlleredeException) {
            logger.info("Hendelse med ID ${hendelse.id} allerede startet. Ignorerer melding.")
        } catch (ex: IngenHendelseForPersonException) {
            // Not good - vi opprettet en hendelse men klarte ikke hente den igjen
            // Kan ha vært en race-condition (eks. en annen pod slettet hendelsen vi nettopp opprettet 🤷‍♂️)
        }
    }

    private fun oppdaterHendelse(hendelse: Hendelse) {
        try {
            hendelseRepository.update(hendelse)

            val eldsteHendelse = hendelseRepository.getEldste(hendelse.personIdent)
            if (eldsteHendelse.id == hendelse.id) {
                oppdaterUgattVarselForBrukerIOpenSearch(hendelse)
            }

            logger.info("Hendelse med id ${hendelse.id} ble oppdatert")
        } catch (ex: IngenHendelseMedIdException) {
            // 2024-12-02, Sondre:
            // Per no ignorer vi melding, då vi forventar å alltid få ei "START"-melding før ei eventuell "OPPDATER"- eller "STOPP"-melding.
            // Dette går fint så lenge vi ikkje har skrudd på "compaction" på topic-et. Dersom vi har "compaction" på er det ikkje gitt
            // at vi berre kan ignorere, sidan vi då potensielt går glipp av hendelsar ved ein eventuell rewind på topic-et.
            logger.warn("Fikk hendelse med operasjon ${Operasjon.OPPDATER} og ID ${hendelse.id}, men ingen hendelse med denne ID-en finnes. Ignorerer melding.")
        } catch (ex: IngenHendelseForPersonException) {
            // Not good - vi oppdaterte en persistert hendelse men klarte ikke hente den igjen
            // Kan ha vært en race-condition (dvs. en annen pod slettet hendelsen vi nettopp oppdaterte 🤷‍♂️)
        }
    }

    private fun stoppHendelse(hendelse: Hendelse) {
        try {
            hendelseRepository.delete(hendelse.id)

            val eldsteHendelse = hendelseRepository.getEldste(hendelse.personIdent)
            oppdaterUgattVarselForBrukerIOpenSearch(eldsteHendelse)

            logger.info("Hendelse med id ${hendelse.id} ble stoppet")
        } catch (ex: IngenHendelseMedIdException) {
            // 2024-12-02, Sondre:
            // Per no ignorer vi melding, då vi forventar å alltid få ei "START"-melding før ei eventuell "OPPDATER"- eller "STOPP"-melding.
            // Dette går fint så lenge vi ikkje har skrudd på "compaction" på topic-et. Dersom vi har "compaction" på er det ikkje gitt
            // at vi berre kan ignorere, sidan vi då potensielt går glipp av hendelsar ved ein eventuell rewind på topic-et.
            logger.warn("Fikk hendelse med operasjon ${Operasjon.STOPP} og ID ${hendelse.id}, men ingen hendelse med denne ID-en finnes. Ignorerer melding.")
        } catch (ex: IngenHendelseForPersonException) {
            // All good - det var ingen flere hendelser for personen etter at vi slettet den som kom inn som argument
            slettUgattVarselForBrukerIOpenSearch(hendelse)
        }
    }

    private fun oppdaterUgattVarselForBrukerIOpenSearch(hendelse: Hendelse) {
        // 2024-11-29, Sondre
        // Egentlig unødvendig if-sjekk så lenge kun Team DAB er på med "utgåtte varsel"
        // Men har den med likevel for å tydeliggjøre at det er "utgått varsel"-feltet i OpenSearch
        // som oppdateres her. Vi må huske å oppdatere håndtering etterhvert som denne tjenesten
        // blir mer generalisert/får flere produsenter
        if (Kategori.UTGATT_VARSEL == hendelse.kategori) {
            // TODO: 2024-11-29, Sondre - Her konverterer vi bare ukritisk til Fnr, selv om NorskIdent også kan være f.eks. D-nummer
            val aktorId = pdlIdentRepository.hentAktorIdForAktivBruker(Fnr.of(hendelse.personIdent.get()))
            opensearchIndexerV2.oppdaterUtgattVarsel(hendelse, aktorId)
        }
    }

    private fun slettUgattVarselForBrukerIOpenSearch(hendelse: Hendelse) {
        // 2024-11-29, Sondre
        // Egentlig unødvendig if-sjekk så lenge kun Team DAB er på med "utgåtte varsel"
        // Men har den med likevel for å tydeliggjøre at det er "utgått varsel"-feltet i OpenSearch
        // som oppdateres her. Vi må huske å oppdatere håndtering etterhvert som denne tjenesten
        // blir mer generalisert/får flere produsenter
        if (Kategori.UTGATT_VARSEL == hendelse.kategori) {
            // TODO: 2024-11-29, Sondre - Her konverterer vi bare ukritisk til Fnr, selv om NorskIdent også kan være f.eks. D-nummer
            val aktorId = pdlIdentRepository.hentAktorIdForAktivBruker(Fnr.of(hendelse.personIdent.get()))
            opensearchIndexerV2.slettUtgattVarsel(aktorId)
        }
    }
}