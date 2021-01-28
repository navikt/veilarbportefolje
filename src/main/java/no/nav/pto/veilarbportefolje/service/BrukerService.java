package no.nav.pto.veilarbportefolje.service;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.pdl.AktorOppslagClient;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class BrukerService {

    private final BrukerRepository brukerRepository;
    private final AktorOppslagClient aktorOppslagClient;

    @Autowired
    public BrukerService(BrukerRepository brukerRepository, AktorOppslagClient aktorOppslagClient) {
        this.brukerRepository = brukerRepository;
        this.aktorOppslagClient = aktorOppslagClient;
    }

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
        stringMap.forEach((key, value) -> typeMap.put(new Fnr(key), value.map(PersonId::of)));
        return typeMap;
    }

    public Try<PersonId> hentPersonidFraAktoerid(AktorId aktoerId) {
        return brukerRepository.retrievePersonid(aktoerId)
                .map(personId -> personId == null ? getPersonIdFromFnr(aktoerId) : personId)
                .onFailure(e -> log.warn("Kunne ikke hente/mappe personId for aktorid: " + aktoerId, e));
    }


    public PersonId getPersonIdFromFnr(AktorId aktoerId) {
        Fnr fnr = aktorOppslagClient.hentFnr(aktoerId);

        PersonId nyPersonId = brukerRepository.retrievePersonidFromFnr(fnr).get();

        AktorId nyAktorIdForPersonId = Try.of(() ->
                aktorOppslagClient.hentAktorId(fnr))
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
    }

    public Optional<VeilederId> hentVeilederForBruker(AktorId aktoerId) {
        return brukerRepository.hentVeilederForBruker(aktoerId);
    }

    private Optional<Fnr> hentFnrFraAktoerregister(AktorId aktoerId) {
            return Optional
                    .ofNullable(aktorOppslagClient.hentFnr(aktoerId));
    }

}
