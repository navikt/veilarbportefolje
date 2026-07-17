package no.nav.pto.veilarbportefolje.lagredefilter.minefilter

import no.nav.common.json.JsonUtils
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

private val objectMapper = JsonUtils.getMapper()

private val filtervalgDefaults: Filtervalg by lazy { byggFiltervalgDefaults() }

fun ekstraherAktiveFiltervalg(filtervalg: Filtervalg): ObjectNode {
    val fullNode = objectMapper.valueToTree<ObjectNode>(filtervalg)
    val defaultsNode = objectMapper.valueToTree<ObjectNode>(filtervalgDefaults)

    val aktive = objectMapper.createObjectNode()
    fullNode.properties().forEach { (navn, verdi) ->
        if (erAktivtFelt(navn, verdi, defaultsNode.get(navn), filtervalg)) {
            aktive.set(navn, verdi)
        }
    }
    return aktive
}

private fun erAktivtFelt(
    navn: String,
    verdi: JsonNode,
    defaultVerdi: JsonNode?,
    filtervalg: Filtervalg
): Boolean {
    if (navn == Filtervalg::aktiviteter.name) {
        return filtervalg.harAktiviteterAvansert()
    }
    return defaultVerdi == null || verdi != defaultVerdi
}

fun rekonstruerFiltervalgFraAktive(aktive: JsonNode): Filtervalg {
    val merged = objectMapper.valueToTree<ObjectNode>(filtervalgDefaults)
    aktive.properties().forEach { (navn, verdi) ->
        merged.set(navn, verdi)
    }
    return objectMapper.treeToValue(merged, Filtervalg::class.java)
}

private fun byggFiltervalgDefaults(): Filtervalg {
    val ctor = Filtervalg::class.primaryConstructor
        ?: error("Filtervalg må ha en primær-konstruktør")

    val args = ctor.parameters.associateWith { param -> defaultForType(param.type) }
    return ctor.callBy(args)
}

private fun defaultForType(type: KType): Any? {
    if (type.isMarkedNullable) return null
    val classifier = type.classifier as? KClass<*> ?: return null
    return when {
        classifier == String::class -> ""
        classifier.isSubclassOf(List::class) -> emptyList<Any>()
        classifier.isSubclassOf(Set::class) -> emptySet<Any>()
        classifier.isSubclassOf(Map::class) -> emptyMap<Any, Any>()
        else -> null
    }
}
