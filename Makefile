# Adjust to your local settings
SDK_DIR = /home/android/android-sdks
API_LEVEL = 15
KEYALIAS = rss


# From here on, the file should be left unchanged
AAPT = $(SDK_DIR)/platform-tools/aapt
ANDROID_JAR = $(SDK_DIR)/platforms/android-$(API_LEVEL)/android.jar
DX_JAR = $(SDK_DIR)/platform-tools/lib/dx.jar
SDKLIB_JAR = $(SDK_DIR)/tools/lib/sdklib.jar
ZIPALIGN = $(SDK_DIR)/tools/zipalign


all: zipalign

icons: res/drawable/icon.png res/drawable/feed.png res/drawable/feed_grey.png res/drawable/ic_statusbar_rss.png res/drawable-v9/ic_statusbar_rss.png res/drawable-v11/ic_statusbar_rss.png

res/drawable/icon.png: launcher.svg
	mkdir -p res/drawable
	convert launcher.svg -resize 60x60 -background Transparent -bordercolor Transparent -border 4x6 \( +clone -background Transparent -shadow 40x1+0+3 \) +swap -layers merge +repage res/drawable/icon.png
	convert -extract 72x72+0+0 +repage res/drawable/icon.png res/drawable/icon.png

res/drawable/feed.png: launcher.svg
	mkdir -p res/drawable
	convert launcher.svg -resize 19x19 -background Transparent -bordercolor Transparent -border 23x23 res/drawable/feed.png
	convert -extract 44x44+0+21 +repage res/drawable/feed.png res/drawable/feed.png

res/drawable/feed_grey.png: launcher.svg
	mkdir -p res/drawable
	convert launcher.svg -resize 19x19 -background Transparent -type Grayscale -bordercolor Transparent -border 23x23 res/drawable/feed_grey.png
	convert -extract 44x44+0+21 +repage res/drawable/feed_grey.png res/drawable/feed_grey.png

res/drawable/ic_statusbar_rss.png: status_icon.svg
	mkdir -p res/drawable
	convert status_icon.svg -background Transparent -resize 21x21 -bordercolor Transparent -border 2 res/drawable/ic_statusbar_rss.png

res/drawable-v9/ic_statusbar_rss.png: status_icon_23.svg
	mkdir -p res/drawable-v9
	convert status_icon_23.svg -background Transparent -resize 21x21 -bordercolor Transparent -border 2 res/drawable-v9/ic_statusbar_rss.png

res/drawable-v11/ic_statusbar_rss.png: status_icon_30.svg
	mkdir -p res/drawable-v11
	convert status_icon_30.svg -background Transparent -resize 21x21 -bordercolor Transparent -border 2 res/drawable-v11/ic_statusbar_rss.png

aapt: icons AndroidManifest.xml res
	mkdir -p gen/de/shandschuh/sparserss/
	mkdir -p bin
	$(AAPT) p -f -M AndroidManifest.xml -F bin/resources.ap_ -I $(ANDROID_JAR) -S res -m -J gen

javac: aapt
	mkdir -p bin/classes
	javac -d bin/classes -sourcepath gen gen/de/shandschuh/sparserss/*.java
	javac -cp bin/classes:$(ANDROID_JAR) -d bin/classes -sourcepath src `find src -name *.java -print`

bin/classes.dex: javac
	java -jar $(DX_JAR) --dex --output=bin/classes.dex bin/classes

bin/SparseRSS_unsigned.apk: bin/classes.dex
	java -cp $(SDKLIB_JAR) com.android.sdklib.build.ApkBuilderMain bin/SparseRSS_unsigned.apk -u -z bin/resources.ap_ -f bin/classes.dex

bin/SparseRSS_signed.apk: bin/SparseRSS_unsigned.apk
	jarsigner -keystore keystore -signedjar bin/SparseRSS_signed.apk bin/SparseRSS_unsigned.apk $(KEYALIAS)

zipalign: bin/SparseRSS_signed.apk
	$(ZIPALIGN) 4 bin/SparseRSS_signed.apk bin/SparseRSS_signed_aligned.apk

clean:
	rm -fr res/drawable*
	rm -fr gen
	rm -fr bin
