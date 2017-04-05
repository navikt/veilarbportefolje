package utils

case class RequestFilter(
    alder: List[String] = List(),
    fodselsdagIMnd: List[String] = List(),
    formidlingsgruppe: List[String] = List(),
    servicegruppe: List[String] = List(),
    kjonn: List[String] = List(),
    inaktiveBrukere: Boolean = false,
    rettighetsgruppe: List[String] = List(),
    innsatsgruppe: List[String] = List(),
    nyeBrukere: Boolean = false,
    ytelse: String = null
)

