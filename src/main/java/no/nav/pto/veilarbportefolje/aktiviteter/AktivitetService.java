package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.aktiviteter.domene.Aktivitet;
import no.nav.pto.veilarbportefolje.aktiviteter.domene.AktivitetsType;
import no.nav.pto.veilarbportefolje.aktiviteter.dto.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakService;
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger;
import no.nav.pto.veilarbportefolje.domene.Motedeltaker;
import no.nav.pto.veilarbportefolje.domene.MoteplanDTO;
import no.nav.pto.veilarbportefolje.domene.Moteplan;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
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

        boolean erTiltaksaktivitet = KafkaAktivitetMelding.AktivitetTypeData.TILTAK == aktivitetData.getAktivitetType();

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
        boolean erTiltakskodeStottet = tiltakskodeTiltaksnavnMap.containsKey(aktivitetData.getTiltakskode());
        if (erTiltakskodeStottet) {
            boolean skalIndeksereBruker = tiltakService.behandleKafkaMelding(aktivitetData);

            if (skalIndeksereBruker) {
                opensearchIndexer.indekser(aktorId);
            }
        } else {
            // TODO 05.07.23: Finne en bedre måte å håndtere dette på - nå bare ignorerer vi alt som ikke er MIDLONTIL eller VARLONTIL.
            // Dette er greit per nå da vi uansett får dataen vi trenger fra Arena.
            secureLog.debug("Mottok aktivitet med aktivitetId: " + aktivitetData.getAktivitetId() + " med uventet tiltakskode: " + aktivitetData.getTiltakskode() + " fra ny kilde. Tiltak ble ikke lagret.");
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
        List<Aktivitet> utdanningsAktiviteter = aktiviteterRepositoryV2.getPasserteAktiveUtdanningsAktiviter();
        log.info("Skal markere: {} utdanningsaktivteter som utgått", utdanningsAktiviteter.size());
        utdanningsAktiviteter.forEach(aktivitet -> {
                    if (!AktivitetsType.utdanningaktivitet.name().equals(aktivitet.getAktivitetType())) {
                        log.error("Feil i utdanningsaktivteter sql!!!");
                        return;
                    }
                    secureLog.info("Deaktiverer utdaningsaktivitet: {}, med utløpsdato: {}", aktivitet.getAktivitetID(), aktivitet.getTilDato());
                    aktiviteterRepositoryV2.setTilFullfort(aktivitet.getAktivitetID());
                }
        );
    }

    public List<MoteplanDTO> hentMoteplan(VeilederId veilederIdent, EnhetId enhet, BrukerinnsynTilganger brukerInnsynTilganger) {
        List<Moteplan> moteplans = aktiviteterRepositoryV2.hentFremtidigeMoter(veilederIdent, enhet);
        List<MoteplanDTO> moteplanerMedVarighet = moteplans.stream().map(MoteplanDTO::of).toList();

        return sensurerMoteplaner(moteplanerMedVarighet, brukerInnsynTilganger);
    }

    private List<MoteplanDTO> sensurerMoteplaner(List<MoteplanDTO> moteplans, BrukerinnsynTilganger brukerInnsynTilganger) {
        List<MoteplanDTO> sensurertListe = new ArrayList<>();

        List<String> fnrs = moteplans.stream().map(MoteplanDTO::deltaker).map(Motedeltaker::fnr).toList();
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

    private static MoteplanDTO skjermMoteplan(MoteplanDTO moteplan) {
        return new MoteplanDTO(skjermetDeltaker, moteplan.dato(), moteplan.varighetMinutter(), moteplan.avtaltMedNav());
    }
}
