package no.nav.fo.consumer;


import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.BrukerOppdatering;
import no.nav.fo.domene.Brukerdata;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.AktoerService;
import no.nav.fo.util.MetricsUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
public class DialogDataFeedHandler implements FeedCallback<DialogDataFraFeed> {

    private final PersistentOppdatering persistentOppdatering;
    private final JdbcTemplate db;
    private final AktoerService aktoerService;

    @Inject
    public DialogDataFeedHandler(PersistentOppdatering persistentOppdatering, JdbcTemplate db, AktoerService aktoerService) {
        this.persistentOppdatering = persistentOppdatering;
        this.db = db;
        this.aktoerService = aktoerService;
    }

    @Override
    @Transactional
    public void call(String lastEntry, List<DialogDataFraFeed> data) {
        data.forEach(this::behandleDialogData);
        db.update("UPDATE METADATA SET dialogaktor_sist_oppdatert = ?", Date.from(ZonedDateTime.parse(lastEntry).toInstant()));
        Event event = MetricsFactory.createEvent("datamotattfrafeed");
        event.report();
    }

    private void behandleDialogData(DialogDataFraFeed dialog) {
        try {
            MetricsUtils.timed("feed.dialog.objekt",
                    () -> {
                        DialogBrukerOppdatering oppdatering = new DialogBrukerOppdatering(dialog, aktoerService.hentPersonidFraAktoerid(dialog.aktorId));
                        persistentOppdatering.lagre(oppdatering);
                        return null;
                    },
                    (timer, hasFailed) -> {
                        if (hasFailed) {
                            timer.addTagToReport("aktorhash", DigestUtils.md5Hex(dialog.aktorId).toUpperCase());
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Feil ved behandlig av aktivitetdata fra feed med aktorid {}, {}", dialog.aktorId, e.getMessage());
        }
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
                    .setVenterPaSvarFraBruker(LocalDateTime.ofInstant(dialog.venterPaSvar.toInstant(), ZoneId.systemDefault()))
                    .setVenterPaSvarFraNav(LocalDateTime.ofInstant(dialog.harUbehandlet.toInstant(), ZoneId.systemDefault()));
        }
    }
}
