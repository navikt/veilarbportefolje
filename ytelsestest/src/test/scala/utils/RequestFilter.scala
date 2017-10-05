package utils

case class RequestFilter(
    aktiviteter: AktivitetRequestFilter = AktivitetRequestFilter(),
    alder: List[String] = List (),
    brukerstatus: String = "NYE_BRUKERE",
    fodselsdagIMnd: List[String] = List (),
    formidlingsgruppe: List[String] = List (),
    innsatsgruppe: List[String] = List (),
    kjonn: List[String] = List (),
    rettighetsgruppe: List[String] = List (),
    servicegruppe: List[String] = List (),
    veiledere: List[String] = List (),
    ytelse: String = null
)

