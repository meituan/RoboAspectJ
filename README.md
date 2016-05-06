RoboAspectJ
=====

RoboAspectJ is a Gradle plugin to introduce [AspectJ](https://eclipse.org/aspectj/) (**A**spect-**O**rient **P**rogramming) to Android project.
It compiles aspects(if needed) and weave them all together in **FULL-PROJECT** scope. This means project
(or subproject) sources, external libraries and local dependencies will all be dealt with by default.

> Note: This plugin may change due to the modification of [transform-api](http://tools.android.com/tech-docs/new-build-system/transform-api).
> So you may keep track of RoboAspectJ to make sure you're using the most recent version.

latest version: **v0.9.1**

Prerequisite
-----
[Android Plugin](http://developer.android.com/tools/revisions/gradle-plugin.html) (application) 2.1.0

Apply
-----
Add plugin dependency in buildscript classpath:

``` groovy
buildscript {
    dependencies {
        classpath 'com.meituan.gradle:roboaspectj:0.9.+'
    }
}
```

Apply plugin:

``` groovy
apply plugin: 'com.meituan.roboaspectj'
```

Coding
-----

There are basically 2 ways to write your aspects and weave them into production code:

### As Source
write aspects in **@AspectJ syntax** under your project's java source directory. e.g. `{$projectDir}/src/main/java/`

### As Library

compile and bundle your aspects independently using ajc, then make it dependency in build script. For example:

``` groovy
compile 'com.example.myaspects:library:1.0'
```

> This way may be a little bit complicated. But it's suitable for those who want to maintain their aspects as an independent project.

Variant-Specific Concern
------

While RoboAspectJ is registered globally, we still can do our crosscutting concern under specific variant.
Actually, this is already done by [Android plugin](http://tools.android.com/tech-docs/new-build-system/user-guide#TOC-Build-Variants).

### As Source

put variant-specific aspects under corresponding folder.

For example, I want to do some performance monitoring in `myflavor`, so I will write aspects under `${projectDir}/src/myflavor/java/`.

### As Library

add variant-specific aspects dependency to corresponding configuration scope.

``` groovy
myflavorCompile 'com.example.myaspects:library:1.0'
```

Configuration
-----

There is an extension `aspectj` for you to do some tweaking.

### Exclude

If you want to leave some artifact untouched from AspectJ, using:

``` groovy
aspectj {
	exclude group: 'com.google.android', module: 'support-v4'
}
```

### Java runtime

When applying `rxjava` or `retrolambda`, you may need `jrt.jar` as classpath. Configure it by:

``` groovy
aspectj {
	javartNeeded true
}
```

### Disable

For debug or performance use, you can disable weaving:
``` groovy
aspectj {
    enable false    //by default, it's true and you don't have to add this statement.
}
```
alternatively, set `roboaspectj.enable` property `false` when run gradle.

```
$ gradle clean assembleDebug -Droboaspectj.enable=false
```
Maybe you want it to be smarter to disable it when it's a debug flavor, then add this to your build
script:

``` groovy
aspectj {
    disableWhenDebug true // default is false
}
```

or, specify `roboaspectj.disableWhenDebug` property.

```
$ gradle clean assembleDebug -Droboaspectj.disableWhenDebug=true
```

Though weaving is disabled, Aspects and AspectJ compile dependencies are all still there. It's only
the weaving step doesn't happen.

> Note: Corresponding property has precedence over config in build script in both of these 2 cases.
For instance, weaving will not take effect when your `roboaspectj.enable` property is `false`,
no matter what you config in build script.

License
-------
Code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0.txt).

Feedback
-----
This plugin is currently a prototype, and it still has much to improve. Feel free to contact: [xuxizhi@meituan.com](mailto:xuxizhi@meituan.com)
