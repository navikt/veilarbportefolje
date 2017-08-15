package no.nav.fo.consumer.GR202;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class KopierGR202FraArena {
    static Logger logger = LoggerFactory.getLogger(KopierGR202FraArena.class);

    @Value("${tiltak.sftp.brukernavn}")
    private String sftpBrukernavn;

    @Value("${tiltak.sftp.url}")
    private String sftpUrl;

    public List<String> kopier() {
        JSch jSch = new JSch();
        List<String> files = new ArrayList<>();
        try {
            Session session = jSch.getSession(sftpBrukernavn, sftpUrl, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp channelSftp = (ChannelSftp) channel;
//            String gr202Path = "/gr202";
//            channelSftp.cd(gr202Path);
            files.add("host: "+session.getHost());
            files.add("connected: "+session.isConnected());
            Vector gr202Files = channelSftp.ls("");
            for(int i = 0; i < gr202Files.size(); i++) {
                ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) gr202Files.get(i);
                files.add(entry.getFilename());
            }
//            channelSftp.get("/gr202/t5/arena_paagaaende_aktiviteter.xml", "arena_paagaaende_aktiviteter.xml");
            channelSftp.exit();
            session.disconnect();
        } catch (JSchException | SftpException e) {
            e.printStackTrace();
        }
        return files;
    }
}
