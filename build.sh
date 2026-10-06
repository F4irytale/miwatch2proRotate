#!/usr/bin/env bash
# 不依赖 Gradle，直接用 SDK 工具打包签名 APK
set -euo pipefail
cd "$(dirname "$0")"

# Android SDK 路径：优先读环境变量，其次用下面的默认值
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/e/flclashwearos/toolchain/android-sdk}}"
if [ ! -d "$SDK" ]; then
  echo "找不到 Android SDK：$SDK" >&2
  echo "请设置环境变量 ANDROID_SDK_ROOT 指向你的 SDK 目录。" >&2
  exit 1
fi

# SDK 里的可执行文件在 Windows 上带后缀，Linux/macOS 不带
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) EXE=".exe"; SCRIPT=".bat" ;;
  *)                    EXE="";     SCRIPT="" ;;
esac

BT=$SDK/build-tools/36.0.0
JAR=$SDK/platforms/android-35/android.jar
OUT=build

rm -rf "$OUT"
mkdir -p "$OUT/res" "$OUT/classes" "$OUT/dex"

"$BT/aapt2$EXE" compile --dir res -o "$OUT/res/res.zip"
"$BT/aapt2$EXE" link -o "$OUT/unsigned.apk" -I "$JAR" \
  --manifest AndroidManifest.xml --java "$OUT/gen" "$OUT/res/res.zip"

javac -source 11 -target 11 -encoding UTF-8 -Xlint:-options \
  -classpath "$JAR" -d "$OUT/classes" \
  $(find src "$OUT/gen" -name '*.java')

"$BT/d8$SCRIPT" --min-api 30 --lib "$JAR" --output "$OUT/dex" \
  $(find "$OUT/classes" -name '*.class')

cp "$OUT/unsigned.apk" "$OUT/with-dex.apk"
python -c "import sys,zipfile; zipfile.ZipFile(sys.argv[1],'a',zipfile.ZIP_DEFLATED).write(sys.argv[2],'classes.dex')" \
  "$OUT/with-dex.apk" "$OUT/dex/classes.dex"

"$BT/zipalign$EXE" -f -p 4 "$OUT/with-dex.apk" "$OUT/aligned.apk"

KS=debug.keystore
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
    -alias debug -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Rotate Debug" >/dev/null 2>&1
fi

"$BT/apksigner$SCRIPT" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out RotateApp.apk "$OUT/aligned.apk"
echo "OK: $(pwd)/RotateApp.apk"
