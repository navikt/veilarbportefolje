package no.nav.fo.database;

import no.nav.fo.domene.BrukerOppdatering;
import no.nav.fo.domene.Brukerdata;
import no.nav.fo.service.SolrService;

import javax.inject.Inject;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class PersistentOppdatering {

    @Inject
    SolrService solrService;

    @Inject
    BrukerRepository brukerRepository;

    public void lagre(BrukerOppdatering brukerOppdatering) {
        Brukerdata brukerdata = hentBruker(brukerOppdatering.getPersonid());
        brukerOppdatering.applyTo(brukerdata);
        lagreIDB(singletonList(brukerdata));
        lagreISolr(brukerdata);
    }

    public void lagreBrukeroppdateringerIDB(List<BrukerOppdatering> brukerOppdatering) {
        List<Brukerdata> brukere = brukerOppdatering.stream().map((oppdatering) -> {
            Brukerdata brukerdata = hentBruker(oppdatering.getPersonid());
            return oppdatering.applyTo(brukerdata);
        }).collect(toList());

        lagreIDB(brukere);
    }

    public void lagreIDB(List<Brukerdata> brukerdata) {
        brukerRepository.insertOrUpdateBrukerdata(brukerdata);
    }


    public void lagreISolr(Brukerdata brukerdata) {
        solrService.indekserBrukerdata(brukerdata.getPersonid());
    }

    private Brukerdata hentBruker(String personid) {
        return brukerRepository.retrieveBrukerdata(personid);

    }
}
