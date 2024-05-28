--
-- PostgreSQL database dump
--

-- Dumped from database version 11.5 (Debian 11.5-3.pgdg90+1)
-- Dumped by pg_dump version 11.5 (Debian 11.5-3.pgdg90+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: bruker_identer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_identer (
    person character varying(25) NOT NULL,
    ident character varying(30) NOT NULL,
    historisk boolean NOT NULL,
    gruppe character varying(30) NOT NULL
);


--
-- Name: aktive_identer; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.aktive_identer AS
 SELECT bi_a.ident AS aktorid,
    bi_f.ident AS fnr
   FROM (public.bruker_identer bi_a
     JOIN public.bruker_identer bi_f ON (((bi_a.person)::text = (bi_f.person)::text)))
  WHERE (((bi_a.gruppe)::text = 'AKTORID'::text) AND (NOT bi_a.historisk) AND ((bi_f.gruppe)::text = 'FOLKEREGISTERIDENT'::text) AND (NOT bi_f.historisk));


--
-- Name: aktiviteter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktiviteter (
    aktivitetid character varying(20) NOT NULL,
    aktoerid character varying(20) NOT NULL,
    aktivitettype character varying(255),
    avtalt boolean DEFAULT true,
    fradato timestamp without time zone,
    tildato timestamp without time zone,
    status character varying(255),
    version bigint,
    cv_kan_deles_status character varying(20),
    svarfrist_stilling_fra_nav date
);


--
-- Name: arbeidsliste; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeidsliste (
    aktoerid character varying(20) NOT NULL,
    sist_endret_av_veilederident character varying(20),
    kommentar character varying(1000),
    frist timestamp without time zone,
    endringstidspunkt timestamp without time zone,
    overskrift character varying(500),
    kategori character varying(10),
    nav_kontor_for_arbeidsliste character varying(24)
);


--
-- Name: arbeidsliste_kopi_2023_12_19; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeidsliste_kopi_2023_12_19 (
    aktoerid character varying(20),
    sist_endret_av_veilederident character varying(20),
    kommentar character varying(1000),
    frist timestamp without time zone,
    endringstidspunkt timestamp without time zone,
    overskrift character varying(500),
    kategori character varying(10),
    nav_kontor_for_arbeidsliste character varying(24)
);


--
-- Name: arbeidsliste_kopi_2024_01_16; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeidsliste_kopi_2024_01_16 (
    aktoerid character varying(20),
    sist_endret_av_veilederident character varying(20),
    kommentar character varying(1000),
    frist timestamp without time zone,
    endringstidspunkt timestamp without time zone,
    overskrift character varying(500),
    kategori character varying(10),
    nav_kontor_for_arbeidsliste character varying(24)
);


--
-- Name: bruker_cv; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_cv (
    aktoerid character varying(20) NOT NULL,
    har_delt_cv boolean DEFAULT false NOT NULL,
    cv_eksisterer boolean DEFAULT false NOT NULL,
    siste_melding_mottatt timestamp without time zone
);


--
-- Name: bruker_data; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_data (
    freg_ident character varying(30) NOT NULL,
    fornavn character varying(90),
    mellomnavn character varying(90),
    etternavn character varying(90),
    kjoenn character varying(5),
    er_doed boolean,
    foedselsdato date,
    foedeland character varying(10),
    talespraaktolk character varying(20),
    tegnspraaktolk character varying(20),
    tolkbehovsistoppdatert date,
    bydelsnummer character varying(10),
    kommunenummer character varying(10),
    utenlandskadresse character varying(10),
    bostedsistoppdatert date,
    diskresjonkode character varying(3),
    sikkerhetstiltak_type character varying(20),
    sikkerhetstiltak_beskrivelse character varying(255),
    sikkerhetstiltak_gyldigfra date,
    sikkerhetstiltak_gyldigtil date,
    harukjentbosted boolean DEFAULT false
);


--
-- Name: bruker_data_barn; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_data_barn (
    barn_ident character varying(30) NOT NULL,
    barn_foedselsdato date NOT NULL,
    barn_diskresjonkode character varying(3)
);


--
-- Name: bruker_profilering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_profilering (
    aktoerid character varying(20) NOT NULL,
    profilering_resultat character varying(40),
    profilering_tidspunkt timestamp without time zone
);


--
-- Name: bruker_registrering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_registrering (
    aktoerid character varying(20) NOT NULL,
    brukers_situasjon character varying(40),
    registrering_opprettet timestamp without time zone,
    utdanning character varying(35),
    utdanning_bestatt character varying(15),
    utdanning_godkjent character varying(15)
);


--
-- Name: bruker_statsborgerskap; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_statsborgerskap (
    freg_ident character varying(30) NOT NULL,
    statsborgerskap character varying(20),
    gyldig_fra date,
    gyldig_til date
);


--
-- Name: brukertiltak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brukertiltak (
    aktivitetid character varying(25) NOT NULL,
    aktoerid character varying(20),
    personid character varying(20),
    tiltakskode character varying(10),
    tildato timestamp without time zone,
    fradato timestamp without time zone,
    version bigint
);


--
-- Name: brukertiltak_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brukertiltak_v2 (
    aktivitetid character varying(25) NOT NULL,
    aktoerid character varying(20),
    tiltakskode character varying(10),
    tildato timestamp without time zone,
    fradato timestamp without time zone,
    version bigint,
    status character varying(255)
);


--
-- Name: dialog; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dialog (
    aktoerid character varying(20) NOT NULL,
    venter_pa_bruker timestamp without time zone,
    venter_pa_nav timestamp without time zone
);


--
-- Name: endring_i_registrering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.endring_i_registrering (
    aktoerid character varying(20) NOT NULL,
    brukers_situasjon character varying(40),
    brukers_situasjon_sist_endret timestamp without time zone
);


--
-- Name: enslige_forsorgere; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enslige_forsorgere (
    vedtakid bigint NOT NULL,
    personident character varying(30) NOT NULL,
    stonadstype integer,
    vedtaksresultat integer,
    oppdatert timestamp without time zone DEFAULT now()
);


--
-- Name: enslige_forsorgere_aktivitet_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enslige_forsorgere_aktivitet_type (
    id integer NOT NULL,
    aktivitet_type character varying(100) NOT NULL
);


--
-- Name: enslige_forsorgere_aktivitet_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.enslige_forsorgere_aktivitet_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: enslige_forsorgere_aktivitet_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.enslige_forsorgere_aktivitet_type_id_seq OWNED BY public.enslige_forsorgere_aktivitet_type.id;


--
-- Name: enslige_forsorgere_barn; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enslige_forsorgere_barn (
    vedtakid bigint,
    fnr character varying(11),
    termindato date
);


--
-- Name: enslige_forsorgere_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enslige_forsorgere_periode (
    vedtakid bigint,
    fra_dato timestamp without time zone,
    til_dato timestamp without time zone,
    periodetype integer,
    aktivitetstype integer
);


--
-- Name: enslige_forsorgere_stonad_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enslige_forsorgere_stonad_type (
    id integer NOT NULL,
    stonad_type character varying(100) NOT NULL
);


--
-- Name: enslige_forsorgere_stonad_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.enslige_forsorgere_stonad_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: enslige_forsorgere_stonad_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.enslige_forsorgere_stonad_type_id_seq OWNED BY public.enslige_forsorgere_stonad_type.id;


--
-- Name: enslige_forsorgere_vedtaksperiode_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enslige_forsorgere_vedtaksperiode_type (
    id integer NOT NULL,
    periode_type character varying(100) NOT NULL
);


--
-- Name: enslige_forsorgere_vedtaksperiode_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.enslige_forsorgere_vedtaksperiode_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: enslige_forsorgere_vedtaksperiode_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.enslige_forsorgere_vedtaksperiode_type_id_seq OWNED BY public.enslige_forsorgere_vedtaksperiode_type.id;


--
-- Name: enslige_forsorgere_vedtaksresultat_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enslige_forsorgere_vedtaksresultat_type (
    id integer NOT NULL,
    vedtaksresultat_type character varying(100) NOT NULL
);


--
-- Name: enslige_forsorgere_vedtaksresultat_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.enslige_forsorgere_vedtaksresultat_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: enslige_forsorgere_vedtaksresultat_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.enslige_forsorgere_vedtaksresultat_type_id_seq OWNED BY public.enslige_forsorgere_vedtaksresultat_type.id;


--
-- Name: fargekategori; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fargekategori (
    id uuid NOT NULL,
    fnr character varying(11) NOT NULL,
    verdi character varying(25),
    sist_endret timestamp without time zone NOT NULL,
    sist_endret_av_veilederident character varying(7)
);


--
-- Name: fargekategori_kopi_2024_01_16; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fargekategori_kopi_2024_01_16 (
    id uuid,
    fnr character varying(11),
    verdi character varying(25),
    sist_endret timestamp without time zone,
    sist_endret_av_veilederident character varying(7)
);


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: foreldreansvar; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.foreldreansvar (
    foresatt_ident character varying(30) NOT NULL,
    barn_ident character varying(30) NOT NULL
);


--
-- Name: gruppe_aktiviter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gruppe_aktiviter (
    moteplan_id character varying(25) NOT NULL,
    veiledningdeltaker_id character varying(25) NOT NULL,
    aktoerid character varying(20),
    moteplan_startdato timestamp without time zone,
    moteplan_sluttdato timestamp without time zone,
    hendelse_id bigint,
    aktiv boolean DEFAULT false
);


--
-- Name: huskelapp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.huskelapp (
    huskelapp_id uuid,
    fnr character varying(11) NOT NULL,
    enhet_id character varying(4) NOT NULL,
    endret_av_veileder character varying(10) NOT NULL,
    endret_dato timestamp without time zone DEFAULT now(),
    frist timestamp without time zone,
    kommentar character varying(200),
    status character varying(10),
    endrings_id integer NOT NULL
);


--
-- Name: huskelapp_endrings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.huskelapp_endrings_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: huskelapp_endrings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.huskelapp_endrings_id_seq OWNED BY public.huskelapp.endrings_id;


--
-- Name: kafka_consumer_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kafka_consumer_record (
    id bigint NOT NULL,
    topic character varying(100) NOT NULL,
    partition integer NOT NULL,
    record_offset bigint NOT NULL,
    retries integer DEFAULT 0 NOT NULL,
    last_retry timestamp(6) without time zone,
    key bytea,
    value bytea,
    headers_json text,
    record_timestamp bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: kafka_consumer_record_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.kafka_consumer_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lest_arena_hendelse_aktivitet; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lest_arena_hendelse_aktivitet (
    aktivitetid character varying(25) NOT NULL,
    hendelse_id bigint
);


--
-- Name: lest_arena_hendelse_ytelse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lest_arena_hendelse_ytelse (
    vedtakid character varying(25) NOT NULL,
    hendelse_id bigint
);


--
-- Name: nom_skjerming; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nom_skjerming (
    fodselsnr character varying(33) NOT NULL,
    er_skjermet boolean DEFAULT false,
    skjermet_fra timestamp without time zone,
    skjermet_til timestamp without time zone
);


--
-- Name: oppfolging_data; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppfolging_data (
    aktoerid character varying(20) NOT NULL,
    veilederid character varying(20),
    oppfolging boolean DEFAULT false NOT NULL,
    ny_for_veileder boolean DEFAULT false NOT NULL,
    manuell boolean DEFAULT false NOT NULL,
    startdato timestamp without time zone
);


--
-- Name: oppfolgingsbruker_arena_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppfolgingsbruker_arena_v2 (
    fodselsnr character varying(33) NOT NULL,
    formidlingsgruppekode character varying(15),
    iserv_fra_dato timestamp without time zone,
    nav_kontor character varying(24),
    kvalifiseringsgruppekode character varying(15),
    rettighetsgruppekode character varying(15),
    hovedmaalkode character varying(30),
    endret_dato timestamp without time zone
);


--
-- Name: opplysninger_om_arbeidssoeker; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.opplysninger_om_arbeidssoeker (
    id integer NOT NULL,
    opplysninger_om_arbeidssoeker_id uuid NOT NULL,
    periode_id uuid NOT NULL,
    sendt_inn_tidspunkt timestamp without time zone NOT NULL,
    utdanning_nus_kode character varying(3),
    utdanning_bestatt character varying(8),
    utdanning_godkjent character varying(8)
);


--
-- Name: opplysninger_om_arbeidssoeker_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.opplysninger_om_arbeidssoeker_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: opplysninger_om_arbeidssoeker_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.opplysninger_om_arbeidssoeker_id_seq OWNED BY public.opplysninger_om_arbeidssoeker.id;


--
-- Name: opplysninger_om_arbeidssoeker_jobbsituasjon; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.opplysninger_om_arbeidssoeker_jobbsituasjon (
    id integer NOT NULL,
    opplysninger_om_arbeidssoeker_id uuid NOT NULL,
    jobbsituasjon character varying(255) NOT NULL
);


--
-- Name: opplysninger_om_arbeidssoeker_jobbsituasjon_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.opplysninger_om_arbeidssoeker_jobbsituasjon_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: opplysninger_om_arbeidssoeker_jobbsituasjon_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.opplysninger_om_arbeidssoeker_jobbsituasjon_id_seq OWNED BY public.opplysninger_om_arbeidssoeker_jobbsituasjon.id;


--
-- Name: pdl_person_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pdl_person_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: profilering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.profilering (
    id integer NOT NULL,
    periode_id uuid NOT NULL,
    profilering_resultat character varying(40) NOT NULL,
    sendt_inn_tidspunkt timestamp without time zone NOT NULL
);


--
-- Name: profilering_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.profilering_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: profilering_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.profilering_id_seq OWNED BY public.profilering.id;


--
-- Name: scheduled_tasks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scheduled_tasks (
    task_name text NOT NULL,
    task_instance text NOT NULL,
    task_data bytea,
    execution_time timestamp with time zone NOT NULL,
    picked boolean NOT NULL,
    picked_by text,
    last_success timestamp with time zone,
    last_failure timestamp with time zone,
    consecutive_failures integer,
    last_heartbeat timestamp with time zone,
    version bigint NOT NULL
);


--
-- Name: shedlock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shedlock (
    name character varying(256) NOT NULL,
    lock_until timestamp(3) without time zone,
    locked_at timestamp(3) without time zone,
    locked_by character varying(255)
);


--
-- Name: siste_14a_vedtak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.siste_14a_vedtak (
    bruker_id character varying(20) NOT NULL,
    hovedmal character varying(30),
    innsatsgruppe character varying(40) NOT NULL,
    fattet_dato timestamp with time zone NOT NULL,
    fra_arena boolean NOT NULL
);


--
-- Name: siste_arbeidssoeker_periode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.siste_arbeidssoeker_periode (
    arbeidssoker_periode_id uuid,
    fnr character varying(11) NOT NULL
);


--
-- Name: siste_endring; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.siste_endring (
    aktoerid character varying(20) NOT NULL,
    aktivitetid character varying(20),
    siste_endring_kategori character varying(45) NOT NULL,
    siste_endring_tidspunkt timestamp without time zone,
    er_sett boolean DEFAULT false
);


--
-- Name: tiltakkodeverket; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tiltakkodeverket (
    kode character varying(10) NOT NULL,
    verdi character varying(80)
);


--
-- Name: utkast_14a_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.utkast_14a_status (
    aktoerid character varying(20) NOT NULL,
    vedtakid character varying(20) NOT NULL,
    vedtakstatus character varying(40),
    innsatsgruppe character varying(40),
    hovedmal character varying(30),
    ansvarlig_veilderident character varying(20),
    ansvarlig_veildernavn character varying(60),
    endret_tidspunkt timestamp without time zone
);


--
-- Name: ytelse_status_for_bruker; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ytelse_status_for_bruker (
    aktoerid character varying(20) NOT NULL,
    utlopsdato timestamp without time zone,
    dagputlopuke integer,
    permutlopuke integer,
    aapmaxtiduke integer,
    aapunntakdagerigjen integer,
    ytelse character varying(40),
    antalldagerigjen integer,
    endret_dato timestamp without time zone
);


--
-- Name: ytelsesvedtak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ytelsesvedtak (
    vedtaksid character varying(20) NOT NULL,
    aktorid character varying(20) NOT NULL,
    personid character varying(20),
    ytelsestype character varying(15),
    saksid character varying(30),
    sakstypekode character varying(10),
    rettighetstypekode character varying(10),
    startdato timestamp without time zone,
    utlopsdato timestamp without time zone,
    antallukerigjen integer,
    antallpermitteringsuker integer,
    antalldagerigjenunntak integer,
    antalldagerigjen integer,
    endret_dato timestamp without time zone
);


--
-- Name: enslige_forsorgere_aktivitet_type id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere_aktivitet_type ALTER COLUMN id SET DEFAULT nextval('public.enslige_forsorgere_aktivitet_type_id_seq'::regclass);


--
-- Name: enslige_forsorgere_stonad_type id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere_stonad_type ALTER COLUMN id SET DEFAULT nextval('public.enslige_forsorgere_stonad_type_id_seq'::regclass);


--
-- Name: enslige_forsorgere_vedtaksperiode_type id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere_vedtaksperiode_type ALTER COLUMN id SET DEFAULT nextval('public.enslige_forsorgere_vedtaksperiode_type_id_seq'::regclass);


--
-- Name: enslige_forsorgere_vedtaksresultat_type id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere_vedtaksresultat_type ALTER COLUMN id SET DEFAULT nextval('public.enslige_forsorgere_vedtaksresultat_type_id_seq'::regclass);


--
-- Name: huskelapp endrings_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.huskelapp ALTER COLUMN endrings_id SET DEFAULT nextval('public.huskelapp_endrings_id_seq'::regclass);


--
-- Name: opplysninger_om_arbeidssoeker id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opplysninger_om_arbeidssoeker ALTER COLUMN id SET DEFAULT nextval('public.opplysninger_om_arbeidssoeker_id_seq'::regclass);


--
-- Name: opplysninger_om_arbeidssoeker_jobbsituasjon id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opplysninger_om_arbeidssoeker_jobbsituasjon ALTER COLUMN id SET DEFAULT nextval('public.opplysninger_om_arbeidssoeker_jobbsituasjon_id_seq'::regclass);


--
-- Name: profilering id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profilering ALTER COLUMN id SET DEFAULT nextval('public.profilering_id_seq'::regclass);


--
-- Name: aktiviteter aktiviteter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktiviteter
    ADD CONSTRAINT aktiviteter_pkey PRIMARY KEY (aktivitetid);


--
-- Name: arbeidsliste arbeidsliste_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsliste
    ADD CONSTRAINT arbeidsliste_pkey PRIMARY KEY (aktoerid);


--
-- Name: bruker_cv bruker_cv_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_cv
    ADD CONSTRAINT bruker_cv_pkey PRIMARY KEY (aktoerid);


--
-- Name: bruker_data_barn bruker_data_barn_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_data_barn
    ADD CONSTRAINT bruker_data_barn_pkey PRIMARY KEY (barn_ident);


--
-- Name: bruker_data bruker_data_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_data
    ADD CONSTRAINT bruker_data_pkey PRIMARY KEY (freg_ident);


--
-- Name: bruker_identer bruker_identer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_identer
    ADD CONSTRAINT bruker_identer_pkey PRIMARY KEY (ident);


--
-- Name: bruker_profilering bruker_profilering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_profilering
    ADD CONSTRAINT bruker_profilering_pkey PRIMARY KEY (aktoerid);


--
-- Name: bruker_registrering bruker_registrering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_registrering
    ADD CONSTRAINT bruker_registrering_pkey PRIMARY KEY (aktoerid);


--
-- Name: brukertiltak brukertiltak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brukertiltak
    ADD CONSTRAINT brukertiltak_pkey PRIMARY KEY (aktivitetid);


--
-- Name: brukertiltak_v2 brukertiltak_v2_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brukertiltak_v2
    ADD CONSTRAINT brukertiltak_v2_pkey PRIMARY KEY (aktivitetid);


--
-- Name: dialog dialog_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dialog
    ADD CONSTRAINT dialog_pkey PRIMARY KEY (aktoerid);


--
-- Name: endring_i_registrering endring_i_registrering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.endring_i_registrering
    ADD CONSTRAINT endring_i_registrering_pkey PRIMARY KEY (aktoerid);


--
-- Name: enslige_forsorgere_aktivitet_type enslige_forsorgere_aktivitet_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere_aktivitet_type
    ADD CONSTRAINT enslige_forsorgere_aktivitet_type_pkey PRIMARY KEY (id);


--
-- Name: enslige_forsorgere enslige_forsorgere_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere
    ADD CONSTRAINT enslige_forsorgere_pkey PRIMARY KEY (vedtakid);


--
-- Name: enslige_forsorgere_stonad_type enslige_forsorgere_stonad_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere_stonad_type
    ADD CONSTRAINT enslige_forsorgere_stonad_type_pkey PRIMARY KEY (id);


--
-- Name: enslige_forsorgere_vedtaksperiode_type enslige_forsorgere_vedtaksperiode_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere_vedtaksperiode_type
    ADD CONSTRAINT enslige_forsorgere_vedtaksperiode_type_pkey PRIMARY KEY (id);


--
-- Name: enslige_forsorgere_vedtaksresultat_type enslige_forsorgere_vedtaksresultat_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere_vedtaksresultat_type
    ADD CONSTRAINT enslige_forsorgere_vedtaksresultat_type_pkey PRIMARY KEY (id);


--
-- Name: fargekategori fargekategori_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fargekategori
    ADD CONSTRAINT fargekategori_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: foreldreansvar foreldreansvar_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foreldreansvar
    ADD CONSTRAINT foreldreansvar_pkey PRIMARY KEY (foresatt_ident, barn_ident);


--
-- Name: gruppe_aktiviter gruppe_aktiviter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gruppe_aktiviter
    ADD CONSTRAINT gruppe_aktiviter_pkey PRIMARY KEY (moteplan_id, veiledningdeltaker_id);


--
-- Name: huskelapp huskelapp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.huskelapp
    ADD CONSTRAINT huskelapp_pkey PRIMARY KEY (endrings_id);


--
-- Name: kafka_consumer_record kafka_consumer_record_topic_partition_record_offset_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kafka_consumer_record
    ADD CONSTRAINT kafka_consumer_record_topic_partition_record_offset_key UNIQUE (topic, partition, record_offset);


--
-- Name: lest_arena_hendelse_aktivitet lest_arena_hendelse_aktivitet_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lest_arena_hendelse_aktivitet
    ADD CONSTRAINT lest_arena_hendelse_aktivitet_pkey PRIMARY KEY (aktivitetid);


--
-- Name: lest_arena_hendelse_ytelse lest_arena_hendelse_ytelse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lest_arena_hendelse_ytelse
    ADD CONSTRAINT lest_arena_hendelse_ytelse_pkey PRIMARY KEY (vedtakid);


--
-- Name: nom_skjerming nom_skjerming_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nom_skjerming
    ADD CONSTRAINT nom_skjerming_pkey PRIMARY KEY (fodselsnr);


--
-- Name: oppfolging_data oppfolging_data_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppfolging_data
    ADD CONSTRAINT oppfolging_data_pkey PRIMARY KEY (aktoerid);


--
-- Name: oppfolgingsbruker_arena_v2 oppfolgingsbruker_arena_v2_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppfolgingsbruker_arena_v2
    ADD CONSTRAINT oppfolgingsbruker_arena_v2_pkey PRIMARY KEY (fodselsnr);


--
-- Name: opplysninger_om_arbeidssoeker_jobbsituasjon opplysninger_om_arbeidssoeker_jobbsituasjon_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opplysninger_om_arbeidssoeker_jobbsituasjon
    ADD CONSTRAINT opplysninger_om_arbeidssoeker_jobbsituasjon_pkey PRIMARY KEY (id);


--
-- Name: opplysninger_om_arbeidssoeker opplysninger_om_arbeidssoeker_opplysninger_om_arbeidssoeker_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opplysninger_om_arbeidssoeker
    ADD CONSTRAINT opplysninger_om_arbeidssoeker_opplysninger_om_arbeidssoeker_key UNIQUE (opplysninger_om_arbeidssoeker_id);


--
-- Name: opplysninger_om_arbeidssoeker opplysninger_om_arbeidssoeker_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opplysninger_om_arbeidssoeker
    ADD CONSTRAINT opplysninger_om_arbeidssoeker_pkey PRIMARY KEY (id);


--
-- Name: profilering profilering_periode_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profilering
    ADD CONSTRAINT profilering_periode_id_key UNIQUE (periode_id);


--
-- Name: profilering profilering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profilering
    ADD CONSTRAINT profilering_pkey PRIMARY KEY (id);


--
-- Name: scheduled_tasks scheduled_tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheduled_tasks
    ADD CONSTRAINT scheduled_tasks_pkey PRIMARY KEY (task_name, task_instance);


--
-- Name: shedlock shedlock_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shedlock
    ADD CONSTRAINT shedlock_pkey PRIMARY KEY (name);


--
-- Name: siste_14a_vedtak siste_14a_vedtak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.siste_14a_vedtak
    ADD CONSTRAINT siste_14a_vedtak_pkey PRIMARY KEY (bruker_id);


--
-- Name: siste_arbeidssoeker_periode siste_arbeidssoeker_periode_arbeidssoker_periode_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.siste_arbeidssoeker_periode
    ADD CONSTRAINT siste_arbeidssoeker_periode_arbeidssoker_periode_id_key UNIQUE (arbeidssoker_periode_id);


--
-- Name: siste_arbeidssoeker_periode siste_arbeidssoeker_periode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.siste_arbeidssoeker_periode
    ADD CONSTRAINT siste_arbeidssoeker_periode_pkey PRIMARY KEY (fnr);


--
-- Name: siste_endring siste_endring_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.siste_endring
    ADD CONSTRAINT siste_endring_pkey PRIMARY KEY (aktoerid, siste_endring_kategori);


--
-- Name: tiltakkodeverket tiltakkodeverket_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tiltakkodeverket
    ADD CONSTRAINT tiltakkodeverket_pkey PRIMARY KEY (kode);


--
-- Name: utkast_14a_status vedtakstatus_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utkast_14a_status
    ADD CONSTRAINT vedtakstatus_pkey PRIMARY KEY (aktoerid);


--
-- Name: ytelse_status_for_bruker ytelse_status_for_bruker_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ytelse_status_for_bruker
    ADD CONSTRAINT ytelse_status_for_bruker_pkey PRIMARY KEY (aktoerid);


--
-- Name: ytelsesvedtak ytelsesvedtak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ytelsesvedtak
    ADD CONSTRAINT ytelsesvedtak_pkey PRIMARY KEY (vedtaksid);


--
-- Name: aktoer_aktivitet_indx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX aktoer_aktivitet_indx ON public.aktiviteter USING btree (aktoerid);


--
-- Name: aktoer_tiltak_indx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX aktoer_tiltak_indx ON public.brukertiltak USING btree (aktoerid);


--
-- Name: aktoerid_gruppe_aktiviter_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX aktoerid_gruppe_aktiviter_idx ON public.gruppe_aktiviter USING btree (aktoerid);


--
-- Name: aktorid_ytelser_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX aktorid_ytelser_idx ON public.ytelsesvedtak USING btree (aktorid);


--
-- Name: brukertiltak_v2_aktoerid_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX brukertiltak_v2_aktoerid_index ON public.brukertiltak_v2 USING btree (aktoerid);


--
-- Name: enslige_forsorgere_aktivitet_type_indx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX enslige_forsorgere_aktivitet_type_indx ON public.enslige_forsorgere_aktivitet_type USING btree (aktivitet_type);


--
-- Name: fargekategori_fnr_unique_index; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX fargekategori_fnr_unique_index ON public.fargekategori USING btree (fnr);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: huskelapp_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX huskelapp_status ON public.huskelapp USING btree (status);


--
-- Name: huskelappenhetid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX huskelappenhetid ON public.huskelapp USING btree (enhet_id);


--
-- Name: huskelappfnr; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX huskelappfnr ON public.huskelapp USING btree (fnr);


--
-- Name: huskelappid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX huskelappid ON public.huskelapp USING btree (huskelapp_id);


--
-- Name: idx_bruker_identer_gruppe; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bruker_identer_gruppe ON public.bruker_identer USING btree (gruppe);


--
-- Name: idx_bruker_identer_historisk; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bruker_identer_historisk ON public.bruker_identer USING btree (historisk);


--
-- Name: idx_bruker_identer_person; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bruker_identer_person ON public.bruker_identer USING btree (person);


--
-- Name: idx_foreldreansvar_barn_ident; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_foreldreansvar_barn_ident ON public.foreldreansvar USING btree (barn_ident);


--
-- Name: idx_freg_ident; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_freg_ident ON public.bruker_statsborgerskap USING btree (freg_ident);


--
-- Name: idx_gyldig_til; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gyldig_til ON public.bruker_statsborgerskap USING btree (gyldig_til);


--
-- Name: idx_oppfolging_data_oppfolging; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oppfolging_data_oppfolging ON public.oppfolging_data USING btree (oppfolging);


--
-- Name: nav_kontor_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX nav_kontor_idx ON public.oppfolgingsbruker_arena_v2 USING btree (nav_kontor);


--
-- Name: oppfolging_data_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX oppfolging_data_idx ON public.oppfolging_data USING btree (veilederid);


--
-- Name: periode_enslige_forsorgere_vedtaksperiode_type_indx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX periode_enslige_forsorgere_vedtaksperiode_type_indx ON public.enslige_forsorgere_vedtaksperiode_type USING btree (periode_type);


--
-- Name: personid_brukertiltak_v2_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX personid_brukertiltak_v2_idx ON public.brukertiltak USING btree (aktoerid);


--
-- Name: personident_enslige_forsorgere_indx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX personident_enslige_forsorgere_indx ON public.enslige_forsorgere USING btree (personident);


--
-- Name: stonad_enslige_forsorgere_stonad_type_indx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX stonad_enslige_forsorgere_stonad_type_indx ON public.enslige_forsorgere_stonad_type USING btree (stonad_type);


--
-- Name: vedtaksresultat_enslige_forsorgere_vedtaksresultat_type_indx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX vedtaksresultat_enslige_forsorgere_vedtaksresultat_type_indx ON public.enslige_forsorgere_vedtaksresultat_type USING btree (vedtaksresultat_type);


--
-- Name: enslige_forsorgere_barn enslige_forsorgere_barn_vedtakid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere_barn
    ADD CONSTRAINT enslige_forsorgere_barn_vedtakid_fkey FOREIGN KEY (vedtakid) REFERENCES public.enslige_forsorgere(vedtakid);


--
-- Name: enslige_forsorgere_periode enslige_forsorgere_periode_aktivitetstype_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere_periode
    ADD CONSTRAINT enslige_forsorgere_periode_aktivitetstype_fkey FOREIGN KEY (aktivitetstype) REFERENCES public.enslige_forsorgere_aktivitet_type(id);


--
-- Name: enslige_forsorgere_periode enslige_forsorgere_periode_periodetype_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere_periode
    ADD CONSTRAINT enslige_forsorgere_periode_periodetype_fkey FOREIGN KEY (periodetype) REFERENCES public.enslige_forsorgere_vedtaksperiode_type(id);


--
-- Name: enslige_forsorgere_periode enslige_forsorgere_periode_vedtakid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere_periode
    ADD CONSTRAINT enslige_forsorgere_periode_vedtakid_fkey FOREIGN KEY (vedtakid) REFERENCES public.enslige_forsorgere(vedtakid);


--
-- Name: enslige_forsorgere enslige_forsorgere_stonadstype_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere
    ADD CONSTRAINT enslige_forsorgere_stonadstype_fkey FOREIGN KEY (stonadstype) REFERENCES public.enslige_forsorgere_stonad_type(id);


--
-- Name: enslige_forsorgere enslige_forsorgere_vedtaksresultat_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enslige_forsorgere
    ADD CONSTRAINT enslige_forsorgere_vedtaksresultat_fkey FOREIGN KEY (vedtaksresultat) REFERENCES public.enslige_forsorgere_vedtaksresultat_type(id);


--
-- Name: opplysninger_om_arbeidssoeker fk_arbeidssoeker_periode_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opplysninger_om_arbeidssoeker
    ADD CONSTRAINT fk_arbeidssoeker_periode_id FOREIGN KEY (periode_id) REFERENCES public.siste_arbeidssoeker_periode(arbeidssoker_periode_id) ON DELETE CASCADE;


--
-- Name: foreldreansvar fk_barn_ident; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foreldreansvar
    ADD CONSTRAINT fk_barn_ident FOREIGN KEY (barn_ident) REFERENCES public.bruker_data_barn(barn_ident) ON UPDATE CASCADE;


--
-- Name: foreldreansvar fk_foresatt_ident; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foreldreansvar
    ADD CONSTRAINT fk_foresatt_ident FOREIGN KEY (foresatt_ident) REFERENCES public.bruker_data(freg_ident) ON DELETE CASCADE;


--
-- Name: bruker_statsborgerskap fk_freg_ident; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_statsborgerskap
    ADD CONSTRAINT fk_freg_ident FOREIGN KEY (freg_ident) REFERENCES public.bruker_data(freg_ident) ON DELETE CASCADE;


--
-- Name: opplysninger_om_arbeidssoeker_jobbsituasjon opplysninger_om_arbeidssoeker_opplysninger_om_arbeidssoeke_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.opplysninger_om_arbeidssoeker_jobbsituasjon
    ADD CONSTRAINT opplysninger_om_arbeidssoeker_opplysninger_om_arbeidssoeke_fkey FOREIGN KEY (opplysninger_om_arbeidssoeker_id) REFERENCES public.opplysninger_om_arbeidssoeker(opplysninger_om_arbeidssoeker_id) ON DELETE CASCADE;


--
-- Name: profilering profilering_periode_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profilering
    ADD CONSTRAINT profilering_periode_id_fkey FOREIGN KEY (periode_id) REFERENCES public.siste_arbeidssoeker_periode(arbeidssoker_periode_id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

