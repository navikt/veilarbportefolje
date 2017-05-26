package no.nav.fo.consumer;


import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.BrukerOppdatering;
import no.nav.fo.domene.Brukerdata;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.exception.FantIkkePersonIdException;
import no.nav.fo.feed.consumer.FeedCallback;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;

public class DialogDataFeedHandler implements FeedCallback<DialogDataFraFeed> {

    @Inject
    BrukerRepository brukerRepository;

    @Inject
    PersistentOppdatering persistentOppdatering;

    @Override
    public void call(List<DialogDataFraFeed> data) {
        data.stream()
                .map((dialog) -> new BrukerOppdatering() {
                    private String personId = brukerRepository
                            .retrievePersonIdFromAktoerId(dialog.aktorId)
                            .orElseThrow(() -> new FantIkkePersonIdException(dialog.aktorId));

                    @Override
                    public String getPersonid() {
                        return personId;
                    }

                    @Override
                    public Brukerdata applyTo(Brukerdata bruker) {
                        return bruker
                                .setVenterPaSvarFraBruker(dialog.venterPaSvar ? LocalDateTime.now() : null)
                                .setVenterPaSvarFraNav(dialog.harUbehandlet ? LocalDateTime.now() : null);
                    }
                })
                .forEach(persistentOppdatering::lagre);
    }
}
