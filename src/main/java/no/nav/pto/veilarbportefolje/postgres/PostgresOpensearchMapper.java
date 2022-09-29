package no.nav.pto.veilarbportefolje.postgres;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkService;
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.postgres.utils.AktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.utils.AvtaltAktivitetEntity;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukPDLBrukerdata;
import static no.nav.pto.veilarbportefolje.postgres.PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon;
import static no.nav.pto.veilarbportefolje.postgres.PostgresAktivitetMapper.kalkulerGenerellAktivitetInformasjon;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostgresOpensearchMapper {
    private final AktivitetOpensearchService aktivitetOpensearchService;
    private final SisteEndringService sisteEndringService;
    private final PdlService pdlService;
    private final UnleashService unleashService;

    private final KodeverkService kodeverkService;

    public List<OppfolgingsBruker> flettInnAktivitetsData(List<OppfolgingsBruker> brukere) {
        List<AktorId> aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).map(AktorId::of).toList();
        Map<AktorId, List<AktivitetEntityDto>> avtalteAktiviterMap = aktivitetOpensearchService.hentAvtaltAktivitetData(aktoerIder);
        Map<AktorId, List<AktivitetEntityDto>> ikkeAvtalteAktiviterMap = aktivitetOpensearchService.hentIkkeAvtaltAktivitetData(aktoerIder);
        brukere.forEach(bruker -> {
                    AktorId aktorId = AktorId.of(bruker.getAktoer_id());

                    List<AktivitetEntityDto> avtalteAktiviteter = avtalteAktiviterMap.get(aktorId) != null ? avtalteAktiviterMap.get(aktorId) : new ArrayList<>();
                    List<AktivitetEntityDto> ikkeAvtalteAktiviteter = ikkeAvtalteAktiviterMap.get(aktorId) != null ? ikkeAvtalteAktiviterMap.get(aktorId) : new ArrayList<>();

                    AvtaltAktivitetEntity avtaltAktivitetData = kalkulerAvtalteAktivitetInformasjon(avtalteAktiviteter);
                    AktivitetEntity alleAktiviteterData = kalkulerGenerellAktivitetInformasjon(
                            Stream.concat(avtalteAktiviteter.stream(), ikkeAvtalteAktiviteter.stream()).toList()
                    );
                    flettInnAvtaltAktivitetData(avtaltAktivitetData, bruker);
                    flettInnAktivitetData(alleAktiviteterData, bruker);
                }
        );

        return brukere;
    }

    public void flettInnSisteEndringerData(List<OppfolgingsBruker> brukere) {
        List<AktorId> aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).map(AktorId::of).toList();
        Map<AktorId, Map<String, Endring>> sisteEndringerDataPostgres = sisteEndringService.hentSisteEndringerFraPostgres(aktoerIder);
        brukere.forEach(bruker -> {
            bruker.setSiste_endringer(sisteEndringerDataPostgres.getOrDefault(AktorId.of(bruker.getAktoer_id()), new HashMap<>()));
        });
    }

    private void flettInnAktivitetData(AktivitetEntity aktivitetData, OppfolgingsBruker bruker) {
        bruker.setAlle_aktiviteter_mote_startdato(aktivitetData.getAktivitetMoteStartdato());
        bruker.setAlle_aktiviteter_mote_utlopsdato(aktivitetData.getAktivitetMoteUtlopsdato());
        bruker.setAlle_aktiviteter_stilling_utlopsdato(aktivitetData.getAktivitetStillingUtlopsdato());
        bruker.setAlle_aktiviteter_egen_utlopsdato(aktivitetData.getAktivitetEgenUtlopsdato());
        bruker.setAlle_aktiviteter_behandling_utlopsdato(aktivitetData.getAktivitetBehandlingUtlopsdato());
        bruker.setAlle_aktiviteter_ijobb_utlopsdato(aktivitetData.getAktivitetIjobbUtlopsdato());
        bruker.setAlle_aktiviteter_sokeavtale_utlopsdato(aktivitetData.getAktivitetSokeavtaleUtlopsdato());
        bruker.setNeste_cv_kan_deles_status(aktivitetData.getNesteCvKanDelesStatus());
        bruker.setNeste_svarfrist_stilling_fra_nav(aktivitetData.getNesteSvarfristStillingFraNav());
        bruker.setAlleAktiviteter(aktivitetData.getAlleAktiviteter());
        // NOTE: tiltak, gruppeaktiviteter, og utdanningsaktiviteter blir håndtert av copy_to feltet i opensearch
        // Dette gjøres da disse aktivitetene ikke kan være satt til "ikke avtalt"
    }

    private void flettInnAvtaltAktivitetData(AvtaltAktivitetEntity aktivitetData, OppfolgingsBruker bruker) {
        bruker.setNyesteutlopteaktivitet(aktivitetData.getNyesteUtlopteAktivitet());
        bruker.setAktivitet_start(aktivitetData.getAktivitetStart());
        bruker.setNeste_aktivitet_start(aktivitetData.getNesteAktivitetStart());
        bruker.setForrige_aktivitet_start(aktivitetData.getForrigeAktivitetStart());
        bruker.setAktivitet_mote_utlopsdato(aktivitetData.getAktivitetMoteUtlopsdato());
        bruker.setAktivitet_mote_startdato(aktivitetData.getAktivitetMoteStartdato());
        bruker.setAktivitet_stilling_utlopsdato(aktivitetData.getAktivitetStillingUtlopsdato());
        bruker.setAktivitet_egen_utlopsdato(aktivitetData.getAktivitetEgenUtlopsdato());
        bruker.setAktivitet_behandling_utlopsdato(aktivitetData.getAktivitetBehandlingUtlopsdato());
        bruker.setAktivitet_ijobb_utlopsdato(aktivitetData.getAktivitetIjobbUtlopsdato());
        bruker.setAktivitet_sokeavtale_utlopsdato(aktivitetData.getAktivitetSokeavtaleUtlopsdato());
        bruker.setAktivitet_tiltak_utlopsdato(aktivitetData.getAktivitetTiltakUtlopsdato());
        bruker.setAktivitet_utdanningaktivitet_utlopsdato(aktivitetData.getAktivitetUtdanningaktivitetUtlopsdato());
        bruker.setAktivitet_gruppeaktivitet_utlopsdato(aktivitetData.getAktivitetGruppeaktivitetUtlopsdato());

        bruker.setAktiviteter(aktivitetData.getAktiviteter());
        bruker.setTiltak(aktivitetData.getTiltak());
    }

    public void flettInnStatsborgerskapData(List<OppfolgingsBruker> brukere) {
        if (brukPDLBrukerdata(unleashService)) {
            List<Fnr> fnrs = brukere.stream().map(OppfolgingsBruker::getFnr).map(Fnr::of).collect(Collectors.toList());
            Map<Fnr, List<Statsborgerskap>> statsborgerskaps = pdlService.hentStatsborgerskap(fnrs);
            brukere.forEach(bruker -> {
                List<Statsborgerskap> statsborgerskapList = statsborgerskaps.getOrDefault(Fnr.of(bruker.getFnr()), Collections.emptyList());
                bruker.setHarFlereStatsborgerskap(statsborgerskapList.size() > 1);
                bruker.setHovedStatsborgerskap(getHovedStatsborgerskap(statsborgerskapList));
            });
        }
    }

    private Statsborgerskap getHovedStatsborgerskap(List<Statsborgerskap> statsborgerskaps) {

        if (statsborgerskaps.isEmpty()) {
            return null;
        } else if (statsborgerskaps.size() == 1) {
            return getHovedStatsborgetskapMedFulltLandNavn(statsborgerskaps.get(0));
        } else {
            return getHovedStatsborgerskapFraList(statsborgerskaps);
        }
    }

    private Statsborgerskap getHovedStatsborgerskapFraList(List<Statsborgerskap> statsborgerskaps) {
        Optional<Statsborgerskap> norskStatsborgerskap = statsborgerskaps.stream()
                .filter(statsborgerskap -> statsborgerskap.getStatsborgerskap().equals("NOR"))
                .findFirst();

        if (norskStatsborgerskap.isPresent()) {
            return getHovedStatsborgetskapMedFulltLandNavn(norskStatsborgerskap.get());
        } else {
            statsborgerskaps.sort(Comparator.comparing(Statsborgerskap::getGyldigFra, Comparator.nullsLast(Comparator.naturalOrder())));
            return getHovedStatsborgetskapMedFulltLandNavn(statsborgerskaps.get(0));
        }
    }

    private Statsborgerskap getHovedStatsborgetskapMedFulltLandNavn(Statsborgerskap statsborgerskap) {
        return new Statsborgerskap(kodeverkService.getBeskrivelseForLandkode(statsborgerskap.getStatsborgerskap()),
                statsborgerskap.getGyldigFra(),
                statsborgerskap.getGyldigTil());
    }
}
