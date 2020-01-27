package no.nav.pto.veilarbportefolje.arbeidsliste;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.DateUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;


@Data
@Accessors(chain = true)
@Getter
@RequiredArgsConstructor
public class Arbeidsliste {
    final VeilederId sistEndretAv;
    final ZonedDateTime endringstidspunkt;
    final String overskrift;
    final String kommentar;
    final ZonedDateTime frist;
    Boolean isOppfolgendeVeileder;
    Boolean arbeidslisteAktiv;
    Boolean harVeilederTilgang;

    public static Arbeidsliste of(OppfolgingsBruker bruker) {
        Boolean arbeidslisteAktiv = bruker.isArbeidsliste_aktiv();
        VeilederId sistEndretAv = VeilederId.of(bruker.getArbeidsliste_sist_endret_av_veilederid());

        ZonedDateTime endringstidspunkt = null;
        if (bruker.getArbeidsliste_endringstidspunkt() != null) {
            Instant instant = Instant.parse(bruker.getArbeidsliste_endringstidspunkt());
            endringstidspunkt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        String overskrift = bruker.getArbeidsliste_overskrift();
        String kommentar = bruker.getArbeidsliste_kommentar();

        ZonedDateTime frist = null;
        if (bruker.getArbeidsliste_frist() != null) {
            frist = toZonedDateTime(dateIfNotFarInTheFutureDate(Instant.parse(bruker.getArbeidsliste_frist())));
        }

        return new Arbeidsliste(sistEndretAv, endringstidspunkt, overskrift, kommentar, frist)
                .setArbeidslisteAktiv(arbeidslisteAktiv);
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
                        @JsonProperty("harVeilederTilgang") Boolean harVeilederTilgang) {
        this.sistEndretAv = sistEndretAv;
        this.endringstidspunkt = endringstidspunkt;
        this.overskrift = overskrift;
        this.kommentar = kommentar;
        this.frist = frist;
        this.isOppfolgendeVeileder = isOppfolgendeVeileder;
        this.arbeidslisteAktiv = arbeidslisteAktiv;
        this.harVeilederTilgang = harVeilederTilgang;
    }
}
