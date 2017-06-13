package no.nav.fo.service.impl;

import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.Optional;

public class ArbeidslisteServiceImpl implements ArbeidslisteService {
    @Inject
    private AktoerV2 aktoerV2;

    @Inject
    BrukerRepository brukerRepository;

    @Inject
    ArbeidslisteRepository arbeidslisteRepository;

    @Override
    public Optional<Arbeidsliste> getArbeidsliste(String fnr) {
        return arbeidslisteRepository.retrieveArbeidsliste(fnr);
    }

    @Override
    public Optional<Arbeidsliste> createArbeidsliste(String fnr, String kommentar, Timestamp frist) {
        //get aktoerid

        Arbeidsliste insertData = new Arbeidsliste()
                .setKommentar(kommentar)
                .setFrist(frist);

        getAktoerId(fnr)
                .ifPresent(id -> arbeidslisteRepository.insertArbeidsliste(id, insertData));

        return Optional.empty();
    }

    @Override
    public Optional<Arbeidsliste> updateArbeidsliste(String fnr, String kommentar, Timestamp dato) {
        return Optional.empty();
    }

    @Override
    public Optional<Arbeidsliste> deleteArbeidsliste(String fnr) {
        return Optional.empty();
    }

    public Optional<String> getAktoerId(String fnr) {
        Optional<String> maybeAktoerId = Optional.empty();

        WSHentAktoerIdForIdentRequest request = new WSHentAktoerIdForIdentRequest();
        request.setIdent(fnr);
        try {
            maybeAktoerId = Optional.ofNullable(aktoerV2.hentAktoerIdForIdent(request).getAktoerId());
        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) {
            e.printStackTrace();
        }
        return maybeAktoerId;
    }
}
