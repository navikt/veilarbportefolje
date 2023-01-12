package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.auth.Skjermettilgang;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.domene.Motedeltaker;
import no.nav.pto.veilarbportefolje.domene.Moteplan;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static no.nav.pto.veilarbportefolje.domene.Motedeltaker.skjermetDeltaker;

@Slf4j
@Service
@RequiredArgsConstructor
public class AktivitetService extends KafkaCommonConsumerService<KafkaAktivitetMelding> {
    private final AktiviteterRepositoryV2 aktiviteterRepositoryV2;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;
    private final SisteEndringService sisteEndringService;
    private final OpensearchIndexer opensearchIndexer;
    private final UnleashService unleashService;

    public void behandleKafkaMeldingLogikk(KafkaAktivitetMelding aktivitetData) {
        AktorId aktorId = AktorId.of(aktivitetData.getAktorId());
        // TODO: Bytt ut med sjekk på aktivitetData.aktivitetType (eks: KafkaAktivitetMelding.AktivitetTypeData.TILTAK == aktivitetData.aktivitetType)
        if (FeatureToggle.brukNyKildeForTiltaksaktiviteter(unleashService)) {
            boolean erTiltaksaktivitet = true;
            if (erTiltaksaktivitet) {
                // TODO: Håndter tiltaksaktiviteter
            } else {
                // TODO: Håndter andre (aktivitetsplan) aktiviteter
            }
        } else {
            sisteEndringService.behandleAktivitet(aktivitetData);
            boolean bleProsessert = aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitetData);
            if (bleProsessert) {
                opensearchIndexer.indekser(aktorId);
            }
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
                    log.info("Deaktiverer utdaningsaktivitet: {}, med utløpsdato: {}", aktivitetDTO.getAktivitetID(), aktivitetDTO.getTilDato());
                    aktiviteterRepositoryV2.setTilFullfort(aktivitetDTO.getAktivitetID());
                }
        );
    }

    public List<Moteplan> hentMoteplan(VeilederId veilederIdent, EnhetId enhet, Skjermettilgang skjermettilgang) {
        List<Moteplan> moteplans = aktiviteterRepositoryV2.hentFremtidigeMoter(veilederIdent, enhet);

        return sensurerMoteplaner(moteplans, skjermettilgang);
    }

    private List<Moteplan> sensurerMoteplaner(List<Moteplan> moteplans, Skjermettilgang skjermettilgang) {
        List<Moteplan> sensurertListe = new ArrayList<>();

        List<String> fnrs = moteplans.stream().map(Moteplan::deltaker).map(Motedeltaker::fnr).toList();
        List<String> skjermedeFnrs = oppfolgingsbrukerRepository.finnSkjulteBrukere(fnrs, skjermettilgang);

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
