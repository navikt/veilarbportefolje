package no.nav.pto.veilarbportefolje.elastic.domene;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategorier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Data
@Accessors(chain = true)
public class SisteEndring {
    String mal;

    String ny_stilling;
    String ny_ijobb;
    String ny_egen;
    String ny_behandling;

    String fullfort_stilling;
    String fullfort_ijobb;
    String fullfort_egen;
    String fullfort_behandling;
    String fullfort_sokeavtale;

    String avbrutt_stilling;
    String avbrutt_ijobb;
    String avbrutt_egen;
    String avbrutt_behandling;
    String avbrutt_sokeavtale;

    public void setTidspunktForKategori(String kategori, String value) {
        if (SisteEndringsKategorier.contains(kategori)) {
            switch (SisteEndringsKategorier.valueOf(kategori)) {
                case MAL:
                    mal = value;
                    return;
                case NY_STILLING:
                    ny_stilling = value;
                    return;
                case NY_IJOBB:
                    ny_ijobb = value;
                    return;
                case NY_EGEN:
                    ny_egen = value;
                    return;
                case NY_BEHANDLING:
                    ny_behandling = value;
                    return;
                case FULLFORT_STILLING:
                    fullfort_stilling = value;
                    return;
                case FULLFORT_IJOBB:
                    fullfort_ijobb = value;
                    return;
                case FULLFORT_EGEN:
                    fullfort_egen = value;
                    return;
                case FULLFORT_BEHANDLING:
                    fullfort_behandling = value;
                    return;
                case AVBRUTT_STILLING:
                    avbrutt_stilling = value;
                    return;
                case AVBRUTT_IJOBB:
                    avbrutt_ijobb = value;
                    return;
                case AVBRUTT_EGEN:
                    avbrutt_egen = value;
                    return;
                case AVBRUTT_BEHANDLING:
                    avbrutt_behandling = value;
                    return;
            }
        }
        log.warn("Did not map kategori to bruker: " + kategori);
    }

    @SneakyThrows
    public void setAggregerteVerdier(List<String> kategorier, OppfolgingsBruker oppfolgingsBruker) {
        String sisteKategori = null;
        Instant sisteTid = null;
        for (String kategori : kategorier) {
            if (SisteEndringsKategorier.contains(kategori)) {
                switch (SisteEndringsKategorier.valueOf(kategori)) {
                    case MAL:
                        if (eventIsMoreRecent(sisteTid, mal)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(mal);
                        }
                        break;
                    case NY_STILLING:
                        if (eventIsMoreRecent(sisteTid, ny_stilling)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(ny_stilling);
                        }
                        break;
                    case NY_IJOBB:
                        if (eventIsMoreRecent(sisteTid, ny_ijobb)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(ny_ijobb);
                        }
                        break;
                    case NY_EGEN:
                        if (eventIsMoreRecent(sisteTid, ny_egen)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(ny_egen);
                        }
                        break;
                    case NY_BEHANDLING:
                        if (eventIsMoreRecent(sisteTid, ny_behandling)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(ny_behandling);
                        }
                        break;
                    case FULLFORT_STILLING:
                        if (eventIsMoreRecent(sisteTid, fullfort_stilling)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(fullfort_stilling);
                        }
                        break;
                    case FULLFORT_IJOBB:
                        if (eventIsMoreRecent(sisteTid, fullfort_ijobb)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(fullfort_ijobb);
                        }
                        break;
                    case FULLFORT_EGEN:
                        if (eventIsMoreRecent(sisteTid, fullfort_egen)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(fullfort_egen);
                        }
                        break;
                    case FULLFORT_BEHANDLING:
                        if (eventIsMoreRecent(sisteTid, fullfort_behandling)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(fullfort_behandling);
                        }
                        break;
                    case FULLFORT_SOKEAVTALE:
                        if (eventIsMoreRecent(sisteTid, fullfort_sokeavtale)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(fullfort_sokeavtale);
                        }
                        break;
                    case AVBRUTT_STILLING:
                        if (eventIsMoreRecent(sisteTid, avbrutt_stilling)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(avbrutt_stilling);
                        }
                        break;
                    case AVBRUTT_IJOBB:
                        if (eventIsMoreRecent(sisteTid, avbrutt_ijobb)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(avbrutt_ijobb);
                        }
                        break;
                    case AVBRUTT_EGEN:
                        if (eventIsMoreRecent(sisteTid, avbrutt_egen)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(avbrutt_egen);
                        }
                        break;
                    case AVBRUTT_BEHANDLING:
                        if (eventIsMoreRecent(sisteTid, avbrutt_behandling)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(avbrutt_behandling);
                        }
                    case AVBRUTT_SOKEAVTALE:
                        if (eventIsMoreRecent(sisteTid, avbrutt_sokeavtale)) {
                            sisteKategori = kategori;
                            sisteTid = instantOrNull(avbrutt_sokeavtale);
                        }
                        break;
                }
            }
        }

        if (sisteTid != null){
            oppfolgingsBruker.setAggregert_siste_endring_kategori(sisteKategori);
            oppfolgingsBruker.setAggregert_siste_endring_tidspunkt(LocalDateTime.ofInstant(sisteTid, ZoneId.of("Europe/Oslo")));
        }
    }

    private boolean eventIsMoreRecent(Instant currentMostRecent, String comp) {
        return currentMostRecent == null || (comp != null && ZonedDateTime.parse(comp).toInstant().isAfter(currentMostRecent));
    }

    private Instant instantOrNull(String iso_8601) {
        if (iso_8601 == null) {
            return null;
        }
        try{//Feilet i github med: "2020-05-28T09:47:42.480+02:00"
            return ZonedDateTime.parse(iso_8601).toInstant();
        }catch (Exception e){
            System.out.println("Feilet med: " + iso_8601);
        }
        return null;
    }

}
