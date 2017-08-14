package no.nav.fo.consumer.GR202;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class KopierGR202FraArena {
    static Logger logger = LoggerFactory.getLogger(KopierGR202FraArena.class);

    @Value("${tiltak.sftp.brukernavn}")
    private String sftpBrukernavn;

    @Value("${tiltak.sftp.url}")
    private String sftpUrl;

    public void kopier() {
        JSch jSch = new JSch();
        try {
            Session session = jSch.getSession(sftpBrukernavn, sftpUrl, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp channelSftp = (ChannelSftp) channel;
            channelSftp.get("/gr202/t5/arena_paagaaende_aktiviteter.xml", "arena_paagaaende_aktiviteter.xml");
            channelSftp.exit();
            session.disconnect();
        } catch (JSchException | SftpException e) {
            e.printStackTrace();
        }


    }
}
