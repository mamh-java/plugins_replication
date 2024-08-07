// Copyright (C) 2011 The Android Open Source Project
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

import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Objects;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;

/**
 * Looks up a remote's password in secure.config.
 *
 * @deprecated This class is not secure and should no longer be used; it was allowing to record and
 *     read credentials in clear text in secure.config even though the Gerrit administrator
 *     installed a proper SecureStore implementation such as the secure-config.jar libModule.
 */
@Deprecated(forRemoval = true)
class LegacyCredentialsFactory implements CredentialsFactory {
  private final Config config;

  @Inject
  public LegacyCredentialsFactory(SitePaths site) throws ConfigInvalidException, IOException {
    config = load(site);
  }

  private static Config load(SitePaths site) throws ConfigInvalidException, IOException {
    FileBasedConfig cfg = new FileBasedConfig(site.secure_config.toFile(), FS.DETECTED);
    if (cfg.getFile().exists() && cfg.getFile().length() > 0) {
      try {
        cfg.load();
      } catch (ConfigInvalidException e) {
        throw new ConfigInvalidException(
            String.format("Config file %s is invalid: %s", cfg.getFile(), e.getMessage()), e);
      } catch (IOException e) {
        throw new IOException(
            String.format("Cannot read %s: %s", cfg.getFile(), e.getMessage()), e);
      }
    }
    return cfg;
  }

  @Override
  public CredentialsProvider create(String remoteName) {
    String user = Objects.toString(config.getString("remote", remoteName, "username"), "");
    String pass = Objects.toString(config.getString("remote", remoteName, "password"), "");
    return new UsernamePasswordCredentialsProvider(user, pass);
  }
}
