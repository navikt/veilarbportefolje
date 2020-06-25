package no.nav.pto.veilarbportefolje.feedconsumer;

import no.nav.common.auth.Subject;
import no.nav.pto.veilarbportefolje.feed.common.FeedAuthorizationModule;
import java.util.List;

import static no.nav.pto.veilarbportefolje.feedconsumer.Utils.getCommaSeparatedUsers;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class OidcFeedAuthorizationModule implements FeedAuthorizationModule {

    @Override
    public boolean isRequestAuthorized(String feedname) {
        return no.nav.common.auth.SubjectHandler.getSubject().map(Subject::getUid).map(String::toLowerCase).map(username -> {
            String allowedUsersString = getRequiredProperty(feedname + ".feed.brukertilgang");
            List<String> allowedUsers = getCommaSeparatedUsers(allowedUsersString);
            return allowedUsers.contains(username);
        }).orElse(false);
    }
}
