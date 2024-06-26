@PLUGIN@ extension points
=========================

The replication plugin exposes an extension point to allow influencing its behaviour from another plugin or a script.
Extension points can be defined from the replication plugin only when it is loaded as [libModule](../../../Documentation/config-gerrit.html#gerrit.installModule) and
implemented by another plugin by declaring a `provided` dependency from the replication plugin.

Install extension libModule
---------------------------

The replication plugin's extension points are defined in the `c.g.g.p.r.ReplicationExtensionPointModule`
that needs to be configured as libModule.

Create a symbolic link from `$GERRIT_SITE/plugins/replication.jar` into `$GERRIT_SITE/lib`
and then add the replication extension module to the `gerrit.config`.

Example:

```ini
[gerrit]
  installModule = com.googlesource.gerrit.plugins.replication.ReplicationExtensionPointModule
```

> **NOTE**: Use and configuration of the replication plugin as library module requires a Gerrit server restart and does not support hot plugin install or upgrade.

Extension points
----------------

* `com.googlesource.gerrit.plugins.replication.AdminApiFactory`

  Create an instance of `AdminApi` for a given remote URL. The default implementation
  provides API instances for local FS, remote SSH, and remote Gerrit.

  Only one factory at a time is supported. The implementation needs to be bound as a
  `DynamicItem`.

  Example:

  ```java
  DynamicItem.bind(binder(), AdminApiFactory.class).to(AdminApiFactoryImpl.class);
  ```

@PLUGIN@ Cross Plugin Communication
===================================

The @PLUGIN@ plugin exposes _ApiModule_ that allows to provide _Cross Plugin
Communication_.  Extension points can be defined from the replication plugin when it is loaded
as [ApiModule](../../../Documentation/dev-plugins.html#_cross_plugin_communication) and
implemented by another plugin by declaring a `provided` dependency from the replication plugin api.

Build the @plugin@'s API jar
----------------------------

The replication plugin's extension points are defined in the `c.g.g.p.r.a.ApiModule`
that needs to be built from source and loaded in Gerrit as ApiModule.

```
$ bazelisk build plugins/replication:replication-api
$ cp bazel-bin/plugins/replication/replication-api.jar $GERRIT_SITE/plugins
```

> **NOTE**: Use and configuration of the replication-api as ApiModule is compatible with
> Gerrit v3.9 onwards and requires a Gerrit server restart; it does not support hot plugin install
> or upgrade.


Setup
-----

Check the [official documentation](https://gerrit-review.googlesource.com/Documentation/dev-plugins.html#_cross_plugin_communication)
on how to setup your project.

Working with [Extension Points](./extension-point.md)
-----------------------------------------------------

In order to use both, the _Cross Plugin Communication_ and replication
_Extension Points_, follow the [Install extension libModule](./extension-point.md#install-extension-libmodule)
steps and make sure that `replication.jar` is only present in `lib/` directory.

Exposed API
-----------

* `com.googlesource.gerrit.plugins.replication.api.ReplicationConfigOverrides`

  Override current replication configuration from external source (eg. git
  repository, ZooKeeper).

  Replication plugin will still use configuration from `$gerrit_site/etc/`, but
  with overrides it can be modified dynamically from external source, similarly to
  how `git config` uses _user_ and _repository_ configuration files.

  Only one override at a time is supported. The implementation needs to bind a
  `DynamicItem`.

  ```java
  DynamicItem.bind(binder(), ReplicationConfigOverrides.class).to(ReplicationConfigOverridesImpl.class);
  ```

* `com.googlesource.gerrit.plugins.replication.api.ReplicationPushFilter`

  Filter out the ref updates pushed to a remote instance.
  Only one filter at a time is supported. Filter implementation needs to bind a `DynamicItem`.

  Default: no filtering

  Example:

  ```java
  DynamicItem.bind(binder(), ReplicationPushFilter.class).to(ReplicationPushFilterImpl.class);
  ```
