# 変更前: FROM openjdk:8-jdk-slim
# 変更後: MavenがプリインストールされたOpen JDK 8のイメージを使用
FROM maven:3-openjdk-8 as build

WORKDIR /app

# Mavenキャッシュを最適化するために、まずpom.xmlだけをコピーして依存関係をダウンロードします
COPY pom.xml .
RUN mvn dependency:go-offline -B

# ソースコードをコピーします
COPY src ./src

# アプリケーションをビルドします
RUN mvn clean package -Dmaven.test.skip=true

# --- ここからランタイムイメージのフェーズ ---
# アプリケーションを実行するための軽量なJREイメージを使用
FROM openjdk:8-jre-slim

WORKDIR /app

# ビルドされたJARファイルを、コンテナ内で「app.jar」という名前でコピーします。
COPY --from=build /app/target/*.jar app.jar

# アプリケーションの起動コマンド
ENTRYPOINT ["java", "-jar", "app.jar"]