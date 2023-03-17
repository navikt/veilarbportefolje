package no.nav.pto.veilarbportefolje.ensligforsorger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.*;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.VedtakOvergangsstønadArbeidsoppfølging;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.pto.veilarbportefolje.util.DateUtils.fnrToDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
public class EnsligeForsorgereRepository {
    private final JdbcTemplate db;

    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;

    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate namedDb;

    private final Cache<String, Integer> stonadstypeCache = Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.MINUTES)
            .maximumSize(10)
            .build();
    private final Cache<String, Integer> vedtakresultatCache = Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.MINUTES)
            .maximumSize(10)
            .build();

    private final Cache<String, Integer> periodeTypeCache = Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.MINUTES)
            .maximumSize(10)
            .build();

    private final Cache<String, Integer> vettakAktivitetstypeCache = Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.MINUTES)
            .maximumSize(10)
            .build();

    public void lagreOvergangsstonad(VedtakOvergangsstønadArbeidsoppfølging vedtakOvergangsstønadArbeidsoppfølging) {

        long vedtakId = vedtakOvergangsstønadArbeidsoppfølging.vedtakId();
        Integer stonadstypeId = hentStonadstype(vedtakOvergangsstønadArbeidsoppfølging.stønadstype().toString());
        Integer vedtakResultatId = hentVedtakresultat(vedtakOvergangsstønadArbeidsoppfølging.vedtaksresultat().toString());

        String sql = """
                INSERT INTO ENSLIGE_FORSORGERE(VEDTAKID, PERSONIDENT, STONADSTYPE, VEDTAKSRESULTAT)
                VALUES (?, ?, ?, ?) ON CONFLICT (VEDTAKID)
                DO UPDATE SET(PERSONIDENT, STONADSTYPE, VEDTAKSRESULTAT)
                = (excluded.PERSONIDENT, excluded.STONADSTYPE, excluded.VEDTAKSRESULTAT);
                """;
        db.update(sql, vedtakId, vedtakOvergangsstønadArbeidsoppfølging.personIdent(),
                stonadstypeId, vedtakResultatId);

        List<EnsligeForsorgereVedtakPeriode> ensligeForsorgereVedtakPerioder = vedtakOvergangsstønadArbeidsoppfølging.periode().stream().map(vedtakPeriode -> new EnsligeForsorgereVedtakPeriode(vedtakId, vedtakPeriode.fom(), vedtakPeriode.tom(), hentPeriodetype(vedtakPeriode.periodetype().toString()), hentVedtakAktivitetstype(vedtakPeriode.aktivitetstype().toString()))).toList();
        lagreEnsligeForsorgereVedtakPerioder(vedtakId, ensligeForsorgereVedtakPerioder);

        List<EnsligeForsorgereBarn> enslige_forsorgere_barn = vedtakOvergangsstønadArbeidsoppfølging.barn().stream().map(enslige_forsorgere_barn_dto -> new EnsligeForsorgereBarn(enslige_forsorgere_barn_dto.fødselsnummer(), enslige_forsorgere_barn_dto.termindato())).collect(Collectors.toList());
        lagreDataForEnsligeForsorgereBarn(vedtakId, enslige_forsorgere_barn);
    }

    private void lagreEnsligeForsorgereVedtakPerioder(long vedtakId, List<EnsligeForsorgereVedtakPeriode> vedtakPerioder) {
        if (!vedtakPerioder.isEmpty()) {
            vedtakPerioder.forEach(period -> lagreEnsligeForsorgereVedtakPeriod(vedtakId, period.periode_fra(), period.periode_til(), period.periodetype(), period.aktivitetstype()));
        }
    }

    private void lagreEnsligeForsorgereVedtakPeriod(long vedtakid, LocalDate period_fom, LocalDate period_tom, Integer periodeType, Integer aktivitetsType) {
        String sql = """
                INSERT INTO enslige_forsorgere_periode(VEDTAKID, FRA_DATO, TIL_DATO, PERIODETYPE, AKTIVITETSTYPE)
                VALUES (?, ?, ?, ?, ?) ON CONFLICT (VEDTAKID)
                DO UPDATE SET(FRA_DATO, TIL_DATO, PERIODETYPE, AKTIVITETSTYPE) =
                (excluded.fra_dato, excluded.til_dato, excluded.periodetype, excluded.aktivitetstype)
                """;
        db.update(sql, vedtakid, period_fom, period_tom, periodeType, aktivitetsType);
    }

    private void lagreDataForEnsligeForsorgereBarn(long vedtakId, List<EnsligeForsorgereBarn> enslige_forsorgere_barn) {
        if (enslige_forsorgere_barn.isEmpty()) {
            return;
        }
        String sql = """
                DELETE FROM enslige_forsorgere_barn WHERE vedtakid = ?
                """;
        db.update(sql, vedtakId);

        enslige_forsorgere_barn.forEach(enslige_forsorgere_barnet ->
                lagreDataForEnsligeForsorgereBarnIDB(vedtakId, enslige_forsorgere_barnet.fnr(), enslige_forsorgere_barnet.terminDato())
        );
    }

    private void lagreDataForEnsligeForsorgereBarnIDB(long vedtakId, String fnr, LocalDate terminDato) {
        String sql = """
                INSERT INTO enslige_forsorgere_barn(VEDTAKID, FNR, TERMINDATO)
                VALUES (?, ?, ?)
                """;
        db.update(sql, vedtakId, fnr, terminDato);
    }

    private Integer hentStonadstype(String stonadTypeConst) {
        return tryCacheFirst(stonadstypeCache, stonadTypeConst,
                () -> this.hentStonadstypeFraDB(stonadTypeConst));
    }

    private Integer hentStonadstypeFraDB(String stonadTypeConst) {
        String sql = "SELECT ID FROM enslige_forsorgere_STONAD_TYPE WHERE STONAD_TYPE = :stonadType::varchar";
        Optional<Integer> stonadTypeIdOptional = Optional.of(namedDb.queryForObject(sql, new MapSqlParameterSource("stonadType", stonadTypeConst), Integer.class));

        return stonadTypeIdOptional.orElseGet(() -> lagreStonadstype(stonadTypeConst));
    }

    public Integer lagreStonadstype(String stonadTypeConst) {
        try {
            GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
            db.update(conn -> {
                PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO enslige_forsorgere_stonad_type(stonad_type) VALUES (?) RETURNING id", Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, stonadTypeConst);
                return preparedStatement;
            }, generatedKeyHolder);
            return generatedKeyHolder.getKey().intValue();
        } catch (Exception e) {
            throw new RuntimeException("Kan ikke lagre stonadstype " + e);
        }
    }

    private Integer hentVedtakresultat(String vedtakResultatConst) {
        return tryCacheFirst(vedtakresultatCache, vedtakResultatConst, () -> this.hentVedtakresultatFraDB(vedtakResultatConst));
    }

    private Integer hentVedtakresultatFraDB(String vedtakResultatConst) {
        String sql = "SELECT ID FROM enslige_forsorgere_vedtaksresultat_type WHERE vedtaksresultat_type = :vedtaksresultat_type::varchar";
        Optional<Integer> vedtakResultatTypeIdOptional = Optional.of(namedDb.queryForObject(sql, new MapSqlParameterSource("vedtaksresultat_type", vedtakResultatConst), Integer.class));

        return vedtakResultatTypeIdOptional.orElseGet(() -> lagreVedtakresultat(vedtakResultatConst));
    }

    public Integer lagreVedtakresultat(String vedtakResultatTypeConst) {
        try {
            GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
            db.update(conn -> {
                PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO enslige_forsorgere_vedtaksresultat_type(vedtaksresultat_type) VALUES (?) RETURNING id", Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, vedtakResultatTypeConst);
                return preparedStatement;
            }, generatedKeyHolder);
            return generatedKeyHolder.getKey().intValue();
        } catch (Exception e) {
            throw new RuntimeException("Kan ikke lagre vedtakresultat " + e);
        }
    }

    private Integer hentPeriodetype(String periodeTypeConst) {
        return tryCacheFirst(periodeTypeCache, periodeTypeConst, () -> this.hentPeriodetypeFraDB(periodeTypeConst));
    }

    private Integer hentPeriodetypeFraDB(String periodeTypeConst) {
        String sql = "SELECT ID FROM enslige_forsorgere_vedtaksperiode_type WHERE periode_type = :periode_type::varchar";
        Optional<Integer> vedtakPeriodetypeIdOptional = Optional.of(namedDb.queryForObject(sql, new MapSqlParameterSource("periode_type", periodeTypeConst), Integer.class));

        return vedtakPeriodetypeIdOptional.orElseGet(() -> lagreVedtakperiodeType(periodeTypeConst));
    }

    public Integer lagreVedtakperiodeType(String periodeTypeConst) {
        try {
            GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
            db.update(conn -> {
                PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO enslige_forsorgere_vedtaksperiode_type(periode_type) VALUES (?) RETURNING id", Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, periodeTypeConst);
                return preparedStatement;
            }, generatedKeyHolder);
            return generatedKeyHolder.getKey().intValue();
        } catch (Exception e) {
            throw new RuntimeException("Kan ikke lagre vedtak periodetype " + e);
        }
    }

    private Integer hentVedtakAktivitetstype(String aktivitetsType) {
        return tryCacheFirst(vettakAktivitetstypeCache, aktivitetsType, () -> this.hentVedtakAktivitetstypeFraDB(aktivitetsType));
    }

    private Integer hentVedtakAktivitetstypeFraDB(String aktivitetsType) {
        String sql = "SELECT ID FROM enslige_forsorgere_aktivitet_type WHERE aktivitet_type = :aktivitet_type::varchar";
        Optional<Integer> vedtakAktivitetstypeIdOptional = Optional.of(namedDb.queryForObject(sql, new MapSqlParameterSource("aktivitet_type", aktivitetsType), Integer.class));

        return vedtakAktivitetstypeIdOptional.orElseGet(() -> lagreVedtakAktivitetstype(aktivitetsType));
    }

    public Integer lagreVedtakAktivitetstype(String aktivitetsType) {
        try {
            GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
            db.update(conn -> {
                PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO enslige_forsorgere_aktivitet_type(aktivitet_type) VALUES (?) RETURNING id", Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, aktivitetsType);
                return preparedStatement;
            }, generatedKeyHolder);
            return generatedKeyHolder.getKey().intValue();
        } catch (Exception e) {
            throw new RuntimeException("Kan ikke lagre vedtak aktivitetstype " + e);
        }
    }


    public Optional<EnsligeForsorgerOvergangsstønadTiltak> hentOvergangsstønadForEnsligeForsorger(String personIdent) {
        String sql = """
                 SELECT ef.personIdent, ef.vedtakId, efp.fra_dato, efp.til_dato,
                       vperiode_type.PERIODE_TYPE as vedtaksPeriodeType,
                       EAT.AKTIVITET_TYPE as aktivitetsType
                FROM enslige_forsorgere ef
                JOIN enslige_forsorgere_periode efp on ef.vedtakid = efp.vedtakid
                LEFT JOIN enslige_forsorgere_VEDTAKSPERIODE_TYPE vperiode_type on efp.PERIODETYPE = vperiode_type.ID
                LEFT JOIN enslige_forsorgere_AKTIVITET_TYPE EAT on efp.AKTIVITETSTYPE = EAT.ID
                LEFT JOIN enslige_forsorgere_VEDTAKSRESULTAT_TYPE EVT on ef.VEDTAKSRESULTAT = EVT.ID
                LEFT JOIN enslige_forsorgere_STONAD_TYPE EST on EST.ID = ef.STONADSTYPE
                WHERE est.STONAD_TYPE = ?
                  AND EVT.VEDTAKSRESULTAT_TYPE = ?
                  AND ef.personIdent = ? LIMIT 1;
                 """;

        return dbReadOnly.queryForList(sql, Stønadstype.OVERGANGSSTØNAD.toString(), Vedtaksresultat.INNVILGET.toString(), personIdent)
                .stream().map(this::mapTilTiltak)
                .findFirst();
    }

    public List<EnsligeForsorgerOvergangsstønadTiltak> hentOvergangsstønadForEnsligeForsorger(List<Fnr> personIdenter) {
        String personIdenterStr = personIdenter.stream().map(Fnr::get).collect(Collectors.joining(",", "{", "}"));
        String sql = """
                 SELECT ef.personIdent, ef.vedtakId, efp.fra_dato, efp.til_dato,
                       vperiode_type.PERIODE_TYPE as vedtaksPeriodeType,
                       EAT.AKTIVITET_TYPE as aktivitetsType
                FROM enslige_forsorgere ef
                JOIN enslige_forsorgere_periode efp on ef.vedtakid = efp.vedtakid
                LEFT JOIN enslige_forsorgere_VEDTAKSPERIODE_TYPE vperiode_type on efp.PERIODETYPE = vperiode_type.ID
                LEFT JOIN enslige_forsorgere_AKTIVITET_TYPE EAT on efp.AKTIVITETSTYPE = EAT.ID
                LEFT JOIN enslige_forsorgere_VEDTAKSRESULTAT_TYPE EVT on ef.VEDTAKSRESULTAT = EVT.ID
                LEFT JOIN enslige_forsorgere_STONAD_TYPE EST on EST.ID = ef.STONADSTYPE
                WHERE est.STONAD_TYPE = ?
                  AND EVT.VEDTAKSRESULTAT_TYPE = ?
                  AND ef.personIdent = ANY (?::varchar[]);
                 """;

        return dbReadOnly.queryForList(sql, Stønadstype.OVERGANGSSTØNAD.toString(), Vedtaksresultat.INNVILGET.toString(), personIdenterStr)
                .stream().map(this::mapTilTiltak)
                .collect(Collectors.toList());
    }

    public Optional<LocalDate> hentYngsteBarn(Long vedtakId) {
        String sql = """
                SELECT fnr, termindato FROM enslige_forsorgere_barn WHERE vedtakid = ?
                """;
        return dbReadOnly.queryForList(sql, vedtakId).
                stream().map(this::mapTilBarn)
                .map(barn -> hentBarnetsFødselsdato(vedtakId, barn.fnr(), barn.terminDato()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(LocalDate::compareTo);
    }

    private Optional<LocalDate> hentBarnetsFødselsdato(Long vedtakId, String fnr, LocalDate terminDato) {
        if (fnr != null) {
            try {
                return Optional.of(fnrToDate(fnr));
            } catch (ParseException e) {
                secureLog.warn("Kan ikke parse fnr for ef barn for vedtakId: " + vedtakId);

            }
        } else if (terminDato != null) {
            return Optional.of(terminDato);
        }
        return Optional.empty();

    }


    @SneakyThrows
    private EnsligeForsorgerOvergangsstønadTiltak mapTilTiltak(Map<String, Object> rs) {
        return new EnsligeForsorgerOvergangsstønadTiltak(
                Fnr.of((String) rs.get("personIdent")),
                (Long) rs.get("vedtakId"),
                Periodetype.valueOf((String) rs.get("vedtaksPeriodeType")),
                Aktivitetstype.valueOf((String) rs.get("aktivitetsType")),
                toLocalDateOrNull(String.valueOf(rs.get("fra_dato"))),
                toLocalDateOrNull(String.valueOf(rs.get("til_dato"))));
    }

    @SneakyThrows
    private EnsligeForsorgereBarn mapTilBarn(Map<String, Object> rs) {
        return new EnsligeForsorgereBarn((String) rs.get("fnr"), toLocalDateOrNull((java.sql.Date) rs.get("termindato")));
    }

}
