package no.nav.fo.service;


import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
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

    public void tilordneVeilederTilPersonid(BrukerOppdatertInformasjon bruker) {
        String personid = getPersonidFromDBorAktoer(bruker.getAktoerid());
        String aktoerid = bruker.getAktoerid();
        brukerRepository.insertOrUpdateBrukerdata(aktoerid, personid, bruker.getVeileder(),bruker.getOppdatert());
    }

    private String getPersonidFromDBorAktoer(String aktoerid) {
        LOG.debug(String.format("Henter personid for aktoerid%s fra database", aktoerid));
        String personid = null;
        List<Map<String,Object>> aktoerIdToPersonId = brukerRepository.retrievePersonid(aktoerid);
        if(aktoerIdToPersonId.size() > 0) {
            return (String) aktoerIdToPersonId.get(0).get("PERSONID");
        }
        return hentPersonIdOgOppdaterDB(aktoerid);
    }

    private String hentPersonIdOgOppdaterDB(String aktoerid) {
        LOG.debug(String.format("Personid ikke funnet i database. Henter personid for aktoerid %s og lagrer det til database", aktoerid));
        String personid;

        try {
            String fnr = aktoerV2.hentIdentForAktoerId(
                    new WSHentIdentForAktoerIdRequest()
                            .withAktoerId(aktoerid)
            ).getIdent();

            List<Map<String,Object>> fnrToPersonid = brukerRepository.retrievePersonidFromFnr(fnr);

            if(fnrToPersonid.size() == 0) {
                LOG.error(String.format("Kunne ikke finne fnr i databasen"));
                return null;
            }

            personid = getPersonidFromBigDecimal((BigDecimal) fnrToPersonid.get(0).get("PERSON_ID"));

            brukerRepository.insertAktoeridToPersonidMapping(aktoerid, personid);
            LOG.debug(String.format("Personid %s og aktoerid %s lagret til database", personid, aktoerid));
        } catch (HentIdentForAktoerIdPersonIkkeFunnet e) {
            LOG.error(String.format("Kunne ikke finne ident for aktoerid %s", aktoerid));
            return null;
        } catch (Exception e) {
            LOG.error("Kunne ikke hente personid og skrive det til database");
            return null;
        }
        return personid;
    }

    public String getPersonidFromBigDecimal(BigDecimal personid) {
        return Integer.toString(personid.intValue());
    }
}
