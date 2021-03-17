package no.nav.pto.veilarbportefolje.service;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.AUTO_SLETT;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrukerService {

    private final BrukerRepository brukerRepository;
    private final AktorClient aktorClient;
    private final ElasticServiceV2 elasticServiceV2;
    private final UnleashService unleashService;

    public Optional<AktorId> hentAktorId(Fnr fnr) {
        return brukerRepository.hentBrukerFraView(fnr)
                .map(OppfolgingsBruker::getAktoer_id)
                .map(AktorId::new);
    }

    public Optional<String> hentNavKontor(AktorId aktoerId) {
        return brukerRepository
                .hentNavKontorFraView(aktoerId)
                .or(() -> hentFnrFraAktoerregister(aktoerId)
                        .flatMap(brukerRepository::hentNavKontorFraDbLinkTilArena));
    }

    public Optional<String> hentNavKontorFraDbLinkTilArena(Fnr fnr) {
        return brukerRepository.hentNavKontorFraDbLinkTilArena(fnr);
    }

    public Map<Fnr, Optional<PersonId>> hentPersonidsForFnrs(List<Fnr> fnrs) {
        Map<Fnr, Optional<PersonId>> typeMap = new HashMap<>();
        Map<String, Optional<String>> stringMap = brukerRepository.retrievePersonidFromFnrs(fnrs.stream().map(Fnr::toString).collect(toList()));
        stringMap.forEach((key, value) -> typeMap.put(Fnr.ofValidFnr(key), value.map(PersonId::of)));
        return typeMap;
    }

    public Try<PersonId> hentPersonidFraAktoerid(AktorId aktoerId) {
        return brukerRepository.retrievePersonid(aktoerId)
                .map(personId -> personId == null ? getPersonIdFromFnr(aktoerId) : personId)
                .onFailure(e -> log.warn("Kunne ikke hente/mappe personId for aktorid: " + aktoerId, e));
    }


    public PersonId getPersonIdFromFnr(AktorId aktoerId) {
        Fnr fnr = aktorClient.hentFnr(aktoerId);

        PersonId nyPersonId = brukerRepository.retrievePersonidFromFnr(fnr).get();

        AktorId nyAktorIdForPersonId = Try.of(() ->
                aktorClient.hentAktorId(fnr))
                .get();

        updateGjeldeFlaggOgInsertAktoeridPaNyttMapping(aktoerId, nyPersonId, nyAktorIdForPersonId);
        return nyPersonId;
    }

    @Transactional
    void updateGjeldeFlaggOgInsertAktoeridPaNyttMapping(AktorId aktoerId, PersonId personId, AktorId aktoerIdFraTPS) {
        if (personId == null) {
            return;
        }

        if (!aktoerId.equals(aktoerIdFraTPS)) {
            brukerRepository.insertGamleAktorIdMedGjeldeneFlaggNull(aktoerId, personId);
        } else {
            brukerRepository.setGjeldeneFlaggTilNull(personId);
            brukerRepository.insertAktoeridToPersonidMapping(aktoerId, personId);
        }
        if(unleashService.isEnabled(AUTO_SLETT)) {
            brukerRepository.hentGamleAktorIder(personId).ifPresent(elasticServiceV2::slettDokumenter);
        }
    }

    public Optional<VeilederId> hentVeilederForBruker(AktorId aktoerId) {
        return brukerRepository.hentVeilederForBruker(aktoerId);
    }

    private Optional<Fnr> hentFnrFraAktoerregister(AktorId aktoerId) {
            return Optional.ofNullable(aktorClient.hentFnr(aktoerId));
    }

}
