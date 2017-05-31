package no.nav.fo.service;


import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.BrukerinformasjonFraFeed;
import no.nav.fo.exception.FantIkkePersonIdException;
import org.slf4j.Logger;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;

public class OppdaterBrukerdataFletter {

    private static final Logger LOG = getLogger(OppdaterBrukerdataFletter.class);

    @Inject
    private AktoerService aktoerService;

    @Inject
    PersistentOppdatering persistentOppdatering;


    public void tilordneVeilederTilPersonId(BrukerOppdatertInformasjon bruker) {
        String personId = aktoerService.hentPersonidFraAktoerid(bruker.getAktoerid());
        if (personId == null) {
            throw new FantIkkePersonIdException(bruker.getAktoerid());
        }
        BrukerinformasjonFraFeed brukerinformasjonFraFeed = new BrukerinformasjonFraFeed().setPersonid(personId);
        persistentOppdatering.lagre(bruker.applyTo(brukerinformasjonFraFeed));

    }
}
