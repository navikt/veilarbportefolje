package no.nav.fo.consumer;


import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.BrukerOppdatering;
import no.nav.fo.domene.Brukerdata;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.feed.consumer.FeedCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
public class DialogDataFeedHandler implements FeedCallback<DialogDataFraFeed> {

    private final BrukerRepository brukerRepository;
    private final PersistentOppdatering persistentOppdatering;
    private final JdbcTemplate db;

    @Inject
    public DialogDataFeedHandler(BrukerRepository brukerRepository, PersistentOppdatering persistentOppdatering, JdbcTemplate db) {
        this.brukerRepository = brukerRepository;
        this.persistentOppdatering = persistentOppdatering;
        this.db = db;
    }

    @Override
    @Transactional
    public void call(String lastEntry, List<DialogDataFraFeed> data) {
        data.stream()
                .map((dialog) -> new DialogBrukerOppdatering(dialog, brukerRepository.retrievePersonIdFromAktoerId(dialog.aktorId)))
                .forEach((oppdatering) -> {
                    if (oppdatering.harPersonid()) {
                        persistentOppdatering.lagre(oppdatering);
                    } else {
                        log.warn("Fant ikke bruker med aktorID", oppdatering.dialog.aktorId);
                    }
                });
        db.update("UPDATE METADATA SET dialogaktor_sist_oppdatert = ?", Date.from(ZonedDateTime.parse(lastEntry).toInstant()));
    }

    static class DialogBrukerOppdatering implements BrukerOppdatering {
        private final DialogDataFraFeed dialog;
        private Optional<String> personId;

        public DialogBrukerOppdatering(DialogDataFraFeed dialog, Optional<String> personId) {
            this.dialog = dialog;
            this.personId = personId;
        }

        @Override
        public String getPersonid() {
            return personId.get();
        }

        @Override
        public Brukerdata applyTo(Brukerdata bruker) {
            return bruker
                    .setVenterPaSvarFraBruker(dialog.venterPaSvar ? LocalDateTime.now() : null)
                    .setVenterPaSvarFraNav(dialog.harUbehandlet ? LocalDateTime.now() : null);
        }

        public boolean harPersonid() {
            return this.personId.isPresent();
        }
    }
}
