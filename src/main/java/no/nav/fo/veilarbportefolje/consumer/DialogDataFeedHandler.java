package no.nav.fo.veilarbportefolje.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.feed.DialogDataFraFeed;
import no.nav.fo.veilarbportefolje.feed.DialogFeedRepository;
import no.nav.fo.veilarbportefolje.indeksering.IndekseringService;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbportefolje.consumer.AsyncAwaitUtils.async;
import static no.nav.fo.veilarbportefolje.consumer.AsyncAwaitUtils.await;


@Slf4j
public class DialogDataFeedHandler implements FeedCallback<DialogDataFraFeed> {

    public static final String DIALOGAKTOR_SIST_OPPDATERT = "dialogaktor_sist_oppdatert";
    private final BrukerRepository brukerRepository;
    private final IndekseringService indekseringService;
    private final DialogFeedRepository dialogFeedRepository;

    @Inject
    public DialogDataFeedHandler(BrukerRepository brukerRepository,
                                 IndekseringService indekseringService,
                                 DialogFeedRepository dialogFeedRepository) {
        this.brukerRepository = brukerRepository;
        this.indekseringService = indekseringService;
        this.dialogFeedRepository = dialogFeedRepository;
    }

    @Override
    @Transactional
    public void call(String lastEntry, List<DialogDataFraFeed> data) {

        List<Runnable> jobs = data.stream()
                .map(this::toRunnable)
                .collect(toList());

        CompletableFuture future = async(jobs);
        await(future);

        brukerRepository.updateMetadata(DIALOGAKTOR_SIST_OPPDATERT, Date.from(ZonedDateTime.parse(lastEntry).toInstant()));
    }

    private Runnable toRunnable(DialogDataFraFeed info) {
        return () -> {
            dialogFeedRepository.oppdaterDialogInfoForBruker(info);
            indekseringService.indekserAsynkront(AktoerId.of(info.getAktorId()));
        };
    }
}
