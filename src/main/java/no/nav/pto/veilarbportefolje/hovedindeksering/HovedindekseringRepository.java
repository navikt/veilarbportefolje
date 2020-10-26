package no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.PersonId;
import no.nav.sbl.sql.SqlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.util.DbUtils.not;
import static no.nav.pto.veilarbportefolje.util.StreamUtils.batchProcess;
import static no.nav.sbl.sql.SqlUtils.update;
import static no.nav.sbl.sql.where.WhereClause.in;

@Repository
public class HovedindekseringRepository {

    private final JdbcTemplate db;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public HovedindekseringRepository(JdbcTemplate db, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.db = db;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public void upsertBrukerdata(Brukerdata brukerdata) {
        brukerdata.toUpsertQuery(db).execute();
    }

    public void insertOrUpdateBrukerdata(List<Brukerdata> brukerdata, Collection<String> finnesIDb) {
        Map<Boolean, List<Brukerdata>> eksisterendeBrukere = brukerdata
                .stream()
                .collect(groupingBy((data) -> finnesIDb.contains(data.getPersonid())));

        Brukerdata.batchUpdate(db, eksisterendeBrukere.getOrDefault(true, emptyList()));

        eksisterendeBrukere
                .getOrDefault(false, emptyList())
                .forEach(this::upsertBrukerdata);
    }

    public List<Brukerdata> retrieveBrukerdata(List<String> personIds) {
        return SqlUtils.select(db, Table.BRUKER_DATA.TABLE_NAME, BrukerRepository::toBrukerData)
                .column("*")
                .where(in(Table.BRUKER_DATA.PERSONID, personIds))
                .executeToList();
    }

    public void slettYtelsesdata() {
        update(db, "bruker_data")
                .set("ytelse", (Object) null)
                .set("utlopsdato", (Object) null)
                .set("utlopsdatoFasett", (Object) null)
                .set("dagputlopuke", (Object) null)
                .set("dagputlopukefasett", (Object) null)
                .set("permutlopuke", (Object) null)
                .set("permutlopukefasett", (Object) null)
                .set("aapmaxtiduke", (Object) null)
                .set("aapmaxtidukefasett", (Object) null)
                .set("aapunntakdagerigjen", (Object) null)
                .set("aapunntakukerigjenfasett", (Object) null)
                .execute();
    }

    public Map<Fnr, Optional<PersonId>> hentPersonidsForFnrs(List<Fnr> fnrs) {
        Map<Fnr, Optional<PersonId>> typeMap = new HashMap<>();
        Map<String, Optional<String>> stringMap = retrievePersonidFromFnrs(fnrs.stream().map(Fnr::toString).collect(toList()));
        stringMap.forEach((key, value) -> typeMap.put(new Fnr(key), value.map(PersonId::of)));
        return typeMap;
    }

    public Map<String, Optional<String>> retrievePersonidFromFnrs(Collection<String> fnrs) {
        Map<String, Optional<String>> brukere = new HashMap<>(fnrs.size());

        batchProcess(1000, fnrs, (fnrBatch) -> {
            Map<String, Object> params = new HashMap<>();
            params.put("fnrs", fnrBatch);
            String sql = getPersonIdsFromFnrsSQL();
            Map<String, Optional<String>> fnrPersonIdMap = namedParameterJdbcTemplate.queryForList(sql, params)
                    .stream()
                    .map((rs) -> Tuple.of(
                            (String) rs.get("FODSELSNR"),
                            rs.get("PERSON_ID").toString())
                    )
                    .collect(Collectors.toMap(Tuple2::_1, personData -> Optional.of(personData._2())));

            brukere.putAll(fnrPersonIdMap);
        });

        fnrs.stream()
                .filter(not(brukere::containsKey))
                .forEach((ikkeFunnetBruker) -> brukere.put(ikkeFunnetBruker, empty()));

        return brukere;
    }

    private String getPersonIdsFromFnrsSQL() {
        return
                "SELECT " +
                "person_id, " +
                "fodselsnr " +
                "FROM " +
                "OPPFOLGINGSBRUKER " +
                "WHERE " +
                "fodselsnr in (:fnrs)";
    }
}
