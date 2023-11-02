# Manually Building and Pushing UnicodeJSPs to Docker / GCP Run

- This page is Under Construction by Steven `@srl295`

- see [index.md](./index.md) for the prior documentation

## maven stuff

- local build

```
mvn -B package -am -pl UnicodeJsps -DskipTests=true
```

- make a copy of CLDR - lots of ways to do this

```
git clone --reference-if-able ~/src/cldr https://github.com/unicode-org/cldr.git
mkdir -p UnicodeJsps/target && tar -cpz --exclude=.git -f UnicodeJsps/target/cldr-unicodetools.tgz ./cldr/ ./unicodetools/
```

## docker stuff

- build it

```
docker build -t unicode/unicode-jsps UnicodeJsps/
```

- try it

```
docker run --rm -p 8080:8080 unicode/unicode-jsps
```

=> <http://127.0.0.1:8080>


## cloudy stuff

- install gcloud sdk

- `gcloud init`

- login to docker

```
gcloud auth configure-docker us-central1-docker.pkg.dev
```

- build docker image and run it

```
docker build -t us-central1-docker.pkg.dev/goog-unicode-dev/unicode-jsps/unicode-jsps:latest UnicodeJsps/
docker run --rm -p 8080:8080 us-central1-docker.pkg.dev/goog-unicode-dev/unicode-jsps/unicode-jsps:latest
```

- push docker image

_(takes a while - ~4G to push)_

```
docker push us-central1-docker.pkg.dev/goog-unicode-dev/unicode-jsps/unicode-jsps:latest
```
