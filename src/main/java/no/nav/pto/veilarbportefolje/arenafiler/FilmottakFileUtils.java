package no.nav.pto.veilarbportefolje.arenafiler;

import io.micrometer.core.instrument.Timer;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.TiltakOgAktiviteterForBrukere;
import no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig.SftpConfig;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static no.nav.metrics.MetricsFactory.getMeterRegistry;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig.AKTIVITETER_SFTP;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig.LOPENDEYTELSER_SFTP;

@Slf4j
public class FilmottakFileUtils {

    private static Timer timerYtelse = Timer.builder("portefolje_ytelse_sftp").tag("hostname", "sftp://filmottak").register(getMeterRegistry());
    private static Timer timerTiltak = Timer.builder("portefolje_tiltak_sftp").tag("hostname", "sftp://filmottak-loependeytelser").register(getMeterRegistry());

    public static Try<FileObject> hentTiltaksFil() {
        return timerTiltak.record(() -> hentFil(AKTIVITETER_SFTP));
    }

    public static Try<FileObject> hentYtelseFil() {
        return timerYtelse.record(() -> hentFil(LOPENDEYTELSER_SFTP));
    }

    private static Try<FileObject> hentFil(SftpConfig sftpConfig) {
        log.info("Starter henting av fil fra {}", sftpConfig.getUrl());
        try {
            return FilmottakFileUtils.hentFilViaSftp(sftpConfig);
        } catch (FileSystemException e) {
            log.info("Henting av fil fra {} feilet", sftpConfig.getUrl());
            return Try.failure(e);
        } finally {
            log.info("Henting av fil fra {} ferdig!", sftpConfig.getUrl());
        }
    }

    static Try<FileObject> hentFilViaSftp(SftpConfig sftpConfig) throws FileSystemException {
        FileSystemOptions fsOptions = new FileSystemOptions();
        SftpFileSystemConfigBuilder sftpFileSystemConfigBuilder = SftpFileSystemConfigBuilder.getInstance();
        sftpFileSystemConfigBuilder.setPreferredAuthentications(fsOptions, "password");
        sftpFileSystemConfigBuilder.setStrictHostKeyChecking(fsOptions, "no");

        DefaultFileSystemConfigBuilder defaultFileSystemConfigBuilder = DefaultFileSystemConfigBuilder.getInstance();
        defaultFileSystemConfigBuilder.setUserAuthenticator(fsOptions, new StaticUserAuthenticator("", sftpConfig.getUsername(), sftpConfig.getPassword()));

        FileSystemManager fsManager = VFS.getManager();
        return Try.of(() -> fsManager.resolveFile(sftpConfig.getUrl(), fsOptions));
    }

    public static Try<TiltakOgAktiviteterForBrukere> unmarshallTiltakFil(FileObject fileObject) {
        return Try.of(() -> fileObject.getContent().getInputStream()).flatMap(file -> unmarshallFile(file, TiltakOgAktiviteterForBrukere.class));
    }

    public static Try<LoependeYtelser> unmarshallLoependeYtelserFil(FileObject fileObject) {
        return Try.of(() -> fileObject.getContent().getInputStream()).flatMap(file -> unmarshallFile(file, LoependeYtelser.class));
    }

    static <T> Try<T> unmarshallFile(InputStream is, Class<T> declaredType) {
        return Try.of(() -> {
            JAXBContext jaxb = JAXBContext.newInstance(declaredType.getPackage().getName());
            Unmarshaller unmarshaller = jaxb.createUnmarshaller();
            StreamSource source = new StreamSource(is);
            JAXBElement<T> jaxbElement = unmarshaller.unmarshal(source, declaredType);
            return jaxbElement.getValue();
        });
    }

    public static Try<Long> getLastModifiedTimeInMillis(SftpConfig sftpConfig) {
        return Try.of(
                () -> hentFil(sftpConfig)
                        .get()
                        .getContent()
                        .getLastModifiedTime())
                .onFailure(e -> log.warn(String.format("Kunne ikke hente ut fil via nfs: %s", sftpConfig.getUrl())));
    }

    public static long hoursSinceLastChanged(LocalDateTime lastChanged) {
        return ChronoUnit.HOURS.between(lastChanged, LocalDateTime.now());
    }
}
