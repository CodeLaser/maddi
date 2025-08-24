cd src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/jdk/
cd openjdk-24.0.2
jar cf ../openjdk-24.0.2.jar *.json
cd ../openjdk-21.0.8/
jar cf ../openjdk-21.0.8.jar *.json
cd ../../libs/
jar cf ../libs.jar */*.json

