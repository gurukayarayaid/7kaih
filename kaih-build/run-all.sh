#!/bin/bash
# ============================================================
# Build APK 7 KAIH — toolchain offline lengkap (idempotent)
# Semua di /home/user/kaih-build agar persisten
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
echo "JAVA_HOME=$JH"
export JAVA_HOME=$JH
export PATH=$JAVA_HOME/bin:$PATH
java -version 2>&1 | head -1

echo "### [1] Kotlin compiler 2.0.20 (npm)"
if [ ! -f $KB/kotlinc/package/bin/kotlinc ]; then
  curl -s -o $KB/kc.tgz "https://registry.npmjs.org/kotlin-compiler/-/kotlin-compiler-2.0.20.tgz"
  mkdir -p $KB/kotlinc && tar -xzf $KB/kc.tgz -C $KB/kotlinc
fi
$KB/kotlinc/package/bin/kotlinc -version 2>&1 | head -1

echo "### [2] android.jar API 34 (Sable)"
if [ ! -f $KB/sable/android-34/android.jar ]; then
  export GIT_TERMINAL_PROMPT=0
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/Sable/android-platforms.git $KB/sable 2>&1 | tail -1
  (cd $KB/sable && git sparse-checkout set android-34 2>&1 | tail -1)
fi
ls -la $KB/sable/android-34/android.jar

echo "### [3] R8/D8 (LineageOS prebuilt)"
if [ ! -f $KB/r8pre/r8-master.jar ]; then
  export GIT_TERMINAL_PROMPT=0
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/LineageOS/android_prebuilts_r8.git $KB/r8pre 2>&1 | tail -1
  (cd $KB/r8pre && git sparse-checkout set --no-cone "/r8-master.jar" && git checkout -q)
fi
ls -la $KB/r8pre/r8-master.jar

echo "### [4] aapt2 (npm aaptjs3)"
if [ ! -x $KB/tools/aapt2 ]; then
  curl -s -o $KB/aaptjs3.tgz "https://registry.npmjs.org/aaptjs3/-/aaptjs3-2.0.2.tgz"
  mkdir -p $KB/tools && cd $KB/tools && tar -xzf $KB/aaptjs3.tgz package/bin/x64/linux/aapt2
  cp package/bin/x64/linux/aapt2 ./aapt2 && chmod +x ./aapt2 && rm -rf package
  cd $KB
fi
$KB/tools/aapt2 version

echo "### [5] Cache repos (compose/firebase jars)"
export GIT_TERMINAL_PROMPT=0
if [ ! -d $KB/baby/.git ]; then
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/LexChien/BabyGrowthApp.git $KB/baby 2>&1 | tail -1
  (cd $KB/baby && git sparse-checkout set Android/BabyGrowth/.gradle/caches/modules-2/files-2.1 2>&1 | tail -1)
fi
if [ ! -d $KB/fr/.git ]; then
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/Twigg99/foodRecipeApp.git $KB/fr 2>&1 | tail -1
  (cd $KB/fr && git sparse-checkout set caches/transforms-3 2>&1 | tail -1)
fi
if [ ! -d $KB/co/.git ]; then
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/BTierEditor/Co-Lab-Connect.git $KB/co 2>&1 | tail -1
  (cd $KB/co && git sparse-checkout set caches/transforms-3 2>&1 | tail -1)
fi
if [ ! -d $KB/hoops/.git ]; then
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/Boba0101/hoops_lab_repo.git $KB/hoops 2>&1 | tail -1
  (cd $KB/hoops && git sparse-checkout set android/caches/modules-2/files-2.1 2>&1 | tail -1)
fi
echo "cache repos OK"

echo "### [6] Un-shade compose compiler plugin"
ASM=$KB/baby/Android/BabyGrowth/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.7/73d7b3086e14beb604ced229c302feff6449723/asm-9.7.jar
ASMC=$KB/baby/Android/BabyGrowth/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm-commons/9.7/e86dda4696d3c185fcc95d8d311904e7ce38a53f/asm-commons-9.7.jar
ASMT=$KB/baby/Android/BabyGrowth/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm-tree/9.7/e446a17b175bfb733b87c5c2560ccb4e57d69f1a/asm-tree-9.7.jar
if [ ! -f $KB/compose-compiler-unshaded.jar ]; then
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
  $KB/kotlinc/package/bin/kotlinc $KB/unshade/U.kt -classpath $ASM:$ASMC:$ASMT -include-runtime -d $KB/unshade/u.jar 2>&1 | head -3
  java -cp $KB/unshade/u.jar:$ASM:$ASMC:$ASMT UKt $KB/kotlinc/package/lib/compose-compiler.jar $KB/compose-compiler-unshaded.jar
fi
echo "compose plugin unshaded OK"

echo "### [7] Staging jars"
B=$KB/baby/Android/BabyGrowth/.gradle/caches/modules-2/files-2.1
H=$KB/hoops/android/caches/modules-2/files-2.1
mkdir -p $KB/kaih/jars $KB/kaih/dex $KB/kaih/classes $KB/kaih/res $KB/kaih/dex-out $KB/kaih/gen
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
# apksig dari baby
APKSIG=$(find $B/com.android.tools.build/apksig/8.9.0 -name "apksig-8.9.0.jar" 2>/dev/null | head -1)
cp "$APKSIG" $KB/kaih/apksig.jar && echo "apksig OK ($(stat -c%s $KB/kaih/apksig.jar)B)"
# protolite dari hoops
PL=$(find $H/com.google.firebase/protolite-well-known-types/18.0.0 -name "*.aar" 2>/dev/null | head -1)
[ -n "$PL" ] && unzip -o -q "$PL" classes.jar -d /tmp/plx && cp /tmp/plx/classes.jar $KB/kaih/jars/protolite.jar && echo "protolite OK"
# Transform classes.jar (fr) -> tt-*.jar
python3 - <<'PY'
import os, shutil, glob
dst = "/home/user/kaih-build/kaih/jars"
dstd = "/home/user/kaih-build/kaih/dex"
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
print("transform classes.jar:", n, "dex:", m)
PY
echo "jars total: $(ls $KB/kaih/jars | wc -l), dex: $(ls $KB/kaih/dex | wc -l)"

echo "### [8] Res + manifest + firebase config"
rm -rf $KB/kaih/res && cp -r $REPO/app/src/main/res $KB/kaih/res
python3 - <<'PY'
import json, os
with open('/home/user/7kaih/android/app/google-services.json') as f:
    raw = "\n".join(l for l in f.read().splitlines() if not l.strip().startswith("#"))
    g = json.loads(raw)
pi = g.get("project_info", {})
c0 = (g.get("client") or [{}])[0]
ci = c0.get("client_info", {})
keys = [k.get("current_key","") for k in c0.get("api_key", [])]
api = keys[0] if keys else ""
web = ""
for oc in c0.get("oauth_client", []):
    if oc.get("client_type") == 3:
        web = oc.get("client_id", "")
vals = {
    "google_app_id": ci.get("mobilesdk_app_id", ""),
    "google_api_key": api,
    "google_crash_reporting_api_key": api,
    "gcm_defaultSenderId": pi.get("project_number", ""),
    "google_storage_bucket": pi.get("storage_bucket", ""),
    "project_id": pi.get("project_id", ""),
    "default_web_client_id": web,
}
lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
for k, v in vals.items():
    lines.append(f'    <string name="{k}" translatable="false">{v}</string>')
lines.append("</resources>")
os.makedirs("/home/user/kaih-build/kaih/res/values", exist_ok=True)
open("/home/user/kaih-build/kaih/res/values/firebase_config.xml", "w").write("\n".join(lines))
print("firebase config:", pi.get("project_id"), "| app_id:", ci.get("mobilesdk_app_id", "")[:30])
PY
sed 's|<manifest |<manifest package="com.sdn.semambung.kaih" |' $REPO/app/src/main/AndroidManifest.xml > $KB/kaih/AndroidManifest.xml

echo "### [9] aapt2 link"
$KB/tools/aapt2 compile --dir $KB/kaih/res -o $KB/kaih/compiled.zip
$KB/tools/aapt2 link -o $KB/kaih/base.apk \
  -I $KB/sable/android-34/android.jar \
  --manifest $KB/kaih/AndroidManifest.xml \
  --min-sdk-version 26 --target-sdk-version 34 \
  --version-code 1 --version-name "1.0" \
  $KB/kaih/compiled.zip
echo "base.apk: $(stat -c%s $KB/kaih/base.apk) bytes"

echo "### [10] Kompilasi Kotlin"
CP=$(ls $KB/kaih/jars/*.jar | tr '\n' ':')$KB/sable/android-34/android.jar
find $REPO/app/src/main/java -name "*.kt" | sort > $KB/kaih/sources.txt
rm -rf $KB/kaih/classes && mkdir -p $KB/kaih/classes
$KB/kotlinc/package/bin/kotlinc \
  -Xplugin=$KB/compose-compiler-unshaded.jar \
  -classpath "$CP" \
  -jvm-target 1.8 \
  -d $KB/kaih/classes \
  @$KB/kaih/sources.txt 2>&1 | grep -E "error:" | head -20 || true
echo "kelas: $(find $KB/kaih/classes -name '*.class' | wc -l)"
[ $(find $KB/kaih/classes -name '*.class' | wc -l) -gt 0 ] || { echo "KOMPILASI GAGAL"; exit 1; }

echo "### [11] Jar app"
(cd $KB/kaih/classes && python3 -c "
import zipfile, os
z = zipfile.ZipFile('../app-classes.jar', 'w', zipfile.ZIP_DEFLATED)
for r, _, fs in os.walk('.'):
    for f in fs:
        p = os.path.join(r, f)
        z.write(p, p)
z.close()
print('app-classes.jar OK')
")

echo "### [12] D8 dexing"
DEX_JARS="$KB/kaih/jars/kotlin-stdlib.jar $KB/kaih/jars/annotations.jar $KB/kaih/jars/coroutines-core.jar $KB/kaih/jars/coroutines-android.jar"
DEX_JARS="$DEX_JARS $(ls $KB/kaih/jars/compose-*.jar $KB/kaih/jars/activity*.jar $KB/kaih/jars/lifecycle-*.jar $KB/kaih/jars/core*.jar $KB/kaih/jars/savedstate*.jar $KB/kaih/jars/annotation.jar $KB/kaih/jars/arch-core-common.jar $KB/kaih/jars/collection.jar 2>/dev/null)"
DEX_JARS="$DEX_JARS $KB/kaih/jars/tt-firebase-firestore-24.4.5.jar $KB/kaih/jars/tt-firebase-common-20.3.1.jar $KB/kaih/jars/tt-firebase-components-17.1.0.jar $KB/kaih/jars/tt-firebase-database-collection-18.0.1.jar $KB/kaih/jars/tt-firebase-appcheck-interop-16.1.1.jar $KB/kaih/jars/tt-grpc-android-1.52.1.jar $KB/kaih/jars/tt-play-services-tasks-18.0.2.jar $KB/kaih/jars/tt-play-services-basement-18.1.0.jar $KB/kaih/jars/tt-play-services-base-18.0.1.jar $KB/kaih/jars/protolite.jar $KB/kaih/jars/gson.jar $KB/kaih/jars/guava.jar $KB/kaih/jars/failureaccess.jar"
rm -rf $KB/kaih/dex-out && mkdir -p $KB/kaih/dex-out
java -Xmx3g -cp $KB/r8pre/r8-master.jar com.android.tools.r8.D8 \
  --release --min-api 26 \
  --lib $KB/sable/android-34/android.jar \
  --output $KB/kaih/dex-out \
  $KB/kaih/app-classes.jar $DEX_JARS 2>&1 | tail -3
ls $KB/kaih/dex-out/

echo "### [13] Kemas APK"
python3 - <<'PY'
import zipfile, struct, os, glob
base = "/home/user/kaih-build/kaih/base.apk"
out = "/home/user/kaih-build/kaih/unsigned.apk"
dexout = "/home/user/kaih-build/kaih/dex-out"
extra_dex = sorted(glob.glob("/home/user/kaih-build/kaih/dex/*.dex"))
zin = zipfile.ZipFile(base)
entries = [(i.filename, i) for i in zin.infolist()]
dex_files = sorted(glob.glob(dexout + "/classes*.dex"))
all_dex = dex_files + extra_dex
print("dex dimasukkan:", len(all_dex))

def build_zip(out_path, src_entries, new_entries):
    f = open(out_path, "wb")
    offset = 0
    centrals = []
    def write_entry(name, data, force_store=False, align=4):
        nonlocal offset
        comp = zipfile.ZIP_STORED if force_store else zipfile.ZIP_DEFLATED
        import zlib
        if comp == zipfile.ZIP_STORED:
            crc = zipfile.crc32(data) & 0xffffffff
            raw = data; csize = len(data)
        else:
            co = zlib.compressobj(9, zlib.DEFLATED, -15)
            raw = co.compress(data) + co.flush()
            crc = zipfile.crc32(data) & 0xffffffff
            csize = len(raw)
        pad = 0
        if comp == zipfile.ZIP_STORED:
            pad = (align - ((offset + 30 + len(name)) % align)) % align
        extra = b"\x00" * pad
        local = struct.pack("<IHHHHHIIIHH", 0x04034b50, 20, 0x800, comp, 0, 0, crc, csize, len(data), len(name), pad) + name.encode() + extra
        f.write(local); f.write(raw)
        centrals.append((name, crc, csize, len(data), comp, offset))
        offset += len(local) + csize
    for name, info in src_entries:
        data = zin.read(name)
        write_entry(name, data, force_store=(name == "resources.arsc"))
    n = 0
    for p in all_dex:
        with open(p, "rb") as df:
            data = df.read()
        write_entry("classes.dex" if n == 0 else f"classes{n+1}.dex", data, force_store=True)
        n += 1
    cd_start = offset
    for name, crc, csize, usize, comp, off in centrals:
        rec = struct.pack("<IHHHHHHIIIHHHHHII", 0x02014b50, 20, 20, 0x800, comp, 0, 0, crc, csize, usize, len(name), 0, 0, 0, 0, 0, off) + name.encode()
        f.write(rec); offset += len(rec)
    cd_size = offset - cd_start
    eocd = struct.pack("<IHHHHIIH", 0x06054b50, 0, 0, len(centrals), len(centrals), cd_size, cd_start, 0)
    f.write(eocd); f.close()
build_zip(out, entries, all_dex)
print("unsigned.apk:", os.path.getsize(out))
PY

echo "### [14] Tanda tangan (keystore Wabcraft)"
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
        .setInputApk(File(inApk))
        .setOutputApk(File(outApk))
        .setV1SigningEnabled(true)
        .setV2SigningEnabled(true)
        .build()
        .sign()
    println("APK ditandatangani: $outApk")
}
KT
$KB/kotlinc/package/bin/kotlinc $KB/kaih/Signer.kt -classpath $KB/kaih/apksig.jar -include-runtime -d $KB/kaih/signer.jar 2>&1 | grep -E "error" | head -5 || true
java -cp $KB/kaih/signer.jar:$KB/kaih/apksig.jar SignerKt \
  $REPO/kaih-release.keystore wabcraft2026 wabcraft \
  $KB/kaih/unsigned.apk \
  $KB/KAIH-1.0-release-signed.apk

echo "### [15] Verifikasi"
$KB/tools/aapt2 dump badging $KB/KAIH-1.0-release-signed.apk 2>/dev/null | head -6
unzip -t $KB/KAIH-1.0-release-signed.apk > /dev/null 2>&1 && echo "zip: OK" || echo "zip: RUSAK"
unzip -l $KB/KAIH-1.0-release-signed.apk | grep -E "classes[0-9]*.dex|resources.arsc" | head -8
sha256sum $KB/KAIH-1.0-release-signed.apk
echo "=== SELESAI ==="
