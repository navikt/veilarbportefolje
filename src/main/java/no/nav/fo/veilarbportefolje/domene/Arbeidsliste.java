package no.nav.fo.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.solr.common.SolrDocument;

import java.time.ZonedDateTime;
import java.util.Date;

import static no.nav.fo.veilarbportefolje.util.DateUtils.isRandomFutureDate;
import static no.nav.fo.veilarbportefolje.util.DateUtils.toZonedDateTime;


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

    public static Arbeidsliste of(SolrDocument brukerDokument) {

        Boolean arbeidslisteAktiv = (Boolean) brukerDokument.get("arbeidsliste_aktiv");
        VeilederId sistEndretAv = VeilederId.of((String) brukerDokument.get("arbeidsliste_sist_endret_av_veilederid"));
        ZonedDateTime endringstidspunkt = toZonedDateTime((Date) brukerDokument.get("arbeidsliste_endringstidspunkt"));
        String overskrift = (String) brukerDokument.get("arbeidsliste_overskrift");
        String kommentar = (String) brukerDokument.get("arbeidsliste_kommentar");
        ZonedDateTime frist = toZonedDateTime(dateIfNotSolrMax((Date) brukerDokument.get("arbeidsliste_frist")));

        return new Arbeidsliste(sistEndretAv, endringstidspunkt, overskrift, kommentar, frist)
                .setArbeidslisteAktiv(arbeidslisteAktiv);
    }

    private static Date dateIfNotSolrMax(Date date) {
        return isRandomFutureDate(date) ? null : date;
    }

    @JsonCreator
    public Arbeidsliste(@JsonProperty("sistEndretAv") VeilederId  sistEndretAv,
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
