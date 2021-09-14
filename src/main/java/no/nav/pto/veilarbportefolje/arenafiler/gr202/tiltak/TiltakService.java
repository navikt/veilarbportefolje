package no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.database.EnhetTiltakRepository;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;

@Service
public class TiltakService {
    private final EnhetTiltakRepository enhetTiltakRepository;
    private final Cache<String, Try<EnhetTiltak>> enhetTiltakCache;

    @Autowired
    public TiltakService(EnhetTiltakRepository enhetTiltakRepository) {
        this.enhetTiltakRepository = enhetTiltakRepository;
        enhetTiltakCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    public Try<EnhetTiltak> hentEnhettiltak(String enhet) {
        return tryCacheFirst(enhetTiltakCache, enhet,
                () -> enhetTiltakRepository.retrieveEnhettiltak(enhet));
    }
}
