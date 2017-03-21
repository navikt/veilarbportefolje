package no.nav.fo.service;

import javaslang.control.Try;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.InMemoryDestFile;
import no.nav.fo.util.CopyStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ArenafilService {

    default Try<InputStream> hentArenafil(String server, String username, String password) {
        return Try.of(() -> {
            SSHClient ssh = new SSHClient();
            ssh.addHostKeyVerifier((s, i, publicKey) -> true);
            ssh.connect(server);
            ssh.authPassword(username, password);

            SFTPClient sftpClient = ssh.newSFTPClient();

            CopyStream stream = new CopyStream();

            InMemoryDestFile dest = new InMemoryDestFile() {
                @Override
                public OutputStream getOutputStream() throws IOException {
                    return stream;
                }
            };
            sftpClient.get("GR199/arena_loepende_ytelser.xml", dest);

            return stream.toInputStream();
        });
    }
}