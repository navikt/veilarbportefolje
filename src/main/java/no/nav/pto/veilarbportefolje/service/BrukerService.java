package no.nav.pto.veilarbportefolje.service;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.PersonId;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class BrukerService {

    private final BrukerRepository brukerRepository;
    private final AktorregisterClient aktorregisterClient;

    @Autowired
    public BrukerService(BrukerRepository brukerRepository, AktorregisterClient aktorregisterClient) {
        this.brukerRepository = brukerRepository;
        this.aktorregisterClient = aktorregisterClient;
    }

    public Optional<String> hentNavKontor(AktoerId aktoerId) {
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
        stringMap.forEach((key, value) -> typeMap.put(new Fnr(key), value.map(PersonId::of)));
        return typeMap;
    }

    public Try<PersonId> hentPersonidFraAktoerid(AktoerId aktoerId) {
        return brukerRepository.retrievePersonid(aktoerId)
                .map(personId -> personId == null ? getPersonIdFromFnr(aktoerId) : personId)
                .onFailure(e -> log.warn("Kunne ikke hente/mappe personId for aktorid: " + aktoerId, e));
    }


    public PersonId getPersonIdFromFnr(AktoerId aktoerId) {
        Fnr fnr = Fnr.of(aktorregisterClient.hentFnr(aktoerId.toString()));
        PersonId nyPersonId = brukerRepository.retrievePersonidFromFnr(fnr).get();
        AktoerId nyAktorIdForPersonId = Try.of(() ->
                aktorregisterClient.hentAktorId(fnr.toString()))
                .map(AktoerId::of)
                .get();

        updateGjeldeFlaggOgInsertAktoeridPaNyttMapping(aktoerId, nyPersonId, nyAktorIdForPersonId);
        return nyPersonId;
    }

    @Transactional
    void updateGjeldeFlaggOgInsertAktoeridPaNyttMapping(AktoerId aktoerId, PersonId personId, AktoerId aktoerIdFraTPS) {
        if (personId == null) {
            return;
        }

        if (!aktoerId.equals(aktoerIdFraTPS)) {
            brukerRepository.insertGamleAktoerIdMedGjeldeneFlaggNull(aktoerId, personId);
        } else {
            brukerRepository.setGjeldeneFlaggTilNull(personId);
            brukerRepository.insertAktoeridToPersonidMapping(aktoerId, personId);
        }
    }

    public Optional<VeilederId> hentVeilederForBruker(AktoerId aktoerId) {
        return brukerRepository.hentVeilederForBruker(aktoerId);
    }

    private Optional<Fnr> hentFnrFraAktoerregister(AktoerId aktoerId) {
            return Optional
                    .ofNullable(aktorregisterClient.hentFnr(aktoerId.toString()))
                    .map(Fnr::of);
    }
}
