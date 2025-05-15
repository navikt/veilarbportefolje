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
    @Autowired private val opensearchIndexerV2: OpensearchIndexerV2
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

        when (hendelse.kategori) {
            Kategori.UTGATT_VARSEL -> {
                val eldsteUtgattVarselHendelse =
                    hendelseRepository.getEldste(hendelse.personIdent, Kategori.UTGATT_VARSEL)

                if (eldsteUtgattVarselHendelse.id == hendelse.id) {
                    oppdaterUtgattVarselForBrukerIOpenSearch(hendelse)

                    logger.info("Hendelse med id ${hendelse.id} og kategori ${Kategori.UTGATT_VARSEL} ble lagret i DB og OpenSearch ble oppdatert med ny eldste utgåtte varsel for person.")
                } else {
                    logger.info("Hendelse med id ${hendelse.id} og kategori ${Kategori.UTGATT_VARSEL} ble lagret i DB")
                }
            }

            Kategori.UDELT_SAMTALEREFERAT -> {
                logger.info("Hendelse med id ${hendelse.id} og kategori ${hendelse.kategori} ble lagret i DB")
            }
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

        when (hendelse.kategori) {
            Kategori.UTGATT_VARSEL -> {
                val eldsteUtgattVarselHendelse =
                    hendelseRepository.getEldste(hendelse.personIdent, Kategori.UTGATT_VARSEL)

                if (eldsteUtgattVarselHendelse.id == hendelse.id) {
                    oppdaterUtgattVarselForBrukerIOpenSearch(hendelse)
                    logger.info("Hendelse med id ${hendelse.id} og kategori ${Kategori.UTGATT_VARSEL} ble oppdatert i DB og OpenSearch ble oppdatert med ny eldste utgåtte varsel for person.")
                } else {
                    logger.info("Hendelse med id ${hendelse.id} og kategori ${Kategori.UTGATT_VARSEL} ble oppdatert i DB")
                }
            }

            Kategori.UDELT_SAMTALEREFERAT -> {
                logger.info("Hendelse med id ${hendelse.id} og kategori ${hendelse.kategori} ble oppdatert i DB")
            }
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

        when (hendelse.kategori) {
            Kategori.UTGATT_VARSEL -> {
                val resultatAvGetEldsteUtgattVarselHendelse = try {
                    hendelseRepository.getEldste(hendelse.personIdent, Kategori.UTGATT_VARSEL)
                } catch (ex: IngenHendelseForPersonException) {
                    ex
                }

                if (resultatAvGetEldsteUtgattVarselHendelse is IngenHendelseForPersonException) {
                    // All good - det var ingen flere hendelser for personen etter at vi slettet den som kom inn som argument
                    slettUgattVarselForBrukerIOpenSearch(hendelse)
                    logger.info("Hendelse med id ${hendelse.id} og kategori ${Kategori.UTGATT_VARSEL} ble slettet i DB og utgått varsel ble fjernet for person i OpenSearch siden personen ikke hadde andre hendelser.")
                    return
                }

                if (resultatAvGetEldsteUtgattVarselHendelse is Hendelse) {
                    oppdaterUtgattVarselForBrukerIOpenSearch(resultatAvGetEldsteUtgattVarselHendelse)

                    logger.info("Hendelse med id ${hendelse.id}  og kategori ${Kategori.UTGATT_VARSEL} ble slettet i DB og OpenSearch ble oppdatert med ny eldste utgåtte varsel for person, med id ${resultatAvGetEldsteUtgattVarselHendelse.id}")
                }
            }

            Kategori.UDELT_SAMTALEREFERAT -> {
                logger.info("Hendelse med id ${hendelse.id} og kategori ${hendelse.kategori} ble slettet i DB.")
            }
        }
    }

    private fun oppdaterUtgattVarselForBrukerIOpenSearch(hendelse: Hendelse) {
        // TODO: 2024-11-29, Sondre - Her konverterer vi bare ukritisk til Fnr, selv om NorskIdent også kan være f.eks. D-nummer
        val aktorId = pdlIdentRepository.hentAktorIdForAktivBruker(Fnr.of(hendelse.personIdent.get()))
        opensearchIndexerV2.oppdaterUtgattVarsel(hendelse, aktorId)
    }

    private fun slettUgattVarselForBrukerIOpenSearch(hendelse: Hendelse) {
        // TODO: 2024-11-29, Sondre - Her konverterer vi bare ukritisk til Fnr, selv om NorskIdent også kan være f.eks. D-nummer
        val aktorId = pdlIdentRepository.hentAktorIdForAktivBruker(Fnr.of(hendelse.personIdent.get()))
        opensearchIndexerV2.slettUtgattVarsel(aktorId)
    }
}