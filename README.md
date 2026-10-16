# nut-base

NUT Base (NUT Utility Toolkit Base) - generic pure java 8 library

One repository, one version, 7 jars. Each jar is a Gradle subproject and the
dependency graph between them is a strict DAG, so you only pull what you use.

Example Exception Messages:

- bar must not be null

## Adding a dependency

Published on [JitPack](https://jitpack.io/#francitoshi/nut-base). Add the repository, then only the modules you use:

**Gradle**

```groovy
repositories 
{
    maven { url 'https://jitpack.io' }
}

dependencies 
{
    implementation 'com.github.francitoshi.nut-base:nut-base-commons:0.2.8'
    implementation 'com.github.francitoshi.nut-base:nut-base-collections:0.2.8'
    implementation 'com.github.francitoshi.nut-base:nut-base-io:0.2.8'
    implementation 'com.github.francitoshi.nut-base:nut-base-math:0.2.8'
    implementation 'com.github.francitoshi.nut-base:nut-base-net:0.2.8'
    implementation 'com.github.francitoshi.nut-base:nut-base-crypto:0.2.8'
    implementation 'com.github.francitoshi.nut-base:nut-base-text:0.2.8'
    implementation 'com.github.francitoshi.nut-base:nut-base-ui:0.2.8'
    implementation 'com.github.francitoshi.nut-base:nut-base-util:0.2.8'
    implementation 'com.github.francitoshi.nut-base:nut-base-audio:0.2.8'
}
```


Modules, in dependency order (each one after the modules it depends on):
`nut-base-commons`, `nut-base-collections`, `nut-base-io`, `nut-base-math`, `nut-base-net`, `nut-base-crypto`, `nut-base-text`, `nut-base-ui`, `nut-base-util`, `nut-base-audio`.
Inter-module dependencies come in transitively; replace the version with the latest tag.
