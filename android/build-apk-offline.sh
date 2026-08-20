#!/bin/bash
# ============================================================
# build-apk-offline.sh — Build APK 7 KAIH lengkap tanpa Gradle
# (toolchain diunduh dari PyPI/npm/GitHub; membutuhkan internet)
# Hasil: KAIH-1.0-release-signed.apk (signed Wabcraft)
# ============================================================
set -e
KB=/home/user/kaih-build
REPO=/home/user/7kaih/android
mkdir -p $KB
cd $KB

echo "### [0] JDK (jdk4py via PyPI)"
if [ ! -x $KB/venv/bin/python ]; then python3 -m venv $KB/venv; fi
$KB/venv/bin/pip -q install jdk4py==21.0.8.2 2>&1 | tail -1 || true
JH=$(find $KB/venv -name "java" -path "*/bin/*" | head -1 | sed 's|/bin/java||')
export JAVA_HOME=$JH
export PATH=$JAVA_HOME/bin:$PATH
java -version 2>&1 | head -1

echo "### [1] Kotlin compiler 2.0.20 (npm)"
if [ ! -f $KB/kotlinc19/package/bin/kotlinc ]; then
  curl -s -o $KB/kc.tgz "https://registry.npmjs.org/kotlin-compiler/-/kotlin-compiler-2.0.20.tgz"
  mkdir -p $KB/kotlinc && tar -xzf $KB/kc.tgz -C $KB/kotlinc
fi

echo "### [2] android.jar API 34 (Sable)"
if [ ! -f $KB/sable/android-34/android.jar ]; then
  export GIT_TERMINAL_PROMPT=0
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/Sable/android-platforms.git $KB/sable
  (cd $KB/sable && git sparse-checkout set android-34 >/dev/null 2>&1)
fi

echo "### [3] R8/D8 (LineageOS prebuilt)"
if [ ! -f $KB/r8pre/r8-master.jar ]; then
  export GIT_TERMINAL_PROMPT=0
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/LineageOS/android_prebuilts_r8.git $KB/r8pre
  (cd $KB/r8pre && git sparse-checkout set --no-cone "/r8-master.jar" && git checkout -q)
fi

echo "### [4] aapt2 (npm aaptjs3)"
if [ ! -x $KB/tools/aapt2 ]; then
  curl -s -o $KB/aaptjs3.tgz "https://registry.npmjs.org/aaptjs3/-/aaptjs3-2.0.2.tgz"
  mkdir -p $KB/tools && cd $KB/tools && tar -xzf $KB/aaptjs3.tgz package/bin/x64/linux/aapt2
  cp package/bin/x64/linux/aapt2 ./aapt2 && chmod +x ./aapt2 && rm -rf package && cd $KB
fi

echo "### [5] Cache repos (jar library)"
export GIT_TERMINAL_PROMPT=0
if [ ! -d $KB/baby/.git ]; then
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/LexChien/BabyGrowthApp.git $KB/baby
  (cd $KB/baby && git sparse-checkout set Android/BabyGrowth/.gradle/caches/modules-2/files-2.1 >/dev/null 2>&1)
fi
if [ ! -d $KB/fr/.git ]; then
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/Twigg99/foodRecipeApp.git $KB/fr
  (cd $KB/fr && git sparse-checkout set caches/transforms-3 >/dev/null 2>&1)
fi
if [ ! -d $KB/co/.git ]; then
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/BTierEditor/Co-Lab-Connect.git $KB/co
  (cd $KB/co && git sparse-checkout set caches/transforms-3 >/dev/null 2>&1)
fi
if [ ! -d $KB/hoops/.git ]; then
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/Boba0101/hoops_lab_repo.git $KB/hoops
  (cd $KB/hoops && git sparse-checkout set android/caches/modules-2/files-2.1 >/dev/null 2>&1)
fi

echo "### [6] Un-shade compose compiler plugin"
ASM=$(ls $KB/baby/Android/BabyGrowth/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.7/*/asm-9.7.jar | head -1)
ASMC=$(ls $KB/baby/Android/BabyGrowth/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm-commons/9.7/*/asm-commons-9.7.jar | head -1)
ASMT=$(ls $KB/baby/Android/BabyGrowth/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm-tree/9.7/*/asm-tree-9.7.jar | head -1)
if [ ! -f $KB/compose-compiler-158-unshaded.jar ]; then
  mkdir -p $KB/unshade
  cat > $KB/unshade/U.kt <<'KT'
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

private val PREFIX = "org/jetbrains/kotlin/com/intellij"

class PrefixRemapper : Remapper() {
    override fun map(internalName: String): String =
        if (internalName.startsWith(PREFIX)) "com/intellij" + internalName.removePrefix(PREFIX) else internalName
}

fun main(args: Array<String>) {
    val (inJar, outJar) = args
    val remapper = PrefixRemapper()
    val jf = JarFile(inJar)
    val out = JarOutputStream(File(outJar).outputStream())
    val entries = jf.entries()
    var n = 0
    while (entries.hasMoreElements()) {
        val e = entries.nextElement()
        if (e.isDirectory) continue
        val data = jf.getInputStream(e).readBytes()
        if (e.name.endsWith(".class")) {
            val reader = ClassReader(data)
            val writer = ClassWriter(0)
            reader.accept(ClassRemapper(writer, remapper), 0)
            out.putNextEntry(JarEntry(e.name))
            out.write(writer.toByteArray())
            n++
        } else {
            out.putNextEntry(JarEntry(e.name))
            out.write(data)
        }
        out.closeEntry()
    }
    out.close(); jf.close()
    println("remapped $n classes -> $outJar")
}
KT
  $KB/kotlinc19/package/bin/kotlinc $KB/unshade/U.kt -classpath $ASM:$ASMC:$ASMT -include-runtime -d $KB/unshade/u.jar 2>&1 | head -3
  java -cp $KB/unshade/u.jar:$ASM:$ASMC:$ASMT UKt $KB/kotlinc/package/lib/compose-compiler.jar $KB/compose-compiler-158-unshaded.jar
fi

echo "### [7] Staging jars (dari cache)"
B=$KB/baby/Android/BabyGrowth/.gradle/caches/modules-2/files-2.1
H=$KB/hoops/android/caches/modules-2/files-2.1
mkdir -p $KB/kaih/jars $KB/kaih/dex $KB/kaih/classes $KB/kaih/res $KB/kaih/dex-out $KB/kaih/reslib $KB/kaih/rtxt $KB/kaih/services
stage_aar() { local ga=$1 ver=$2 name=$3; local aar=$(find $B/$ga/$ver -name "*.aar" ! -name "*sources*" 2>/dev/null | head -1); if [ -z "$aar" ]; then echo "MISSING AAR $ga/$ver"; return; fi; local tmp=$(mktemp -d); unzip -o -q "$aar" classes.jar -d "$tmp" && cp "$tmp/classes.jar" $KB/kaih/jars/$name.jar; rm -rf "$tmp"; }
stage_jar() { local ga=$1 ver=$2 name=$3; local j=$(find $B/$ga/$ver -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" 2>/dev/null | head -1); if [ -z "$j" ]; then echo "MISSING JAR $ga/$ver"; return; fi; cp "$j" $KB/kaih/jars/$name.jar; }
stage_aar androidx.compose.ui/ui-android 1.6.3 compose-ui.jar
stage_aar androidx.compose.ui/ui-graphics-android 1.6.3 compose-ui-graphics.jar
stage_aar androidx.compose.ui/ui-text-android 1.6.3 compose-ui-text.jar
stage_aar androidx.compose.ui/ui-unit-android 1.6.3 compose-ui-unit.jar
stage_aar androidx.compose.ui/ui-geometry-android 1.6.3 compose-ui-geometry.jar
stage_aar androidx.compose.ui/ui-util-android 1.6.3 compose-ui-util.jar
stage_aar androidx.compose.foundation/foundation-android 1.6.3 compose-foundation.jar
stage_aar androidx.compose.foundation/foundation-layout-android 1.6.3 compose-foundation-layout.jar
stage_aar androidx.compose.runtime/runtime-android 1.6.3 compose-runtime.jar
stage_aar androidx.compose.runtime/runtime-saveable-android 1.6.3 compose-runtime-saveable.jar
stage_aar androidx.compose.animation/animation-android 1.6.3 compose-animation.jar
stage_aar androidx.compose.animation/animation-core-android 1.6.3 compose-animation-core.jar
stage_aar androidx.compose.material3/material3-android 1.2.1 compose-material3.jar
stage_aar androidx.compose.material/material-icons-extended-android 1.6.3 compose-icons-ext.jar
stage_aar androidx.compose.material/material-icons-core-android 1.6.3 compose-icons-core.jar
stage_aar androidx.compose.material/material-ripple-android 1.6.3 compose-ripple.jar
stage_aar androidx.compose.material/material-android 1.6.3 compose-material.jar
stage_aar androidx.activity/activity-compose 1.8.2 activity-compose.jar
stage_aar androidx.activity/activity-ktx 1.8.2 activity-ktx.jar
stage_aar androidx.activity/activity 1.8.2 activity.jar
stage_aar androidx.lifecycle/lifecycle-viewmodel-compose 2.6.2 lifecycle-viewmodel-compose.jar
stage_aar androidx.lifecycle/lifecycle-runtime-ktx 2.6.2 lifecycle-runtime-ktx.jar
stage_aar androidx.lifecycle/lifecycle-viewmodel-ktx 2.6.2 lifecycle-viewmodel-ktx.jar
stage_aar androidx.lifecycle/lifecycle-runtime 2.6.2 lifecycle-runtime.jar
stage_aar androidx.lifecycle/lifecycle-viewmodel 2.6.2 lifecycle-viewmodel.jar
stage_aar androidx.lifecycle/lifecycle-viewmodel-savedstate 2.6.2 lifecycle-viewmodel-savedstate.jar
stage_aar androidx.lifecycle/lifecycle-livedata-core 2.6.2 lifecycle-livedata-core.jar
stage_jar androidx.lifecycle/lifecycle-common 2.6.2 lifecycle-common.jar
stage_jar androidx.lifecycle/lifecycle-common-java8 2.6.2 lifecycle-common-java8.jar
stage_aar androidx.core/core-ktx 1.12.0 core-ktx.jar
stage_aar androidx.core/core 1.12.0 core.jar
stage_aar androidx.savedstate/savedstate 1.2.1 savedstate.jar
stage_aar androidx.savedstate/savedstate-ktx 1.2.1 savedstate-ktx.jar
stage_jar androidx.annotation/annotation-jvm 1.7.0 annotation.jar
stage_jar androidx.arch.core/core-common 2.2.0 arch-core-common.jar
stage_jar androidx.collection/collection-jvm 1.4.0 collection.jar
stage_jar org.jetbrains.kotlin/kotlin-stdlib 2.0.20 kotlin-stdlib.jar
stage_jar org.jetbrains/annotations 13.0 annotations.jar
stage_jar org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm 1.7.3 coroutines-core.jar
stage_jar org.jetbrains.kotlinx/kotlinx-coroutines-android 1.7.3 coroutines-android.jar
stage_jar com.google.code.gson/gson 2.9.0 gson.jar
stage_jar com.google.guava/guava 31.1-jre guava.jar
stage_jar com.google.guava/failureaccess 1.0.1 failureaccess.jar
# customview-poolingcontainer (WAJIB: ViewCompositionStrategy)
stage_aar androidx.customview/customview-poolingcontainer 1.0.0 customview-poolingcontainer.jar
APKSIG=$(find $B/com.android.tools.build/apksig/8.9.0 -name "apksig-8.9.0.jar" 2>/dev/null | head -1)
cp "$APKSIG" $KB/kaih/apksig.jar
PL=$(find $H/com.google.firebase/protolite-well-known-types/18.0.0 -name "*.aar" 2>/dev/null | head -1)
if [ -n "$PL" ]; then rm -rf /tmp/plx && mkdir -p /tmp/plx && unzip -o -q "$PL" classes.jar -d /tmp/plx && chmod u+w /tmp/plx/classes.jar && rm -f $KB/kaih/jars/protolite.jar && cp /tmp/plx/classes.jar $KB/kaih/jars/protolite.jar && chmod u+w $KB/kaih/jars/protolite.jar; fi
# rename .jar.jar bug + pastikan writable
cd $KB/kaih/jars && for f in *.jar.jar; do mv "$f" "${f%.jar}"; done 2>/dev/null; chmod -R u+w $KB/kaih/jars; cd $KB
echo "jars: $(ls $KB/kaih/jars | wc -l)"

# firebase-auth-interop dari hoops
AI=$(find $H/com.google.firebase/firebase-auth-interop/19.0.2 -name "*.aar" ! -name "*sources*" 2>/dev/null | head -1)
if [ -n "$AI" ]; then rm -rf /tmp/aix && mkdir -p /tmp/aix && unzip -o -q "$AI" classes.jar -d /tmp/aix && chmod u+w /tmp/aix/classes.jar && rm -f $KB/kaih/jars/firebase-auth-interop.jar && cp /tmp/aix/classes.jar $KB/kaih/jars/firebase-auth-interop.jar && chmod u+w $KB/kaih/jars/firebase-auth-interop.jar; fi

echo "### [7b] Ekstrak transform (firebase/grpc/library jar+dex dari cache Gradle)"
python3 - <<'PY'
import os, shutil, glob
dst = "/home/user/kaih-build/kaih/jars"
dstd = "/home/user/kaih-build/kaih/dex"
os.makedirs(dst, exist_ok=True); os.makedirs(dstd, exist_ok=True)
n = m = 0
for base in ["/home/user/kaih-build/fr/caches/transforms-3", "/home/user/kaih-build/co/caches/transforms-3"]:
    for t in glob.glob(base + "/*/transformed/*"):
        name = os.path.basename(t)
        cj = os.path.join(t, "jars", "classes.jar")
        if os.path.isfile(cj) and os.path.getsize(cj) > 100:
            shutil.copy(cj, os.path.join(dst, "tt-" + name + ".jar")); n += 1
        dx = os.path.join(t, "classes.dex")
        if os.path.isfile(dx) and os.path.getsize(dx) > 100:
            shutil.copy(dx, os.path.join(dstd, "tt-" + name + ".dex")); m += 1
print("transform: classes.jar", n, "dex", m)
PY
chmod -R u+w $KB/kaih/jars $KB/kaih/dex 2>/dev/null || true
echo "jars total: $(ls $KB/kaih/jars | wc -l), dex: $(ls $KB/kaih/dex | wc -l)"

echo "### [8] Resource library (compile res + R.txt)"
set +e  # jangan berhenti walau satu resource gagal
compile_aar() { local base=$1 ga=$2 ver=$3 label=$4; local aar=$(find $base/$ga/$ver -name "*.aar" ! -name "*sources*" 2>/dev/null | head -1); [ -z "$aar" ] && return 1; local tmp=$(mktemp -d); unzip -o -q "$aar" -d "$tmp"; chmod -R u+w "$tmp" 2>/dev/null; if [ -d "$tmp/res" ] && [ -n "$(ls -A $tmp/res 2>/dev/null)" ]; then rm -f "$KB/kaih/reslib/$label.zip"; $KB/tools/aapt2 compile --dir "$tmp/res" -o "$KB/kaih/reslib/$label.zip" 2>/dev/null; fi; if [ -f "$tmp/R.txt" ]; then rm -f "$KB/kaih/rtxt/$label.R.txt"; cp "$tmp/R.txt" "$KB/kaih/rtxt/$label.R.txt"; chmod u+w "$KB/kaih/rtxt/$label.R.txt"; fi; rm -rf "$tmp"; }
compile_aar $B androidx.compose.ui/ui-android 1.6.3 ui
compile_aar $B androidx.compose.ui/ui-text-android 1.6.3 ui-text
compile_aar $B androidx.compose.ui/ui-graphics-android 1.6.3 ui-graphics
compile_aar $B androidx.compose.foundation/foundation-android 1.6.3 foundation
compile_aar $B androidx.compose.foundation/foundation-layout-android 1.6.3 foundation-layout
compile_aar $B androidx.compose.material3/material3-android 1.2.1 material3
compile_aar $B androidx.compose.material/material-android 1.6.3 material
compile_aar $B androidx.compose.material/material-ripple-android 1.6.3 material-ripple
compile_aar $B androidx.compose.material/material-icons-core-android 1.6.3 icons-core
compile_aar $B androidx.compose.material/material-icons-extended-android 1.6.3 icons-ext
compile_aar $B androidx.compose.runtime/runtime-android 1.6.3 runtime
compile_aar $B androidx.compose.runtime/runtime-saveable-android 1.6.3 runtime-saveable
compile_aar $B androidx.compose.animation/animation-android 1.6.3 animation
compile_aar $B androidx.compose.animation/animation-core-android 1.6.3 animation-core
compile_aar $B androidx.activity/activity 1.8.2 activity
compile_aar $B androidx.lifecycle/lifecycle-runtime 2.6.2 lifecycle-runtime
compile_aar $B androidx.lifecycle/lifecycle-viewmodel 2.6.2 lifecycle-viewmodel
compile_aar $B androidx.lifecycle/lifecycle-viewmodel-savedstate 2.6.2 lifecycle-vm-savedstate
compile_aar $B androidx.lifecycle/lifecycle-livedata-core 2.6.2 lifecycle-livedata
compile_aar $B androidx.core/core 1.12.0 core
compile_aar $B androidx.savedstate/savedstate 1.2.1 savedstate
compile_aar $B androidx.arch.core/core-runtime 2.2.0 core-runtime
compile_aar $B androidx.emoji2/emoji2 1.3.0 emoji2
compile_aar $B androidx.customview/customview-poolingcontainer 1.0.0 customview-poolingcontainer
compile_aar $B androidx.startup/startup-runtime 1.1.1 startup-runtime
compile_aar $H com.google.firebase/firebase-common 20.4.3 fb-common
compile_aar $H com.google.firebase/firebase-components 17.1.5 fb-components
compile_aar $H com.google.firebase/firebase-database-collection 18.0.1 fb-dbcol
compile_aar $H com.google.firebase/firebase-appcheck-interop 17.0.0 fb-appcheck
compile_aar $H com.google.firebase/firebase-auth-interop 19.0.2 fb-auth
compile_aar $H com.google.firebase/protolite-well-known-types 18.0.0 protolite
compile_aar $H com.google.android.gms/play-services-base 18.0.1 gms-base
compile_aar $H com.google.android.gms/play-services-basement 18.0.0 gms-basement
compile_aar $H com.google.android.gms/play-services-tasks 18.0.2 gms-tasks
compile_aar $H io.grpc/grpc-android 1.57.2 grpc-android
echo "reslib: $(ls $KB/kaih/reslib | wc -l), rtxt: $(ls $KB/kaih/rtxt | wc -l)"
set -e

echo "### [9] Firebase config + manifest"
rm -rf $KB/kaih/res && cp -r $REPO/app/src/main/res $KB/kaih/res
python3 - <<'PY'
import json, os
g = json.load(open('/home/user/7kaih/android/app/google-services.json'))
pi = g["project_info"]; c0 = g["client"][0]; ci = c0["client_info"]
api = c0["api_key"][0]["current_key"]
vals = {"google_app_id": ci["mobilesdk_app_id"], "google_api_key": api,
        "google_crash_reporting_api_key": api, "gcm_defaultSenderId": pi["project_number"],
        "google_storage_bucket": pi["storage_bucket"], "project_id": pi["project_id"], "default_web_client_id": ""}
lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
for k, v in vals.items(): lines.append(f'    <string name="{k}" translatable="false">{v}</string>')
lines.append("</resources>")
os.makedirs("/home/user/kaih-build/kaih/res/values", exist_ok=True)
open("/home/user/kaih-build/kaih/res/values/firebase_config.xml", "w").write("\n".join(lines))
print("firebase:", pi["project_id"])
PY
sed 's|<manifest |<manifest package="com.sdn.semambung.kaih" |' $REPO/app/src/main/AndroidManifest.xml > $KB/kaih/AndroidManifest.xml

echo "### [10] aapt2 link (merged)"
$KB/tools/aapt2 compile --dir $KB/kaih/res -o $KB/kaih/app-res.zip
$KB/tools/aapt2 link -o $KB/kaih/base.apk -I $KB/sable/android-34/android.jar \
  --manifest $KB/kaih/AndroidManifest.xml \
  --min-sdk-version 26 --target-sdk-version 34 --version-code 3 --version-name "1.0.2" \
  --auto-add-overlay --emit-ids $KB/kaih/ids.txt \
  $KB/kaih/reslib/*.zip $KB/kaih/app-res.zip
echo "base.apk: $(stat -c%s $KB/kaih/base.apk)"

echo "### [11] Generate R.jar"
mkdir -p $KB/rgen
cat > $KB/rgen/GenR.kt <<'KT'
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

fun main(args: Array<String>) {
    val idsFile = args[0]; val rtxtDir = args[1]; val outJar = args[2]
    val ids = HashMap<Pair<String,String>, Int>()
    File(idsFile).readLines().forEach { line ->
        val m = Regex("""([\w.]+):(\w+)/([\w.]+)\s*=\s*(0x[0-9a-fA-F]+)""").find(line)
        if (m != null) ids[Pair(m.groupValues[2], m.groupValues[3])] = m.groupValues[4].removePrefix("0x").toLong(16).toInt()
    }
    println("ids.txt: ${ids.size}")
    val pkgMap = listOf(
        "ui" to "androidx/compose/ui", "ui-text" to "androidx/compose/ui/text", "ui-graphics" to "androidx/compose/ui/graphics",
        "foundation" to "androidx/compose/foundation", "foundation-layout" to "androidx/compose/foundation/layout",
        "material3" to "androidx/compose/material3", "material" to "androidx/compose/material", "material-ripple" to "androidx/compose/material/ripple",
        "runtime" to "androidx/compose/runtime", "runtime-saveable" to "androidx/compose/runtime/saveable",
        "animation" to "androidx/compose/animation", "animation-core" to "androidx/compose/animation/core",
        "activity" to "androidx/activity", "lifecycle-runtime" to "androidx/lifecycle/runtime", "lifecycle-viewmodel" to "androidx/lifecycle/viewmodel",
        "core" to "androidx/core", "savedstate" to "androidx/savedstate", "core-runtime" to "androidx/arch/core",
        "fb-common" to "com/google/firebase", "gms-base" to "com/google/android/gms/base", "gms-basement" to "com/google/android/gms/common",
        "customview-poolingcontainer" to "androidx/customview/poolingcontainer", "startup-runtime" to "androidx/startup"
    )
    fun pushInt(mv: org.objectweb.asm.MethodVisitor, v: Int) {
        when (v) {
            in 0..5 -> mv.visitInsn(Opcodes.ICONST_0 + v)
            in Byte.MIN_VALUE..Byte.MAX_VALUE -> mv.visitIntInsn(Opcodes.BIPUSH, v)
            in Short.MIN_VALUE..Short.MAX_VALUE -> mv.visitIntInsn(Opcodes.SIPUSH, v)
            else -> mv.visitLdcInsn(v)
        }
    }
    fun genClass(name: String, intFields: Map<String,Int>, arrayFields: Map<String,IntArray>): ByteArray {
        val cw = ClassWriter(0)
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER, name, null, "java/lang/Object", null)
        intFields.forEach { (fn, v) ->
            val fv = cw.visitField(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL, fn, "I", null, v); fv.visitEnd()
        }
        arrayFields.forEach { (fn, arr) ->
            val fv = cw.visitField(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL, fn, "[I", null, null); fv.visitEnd()
        }
        if (arrayFields.isNotEmpty()) {
            val mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)
            mv.visitCode()
            arrayFields.forEach { (fn, arr) ->
                pushInt(mv, arr.size)
                mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT)
                arr.forEachIndexed { i, v ->
                    mv.visitInsn(Opcodes.DUP); pushInt(mv, i); pushInt(mv, v); mv.visitInsn(Opcodes.IASTORE)
                }
                mv.visitFieldInsn(Opcodes.PUTSTATIC, name, fn, "[I")
            }
            mv.visitInsn(Opcodes.RETURN)
            mv.visitMaxs(6, 0)
            mv.visitEnd()
        }
        cw.visitEnd()
        return cw.toByteArray()
    }
    val out = JarOutputStream(File(outJar).outputStream())
    var total = 0
    val written = HashSet<String>()
    fun tulis(nama: String, data: ByteArray) { if (!written.add(nama)) return; out.putNextEntry(JarEntry(nama)); out.write(data); out.closeEntry(); total++ }
    for ((label, pkg) in pkgMap) {
        val rf = File(rtxtDir, "$label.R.txt")
        if (!rf.exists()) continue
        val ints = LinkedHashMap<String, MutableMap<String,Int>>()
        val arrays = LinkedHashMap<String, MutableMap<String,IntArray>>()
        val styleableArrContent = HashMap<String, List<Int>>()
        val styleableMembers = HashMap<String, MutableList<Pair<String,Int>>>()
        rf.readLines().forEach { line ->
            val mArr = Regex("""int\[\]\s+(\w+)\s+([\w.]+)\s*\{\s*([^}]*)\}""").find(line)
            if (mArr != null) {
                val t = mArr.groupValues[1]; val n = mArr.groupValues[2]
                val content = mArr.groupValues[3].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    .map { if (it.startsWith("0x")) it.removePrefix("0x").toLong(16).toInt() else 0 }
                arrays.getOrPut(t) { LinkedHashMap() }[n] = IntArray(0)
                styleableArrContent[n] = content
                return@forEach
            }
            val m = Regex("""int\s+(\w+)\s+([\w.]+)\s+(\d+|0x[0-9a-fA-F]+)""").find(line)
            if (m != null) {
                val t = m.groupValues[1]; val n = m.groupValues[2]; val raw = m.groupValues[3]
                val v = if (raw.startsWith("0x")) raw.removePrefix("0x").toLong(16).toInt() else raw.toInt()
                if (t == "styleable" && n.contains("_")) {
                    val st = n.substringBeforeLast("_")
                    styleableMembers.getOrPut(st) { mutableListOf() }.add(Pair(n, v))
                } else {
                    val id = ids[Pair(t, n)]
                    if (id != null) ints.getOrPut(t) { LinkedHashMap() }[n] = id
                }
            }
        }
        for ((st, members) in styleableMembers) {
            val sorted = members.sortedBy { it.second }
            val content = styleableArrContent[st] ?: emptyList()
            val arr = IntArray(sorted.size)
            sorted.forEachIndexed { idx, (mname, _) ->
                val attrName = mname.substringAfterLast("_", "")
                val isAndroid = mname.contains("_android_")
                arr[idx] = if (isAndroid) content.getOrElse(idx) { 0 } else ids[Pair("attr", attrName)] ?: content.getOrElse(idx) { 0 }
            }
            arrays.getOrPut("styleable") { LinkedHashMap() }[st] = arr
            for ((mname, idxv) in sorted) ints.getOrPut("styleable") { LinkedHashMap() }[mname] = idxv
        }
        if (ints.isEmpty() && arrays.isEmpty()) { tulis("$pkg/R.class", genClass("$pkg/R", emptyMap(), emptyMap())); continue }
        tulis("$pkg/R.class", genClass("$pkg/R", emptyMap(), emptyMap()))
        for ((t, fields) in ints) { tulis("$pkg/R\$$t.class", genClass("$pkg/R\$$t", fields, arrays[t] ?: emptyMap())) }
        for ((t, arrs) in arrays) { if (t !in ints) tulis("$pkg/R\$$t.class", genClass("$pkg/R\$$t", emptyMap(), arrs)) }
        println("R $pkg")
    }
    out.close()
    println("total: $total -> $outJar")
}
KT
$KB/kotlinc19/package/bin/kotlinc $KB/rgen/GenR.kt -classpath $ASM -include-runtime -d $KB/rgen/genr.jar 2>&1 | grep error | head -3 || true
java -cp $KB/rgen/genr.jar:$ASM GenRKt $KB/kaih/ids.txt $KB/kaih/rtxt $KB/kaih/R.jar 2>&1 | tail -3

echo "### [12] Kompilasi Kotlin app"
CP=$(ls $KB/kaih/jars/*.jar | tr '\n' ':')$KB/sable/android-34/android.jar
find $REPO/app/src/main/java -name "*.kt" | sort > $KB/kaih/sources.txt
rm -rf $KB/kaih/classes && mkdir -p $KB/kaih/classes
$KB/kotlinc19/package/bin/kotlinc -Xplugin=$KB/compose-compiler-158-unshaded.jar -classpath "$CP" \
  -jvm-target 1.8 -d $KB/kaih/classes @$KB/kaih/sources.txt 2>&1 | grep -E "error:" | head -10 || true
echo "kelas: $(find $KB/kaih/classes -name '*.class' | wc -l)"
[ $(find $KB/kaih/classes -name '*.class' | wc -l) -gt 10 ] || { echo "KOMPILASI GAGAL"; exit 1; }
(cd $KB/kaih/classes && python3 -c "
import zipfile, os
z = zipfile.ZipFile('../app-classes.jar','w',zipfile.ZIP_DEFLATED)
for r,_,fs in os.walk('.'):
    for f in fs:
        p=os.path.join(r,f); z.write(p,p)
z.close()")

echo "### [13] D8"
DEX_JARS="$KB/kaih/jars/kotlin-stdlib.jar $KB/kaih/jars/annotations.jar $KB/kaih/jars/coroutines-core.jar $KB/kaih/jars/coroutines-android.jar"
DEX_JARS="$DEX_JARS $(ls $KB/kaih/jars/compose-*.jar $KB/kaih/jars/activity*.jar $KB/kaih/jars/lifecycle-*.jar $KB/kaih/jars/core*.jar $KB/kaih/jars/savedstate*.jar $KB/kaih/jars/annotation.jar $KB/kaih/jars/arch-core-common.jar $KB/kaih/jars/collection.jar 2>/dev/null)"
DEX_JARS="$DEX_JARS $KB/kaih/jars/customview-poolingcontainer.jar"
DEX_JARS="$DEX_JARS $KB/kaih/jars/tt-firebase-firestore-24.4.5.jar $KB/kaih/jars/tt-firebase-common-20.3.1.jar $KB/kaih/jars/tt-firebase-components-17.1.0.jar $KB/kaih/jars/tt-firebase-database-collection-18.0.1.jar $KB/kaih/jars/tt-firebase-appcheck-interop-16.1.1.jar $KB/kaih/jars/tt-grpc-android-1.52.1.jar $KB/kaih/jars/tt-play-services-tasks-18.0.2.jar $KB/kaih/jars/tt-play-services-basement-18.1.0.jar $KB/kaih/jars/tt-play-services-base-18.0.1.jar $KB/kaih/jars/protolite.jar $KB/kaih/jars/gson.jar $KB/kaih/jars/failureaccess.jar $KB/kaih/jars/firebase-auth-interop.jar $KB/kaih/jars/tt-versionedparcelable-1.1.1.jar $KB/kaih/jars/tt-startup-runtime-1.1.1.jar"
rm -rf $KB/kaih/dex-out && mkdir -p $KB/kaih/dex-out
if ! java -Xmx3g -cp $KB/r8pre/r8-master.jar com.android.tools.r8.D8 \
  --release --min-api 26 --lib $KB/sable/android-34/android.jar \
  --output $KB/kaih/dex-out $KB/kaih/app-classes.jar $KB/kaih/R.jar $DEX_JARS > $KB/d8.log 2>&1; then
  echo "D8 GAGAL:"; tail -15 $KB/d8.log; exit 1
fi
ls $KB/kaih/dex-out/

echo "### [14] META-INF/services"
printf 'kotlinx.coroutines.android.AndroidExceptionPreHandler\n' > $KB/kaih/services/kotlinx.coroutines.CoroutineExceptionHandler
printf 'kotlinx.coroutines.android.AndroidDispatcherFactory\n' > $KB/kaih/services/kotlinx.coroutines.internal.MainDispatcherFactory
printf 'io.grpc.internal.PickFirstLoadBalancerProvider\nio.grpc.util.SecretRoundRobinLoadBalancerProvider$Provider\nio.grpc.util.OutlierDetectionLoadBalancerProvider\n' > $KB/kaih/services/io.grpc.LoadBalancerProvider
printf 'io.grpc.internal.DnsNameResolverProvider\n' > $KB/kaih/services/io.grpc.NameResolverProvider
printf 'io.grpc.okhttp.OkHttpChannelProvider\n' > $KB/kaih/services/io.grpc.ManagedChannelProvider

echo "### [15] Kemas APK"
python3 - <<'PY'
import zipfile, struct, os, glob
base="/home/user/kaih-build/kaih/base.apk"; out="/home/user/kaih-build/kaih/unsigned.apk"
dexout="/home/user/kaih-build/kaih/dex-out"; svc_dir="/home/user/kaih-build/kaih/services"
extra_names = ["tt-grpc-api-1.52.1.dex","tt-grpc-context-1.52.1.dex","tt-grpc-core-1.52.1.dex","tt-grpc-okhttp-1.52.1.dex","tt-grpc-protobuf-lite-1.52.1.dex","tt-grpc-stub-1.52.1.dex","tt-okhttp-3.12.1.dex","tt-okio-1.17.5.dex","tt-protobuf-javalite-3.22.3.dex","tt-perfmark-api-0.26.0.dex","tt-firebase-annotations-16.2.0.dex","tt-guava-31.1-android.dex","tt-jsr305-3.0.2.dex","tt-j2objc-annotations-1.3.dex","tt-checker-qual-3.33.0.dex","tt-animal-sniffer-annotations-1.23.dex","tt-error_prone_annotations-2.15.0.dex","tt-javax.inject-1.dex","tt-concurrent-futures-1.1.0.dex","tt-emoji2-1.2.0-runtime.dex","tt-tracing-1.0.0-runtime.dex","tt-interpolator-1.0.0-runtime.dex"]
extra_dex=[]
for n in extra_names:
    p=os.path.join("/home/user/kaih-build/kaih/dex",n)
    if os.path.exists(p): extra_dex.append(p)
    else: print("dex tidak ada:", n)
zin=zipfile.ZipFile(base); entries=[(i.filename,i) for i in zin.infolist()]
all_dex=sorted(glob.glob(dexout+"/classes*.dex"))+extra_dex
print("dex:",len(all_dex))
def build_zip(out_path, src_entries, new_entries):
    f=open(out_path,"wb"); offset=0; centrals=[]
    def write_entry(name,data,force_store=False,align=4):
        nonlocal offset
        comp=zipfile.ZIP_STORED if force_store else zipfile.ZIP_DEFLATED
        import zlib
        if comp==zipfile.ZIP_STORED:
            crc=zipfile.crc32(data)&0xffffffff; raw=data; csize=len(data)
        else:
            co=zlib.compressobj(9,zlib.DEFLATED,-15); raw=co.compress(data)+co.flush()
            crc=zipfile.crc32(data)&0xffffffff; csize=len(raw)
        pad=0
        if comp==zipfile.ZIP_STORED:
            pad=(align-((offset+30+len(name))%align))%align
        extra=b"\x00"*pad
        local=struct.pack("<IHHHHHIIIHH",0x04034b50,20,0x800,comp,0,0,crc,csize,len(data),len(name),pad)+name.encode()+extra
        f.write(local); f.write(raw)
        centrals.append((name,crc,csize,len(data),comp,offset)); offset+=len(local)+csize
    for name,info in src_entries:
        data=zin.read(name); write_entry(name,data,force_store=(name=="resources.arsc"))
    for sf in sorted(os.listdir(svc_dir)):
        with open(os.path.join(svc_dir,sf),"rb") as sv:
            write_entry("META-INF/services/"+sf, sv.read(), force_store=True)
    n=0
    for p in all_dex:
        with open(p,"rb") as df: data=df.read()
        write_entry("classes.dex" if n==0 else f"classes{n+1}.dex",data,force_store=True); n+=1
    cd_start=offset
    for name,crc,csize,usize,comp,off in centrals:
        rec=struct.pack("<IHHHHHHIIIHHHHHII",0x02014b50,20,20,0x800,comp,0,0,crc,csize,usize,len(name),0,0,0,0,0,off)+name.encode()
        f.write(rec); offset+=len(rec)
    cd_size=offset-cd_start
    eocd=struct.pack("<IHHHHIIH",0x06054b50,0,0,len(centrals),len(centrals),cd_size,cd_start,0)
    f.write(eocd); f.close()
build_zip(out,entries,all_dex)
print("unsigned.apk:",os.path.getsize(out))
PY

echo "### [16] Tanda tangan"
cat > $KB/kaih/Signer.kt <<'KT'
import com.android.apksig.ApkSigner
import com.android.apksig.ApkSigner.SignerConfig
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

fun main(args: Array<String>) {
    val (ksPath, pass, alias, inApk, outApk) = args
    val ks = KeyStore.getInstance("JKS")
    ks.load(FileInputStream(ksPath), pass.toCharArray())
    val key = ks.getKey(alias, pass.toCharArray()) as PrivateKey
    val cert = ks.getCertificate(alias) as X509Certificate
    val config = SignerConfig.Builder("wabcraft", key, arrayListOf(cert)).build()
    ApkSigner.Builder(listOf(config))
        .setInputApk(File(inApk)).setOutputApk(File(outApk))
        .setV1SigningEnabled(true).setV2SigningEnabled(true)
        .build().sign()
    println("APK ditandatangani: $outApk")
}
KT
$KB/kotlinc19/package/bin/kotlinc $KB/kaih/Signer.kt -classpath $KB/kaih/apksig.jar -include-runtime -d $KB/kaih/signer.jar 2>&1 | grep error | head -3 || true
java -cp $KB/kaih/signer.jar:$KB/kaih/apksig.jar SignerKt \
  $REPO/kaih-release.keystore wabcraft2026 wabcraft \
  $KB/kaih/unsigned.apk $KB/KAIH-1.0-release-signed.apk

echo "### [17] Verifikasi"
$KB/tools/aapt2 dump badging $KB/KAIH-1.0-release-signed.apk 2>/dev/null | head -5
unzip -t $KB/KAIH-1.0-release-signed.apk >/dev/null 2>&1 && echo "zip OK" || echo "zip RUSAK"
python3 - <<'PY'
import zipfile
z=zipfile.ZipFile("/home/user/kaih-build/KAIH-1.0-release-signed.apk")
names=z.namelist(); m=z.read("AndroidManifest.xml")
print("manifest:", all(s.encode('utf-16-le') in m for s in ["FirebaseInitProvider","ComponentDiscoveryService","FirebaseCommonRegistrar","FirestoreRegistrar","CrashActivity","CrashReportProvider"]))
svc=[n for n in names if n.startswith("META-INF/services/")]
print("services:",len(svc))
dexes=[n for n in names if n.startswith("classes") and n.endswith(".dex")]
all_d=b"".join(z.read(d) for d in dexes)
for s in [b"Lcom/sdn/semambung/kaih/MainActivity",b"Landroidx/customview/poolingcontainer/PoolingContainer",b"Landroidx/concurrent/futures/AbstractResolvableFuture",b"Lcom/google/firebase/FirebaseApp",b"Landroidx/emoji2/text/flatbuffer/MetadataList",b"Landroidx/tracing/Trace",b"Lcom/sdn/semambung/kaih/CrashReportProvider"]:
    print(f"  {s.decode()}: {s in all_d}")
print("WABCRAFT.RSA:", "META-INF/WABCRAFT.RSA" in names)
PY
sha256sum $KB/KAIH-1.0-release-signed.apk
echo "=== SELESAI ==="
