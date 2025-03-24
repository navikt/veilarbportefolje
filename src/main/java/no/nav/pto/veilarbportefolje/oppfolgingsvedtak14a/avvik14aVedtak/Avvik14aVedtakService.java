package no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.avvik14aVedtak;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.PortefoljeMapper;
import no.nav.pto.veilarbportefolje.domene.ArenaHovedmal;
import no.nav.pto.veilarbportefolje.domene.ArenaInnsatsgruppe;
import no.nav.pto.veilarbportefolje.domene.GjeldendeIdenter;
import no.nav.pto.veilarbportefolje.domene.Vedtak14aInfo;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.Gjeldende14aVedtak;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.Gjeldende14aVedtakService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static no.nav.pto.veilarbportefolje.util.EnumUtils.valueOfOrNull;

@RequiredArgsConstructor
@Service
public class Avvik14aVedtakService {

    final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;
    final Gjeldende14aVedtakService gjeldende14aVedtakService;

    public Map<GjeldendeIdenter, Avvik14aVedtak> hentAvvik(Set<GjeldendeIdenter> brukere) {
        if (brukere.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Fnr, OppfolgingsbrukerEntity> fnrOppfolgingsbrukerEntityMap =
                oppfolgingsbrukerRepositoryV3.hentOppfolgingsBrukere(
                        brukere.stream().map(GjeldendeIdenter::getFnr).collect(Collectors.toSet())
                );

        Map<AktorId, Optional<Gjeldende14aVedtak>> aktorIdGjeldende14aVedtakMap =
                gjeldende14aVedtakService.hentGjeldende14aVedtak(
                        brukere.stream().map(GjeldendeIdenter::getAktorId).collect(Collectors.toSet())
                );


        return brukere.stream().map((GjeldendeIdenter identer) -> {
            Vedtak14aInfo vedtak14aInfo = mapTilVedtak14aInfo(fnrOppfolgingsbrukerEntityMap, aktorIdGjeldende14aVedtakMap, identer);
            Avvik14aVedtak avvik14aVedtak = finnAvvik(vedtak14aInfo);

            return Map.entry(identer, avvik14aVedtak);
        }).collect(toMap(Entry::getKey, Entry::getValue));
    }

    private Vedtak14aInfo mapTilVedtak14aInfo(Map<Fnr, OppfolgingsbrukerEntity> fnrOppfolgingsbrukerEntityMap, Map<AktorId, Optional<Gjeldende14aVedtak>> aktorIdGjeldende14aVedtakMap, GjeldendeIdenter identer) {
        Optional<OppfolgingsbrukerEntity> maybeOppfolgingsbruker = Optional.ofNullable(fnrOppfolgingsbrukerEntityMap.getOrDefault(identer.getFnr(), null));
        Optional<Gjeldende14aVedtak> maybeGjeldende14aVedtak = aktorIdGjeldende14aVedtakMap.getOrDefault(identer.getAktorId(), Optional.empty());

        return Vedtak14aInfo.builder()
                .arenaInnsatsgruppe(maybeOppfolgingsbruker.map(OppfolgingsbrukerEntity::kvalifiseringsgruppekode).map(kvalifiseringsgruppeKode -> valueOfOrNull(ArenaInnsatsgruppe.class, kvalifiseringsgruppeKode)).orElse(null))
                .arenaHovedmal(maybeOppfolgingsbruker.map(OppfolgingsbrukerEntity::hovedmaalkode).map(hovedmaal -> valueOfOrNull(ArenaHovedmal.class, hovedmaal)).orElse(null))
                .innsatsgruppe(maybeGjeldende14aVedtak.map(Gjeldende14aVedtak::getInnsatsgruppe).orElse(null))
                .hovedmal(maybeGjeldende14aVedtak.map(Gjeldende14aVedtak::getHovedmal).orElse(null)).build();
    }

    private Avvik14aVedtak finnAvvik(Vedtak14aInfo vedtak14aInfo) {
        ArenaInnsatsgruppe konvertertInnsatsgruppe = PortefoljeMapper.mapTilArenaInnsatsgruppe(vedtak14aInfo.getInnsatsgruppe());
        ArenaHovedmal konvertertHovedmaal = PortefoljeMapper.mapTilArenaHovedmal(vedtak14aInfo.getHovedmal());

        if (vedtak14aInfo.getArenaInnsatsgruppe() == null) {
            return Avvik14aVedtak.INGEN_AVVIK;
        }

        if (konvertertInnsatsgruppe == null) {
            return Avvik14aVedtak.INNSATSGRUPPE_MANGLER_I_NY_KILDE;
        }

        if (vedtak14aInfo.getArenaInnsatsgruppe() != konvertertInnsatsgruppe && vedtak14aInfo.getArenaHovedmal() != null && vedtak14aInfo.getArenaHovedmal() != konvertertHovedmaal) {
            return Avvik14aVedtak.INNSATSGRUPPE_OG_HOVEDMAAL_ULIK;
        }

        if (vedtak14aInfo.getArenaInnsatsgruppe() == konvertertInnsatsgruppe && vedtak14aInfo.getArenaHovedmal() != null && vedtak14aInfo.getArenaHovedmal() != konvertertHovedmaal) {
            return Avvik14aVedtak.HOVEDMAAL_ULIK;
        }

        if (vedtak14aInfo.getArenaInnsatsgruppe() != konvertertInnsatsgruppe && vedtak14aInfo.getArenaHovedmal() == konvertertHovedmaal) {
            return Avvik14aVedtak.INNSATSGRUPPE_ULIK;
        }

        return Avvik14aVedtak.INGEN_AVVIK;
    }

}
