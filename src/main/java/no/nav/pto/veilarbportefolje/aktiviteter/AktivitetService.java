package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakService;
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger;
import no.nav.pto.veilarbportefolje.domene.Motedeltaker;
import no.nav.pto.veilarbportefolje.domene.Moteplan;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakkodeverkMapper.tiltakskodeTiltaksnavnMap;
import static no.nav.pto.veilarbportefolje.domene.Motedeltaker.skjermetDeltaker;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class AktivitetService extends KafkaCommonNonKeyedConsumerService<KafkaAktivitetMelding> {
    private final AktiviteterRepositoryV2 aktiviteterRepositoryV2;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;
    private final SisteEndringService sisteEndringService;
    private final OpensearchIndexer opensearchIndexer;
    private final TiltakService tiltakService;

    public void behandleKafkaMeldingLogikk(KafkaAktivitetMelding aktivitetData) {
        AktorId aktorId = AktorId.of(aktivitetData.getAktorId());
        sisteEndringService.behandleAktivitet(aktivitetData);

        boolean erTiltaksaktivitet = KafkaAktivitetMelding.AktivitetTypeData.TILTAK == aktivitetData.aktivitetType;

        if (erTiltaksaktivitet) {
            // Midlertidig loggmelding ifbm overgang til ny datakilde for lønnstilskudd
            log.info("Behandler tiltaksaktivitet fra ny kilde");
            behandleTiltaksaktivitetMelding(aktivitetData, aktorId);
        } else {
            // Midlertidig loggmelding ifbm overgang til ny datakilde for lønnstilskudd
            log.info("Behandler aktivitetsplanaktivitet");
            behandleAktivitetsplanAktivitetMelding(aktivitetData, aktorId);
        }
    }

    private void behandleAktivitetsplanAktivitetMelding(KafkaAktivitetMelding aktivitetData, AktorId aktorId) {
        boolean bleProsessert = aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitetData);
        if (bleProsessert) {
            opensearchIndexer.indekser(aktorId);
        }
    }

    private void behandleTiltaksaktivitetMelding(KafkaAktivitetMelding aktivitetData, AktorId aktorId) {
        boolean erTiltakskodeStottet = tiltakskodeTiltaksnavnMap.containsKey(aktivitetData.tiltakskode);
        if (erTiltakskodeStottet) {
            boolean skalIndeksereBruker = tiltakService.behandleKafkaMelding(aktivitetData);

            if (skalIndeksereBruker) {
                opensearchIndexer.indekser(aktorId);
            }
        } else {
            // TODO 05.07.23: Finne en bedre måte å håndtere dette på - nå bare ignorerer vi alt som ikke er MIDLONTIL eller VARLONTIL.
            // Dette er greit per nå da vi uansett får dataen vi trenger fra Arena.
            secureLog.debug("Mottok aktivitet med aktivitetId: " + aktivitetData.aktivitetId + " med uventet tiltakskode: " + aktivitetData.tiltakskode + " fra ny kilde. Tiltak ble ikke lagret.");
        }
    }

    public void slettOgIndekserUtdanningsAktivitet(String aktivitetid, AktorId aktorId) {
        aktiviteterRepositoryV2.deleteById(aktivitetid);
        opensearchIndexer.indekser(aktorId);
    }

    public void upsertOgIndekserUtdanningsAktivitet(KafkaAktivitetMelding melding) {
        AktorId aktorId = AktorId.of(melding.getAktorId());
        aktiviteterRepositoryV2.upsertAktivitet(melding);
        opensearchIndexer.indekser(aktorId);
    }

    public void deaktiverUtgatteUtdanningsAktivteter() {
        List<AktivitetDTO> utdanningsAktiviteter = aktiviteterRepositoryV2.getPasserteAktiveUtdanningsAktiviter();
        log.info("Skal markere: {} utdanningsaktivteter som utgått", utdanningsAktiviteter.size());
        utdanningsAktiviteter.forEach(aktivitetDTO -> {
                    if (!AktivitetsType.utdanningaktivitet.name().equals(aktivitetDTO.getAktivitetType())) {
                        log.error("Feil i utdanningsaktivteter sql!!!");
                        return;
                    }
                    secureLog.info("Deaktiverer utdaningsaktivitet: {}, med utløpsdato: {}", aktivitetDTO.getAktivitetID(), aktivitetDTO.getTilDato());
                    aktiviteterRepositoryV2.setTilFullfort(aktivitetDTO.getAktivitetID());
                }
        );
    }

    public List<Moteplan> hentMoteplan(VeilederId veilederIdent, EnhetId enhet, BrukerinnsynTilganger brukerInnsynTilganger) {
        List<Moteplan> moteplans = aktiviteterRepositoryV2.hentFremtidigeMoter(veilederIdent, enhet);

        return sensurerMoteplaner(moteplans, brukerInnsynTilganger);
    }

    private List<Moteplan> sensurerMoteplaner(List<Moteplan> moteplans, BrukerinnsynTilganger brukerInnsynTilganger) {
        List<Moteplan> sensurertListe = new ArrayList<>();

        List<String> fnrs = moteplans.stream().map(Moteplan::deltaker).map(Motedeltaker::fnr).toList();
        List<String> skjermedeFnrs = oppfolgingsbrukerRepository.finnSkjulteBrukere(fnrs, brukerInnsynTilganger);

        moteplans.forEach(plan -> {
            if (skjermedeFnrs.stream().anyMatch(skjermetFnr -> skjermetFnr.equals(plan.deltaker().fnr()))) {
                sensurertListe.add(skjermMoteplan(plan));
            } else {
                sensurertListe.add(plan);
            }
        });

        return sensurertListe;
    }

    private static Moteplan skjermMoteplan(Moteplan moteplan) {
        return new Moteplan(skjermetDeltaker, moteplan.dato(), moteplan.avtaltMedNav());
    }
}
