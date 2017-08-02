package no.nav.fo.service;


import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.BrukerinformasjonFraFeed;
import no.nav.fo.domene.PersonId;
import org.slf4j.Logger;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;

public class OppdaterBrukerdataFletter {

    private static final Logger LOG = getLogger(OppdaterBrukerdataFletter.class);

    @Inject
    PersistentOppdatering persistentOppdatering;


    public void oppdaterSituasjonForBruker(BrukerOppdatertInformasjon bruker, PersonId personId) {

        BrukerinformasjonFraFeed brukerinformasjonFraFeed = new BrukerinformasjonFraFeed().setPersonid(personId.toString());
        persistentOppdatering.lagre(bruker.applyTo(brukerinformasjonFraFeed));
    }
}
