package no.nav.fo.util;


import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.VeilederId;

import javax.ws.rs.InternalServerErrorException;

public class OppfolgingUtils {

    public static boolean erVeielderNyEllerOppfolgendeVeileder(AktoerId aktoerId, VeilederId veilederId, BrukerRepository brukerRepository) {
        VeilederId gjeldendeVeileder = brukerRepository.retrieveVeileder(aktoerId)
                .getOrElseThrow(() -> new InternalServerErrorException());

        return gjeldendeVeileder == null || gjeldendeVeileder.equals(veilederId);
    }

    public static boolean skalArbeidslisteSlettes(BrukerOppdatertInformasjon bruker, BrukerRepository brukerRepository) {
        return bruker.getOppfolging() == null ||
                !bruker.getOppfolging() ||
                bruker.getVeileder() == null ||
                !erVeielderNyEllerOppfolgendeVeileder(new AktoerId(bruker.getAktoerid()), new VeilederId(bruker.getVeileder()), brukerRepository);
    }
}
