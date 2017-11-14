package no.nav.fo.feed;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Oppfolgingstatus;
import no.nav.fo.domene.PersonId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.nav.fo.util.OppfolgingUtils.erBrukerUnderOppfolging;

public class FeedUtils {

    public static List<PersonId> getPresentPersonids(Map<AktoerId, Optional<PersonId>> identMap) {
        return identMap.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    public static Map<Tuple2<AktoerId, PersonId>, Boolean> getErUnderOppfolging(List<AktoerId> aktoerids, Map<AktoerId, Optional<PersonId>> aktoeridToPersonid,
                                                                          Map<PersonId, Oppfolgingstatus> oppfolgingstatus) {
        return aktoerids.stream()
                .filter(a -> aktoeridToPersonid.get(a).isPresent())
                .collect(toMap(
                        a -> Tuple.of(a, aktoeridToPersonid.get(a).get()),
                        a -> erBrukerUnderOppfolging(oppfolgingstatus.get(aktoeridToPersonid.get(a).get()))));
    }
}
