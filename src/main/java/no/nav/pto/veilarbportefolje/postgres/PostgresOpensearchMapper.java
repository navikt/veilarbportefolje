package no.nav.pto.veilarbportefolje.postgres;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NorskIdent;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.*;
import no.nav.pto.veilarbportefolje.domene.GjeldendeIdenter;
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap;
import no.nav.pto.veilarbportefolje.ensligforsorger.EnsligeForsorgereService;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.output.EnsligeForsorgerOvergangsstønadTiltakDto;
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse;
import no.nav.pto.veilarbportefolje.hendelsesfilter.HendelseRepository;
import no.nav.pto.veilarbportefolje.hendelsesfilter.IngenHendelseForPersonException;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkService;
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import no.nav.pto.veilarbportefolje.postgres.utils.AktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.utils.AvtaltAktivitetEntity;
import no.nav.pto.veilarbportefolje.siste14aVedtak.*;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import no.nav.pto.veilarbportefolje.tiltakshendelse.TiltakshendelseRepository;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.pto.veilarbportefolje.postgres.PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon;
import static no.nav.pto.veilarbportefolje.postgres.PostgresAktivitetMapper.kalkulerGenerellAktivitetInformasjon;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostgresOpensearchMapper {
    private final AktivitetOpensearchService aktivitetOpensearchService;
    private final SisteEndringService sisteEndringService;
    private final PdlService pdlService;

    private final KodeverkService kodeverkService;
    private final Avvik14aVedtakService avvik14aService;

    private final BarnUnder18AarService barnUnder18AarService;
    private final EnsligeForsorgereService ensligeForsorgereService;
    private final ArbeidssoekerService arbeidssoekerService;
    private final TiltakshendelseRepository tiltakshendelseRepository;
    private final Siste14aVedtakRepository siste14aVedtakRepository;
    private final HendelseRepository hendelseRepository;

    public void flettInnAktivitetsData(List<OppfolgingsBruker> brukere) {
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

    }

    public void flettInnSisteEndringerData(List<OppfolgingsBruker> brukere) {
        List<AktorId> aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).map(AktorId::of).toList();
        Map<AktorId, Map<String, Endring>> sisteEndringerDataPostgres = sisteEndringService.hentSisteEndringerFraPostgres(aktoerIder);
        brukere.forEach(bruker -> bruker.setSiste_endringer(sisteEndringerDataPostgres.getOrDefault(AktorId.of(bruker.getAktoer_id()), new HashMap<>())));
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
        List<Fnr> fnrs = brukere.stream().map(OppfolgingsBruker::getFnr).map(Fnr::of).collect(Collectors.toList());
        Map<Fnr, List<Statsborgerskap>> statsborgerskaps = pdlService.hentStatsborgerskap(fnrs);
        brukere.forEach(bruker -> {
            List<Statsborgerskap> statsborgerskapList = statsborgerskaps.getOrDefault(Fnr.of(bruker.getFnr()), Collections.emptyList());
            bruker.setHarFlereStatsborgerskap(statsborgerskapList.size() > 1);
            bruker.setHovedStatsborgerskap(getHovedStatsborgerskap(statsborgerskapList));
        });
    }


    private Statsborgerskap getHovedStatsborgerskap(List<Statsborgerskap> statsborgerskaps) {

        if (statsborgerskaps.isEmpty()) {
            return null;
        } else if (statsborgerskaps.size() == 1) {
            return getHovedStatsborgetskapMedFulltLandNavn(statsborgerskaps.getFirst());
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
            return getHovedStatsborgetskapMedFulltLandNavn(statsborgerskaps.getFirst());
        }
    }

    private Statsborgerskap getHovedStatsborgetskapMedFulltLandNavn(Statsborgerskap statsborgerskap) {
        return new Statsborgerskap(kodeverkService.getBeskrivelseForLandkode(statsborgerskap.getStatsborgerskap()),
                statsborgerskap.getGyldigFra(),
                statsborgerskap.getGyldigTil());
    }

    public void flettInnAvvik14aVedtak(List<OppfolgingsBruker> brukere) {
        Map<GjeldendeIdenter, Avvik14aVedtak> avvik14aVedtakList = avvik14aService.hentAvvik(brukere.stream().map(GjeldendeIdenter::of).collect(Collectors.toSet()));
        brukere.forEach(bruker -> bruker.setAvvik14aVedtak(avvik14aVedtakList.get(GjeldendeIdenter.of(bruker))));
    }

    public void flettInnBarnUnder18Aar(List<OppfolgingsBruker> brukere) {
        List<Fnr> brukereFnr = brukere.stream().map(bruker -> Fnr.of(bruker.getFnr())).toList();
        Map<Fnr, List<BarnUnder18AarData>> barnUnder18AarMap = barnUnder18AarService.hentBarnUnder18Aar(brukereFnr);
        brukere.forEach(bruker -> {
            Fnr brukerFnr = Fnr.of(bruker.getFnr());
            if (barnUnder18AarMap.containsKey(brukerFnr)) {
                bruker.setBarn_under_18_aar(barnUnder18AarMap.get(brukerFnr));
            }
        });
    }

    public void flettInnEnsligeForsorgereData(List<OppfolgingsBruker> brukere) {
        Map<Fnr, EnsligeForsorgerOvergangsstønadTiltakDto> fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap = ensligeForsorgereService.hentEnsligeForsorgerOvergangsstønadTiltak(brukere.stream().map(bruker -> Fnr.of(bruker.getFnr())).collect(Collectors.toList()));
        brukere.forEach(bruker -> {
            if (fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.containsKey(Fnr.of(bruker.getFnr()))) {
                bruker.setEnslige_forsorgere_overgangsstonad(fnrEnsligeForsorgerOvergangsstønadTiltakDtoMap.get(Fnr.of(bruker.getFnr())).toEnsligeForsorgereOpensearchDto());
            }
        });
    }

    public void flettInnTiltakshendelser(List<OppfolgingsBruker> brukere) {
        AtomicInteger brukereUtenTiltakshendelse = new AtomicInteger();
        AtomicInteger brukereMedTiltakshendelse = new AtomicInteger();

        brukere.forEach(bruker -> {
            try {
                Tiltakshendelse eldsteTiltakshendelsePaBruker = tiltakshendelseRepository.hentEldsteTiltakshendelse(Fnr.of(bruker.getFnr()));

                if (eldsteTiltakshendelsePaBruker == null) {
                    brukereUtenTiltakshendelse.getAndIncrement();
                } else {
                    brukereMedTiltakshendelse.getAndIncrement();
                }

                bruker.setTiltakshendelse(eldsteTiltakshendelsePaBruker);
            } catch (Error e) {
                log.error("Indeksering – Feil utløst ved henting av eldste tiltakshendelse på bruker.");
            }
        });
        log.info("Indeksering – Brukere med tiltakshendelse: {}, brukere uten tiltakshendelse: {}", brukereMedTiltakshendelse, brukereUtenTiltakshendelse);
    }

    public void flettInnOpplysningerOmArbeidssoekerData(List<OppfolgingsBruker> brukere) {
        List<Fnr> fnrs = brukere.stream().map(OppfolgingsBruker::getFnr).map(Fnr::of).toList();
        List<ArbeidssoekerData> arbeidssoekerDataList = arbeidssoekerService.hentArbeidssoekerData(fnrs);

        brukere.forEach(bruker -> {
            Optional<ArbeidssoekerData> arbeidssoekerData = arbeidssoekerDataList.stream()
                    .filter(data -> data.getFnr().get().equals(bruker.getFnr()))
                    .findFirst();

            arbeidssoekerData.ifPresent(data -> {
                OpplysningerOmArbeidssoeker opplysningerOmArbeidssoeker = data.getOpplysningerOmArbeidssoeker();
                Profilering profilering = data.getProfilering();

                if (opplysningerOmArbeidssoeker != null) {
                    String utdanningBestatt = opplysningerOmArbeidssoeker.getUtdanningBestatt() != null ? opplysningerOmArbeidssoeker.getUtdanningBestatt().name() : null;
                    String utdanningGodkjent = opplysningerOmArbeidssoeker.getUtdanningGodkjent() != null ? opplysningerOmArbeidssoeker.getUtdanningGodkjent().name() : null;
                    bruker.setUtdanning(opplysningerOmArbeidssoeker.getUtdanning().name());
                    bruker.setUtdanning_godkjent(utdanningGodkjent);
                    bruker.setUtdanning_bestatt(utdanningBestatt);
                    bruker.setBrukers_situasjoner(opplysningerOmArbeidssoeker.getJobbsituasjoner().stream().map(JobbSituasjonBeskrivelse::name).toList());
                    bruker.setUtdanning_og_situasjon_sist_endret(opplysningerOmArbeidssoeker.getSendtInnTidspunkt().toLocalDate());
                }
                if (profilering != null) {
                    bruker.setProfilering_resultat(profilering.getProfileringsresultat().name());
                }
            });
        });
    }

    public void flettInnSiste14aVedtak(List<OppfolgingsBruker> brukere) {
        Map<AktorId, Siste14aVedtakForBruker> aktorIdSiste14aVedtakMap = siste14aVedtakRepository.hentSiste14aVedtakForBrukere(brukere.stream().map(bruker ->
                AktorId.of(bruker.getAktoer_id())).collect(Collectors.toSet())
        );
        brukere.forEach(bruker -> {
            Optional<Siste14aVedtakForBruker> maybeSiste14aVedtakForBruker = Optional.ofNullable(aktorIdSiste14aVedtakMap.get(AktorId.of(bruker.getAktoer_id())));
            bruker.setGjeldendeVedtak14a(maybeSiste14aVedtakForBruker.map(siste14aVedtakForBruker -> new GjeldendeVedtak14a(
                    siste14aVedtakForBruker.getInnsatsgruppe(),
                    siste14aVedtakForBruker.getHovedmal(),
                    siste14aVedtakForBruker.getFattetDato()
            )).orElse(null));
        });
    }

    public void flettInnEldsteUtgattVarsel(List<OppfolgingsBruker> brukere) {
        brukere.forEach(bruker -> {
            try {
                Hendelse eldsteHendelsePaPerson = hendelseRepository.getEldsteUtgattVarsel(NorskIdent.of(bruker.getFnr()));
                bruker.setUtgatt_varsel(eldsteHendelsePaPerson.getHendelse());
            } catch (IngenHendelseForPersonException ex) {
                log.info("Fant ingen hendelse/utgått varsel for person, så ingen data å flette inn.");
            }
        });
    }
}
