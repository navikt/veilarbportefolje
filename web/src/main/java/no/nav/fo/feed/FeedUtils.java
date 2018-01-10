package no.nav.fo.feed;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Oppfolgingstatus;
import no.nav.fo.domene.PersonId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.OppfolgingUtils.erBrukerUnderOppfolging;

public class FeedUtils {

    public static List<PersonId> getPresentPersonids(Map<AktoerId, Optional<PersonId>> identMap) {
        return identMap.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    public static Map<Boolean, List<Tuple2<AktoerId, PersonId>>> getErUnderOppfolging(List<AktoerId> aktoerids, Map<AktoerId, Optional<PersonId>> aktoeridToPersonid,
                                                                                      Map<PersonId, Oppfolgingstatus> oppfolgingstatus) {
        return aktoerids.stream()
                .filter((aktoerId) -> aktoeridToPersonid.get(aktoerId).isPresent())
                .map((aktoerId) -> Tuple.of(aktoerId, aktoeridToPersonid.get(aktoerId).get()))
                .collect(groupingBy(
                        identTuple -> erBrukerUnderOppfolging(oppfolgingstatus.get(aktoeridToPersonid.get(identTuple._1()).get()))
                ));
    }

    public static <T> List<T> finnBrukere(Map<Boolean, List<Tuple2<AktoerId, PersonId>>> brukere, Boolean underOppfolging, Function<Tuple2<AktoerId, PersonId>, T> extract) {
        return brukere
                .getOrDefault(underOppfolging, emptyList())
                .stream()
                .map(extract)
                .collect(toList());
    }
}
