SeattleOnAndroid README

---------------

Project dependencies

jdk
android-sdk
ant

---------------

Preparing SeattleOnAndroid project

See: https://seattle.cs.washington.edu/wiki/BuildingSeattleOnAndroid for the
latest details.

---------------

Most common ant commands to use

a) Build a debug version:

ant debug

b) Build an (unsigned) release version:

ant release

c) Clean-up project directory

ant clean

---------------

Signing and aligning the release version

jarsigner -keystore <keystore_path> -storepass <keystore_pass> -keypass <key_pass> <apk_path> <key_name> -digestalg SHA1 -sigfile CERT
zipalign <align> -f <unaligned_apk_path> <output_apk_path>

<keystore_path>: path to keystore file
<keystore_pass>: keystore password
<key_pass>: key password
<apk_path>: path to unsigned apk
<key_name>: name of the private key to use

<align>: alignment in bytes, 4 would probably be a good bet
<unaligned_apk_path>: path to signed, but yet unaligned apk
<output_apk_path>: output path for the aligned apk
