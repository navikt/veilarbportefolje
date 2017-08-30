package no.nav.fo.filmottak;

import io.vavr.control.Try;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class FileUtils {
    public static Try<InputStream> lesYtelsesFil(File file) {
        return Try.of(() -> {
            final byte[] bytes = Files.readAllBytes(file.toPath());
            return new ByteArrayInputStream(bytes);
        });
    }

    public static Try<FileObject> hentTiltakFil(String URI) throws FileSystemException {
        FileSystemOptions fsOptions = new FileSystemOptions();
        SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fsOptions, "no");
        FileSystemManager fsManager = VFS.getManager();
        return Try.of(() -> fsManager.resolveFile(URI, fsOptions));
    }
}
