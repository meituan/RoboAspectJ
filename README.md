RoboAspectJ
=====

This is a Gradle plugin enables [Android Plugin](http://developer.android.com/tools/revisions/gradle-plugin.html)
to compile AspectJ code(if needed) and then weave them into production code in **FULL-PROJECT** scope.
This means project source, external lib, subproject source and local dependencies will all be dealt with by default.

> Note: This plugin may change due to the modification of [transform-api](http://tools.android.com/tech-docs/new-build-system/transform-api). So you may keep track of RoboAspectJ to make sure you're using the most recent version.


Apply
-----
Add plugin dependency in buildscript classpath:

``` groovy
buildscript {
	repositories {
		// your repo
	}

	classpath {
        classpath 'com.meituan.gradle:roboaspectj:0.8.+'

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

compile and bundle your aspects independently using ajc, then make it dependency in build script. Example:

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
aspectj{
	javartNeeded true
}
```

Prerequisite
-----
Android plugin (application) 1.5.0

License
-------
Code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0.txt).

Feedback
-----
This plugin is currently a prototype, and it still has much to improve. Feel free to contact: [xuxizhi@meituan.com](mailto:xuxizhi@meituan.com)
