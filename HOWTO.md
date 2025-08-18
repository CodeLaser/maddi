
Publish to Maven Central
------------------------

Create gpg key

```
# make an RSA-RSA 4096 bit key, with correct email address
gpg --full-generate-key
# initial check
gpg --list-keys
# export private key
gpg --armor --export-secret-keys  3483290483902483024832  > private.key
# test private key
gpg --list-packets ~/.jreleaser/private.key
# generate public key
gpg --armor --export 3483290483902483024832 > ~/.jreleaser/public.key
# send public key so that Maven Central can see it
gpg --keyserver hkps://keys.openpgp.org --send-keys 3483290483902483024832
# has it been received?
gpg --keyserver hkps://keys.openpgp.org --recv-keys 3483290483902483024832
```

Ensure

```
cat ~/.jreleaser/config.properties
JRELEASER_GITHUB_TOKEN=github_pat_11....

JRELEASER_DEPLOY_MAVEN_MAVENCENTRAL_SONATYPE_USERNAME=abc123
JRELEASER_DEPLOY_MAVEN_MAVENCENTRAL_SONATYPE_PASSWORD=ueohureoi890g024ntoehnieo

JRELEASER_GPG_PUBLIC_KEY=/Users/bnaudts/.jreleaser/public.key
JRELEASER_GPG_SECRET_KEY=/Users/bnaudts/.jreleaser/private.key
JRELEASER_GPG_PASSPHRASE=the secret passphrase
```

Then run
```
gradle :maddi-support:clean :maddi-support:publishMavenJavaPublicationToStagingRepository :maddi-support:jreleaserDeploy
```


