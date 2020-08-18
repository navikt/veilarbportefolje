package no.nav.pto.veilarbportefolje.feed.common;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeedWebhookResponse {
    private String melding;
    public String webhookUrl;
}
