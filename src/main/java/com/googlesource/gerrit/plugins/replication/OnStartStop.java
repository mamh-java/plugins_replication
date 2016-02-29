// Copyright (C) 2012 The Android Open Source Project
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

import com.google.common.util.concurrent.Atomics;
import com.google.gerrit.common.EventDispatcher;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.extensions.systemstatus.ServerInformation;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.replication.PushResultProcessing.GitUpdateProcessing;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class OnStartStop implements LifecycleListener {
  private final AtomicReference<Future<?>> pushAllFuture;
  private final ServerInformation srvInfo;
  private final PushAll.Factory pushAll;
  private final ReplicationQueue queue;
  private final ReplicationConfig config;
  private final DynamicItem<EventDispatcher> eventDispatcher;
  private final SchemaFactory<ReviewDb> database;

  @Inject
  OnStartStop(
      ServerInformation srvInfo,
      PushAll.Factory pushAll,
      ReplicationQueue queue,
      ReplicationConfig config,
      DynamicItem<EventDispatcher> eventDispatcher,
      SchemaFactory<ReviewDb> database) {
    this.srvInfo = srvInfo;
    this.pushAll = pushAll;
    this.queue = queue;
    this.config = config;
    this.eventDispatcher = eventDispatcher;
    this.database = database;
    this.pushAllFuture = Atomics.newReference();
  }

  @Override
  public void start() {
    queue.start();

    if (srvInfo.getState() == ServerInformation.State.STARTUP
        && config.isReplicateAllOnPluginStart()) {
      ReplicationState state = new ReplicationState(
          new GitUpdateProcessing(eventDispatcher.get(), database));
      pushAllFuture.set(pushAll.create(
          null, ReplicationFilter.all(), state).schedule(30, TimeUnit.SECONDS));
    }
  }

  @Override
  public void stop() {
    Future<?> f = pushAllFuture.getAndSet(null);
    if (f != null) {
      f.cancel(true);
    }
    queue.stop();
  }
}
