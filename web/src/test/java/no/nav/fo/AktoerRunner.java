package no.nav.fo;


import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdRequest;

import static no.nav.dialogarena.config.util.Util.setProperty;
import static no.nav.fo.StartJettyVeilArbPortefolje.APPLICATION_NAME;
import static no.nav.fo.StartJettyVeilArbPortefolje.SERVICE_USER_NAME;
import static no.nav.fo.config.LocalJndiContextConfig.setServiceUserCredentials;

public class AktoerRunner {

    public static void main(String[] args) throws Exception {
        String fnr = "DUMMY";
        String aktoerid = "DUMMY";

        setServiceUserCredentials(FasitUtils.getServiceUser(SERVICE_USER_NAME, APPLICATION_NAME, "t6"));
        System.setProperty("no.nav.modig.security.sts.url","https://sts-t6.test.local/SecurityTokenServiceProvider/");
        setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", InternbrukerSubjectHandler.class.getName());


        AktoerV2 soapService = factory();
        String hentetAktoerid = soapService.hentAktoerIdForIdent(new WSHentAktoerIdForIdentRequest().withIdent(fnr)).getAktoerId();
        String hentetFnr = soapService.hentIdentForAktoerId(new WSHentIdentForAktoerIdRequest().withAktoerId(aktoerid)).getIdent();
        System.out.println("fnr: "+fnr +" -> aktoerid: "+hentetAktoerid);
        System.out.println("aktoerid: "+aktoerid +" -> fnr: "+hentetFnr);

    }

    public static AktoerV2 factory() {
        return new CXFClient<>(AktoerV2.class)
                .address("https://app-t6.adeo.no/aktoerid/AktoerService/v2")
                .configureStsForSystemUserInFSS()
                .build();
    }

}
