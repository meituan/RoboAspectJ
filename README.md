RoboAspectJ
=====

RoboAspectJ is a Gradle plugin to introduce [AspectJ](https://eclipse.org/aspectj/) (**A**spect-**O**rient **P**rogramming) to Android project.
It compiles aspects(if needed) and weave them all together in **FULL-PROJECT** scope. This means project
(or subproject) sources, external libraries and local dependencies will all be dealt with by default.

> Note: This plugin may change due to the modification of [transform-api](http://tools.android.com/tech-docs/new-build-system/transform-api).
> So you may keep track of RoboAspectJ to make sure you're using the most recent version.

current version: **v0.8.6**

Prerequisite
-----
[Android Plugin](http://developer.android.com/tools/revisions/gradle-plugin.html) (application) 1.5.0

Apply
-----
Add plugin dependency in buildscript classpath:

``` groovy
buildscript {
    dependencies {
        classpath 'com.meituan.gradle:roboaspectj:0.8.+'
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

Or, you want it to be smarter to disable it when it's a debug flavor:

``` groovy
aspectj {
    disableWhenDebug true // default is false
}
```

> Note: Aspects, AspectJ compile dependencies are all still there, they are just not being woven.

License
-------
Code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0.txt).

Feedback
-----
This plugin is currently a prototype, and it still has much to improve. Feel free to contact: [xuxizhi@meituan.com](mailto:xuxizhi@meituan.com)
