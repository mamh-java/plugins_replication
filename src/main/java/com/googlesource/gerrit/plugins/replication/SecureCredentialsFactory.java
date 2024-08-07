// Copyright (C) 2024 The Android Open Source Project
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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.common.UsedAt;
import com.google.gerrit.common.UsedAt.Project;
import com.google.gerrit.server.securestore.SecureStore;
import com.google.inject.Inject;
import java.util.Objects;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/** Looks up a remote's password in SecureStore */
@UsedAt(Project.PLUGIN_PULL_REPLICATION)
public class SecureCredentialsFactory implements CredentialsFactory {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private final SecureStore secureStore;

  @Inject
  public SecureCredentialsFactory(SecureStore secureStore) {
    this.secureStore = secureStore;
  }

  @Override
  public CredentialsProvider create(String remoteName) {
    String user = Objects.toString(secureStore.get("remote", remoteName, "username"), "");
    String pass = Objects.toString(secureStore.get("remote", remoteName, "password"), "");
    return new UsernamePasswordCredentialsProvider(user, pass);
  }

  @Override
  public boolean validate(String remoteName) {
    try {
      String unused = secureStore.get("remote", remoteName, "username");
      unused = secureStore.get("remote", remoteName, "password");
      return true;
    } catch (Throwable t) {
      log.atSevere().withCause(t).log(
          "Credentials for replication remote %s are invalid", remoteName);
      return false;
    }
  }
}
