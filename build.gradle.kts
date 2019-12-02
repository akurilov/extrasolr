plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.2.2")
    implementation("org.zeromq:jeromq:0.5.1")
    implementation("org.apache.solr:solr-solrj:8.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.5.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_13
    targetCompatibility = JavaVersion.VERSION_13
}

tasks {
    wrapper {
        gradleVersion = "6.0"
    }
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
