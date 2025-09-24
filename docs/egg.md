This is the procedure used by eggrobin to build and deploy the JSPs from his
Windows machines. Some of the commands must be run in PowerShell, others in the
wsl bash.

WSL:
```bash
pushd UnicodeJsps
./update-bidic-ucd.sh
popd
wget https://github.com/unicode-org/last-resort-font/releases/latest/download/LastResort-Regular.ttf
mv ./LastResort-Regular.ttf ./UnicodeJsps/src/main/webapp/
```
Powershell:
```powershell
mvn -B package -am -pl UnicodeJsps -DskipTests=true
$CLDR_REF = $(mvn help:evaluate "-Dexpression=cldr.version" -q -DforceStdout).Split("-")[2]
if (-not Test-Path cldr) {
  git clone https://github.com/unicode-org/cldr.git "--reference=..\cldr"
}
Set-Location cldr
git reset --hard $CLDR_REF
```
WSL:
```bash
mkdir -p UnicodeJsps/target && tar -cpz --exclude=.git -f UnicodeJsps/target/cldr-unicodetools.tgz ./cldr/ ./unicodetools/
```
Powershell:
```powershell
mvn compile exec:java '-Dexec.mainClass="org.unicode.jsp.RebuildPropertyCache"' -am -pl unicodetools   "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=." "-DCLDR_DIR=..\cldr\"
```
WSL:
```bash
tar -cpz -f UnicodeJsps/target/generated.tgz ./Generated/
docker build -t us-central1-docker.pkg.dev/goog-unicode-dev/unicode-jsps/unicode-jsps:latest UnicodeJsps/
```
Powershell:
```powershell
docker push us-central1-docker.pkg.dev/goog-unicode-dev/unicode-jsps/unicode-jsps:latest
```