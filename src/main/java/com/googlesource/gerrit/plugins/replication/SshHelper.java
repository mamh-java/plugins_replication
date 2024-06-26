// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.replication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.replication.api.ReplicationConfig;
import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.io.StreamCopyThread;

/** Utility class that provides SSH access to remote URIs. */
public class SshHelper {
  private final Provider<SshSessionFactory> sshSessionFactoryProvider;
  private final int commandTimeout;
  private final int connectionTimeout;

  @Inject
  protected SshHelper(
      ReplicationConfig replicationConfig, Provider<SshSessionFactory> sshSessionFactoryProvider) {
    this.sshSessionFactoryProvider = sshSessionFactoryProvider;
    this.commandTimeout = replicationConfig.getSshCommandTimeout();
    this.connectionTimeout = replicationConfig.getSshConnectionTimeout();
  }

  public int executeRemoteSsh(URIish uri, String cmd, OutputStream errStream) throws IOException {
    RemoteSession ssh = connect(uri);
    Process proc = ssh.exec(cmd, commandTimeout);
    proc.getOutputStream().close();
    StreamCopyThread out = new StreamCopyThread(proc.getInputStream(), errStream);
    StreamCopyThread err = new StreamCopyThread(proc.getErrorStream(), errStream);
    out.start();
    err.start();
    try {
      proc.waitFor();
      out.halt();
      err.halt();
    } catch (InterruptedException interrupted) {
      // Don't wait, drop out immediately.
    }
    ssh.disconnect();
    return proc.exitValue();
  }

  public OutputStream newErrorBufferStream() {
    return new OutputStream() {
      private final StringBuilder out = new StringBuilder();
      private final StringBuilder line = new StringBuilder();

      @Override
      public synchronized String toString() {
        while (out.length() > 0 && out.charAt(out.length() - 1) == '\n') {
          out.setLength(out.length() - 1);
        }
        return out.toString();
      }

      @Override
      public synchronized void write(int b) {
        if (b == '\r') {
          return;
        }

        line.append((char) b);

        if (b == '\n') {
          out.append(line);
          line.setLength(0);
        }
      }
    };
  }

  protected RemoteSession connect(URIish uri) throws TransportException {
    return sshSessionFactoryProvider.get().getSession(uri, null, FS.DETECTED, connectionTimeout);
  }
}
