package no.nav.pto.veilarbportefolje.service;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrukerService {

    private final BrukerRepository brukerRepository;
    private final AktorClient aktorClient;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    public Optional<AktorId> hentAktorId(Fnr fnr) {
        return brukerRepository.hentAktorIdFraView(fnr);
    }

    public Optional<AktorId> hentAktorId(PersonId personid) {
        return brukerRepository.hentAktorIdFraView(personid);
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

    public Try<PersonId> hentPersonidFraAktoerid(AktorId aktoerId) {
        return brukerRepository.retrievePersonid(aktoerId)
                .map(personId -> personId == null ? getPersonIdFromFnr(aktoerId) : personId)
                .onFailure(e -> log.warn("Kunne ikke hente/mappe personId for aktorid: " + aktoerId, e));
    }


    public PersonId getPersonIdFromFnr(AktorId aktoerId) {
        Fnr fnr = aktorClient.hentFnr(aktoerId);

        PersonId nyPersonId = brukerRepository
                .retrievePersonidFromFnr(fnr)
                .orElseThrow(() -> new NoSuchElementException("Fant ikke personId på aktoer: " + aktoerId));

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
            brukerRepository.upsertAktoeridToPersonidMapping(aktoerId, personId);
        }
        brukerRepository.hentGamleAktorIder(personId).ifPresent(opensearchIndexerV2::slettDokumenter);
    }

    public Optional<VeilederId> hentVeilederForBruker(AktorId aktoerId) {
        return brukerRepository.hentVeilederForBruker(aktoerId);
    }

    private Optional<Fnr> hentFnrFraAktoerregister(AktorId aktoerId) {
        return Optional.ofNullable(aktorClient.hentFnr(aktoerId));
    }

}
