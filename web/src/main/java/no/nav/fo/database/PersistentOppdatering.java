package no.nav.fo.database;

import no.nav.fo.domene.BrukerOppdatering;
import no.nav.fo.domene.Brukerdata;
import no.nav.fo.domene.PersonId;
import no.nav.fo.service.SolrService;
import no.nav.fo.util.MetricsUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class PersistentOppdatering {

    @Inject
    SolrService solrService;

    @Inject
    BrukerRepository brukerRepository;

    public Brukerdata hentDataOgLagre(BrukerOppdatering brukerOppdatering) {
        Brukerdata brukerdata = hentBruker(brukerOppdatering.getPersonid());
        brukerOppdatering.applyTo(brukerdata);
        lagreIDB(singletonList(brukerdata));
        return brukerdata;
    }

    public void lagre(BrukerOppdatering brukerOppdatering) {
        lagreISolr(hentDataOgLagre(brukerOppdatering));
    }

    public void lagreBrukeroppdateringerIDB(List<BrukerOppdatering> brukerOppdatering) {
        io.vavr.collection.List.ofAll(brukerOppdatering)
                .sliding(1000, 1000)
                .forEach(MetricsUtils.timed("GR199.upsert1000",(oppdateringer) -> {
                    Map<String, Brukerdata> brukerdata = brukerRepository.retrieveBrukerdata(oppdateringer
                            .toJavaList()
                            .stream()
                            .map(BrukerOppdatering::getPersonid)
                            .collect(toList())
                    )
                            .stream()
                            .collect(toMap(Brukerdata::getPersonid, Function.identity()));


                    List<Brukerdata> brukere = oppdateringer.map((oppdatering) -> {
                        Brukerdata bruker = brukerdata.getOrDefault(
                                oppdatering.getPersonid(),
                                new Brukerdata().setPersonid(oppdatering.getPersonid())
                        );

                        return oppdatering.applyTo(bruker);
                    }).toJavaList();

                    brukerRepository.insertOrUpdateBrukerdata(brukere, brukerdata.keySet());
                }));
    }

    public void lagreIDB(List<Brukerdata> brukerdata) {
        brukerRepository.insertOrUpdateBrukerdata(brukerdata, emptyList());

        //Lagre aktivitetstatuser
        brukerdata
                .stream()
                .filter((data) -> data.getAktivitetStatus() != null)
                .forEach( data -> brukerRepository.upsertAktivitetStatuserForBruker(data.getAktivitetStatus(), data.getAktoerid(), data.getPersonid()));
    }

    public void lagreISolr(Brukerdata brukerdata) {
        solrService.indekserBrukerdata(new PersonId(brukerdata.getPersonid()));
    }

    private Brukerdata hentBruker(String personid) {
        List<Brukerdata> brukerdata = brukerRepository.retrieveBrukerdata(singletonList(personid));
        if (brukerdata.isEmpty()) {
            return new Brukerdata().setPersonid(personid);
        }
        return brukerdata.get(0);
    }
}
