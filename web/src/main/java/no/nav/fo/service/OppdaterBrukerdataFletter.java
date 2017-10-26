package no.nav.fo.service;


import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.BrukerinformasjonFraFeed;
import no.nav.fo.domene.PersonId;

import javax.inject.Inject;

public class OppdaterBrukerdataFletter {

    @Inject
    private PersistentOppdatering persistentOppdatering;

    public void oppdaterOppfolgingForBruker(BrukerOppdatertInformasjon bruker, PersonId personId) {

        BrukerinformasjonFraFeed brukerinformasjonFraFeed = new BrukerinformasjonFraFeed().setPersonid(personId.toString());
        persistentOppdatering.lagre(bruker.applyTo(brukerinformasjonFraFeed));
    }
}
