package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.auth.Skjermettilgang;
import no.nav.pto.veilarbportefolje.domene.Motedeltaker;
import no.nav.pto.veilarbportefolje.domene.Moteplan;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AktivitetService extends KafkaCommonConsumerService<KafkaAktivitetMelding> {
    private final AktiviteterRepositoryV2 aktiviteterRepositoryV2;
    private final OppfolgingsbrukerRepositoryV2 oppfolgingsbrukerRepositoryV2;
    private final BrukerService brukerService;
    private final SisteEndringService sisteEndringService;
    private final OpensearchIndexer opensearchIndexer;

    public void behandleKafkaMeldingLogikk(KafkaAktivitetMelding aktivitetData) {
        AktorId aktorId = AktorId.of(aktivitetData.getAktorId());
        mapAktoerIdTilPersonIdHvisNodvendig(aktorId); // Fanger opp om personId er endret, kan evt. løses ved å sende med personId på topic fra veilarbarena

        sisteEndringService.behandleAktivitet(aktivitetData);
        boolean bleProsessert = aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitetData);
        if (bleProsessert) {
            opensearchIndexer.indekser(aktorId);
        }
    }

    private void mapAktoerIdTilPersonIdHvisNodvendig(AktorId aktorId) {
        brukerService.hentPersonidFraAktoerid(aktorId);
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
        List<String> skjermedeFnrs = oppfolgingsbrukerRepositoryV2.finnSkjulteBrukere(fnrs, skjermettilgang);

        moteplans.forEach(plan -> {
            if (skjermedeFnrs.stream().anyMatch(skjermetFnr -> skjermetFnr.equals(plan.deltaker().fnr()))) {
                sensurertListe.add(plan.getSkjermetMoteplan());
            } else {
                sensurertListe.add(plan);
            }
        });

        return sensurertListe;
    }
}
