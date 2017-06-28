package no.nav.fo.domene;

import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.solr.common.SolrDocument;

import java.time.LocalDateTime;
import java.util.Date;

import static no.nav.fo.util.DateUtils.toLocalDateTime;


@Data
@Accessors(chain = true)
@Getter
public class Arbeidsliste {
    final VeilederId sistEndretAv;
    final LocalDateTime endringstidspunkt;
    final String kommentar;
    final LocalDateTime frist;
    Boolean isOppfolgendeVeileder;

    public static Arbeidsliste of(SolrDocument brukerDokument) {

        VeilederId sistEndretAv = new VeilederId((String) brukerDokument.get("arbeidsliste_sistendretavveileder"));
        LocalDateTime endringstidspunkt = toLocalDateTime((Date) brukerDokument.get("arbeidsliste_endringstidspunkt"));
        String kommentar = (String) brukerDokument.get("arbeidsliste_kommentar");
        LocalDateTime frist = toLocalDateTime((Date) brukerDokument.get("arbeidsliste_frist"));
        Boolean isOppfolgendeVeileder = (Boolean) brukerDokument.get("arbeidsliste_er_oppfolgende_veileder");

        return new Arbeidsliste(sistEndretAv, endringstidspunkt, kommentar, frist)
                .setIsOppfolgendeVeileder(isOppfolgendeVeileder);
    }
}
