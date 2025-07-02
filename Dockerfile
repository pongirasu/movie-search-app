# ベースイメージとしてJava Development Kit 8 (JDK 8) の軽量版を使用します。
FROM openjdk:8-jdk-slim

# アプリケーションのJARファイルを格納する作業ディレクトリを作成します。
WORKDIR /app

# Mavenのビルドに必要なファイルをコピーし、アプリケーションをビルドします。
# テストをスキップする設定です。
COPY pom.xml .
COPY src ./src
RUN mvn -f pom.xml clean package -Dmaven.test.skip=true

# ビルドされたJARファイルを、コンテナ内で「app.jar」という名前でコピーします。
COPY target/*.jar app.jar

# コンテナが起動したときに実行されるコマンドを指定します。
# これにより、Javaアプリケーションが起動します。
ENTRYPOINT ["java", "-jar", "app.jar"]