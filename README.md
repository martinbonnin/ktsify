# ktsify.kts ✨✨

# For a more comprehensive tool, check out [GradleKotlinConverter](https://github.com/bernaferrari/GradleKotlinConverter), which does the same replacement and a lot more.

Turn your `build.gradle` into `build.gradle.kts`.

This script uses a list of regexes to turn single quotes into double ones, dependencies, etc... It's not perfect but it can save you time. Just don't take the output as gospel.

```shell script

# if not done yet, install kscript:
curl -s "https://get.sdkman.io" | bash  # install sdkman
source ~/.bash_profile                  # add sdkman to PATH
sdk install kotlin                      # install Kotlin
sdk install kscript

# run ktsify
./ktsify.kts path/to/project
# your build.gradle and settings.gradle file will be converted to build.gradle.kts and settings.gradle.kts
# backups are kept under .old suffixes
```


## Troubleshooting

### Extra properties

Extra properties are very dynamic and hard to translate to kotlin by nature. This script uses [groovy.util.Eval.x()](http://docs.groovy-lang.org/latest/html/api/groovy/util/Eval.html) to escape them and get their value as `Any`. 

You can also tweak the heuristics that try to detect the extra properties:

```kotlin
fun String.isExtra() = when {
    startsWith("dep") -> true
    startsWith("androidConfig") -> true
    else -> false
}
```

In all cases, you might need to cast the value to your required type 

## Replacements cheat sheet

<table>
<tr><td></td><td><b>Groovy</b></td><td><b>Kotlin</b></td></tr>

<tr>
<td>Strings</td>
<td><pre>'a string'</pre></td>
<td><pre>"a string"</pre></td>
</tr>

<tr>
<td>Conventions</td>
<td>
<pre>
java {
  targetCompatibility JavaVersion.VERSION_1_7
}
</pre>
</td>
<td>
<pre>
withConvention(JavaPluginConvention::class) {
  targetCompatibility = JavaVersion.VERSION_1_7
}
</pre>
</td>
</tr>

<tr>
<td>Extensions</td>
<td>
<pre>
android {
  compileSdkVersion 28
}
</pre>
</td>
<td>
<pre>
extensions.findByType(BaseExtension::class.java)!!.apply {
  compileSdkVersion(28)
}
</pre>
</td>
</tr>

<tr>
<td>Tasks</td>
<td>
<pre>
compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
</pre>
</td>
<td>
<pre>
tasks.withType&lt;KotlinCompile&gt; {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
</pre>
</td>
</tr>
</table>



