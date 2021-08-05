/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.utils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class SftpClient {
    private final String host;
    private final String username;
    private final String password;
    private final int sessionConnectTimeout;
    private final int channelConnectTimeout;

    private final JSch jSch = new JSch();
    private Session session;
    private ChannelSftp sftpChannel;

    public void establishConnection() throws Exception {
        destroyConnection();

        session = jSch.getSession(username, host);
        session.setPassword(password);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect(sessionConnectTimeout);

        sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect(channelConnectTimeout);
    }

    public void destroyConnection() {
        if (session != null) {
            session.disconnect();
            sftpChannel.exit();

            session = null;
            sftpChannel = null;
        }
    }

    public void cd(String directory) throws SftpException {
        sftpChannel.cd(directory);
    }

    public void createFile(InputStream inputStream, String filePath) throws SftpException {
        sftpChannel.put(inputStream, filePath);
    }

    public void moveFile(String oldFilePath, String newFilePath) throws SftpException {
        sftpChannel.rename(oldFilePath, newFilePath);
    }

    public Set<String> listFiles(String directory) throws SftpException {
        return new ArrayList<Object>(sftpChannel.ls(directory)).stream()
                .map(o -> ((ChannelSftp.LsEntry) o))
                .filter(lsEntry -> !lsEntry.getAttrs().isDir())
                .map(ChannelSftp.LsEntry::getFilename)
                .collect(Collectors.toSet());
    }

    public InputStream getFile(String filePath) throws SftpException {
        return sftpChannel.get(filePath);
    }

}
