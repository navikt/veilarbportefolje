package no.nav.pto.veilarbportefolje.arbeidsliste;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.DateUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_VIEW.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;


@Data
@Accessors(chain = true)
@Getter
@RequiredArgsConstructor
public class Arbeidsliste {

    public enum Kategori {
        BLA, GRONN, GUL, LILLA
    }

    final VeilederId sistEndretAv;
    final ZonedDateTime endringstidspunkt;
    final String overskrift;
    final String kommentar;
    final ZonedDateTime frist;
    final Kategori kategori;
    Boolean isOppfolgendeVeileder;
    Boolean arbeidslisteAktiv;
    Boolean harVeilederTilgang;

    public static Arbeidsliste of(OppfolgingsBruker bruker) {
        Boolean arbeidslisteAktiv = bruker.isArbeidsliste_aktiv();
        VeilederId sistEndretAv = VeilederId.of(bruker.getArbeidsliste_sist_endret_av_veilederid());
        String kategori = bruker.getArbeidsliste_kategori();
        Kategori arbeidslisteKategori = Optional.ofNullable(kategori).map(Kategori::valueOf).orElse(null);

        ZonedDateTime endringstidspunkt = null;
        if (bruker.getArbeidsliste_endringstidspunkt() != null) {
            Instant instant = Instant.parse(bruker.getArbeidsliste_endringstidspunkt());
            endringstidspunkt = ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/Oslo"));
        }

        String overskrift = bruker.getArbeidsliste_overskrift();
        String kommentar = bruker.getArbeidsliste_kommentar();

        ZonedDateTime frist = null;
        if (bruker.getArbeidsliste_frist() != null) {
            frist = toZonedDateTime(dateIfNotFarInTheFutureDate(Instant.parse(bruker.getArbeidsliste_frist())));
        }

        return new Arbeidsliste(sistEndretAv, endringstidspunkt, overskrift, kommentar, frist, arbeidslisteKategori)
                .setArbeidslisteAktiv(arbeidslisteAktiv);
    }

    public static Arbeidsliste of(Map<String, Object> row) {
        ZonedDateTime endringstidspunkt = toZonedDateTime((Timestamp) row.get(ARB_ENDRINGSTIDSPUNKT));
        ZonedDateTime frist = toZonedDateTime((Timestamp) row.get(ARB_FRIST));
        VeilederId sistEndretAv = VeilederId.of((String) row.get(ARB_SIST_ENDRET_AV_VEILEDERIDENT));
        Kategori arbeidslisteKategori = Optional.ofNullable((String) row.get(ARB_KATEGORI)).map(Kategori::valueOf).orElse(null);
        String overskrift = (String) row.get(ARB_OVERSKRIFT);
        String kommentar = (String) row.get(ARB_KOMMENTAR);

        return new Arbeidsliste(sistEndretAv, endringstidspunkt, overskrift, kommentar, frist, arbeidslisteKategori)
                .setArbeidslisteAktiv(endringstidspunkt == null);
    }

    private static Date dateIfNotFarInTheFutureDate(Instant instant) {
        return DateUtils.isFarInTheFutureDate(instant) ? null : Date.from(instant);
    }

    @JsonCreator
    public Arbeidsliste(@JsonProperty("sistEndretAv") VeilederId sistEndretAv,
                        @JsonProperty("endringstidspunkt") ZonedDateTime endringstidspunkt,
                        @JsonProperty("overskrift") String overskrift,
                        @JsonProperty("kommentar") String kommentar,
                        @JsonProperty("frist") ZonedDateTime frist,
                        @JsonProperty("isOppfolgendeVeileder") Boolean isOppfolgendeVeileder,
                        @JsonProperty("arbeidslisteAktiv") Boolean arbeidslisteAktiv,
                        @JsonProperty("kategori") Kategori kategori,
                        @JsonProperty("harVeilederTilgang") Boolean harVeilederTilgang) {
        this.sistEndretAv = sistEndretAv;
        this.endringstidspunkt = endringstidspunkt;
        this.overskrift = overskrift;
        this.kommentar = kommentar;
        this.frist = frist;
        this.isOppfolgendeVeileder = isOppfolgendeVeileder;
        this.arbeidslisteAktiv = arbeidslisteAktiv;
        this.harVeilederTilgang = harVeilederTilgang;
        this.kategori = kategori;
    }
}
