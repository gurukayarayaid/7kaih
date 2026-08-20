#!/bin/bash
# ============================================================
# build-test-apks.sh — Build APK uji (minimal + firebase)
# Jalankan SETELAH build-apk-offline.sh (toolchain sudah siap)
# ============================================================
set -e
KB=/home/user/kaih-build
REPO=/home/user/7kaih/android
T=$REPO/test-apk
JH=$(find $KB/venv -name "java" -path "*/bin/*" | head -1 | sed 's|/bin/java||')
export JAVA_HOME=$JH; export PATH=$JAVA_HOME/bin:$PATH
A2=$KB/tools/aapt2

pack_apk() { # $1=base.apk $2=dex_out $3=unsigned.apk
python3 - "$1" "$2" "$3" <<'PY'
import zipfile, struct, os, sys, glob
base, dexout, out = sys.argv[1], sys.argv[2], sys.argv[3]
zin = zipfile.ZipFile(base)
entries = [(i.filename, i) for i in zin.infolist()]
all_dex = sorted(glob.glob(dexout + "/classes*.dex"))
def build_zip(out_path, src_entries, new_entries):
    f = open(out_path, "wb"); offset = 0; centrals = []
    def write_entry(name, data, force_store=False, align=4):
        nonlocal offset
        comp = zipfile.ZIP_STORED if force_store else zipfile.ZIP_DEFLATED
        import zlib
        if comp == zipfile.ZIP_STORED:
            crc = zipfile.crc32(data) & 0xffffffff; raw = data; csize = len(data)
        else:
            co = zlib.compressobj(9, zlib.DEFLATED, -15); raw = co.compress(data) + co.flush()
            crc = zipfile.crc32(data) & 0xffffffff; csize = len(raw)
        pad = 0
        if comp == zipfile.ZIP_STORED:
            pad = (align - ((offset + 30 + len(name)) % align)) % align
        extra = b"\x00" * pad
        local = struct.pack("<IHHHHHIIIHH", 0x04034b50, 20, 0x800, comp, 0, 0, crc, csize, len(data), len(name), pad) + name.encode() + extra
        f.write(local); f.write(raw)
        centrals.append((name, crc, csize, len(data), comp, offset)); offset += len(local) + csize
    for name, info in src_entries:
        data = zin.read(name); write_entry(name, data, force_store=(name == "resources.arsc"))
    n = 0
    for p in new_entries:
        with open(p, "rb") as df: data = df.read()
        write_entry("classes.dex" if n == 0 else f"classes{n+1}.dex", data, force_store=True); n += 1
    cd_start = offset
    for name, crc, csize, usize, comp, off in centrals:
        rec = struct.pack("<IHHHHHHIIIHHHHHII", 0x02014b50, 20, 20, 0x800, comp, 0, 0, crc, csize, usize, len(name), 0, 0, 0, 0, 0, off) + name.encode()
        f.write(rec); offset += len(rec)
    cd_size = offset - cd_start
    eocd = struct.pack("<IHHHHIIH", 0x06054b50, 0, 0, len(centrals), len(centrals), cd_size, cd_start, 0)
    f.write(eocd); f.close()
build_zip(out, entries, all_dex)
print("packed:", os.path.getsize(out))
PY
}

sign() { # $1=unsigned $2=output
java -cp $KB/kaih/signer.jar:$KB/kaih/apksig.jar SignerKt \
  $REPO/kaih-release.keystore wabcraft2026 wabcraft "$1" "$2"
}

# ================= TEST 1: MINIMAL =================
echo "== [TEST 1] minimal =="
rm -rf $KB/test1 && mkdir -p $KB/test1/classes
cp -r $T/res $KB/test1/res
$A2 compile --dir $KB/test1/res -o $KB/test1/res.zip
$A2 link -o $KB/test1/base.apk -I $KB/sable/android-34/android.jar \
  --manifest $T/AndroidManifest.xml --min-sdk-version 26 --target-sdk-version 34 \
  --version-code 1 --version-name "1" $KB/test1/res.zip
CP="$KB/sable/android-34/android.jar:$KB/kaih/jars/kotlin-stdlib-1.9.22.jar"
$KB/kotlinc19/package/bin/kotlinc -classpath "$CP" -jvm-target 1.8 -d $KB/test1/classes \
  $T/src/main/java/com/sdn/semambung/kaih/test/MainActivity.kt
(cd $KB/test1/classes && python3 -c "
import zipfile, os
z = zipfile.ZipFile('../app.jar','w',zipfile.ZIP_DEFLATED)
for r,_,fs in os.walk('.'):
    for f in fs:
        p=os.path.join(r,f); z.write(p,p)
z.close()")
rm -rf $KB/test1/dex && mkdir -p $KB/test1/dex
java -Xmx3g -cp $KB/r8pre/r8-master.jar com.android.tools.r8.D8 --release --min-api 26 \
  --lib $KB/sable/android-34/android.jar --output $KB/test1/dex \
  $KB/test1/app.jar $KB/kaih/jars/kotlin-stdlib-1.9.22.jar
pack_apk $KB/test1/base.apk $KB/test1/dex $KB/test1/unsigned.apk
sign $KB/test1/unsigned.apk $REPO/test-apk/test1-minimal.apk
echo "test1-minimal.apk OK"

# ================= TEST 2: FIREBASE =================
echo "== [TEST 2] firebase =="
rm -rf $KB/test2 && mkdir -p $KB/test2/classes
cp -r $T/res $KB/test2/res
# config firebase
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
os.makedirs("/home/user/kaih-build/test2/res/values", exist_ok=True)
open("/home/user/kaih-build/test2/res/values/firebase_config.xml", "w").write("\n".join(lines))
print("config:", pi["project_id"])
PY
$A2 compile --dir $KB/test2/res -o $KB/test2/res.zip
$A2 link -o $KB/test2/base.apk -I $KB/sable/android-34/android.jar \
  --manifest $T/AndroidManifest-fb.xml --min-sdk-version 26 --target-sdk-version 34 \
  --version-code 1 --version-name "1" $KB/test2/res.zip
CP2="$KB/sable/android-34/android.jar:$KB/kaih/jars/kotlin-stdlib-1.9.22.jar:$KB/kaih/jars/tt-firebase-common-20.3.1.jar:$KB/kaih/jars/tt-firebase-components-17.1.0.jar"
$KB/kotlinc19/package/bin/kotlinc -classpath "$CP2" -jvm-target 1.8 -d $KB/test2/classes \
  $T/src-fb/main/java/com/sdn/semambung/kaih/testfb/MainActivity.kt
(cd $KB/test2/classes && python3 -c "
import zipfile, os
z = zipfile.ZipFile('../app.jar','w',zipfile.ZIP_DEFLATED)
for r,_,fs in os.walk('.'):
    for f in fs:
        p=os.path.join(r,f); z.write(p,p)
z.close()")
FB_JARS="$KB/kaih/jars/kotlin-stdlib-1.9.22.jar $KB/kaih/jars/annotations.jar $KB/kaih/jars/coroutines-core.jar $KB/kaih/jars/coroutines-android.jar"
FB_JARS="$FB_JARS $KB/kaih/jars/collection.jar $KB/kaih/jars/core.jar $KB/kaih/jars/core-ktx.jar $KB/kaih/jars/arch-core-common.jar"
FB_JARS="$FB_JARS $KB/kaih/jars/tt-firebase-firestore-24.4.5.jar $KB/kaih/jars/tt-firebase-common-20.3.1.jar $KB/kaih/jars/tt-firebase-components-17.1.0.jar $KB/kaih/jars/tt-firebase-database-collection-18.0.1.jar $KB/kaih/jars/tt-firebase-appcheck-interop-16.1.1.jar $KB/kaih/jars/tt-grpc-android-1.52.1.jar $KB/kaih/jars/tt-play-services-tasks-18.0.2.jar $KB/kaih/jars/tt-play-services-basement-18.1.0.jar $KB/kaih/jars/tt-play-services-base-18.0.1.jar $KB/kaih/jars/protolite.jar $KB/kaih/jars/gson.jar $KB/kaih/jars/failureaccess.jar $KB/kaih/jars/firebase-auth-interop.jar $KB/kaih/jars/tt-versionedparcelable-1.1.1.jar $KB/kaih/jars/tt-startup-runtime-1.1.1.jar $KB/kaih/jars/customview-poolingcontainer.jar"
rm -rf $KB/test2/dex && mkdir -p $KB/test2/dex
java -Xmx3g -cp $KB/r8pre/r8-master.jar com.android.tools.r8.D8 --release --min-api 26 \
  --lib $KB/sable/android-34/android.jar --output $KB/test2/dex \
  $KB/test2/app.jar $KB/kaih/R.jar $FB_JARS
# tambah dex ekstra (grpc/okhttp/protobuf/guava)
for n in tt-grpc-api-1.52.1.dex tt-grpc-context-1.52.1.dex tt-grpc-core-1.52.1.dex tt-grpc-okhttp-1.52.1.dex tt-grpc-protobuf-lite-1.52.1.dex tt-grpc-stub-1.52.1.dex tt-okhttp-3.12.1.dex tt-okio-1.17.5.dex tt-protobuf-javalite-3.22.3.dex tt-perfmark-api-0.26.0.dex tt-firebase-annotations-16.2.0.dex tt-guava-31.1-android.dex tt-jsr305-3.0.2.dex tt-j2objc-annotations-1.3.dex tt-checker-qual-3.33.0.dex tt-animal-sniffer-annotations-1.23.dex tt-error_prone_annotations-2.15.0.dex tt-javax.inject-1.dex tt-concurrent-futures-1.1.0.dex tt-emoji2-1.2.0-runtime.dex tt-tracing-1.0.0-runtime.dex tt-interpolator-1.0.0-runtime.dex; do
  if [ -f $KB/kaih/dex/$n ]; then cp $KB/kaih/dex/$n $KB/test2/dex/$(echo $n | sed 's/^tt-//'); fi
done
# rename dex berurutan
i=4
for f in $KB/test2/dex/*.dex; do
  base=$(basename $f)
  case $base in classes*.dex) ;; *) mv "$f" "$KB/test2/dex/classes$i.dex"; i=$((i+1));; esac
done
pack_apk $KB/test2/base.apk $KB/test2/dex $KB/test2/unsigned.apk
sign $KB/test2/unsigned.apk $REPO/test-apk/test2-firebase.apk
echo "test2-firebase.apk OK"

# ================= TEST 2B: FIREBASE tanpa provider =================
echo "== [TEST 2B] firebase tanpa provider (tampilkan error di layar) =="
rm -rf $KB/test2b && mkdir -p $KB/test2b/classes
cp -r $T/res $KB/test2b/res
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
os.makedirs("/home/user/kaih-build/test2b/res/values", exist_ok=True)
open("/home/user/kaih-build/test2b/res/values/firebase_config.xml", "w").write("\n".join(lines))
print("config:", pi["project_id"])
PY
$A2 compile --dir $KB/test2b/res -o $KB/test2b/res.zip
$A2 link -o $KB/test2b/base.apk -I $KB/sable/android-34/android.jar \
  --manifest $T/AndroidManifest-fb2.xml --min-sdk-version 26 --target-sdk-version 34 \
  --version-code 1 --version-name "1" $KB/test2b/res.zip
CP2B="$KB/sable/android-34/android.jar:$KB/kaih/jars/kotlin-stdlib-1.9.22.jar:$KB/kaih/jars/tt-firebase-common-20.3.1.jar:$KB/kaih/jars/tt-firebase-components-17.1.0.jar:$KB/kaih/jars/tt-firebase-firestore-24.4.5.jar"
$KB/kotlinc19/package/bin/kotlinc -classpath "$CP2B" -jvm-target 1.8 -d $KB/test2b/classes \
  $T/src-fb2/main/java/com/sdn/semambung/kaih/testfb/MainActivity.kt
(cd $KB/test2b/classes && python3 -c "
import zipfile, os
z = zipfile.ZipFile('../app.jar','w',zipfile.ZIP_DEFLATED)
for r,_,fs in os.walk('.'):
    for f in fs:
        p=os.path.join(r,f); z.write(p,p)
z.close()")
rm -rf $KB/test2b/dex && mkdir -p $KB/test2b/dex
java -Xmx3g -cp $KB/r8pre/r8-master.jar com.android.tools.r8.D8 --release --min-api 26 \
  --lib $KB/sable/android-34/android.jar --output $KB/test2b/dex \
  $KB/test2b/app.jar $KB/kaih/R.jar $FB_JARS
for n in tt-grpc-api-1.52.1.dex tt-grpc-context-1.52.1.dex tt-grpc-core-1.52.1.dex tt-grpc-okhttp-1.52.1.dex tt-grpc-protobuf-lite-1.52.1.dex tt-grpc-stub-1.52.1.dex tt-okhttp-3.12.1.dex tt-okio-1.17.5.dex tt-protobuf-javalite-3.22.3.dex tt-perfmark-api-0.26.0.dex tt-firebase-annotations-16.2.0.dex tt-guava-31.1-android.dex tt-jsr305-3.0.2.dex tt-j2objc-annotations-1.3.dex tt-checker-qual-3.33.0.dex tt-animal-sniffer-annotations-1.23.dex tt-error_prone_annotations-2.15.0.dex tt-javax.inject-1.dex tt-concurrent-futures-1.1.0.dex; do
  if [ -f $KB/kaih/dex/$n ]; then cp $KB/kaih/dex/$n $KB/test2b/dex/$(echo $n | sed 's/^tt-//'); fi
done
i=4
for f in $KB/test2b/dex/*.dex; do
  base=$(basename $f)
  case $base in classes*.dex) ;; *) mv "$f" "$KB/test2b/dex/classes$i.dex"; i=$((i+1));; esac
done
# META-INF/services (coroutines Main dispatcher + grpc)
mkdir -p $KB/test2b/services
printf 'kotlinx.coroutines.android.AndroidExceptionPreHandler\n' > $KB/test2b/services/kotlinx.coroutines.CoroutineExceptionHandler
printf 'kotlinx.coroutines.android.AndroidDispatcherFactory\n' > $KB/test2b/services/kotlinx.coroutines.internal.MainDispatcherFactory
printf 'io.grpc.internal.PickFirstLoadBalancerProvider\nio.grpc.util.SecretRoundRobinLoadBalancerProvider$Provider\nio.grpc.util.OutlierDetectionLoadBalancerProvider\n' > $KB/test2b/services/io.grpc.LoadBalancerProvider
printf 'io.grpc.internal.DnsNameResolverProvider\n' > $KB/test2b/services/io.grpc.NameResolverProvider
printf 'io.grpc.okhttp.OkHttpChannelProvider\n' > $KB/test2b/services/io.grpc.ManagedChannelProvider
# pack dengan services
python3 - "$KB/test2b/base.apk" "$KB/test2b/dex" "$KB/test2b/unsigned.apk" "$KB/test2b/services" <<'PY'
import zipfile, struct, os, sys, glob
base, dexout, out, svc_dir = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
zin = zipfile.ZipFile(base)
entries = [(i.filename, i) for i in zin.infolist()]
all_dex = sorted(glob.glob(dexout + "/classes*.dex"))
def build_zip(out_path, src_entries, new_entries):
    f = open(out_path, "wb"); offset = 0; centrals = []
    def write_entry(name, data, force_store=False, align=4):
        nonlocal offset
        comp = zipfile.ZIP_STORED if force_store else zipfile.ZIP_DEFLATED
        import zlib
        if comp == zipfile.ZIP_STORED:
            crc = zipfile.crc32(data) & 0xffffffff; raw = data; csize = len(data)
        else:
            co = zlib.compressobj(9, zlib.DEFLATED, -15); raw = co.compress(data) + co.flush()
            crc = zipfile.crc32(data) & 0xffffffff; csize = len(raw)
        pad = 0
        if comp == zipfile.ZIP_STORED:
            pad = (align - ((offset + 30 + len(name)) % align)) % align
        extra = b"\x00" * pad
        local = struct.pack("<IHHHHHIIIHH", 0x04034b50, 20, 0x800, comp, 0, 0, crc, csize, len(data), len(name), pad) + name.encode() + extra
        f.write(local); f.write(raw)
        centrals.append((name, crc, csize, len(data), comp, offset)); offset += len(local) + csize
    for name, info in src_entries:
        data = zin.read(name); write_entry(name, data, force_store=(name == "resources.arsc"))
    for sf in sorted(os.listdir(svc_dir)):
        with open(os.path.join(svc_dir, sf), "rb") as sv:
            write_entry("META-INF/services/" + sf, sv.read(), force_store=True)
    n = 0
    for p in new_entries:
        with open(p, "rb") as df: data = df.read()
        write_entry("classes.dex" if n == 0 else f"classes{n+1}.dex", data, force_store=True); n += 1
    cd_start = offset
    for name, crc, csize, usize, comp, off in centrals:
        rec = struct.pack("<IHHHHHHIIIHHHHHII", 0x02014b50, 20, 20, 0x800, comp, 0, 0, crc, csize, usize, len(name), 0, 0, 0, 0, 0, off) + name.encode()
        f.write(rec); offset += len(rec)
    cd_size = offset - cd_start
    eocd = struct.pack("<IHHHHIIH", 0x06054b50, 0, 0, len(centrals), len(centrals), cd_size, cd_start, 0)
    f.write(eocd); f.close()
build_zip(out, entries, all_dex)
print("packed:", os.path.getsize(out))
PY
sign $KB/test2b/unsigned.apk $REPO/test-apk/test2b-firebase-noprovider.apk
echo "test2b-firebase-noprovider.apk OK"

# ================= TEST 3: COMPOSE mini =================
echo "== [TEST 3] compose mini =="
rm -rf $KB/test3 && mkdir -p $KB/test3/classes
cp -r $T/res $KB/test3/res
$A2 compile --dir $KB/test3/res -o $KB/test3/res.zip
$A2 link -o $KB/test3/base.apk -I $KB/sable/android-34/android.jar \
  --manifest $T/AndroidManifest-c.xml --min-sdk-version 26 --target-sdk-version 34 \
  --version-code 1 --version-name "1" $KB/test3/res.zip
CP3="$KB/sable/android-34/android.jar"
CP3="$CP3:$(ls $KB/kaih/jars/activity-compose.jar $KB/kaih/jars/activity-ktx.jar $KB/kaih/jars/compose-ui.jar $KB/kaih/jars/compose-ui-graphics.jar $KB/kaih/jars/compose-ui-text.jar $KB/kaih/jars/compose-ui-unit.jar $KB/kaih/jars/compose-ui-geometry.jar $KB/kaih/jars/compose-ui-util.jar $KB/kaih/jars/compose-foundation.jar $KB/kaih/jars/compose-foundation-layout.jar $KB/kaih/jars/compose-runtime.jar $KB/kaih/jars/compose-runtime-saveable.jar $KB/kaih/jars/compose-material3.jar $KB/kaih/jars/compose-material.jar $KB/kaih/jars/compose-ripple.jar $KB/kaih/jars/compose-animation.jar $KB/kaih/jars/compose-animation-core.jar $KB/kaih/jars/kotlin-stdlib-1.9.22.jar $KB/kaih/jars/annotations.jar $KB/kaih/jars/coroutines-core.jar $KB/kaih/jars/coroutines-android.jar $KB/kaih/jars/core.jar $KB/kaih/jars/activity.jar $KB/kaih/jars/lifecycle-runtime.jar $KB/kaih/jars/lifecycle-viewmodel.jar $KB/kaih/jars/lifecycle-common.jar $KB/kaih/jars/savedstate.jar $KB/kaih/jars/collection.jar 2>/dev/null | tr '\n' ':')"
$KB/kotlinc19/package/bin/kotlinc -Xplugin=$KB/compose-compiler-158-unshaded.jar -classpath "$CP3" \
  -jvm-target 1.8 -d $KB/test3/classes \
  $T/src-c/main/java/com/sdn/semambung/kaih/testc/MainActivity.kt
(cd $KB/test3/classes && python3 -c "
import zipfile, os
z = zipfile.ZipFile('../app.jar','w',zipfile.ZIP_DEFLATED)
for r,_,fs in os.walk('.'):
    for f in fs:
        p=os.path.join(r,f); z.write(p,p)
z.close()")
rm -rf $KB/test3/dex && mkdir -p $KB/test3/dex
CMP_JARS="$(ls $KB/kaih/jars/compose-*.jar $KB/kaih/jars/activity*.jar $KB/kaih/jars/lifecycle-*.jar $KB/kaih/jars/core*.jar $KB/kaih/jars/savedstate*.jar $KB/kaih/jars/annotation.jar $KB/kaih/jars/arch-core-common.jar $KB/kaih/jars/collection.jar $KB/kaih/jars/kotlin-stdlib-1.9.22.jar $KB/kaih/jars/annotations.jar $KB/kaih/jars/coroutines-core.jar $KB/kaih/jars/coroutines-android.jar 2>/dev/null)"
java -Xmx3g -cp $KB/r8pre/r8-master.jar com.android.tools.r8.D8 --release --min-api 26 \
  --lib $KB/sable/android-34/android.jar --output $KB/test3/dex \
  $KB/test3/app.jar $KB/kaih/R.jar $CMP_JARS
pack_apk $KB/test3/base.apk $KB/test3/dex $KB/test3/unsigned.apk
sign $KB/test3/unsigned.apk $REPO/test-apk/test3-compose.apk
echo "test3-compose.apk OK"

echo "=== SELESAI ==="
ls -la $REPO/test-apk/test1-minimal.apk $REPO/test-apk/test2-firebase.apk
sha256sum $REPO/test-apk/test1-minimal.apk $REPO/test-apk/test2-firebase.apk
