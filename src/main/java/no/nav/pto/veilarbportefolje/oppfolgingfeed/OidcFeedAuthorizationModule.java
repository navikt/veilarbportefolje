package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import no.nav.common.auth.subject.Subject;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.pto.veilarbportefolje.feed.common.FeedAuthorizationModule;
import java.util.List;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.pto.veilarbportefolje.oppfolgingfeed.Utils.getCommaSeparatedUsers;
public class OidcFeedAuthorizationModule implements FeedAuthorizationModule {

    @Override
    public boolean isRequestAuthorized(String feedname) {
        return SubjectHandler.getSubject().map(Subject::getUid).map(String::toLowerCase).map(username -> {
            String allowedUsersString = getRequiredProperty(feedname + ".feed.brukertilgang");
            List<String> allowedUsers = getCommaSeparatedUsers(allowedUsersString);
            return allowedUsers.contains(username);
        }).orElse(false);
    }
}
