package no.nav.pto.veilarbportefolje.client;

import lombok.RequiredArgsConstructor;
import no.nav.poao_tilgang.client.Decision;
import no.nav.poao_tilgang.client.TilgangClient;
import no.nav.pto.veilarbportefolje.kodeverk.CacheConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.Cacheable;

@RequiredArgsConstructor
public class CachedTilgangClient implements TilgangClient {

    private final TilgangClient tilgangClient;

    @NotNull
    @Override
    @Cacheable(CacheConfig.TILGANG_TIL_MODIA_CACHE_NAME)
    public Decision harVeilederTilgangTilModia(@NotNull String navIdent) {
        return tilgangClient.harVeilederTilgangTilModia(navIdent);
    }
}
