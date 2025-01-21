package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriService;
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappService;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto_schema.enums.arena.Hovedmaal;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.pto_schema.enums.arena.Rettighetsgruppe;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingsbrukerServiceV2 extends KafkaCommonNonKeyedConsumerService<EndringPaaOppfoelgingsBrukerV2> {
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;
    private final BrukerServiceV2 brukerServiceV2;
    private final OpensearchIndexer opensearchIndexer;
    private final VeilarbarenaClient veilarbarenaClient;
    private final PdlIdentRepository pdlIdentRepository;
    private final VeilarbVeilederClient veilarbVeilederClient;
    private final HuskelappService huskelappService;
    private final FargekategoriService fargekategoriService;
    private final ArbeidslisteService arbeidslisteService;

    @Override
    public void behandleKafkaMeldingLogikk(EndringPaaOppfoelgingsBrukerV2 kafkaMelding) {
        String fodselsnummer = kafkaMelding.getFodselsnummer();

        if (!pdlIdentRepository.erBrukerUnderOppfolging(fodselsnummer)) {
            log.info("Bruker er ikke under oppfølging, ignorerer melding.");
            secureLog.info("Bruker er ikke under oppfølging, ignorerer endring på bruker med fnr: {}", fodselsnummer);
            return;
        }

        ZonedDateTime iservDato = Optional.ofNullable(kafkaMelding.getIservFraDato())
                .map(dato -> ZonedDateTime.of(dato.atStartOfDay(), ZoneId.systemDefault()))
                .orElse(null);

        Fnr fnr = Fnr.of(fodselsnummer);
        EnhetId enhetForBruker = EnhetId.of(kafkaMelding.getOppfolgingsenhet());
        oppdaterEnhetVedKontorbytteHuskelappFargekategoriArbeidsliste(fnr, enhetForBruker);

        OppfolgingsbrukerEntity oppfolgingsbruker = new OppfolgingsbrukerEntity(
                fodselsnummer,
                kafkaMelding.getFormidlingsgruppe().name(),
                iservDato,
                kafkaMelding.getOppfolgingsenhet(),
                Optional.ofNullable(kafkaMelding.getKvalifiseringsgruppe()).map(Kvalifiseringsgruppe::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getRettighetsgruppe()).map(Rettighetsgruppe::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getHovedmaal()).map(Hovedmaal::name).orElse(null),
                kafkaMelding.getSistEndretDato());
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbruker);

        brukerServiceV2.hentAktorId(Fnr.of(fodselsnummer))
                .ifPresent(id -> {
                    secureLog.info("Fikk endring pa oppfolgingsbruker (V2): {}, topic: aapen-fo-endringPaaOppfoelgingsBruker-v2", id);

                    opensearchIndexer.indekser(id);

                });
    }

    private void oppdaterEnhetVedKontorbytteHuskelappFargekategoriArbeidsliste(Fnr fnr, EnhetId enhetForBruker) {
        try {
            Optional<AktorId> aktorIdForBruker = brukerServiceV2.hentAktorId(fnr);
            aktorIdForBruker.ifPresent(aktorId -> {
                Optional<NavKontor> navKontorForBruker = brukerServiceV2.hentNavKontor(fnr);
                if (navKontorForBruker.isPresent() && !Objects.equals(navKontorForBruker.get().getValue(), enhetForBruker.get())) {
                    brukerServiceV2.hentVeilederForBruker(aktorId).ifPresent(veilederForBruker -> {
                        List<String> veiledereMedTilgangTilEnhet = veilarbVeilederClient.hentVeilederePaaEnhetMachineToMachine(enhetForBruker);
                        boolean brukerBlirAutomatiskTilordnetVeileder = veiledereMedTilgangTilEnhet.contains(veilederForBruker.getValue());
                        if (brukerBlirAutomatiskTilordnetVeileder) {
                            fargekategoriService.oppdaterEnhetPaaFargekategori(fnr, enhetForBruker, veilederForBruker);
                            huskelappService.oppdaterEnhetPaaHuskelapp(fnr, enhetForBruker, veilederForBruker);
                            arbeidslisteService.oppdaterEnhetPaaArbeidsliste(fnr, enhetForBruker, veilederForBruker);
                        }
                    });
                }
            });
        } catch (Exception e) {
            secureLog.error("Kunne ikke oppdatere enhet på huskelapp, fargekategori eller arbeidsliste ved kontrobytte for bruker: " + fnr, e);
        }
    }

    public void hentOgLagreOppfolgingsbruker(AktorId aktorId) {
        Fnr fnr = pdlIdentRepository.hentFnrForAktivBruker(aktorId);

        Optional<OppfolgingsbrukerDTO> oppfolgingsbrukerDTO = veilarbarenaClient.hentOppfolgingsbruker(fnr);

        if (oppfolgingsbrukerDTO.isEmpty()) {
            secureLog.error("Fant ingen oppfølgingsbrukerdata for bruker med fnr: {}.", fnr);
            throw new RuntimeException("Fant ingen oppfølgingsbrukerdata for brukeren.");
        }

        OppfolgingsbrukerEntity oppfolgingsbrukerEntity = new OppfolgingsbrukerEntity(
                oppfolgingsbrukerDTO.get().getFodselsnr(),
                oppfolgingsbrukerDTO.get().getFormidlingsgruppekode(),
                oppfolgingsbrukerDTO.get().getIservFraDato(),
                oppfolgingsbrukerDTO.get().getNavKontor(),
                oppfolgingsbrukerDTO.get().getKvalifiseringsgruppekode(),
                oppfolgingsbrukerDTO.get().getRettighetsgruppekode(),
                oppfolgingsbrukerDTO.get().getHovedmaalkode(),
                oppfolgingsbrukerDTO.get().getSistEndretDato()
        );

        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbrukerEntity);
        secureLog.info("Oppfolgingsbruker hentet og lagret for aktorId: {} / fnr: {}.", aktorId, fnr);
    }

    public void slettOppfolgingsbruker(AktorId aktorId, Optional<Fnr> maybeFnr) {
        if (maybeFnr.isEmpty()) {
            secureLog.warn("Kunne ikke slette oppfolgingsbruker med Aktør-ID {}. Årsak fødselsnummer-parameter var tom.", aktorId.get());
            return;
        }

        try {
            int raderSlettet = oppfolgingsbrukerRepositoryV3.slettOppfolgingsbruker(maybeFnr.get());

            if (raderSlettet != 0) {
                log.info("Oppfolgingsbruker slettet.");
                secureLog.info("Oppfolgingsbruker slettet for fnr: {} / Aktør-ID: {}.", maybeFnr.get(), aktorId.get());
            } else {
                log.info("Fant ingen oppfolgingsbruker å slette.");
                secureLog.info("Fant ingen oppfolgingsbruker å slette for fnr: {} / Aktør-ID: {}.", maybeFnr.get(), aktorId.get());
            }
        } catch (Exception e) {
            secureLog.error(
                    String.format("Kunne ikke slette oppfolgingsbruker for fnr: %s / Aktør-ID: %s.", maybeFnr.get(), aktorId.get()),
                    e
            );
            throw new RuntimeException("Kunne ikke slette oppfolgingsbruker");
        }
    }
}



