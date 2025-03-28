package no.nav.pto.veilarbportefolje.arbeidsliste;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.DateUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;


@Data
@Accessors(chain = true)
@Getter
public class Arbeidsliste {

    public enum Kategori {
        BLA, GRONN, GUL, LILLA, LYSEBLA, ORANSJE
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
    String aktoerid;
    String navkontorForArbeidsliste;

    public Arbeidsliste(VeilederId sistEndretAv, ZonedDateTime endringstidspunkt, String overskrift, String kommentar, ZonedDateTime frist, Kategori kategori) {
        this.sistEndretAv = sistEndretAv;
        this.endringstidspunkt = endringstidspunkt;
        this.overskrift = overskrift;
        this.kommentar = kommentar;
        this.frist = frist;
        this.kategori = kategori;
    }

    public static Arbeidsliste of(OppfolgingsBruker bruker) {
        Boolean arbeidslisteAktiv = bruker.isArbeidsliste_aktiv();
        VeilederId sistEndretAv = VeilederId.of(bruker.getArbeidsliste_sist_endret_av_veilederid());
        String kategori = bruker.getArbeidsliste_kategori();
        Kategori arbeidslisteKategori = Optional.ofNullable(kategori).map(Kategori::valueOf).orElse(null);

        ZonedDateTime endringstidspunkt = null;
        if (bruker.getArbeidsliste_endringstidspunkt() != null) {
            Instant instant = Instant.parse(bruker.getArbeidsliste_endringstidspunkt());
            endringstidspunkt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        ZonedDateTime frist = null;
        if (bruker.getArbeidsliste_frist() != null) {
            frist = toZonedDateTime(dateIfNotFarInTheFutureDate(Instant.parse(bruker.getArbeidsliste_frist())));
        }

        return new Arbeidsliste(sistEndretAv, endringstidspunkt, null, null, frist, arbeidslisteKategori)
                .setArbeidslisteAktiv(arbeidslisteAktiv)
                .setNavkontorForArbeidsliste(bruker.getNavkontor_for_arbeidsliste());
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
                        @JsonProperty("harVeilederTilgang") Boolean harVeilederTilgang,
                        @JsonProperty("navkontorForArbeidsliste") String navkontorForArbeidsliste
    ) {
        this.sistEndretAv = sistEndretAv;
        this.endringstidspunkt = endringstidspunkt;
        this.overskrift = overskrift;
        this.kommentar = kommentar;
        this.frist = frist;
        this.isOppfolgendeVeileder = isOppfolgendeVeileder;
        this.arbeidslisteAktiv = arbeidslisteAktiv;
        this.harVeilederTilgang = harVeilederTilgang;
        this.kategori = kategori;
        this.navkontorForArbeidsliste = navkontorForArbeidsliste;
    }

    public Arbeidsliste(
            Kategori kategori
    ) {
        this.sistEndretAv = null;
        this.endringstidspunkt = null;
        this.overskrift = null;
        this.kommentar = null;
        this.frist = null;
        this.isOppfolgendeVeileder = null;
        this.arbeidslisteAktiv = null;
        this.harVeilederTilgang = null;
        this.kategori = kategori;
        this.navkontorForArbeidsliste = null;
    }
}
