package no.nav.fo.service;


import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.exception.FantIkkePersonIdException;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdRequest;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class OppdaterBrukerdataFletter {

    private static final Logger LOG = getLogger(OppdaterBrukerdataFletter.class);

    @Inject
    private AktoerV2 aktoerV2;

    @Inject
    BrukerRepository brukerRepository;

    @Inject
    SolrService solrService;

    public void tilordneVeilederTilPersonId(BrukerOppdatertInformasjon bruker) {
        String personId = hentPersonIdFromDBorAktoer(bruker.getAktoerid());
        String aktoerId = bruker.getAktoerid();
        brukerRepository.insertOrUpdateBrukerdata(aktoerId, personId, bruker.getVeileder(),bruker.getOppdatert());
        solrService.indekserBrukerMedVeileder(personId);

    }

    private String hentPersonIdFromDBorAktoer(String aktoerId) {
        LOG.debug(String.format("Henter personId for aktoerId%s fra database", aktoerId));
        List<Map<String,Object>> aktoerIdToPersonId = brukerRepository.retrievePersonid(aktoerId);
        if(aktoerIdToPersonId.isEmpty()) {
            return hentPersonIdOgOppdaterDB(aktoerId);
        }
        return (String) aktoerIdToPersonId.get(0).get("PERSONID");
    }

    private String hentPersonIdOgOppdaterDB(String aktoerId) {
        LOG.debug(String.format("Personid ikke funnet i database. Henter personId for aktoerId %s og lagrer det til database", aktoerId));
        String personId;

        try {
            String fnr = aktoerV2.hentIdentForAktoerId(
                    new WSHentIdentForAktoerIdRequest()
                            .withAktoerId(aktoerId)
            ).getIdent();

            personId = brukerRepository.retrievePersonidFromFnr(fnr)
                    .map(BigDecimal::intValue)
                    .map(x -> Integer.toString(x))
                    .orElseThrow(() -> new FantIkkePersonIdException(fnr));

            brukerRepository.insertAktoeridToPersonidMapping(aktoerId, personId);

            LOG.debug(String.format("Personid %s og aktoerId %s lagret til database", personId, aktoerId));
        } catch (HentIdentForAktoerIdPersonIkkeFunnet e) {
            LOG.error(String.format("Kunne ikke finne ident for aktoerId %s", aktoerId));
            return null;
        } catch (Exception e) {
            LOG.error("Kunne ikke hente personId og skrive det til database");
            return null;
        }
        return personId;
    }
}
