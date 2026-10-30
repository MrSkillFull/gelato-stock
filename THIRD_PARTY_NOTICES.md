# Avisos de terceiros

## Distribuição do GelatoStock v1.0.0

O código e os recursos próprios são GPL-3.0-only; o texto está em [LICENSE](LICENSE). As licenças abaixo se aplicam aos respectivos materiais de terceiros.

Distribua este documento, `LICENSE` e a pasta `LICENSES/` junto ao APK, por exemplo em um arquivo de avisos anexado à mesma GitHub Release. O APK isolado não contém todos esses textos.

## Bibliotecas do APK release

Foram revisados 76 artefatos JAR/AAR do runtime release, sem contar BOMs ou módulos de metadados multiplataforma como bibliotecas adicionais. A tabela identifica os 52 com código encontrado no DEX ou no mapeamento R8, incluindo código incorporado por inlining. Outros artefatos podem deixar somente recursos, marcadores de versão ou licenças no APK.

Todos os componentes da tabela declaram **Apache-2.0** nos POMs das versões indicadas, exceto `listenablefuture`, confirmado pelo cabeçalho do fonte. A coluna de fonte aponta para o mesmo metadado oficial consultado no cache local. O texto integral está em [LICENSES/Apache-2.0.txt](LICENSES/Apache-2.0.txt); preserve também os avisos que acompanham os fontes ao redistribuí-los. Complementos da Kotlin stdlib estão na seção seguinte.

| Componente | Versão | Fonte da licença |
| --- | --- | --- |
| `androidx.activity:activity-compose` | 1.8.2 | [POM](https://dl.google.com/dl/android/maven2/androidx/activity/activity-compose/1.8.2/activity-compose-1.8.2.pom) |
| `androidx.activity:activity` | 1.8.2 | [POM](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.8.2/activity-1.8.2.pom) |
| `androidx.annotation:annotation-jvm` | 1.9.1 | [POM](https://dl.google.com/dl/android/maven2/androidx/annotation/annotation-jvm/1.9.1/annotation-jvm-1.9.1.pom) |
| `androidx.arch.core:core-common` | 2.2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/arch/core/core-common/2.2.0/core-common-2.2.0.pom) |
| `androidx.arch.core:core-runtime` | 2.2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/arch/core/core-runtime/2.2.0/core-runtime-2.2.0.pom) |
| `androidx.collection:collection-jvm` | 1.5.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/collection/collection-jvm/1.5.0/collection-jvm-1.5.0.pom) |
| `androidx.compose.animation:animation-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/animation/animation-android/1.10.4/animation-android-1.10.4.pom) |
| `androidx.compose.animation:animation-core-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/animation/animation-core-android/1.10.4/animation-core-android-1.10.4.pom) |
| `androidx.compose.foundation:foundation-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-android/1.10.4/foundation-android-1.10.4.pom) |
| `androidx.compose.foundation:foundation-layout-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-layout-android/1.10.4/foundation-layout-android-1.10.4.pom) |
| `androidx.compose.material3:material3-android` | 1.4.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.4.0/material3-android-1.4.0.pom) |
| `androidx.compose.material:material-ripple-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/material/material-ripple-android/1.10.4/material-ripple-android-1.10.4.pom) |
| `androidx.compose.runtime:runtime-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-android/1.10.4/runtime-android-1.10.4.pom) |
| `androidx.compose.runtime:runtime-retain-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-retain-android/1.10.4/runtime-retain-android-1.10.4.pom) |
| `androidx.compose.runtime:runtime-saveable-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-saveable-android/1.10.4/runtime-saveable-android-1.10.4.pom) |
| `androidx.compose.ui:ui-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.10.4/ui-android-1.10.4.pom) |
| `androidx.compose.ui:ui-geometry-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-geometry-android/1.10.4/ui-geometry-android-1.10.4.pom) |
| `androidx.compose.ui:ui-graphics-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-graphics-android/1.10.4/ui-graphics-android-1.10.4.pom) |
| `androidx.compose.ui:ui-text-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-text-android/1.10.4/ui-text-android-1.10.4.pom) |
| `androidx.compose.ui:ui-unit-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-unit-android/1.10.4/ui-unit-android-1.10.4.pom) |
| `androidx.compose.ui:ui-util-android` | 1.10.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-util-android/1.10.4/ui-util-android-1.10.4.pom) |
| `androidx.concurrent:concurrent-futures` | 1.1.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/concurrent/concurrent-futures/1.1.0/concurrent-futures-1.1.0.pom) |
| `androidx.core:core-ktx` | 1.16.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/core/core-ktx/1.16.0/core-ktx-1.16.0.pom) |
| `androidx.core:core-viewtree` | 1.0.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/core/core-viewtree/1.0.0/core-viewtree-1.0.0.pom) |
| `androidx.core:core` | 1.16.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/core/core/1.16.0/core-1.16.0.pom) |
| `androidx.customview:customview-poolingcontainer` | 1.0.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/customview/customview-poolingcontainer/1.0.0/customview-poolingcontainer-1.0.0.pom) |
| `androidx.emoji2:emoji2` | 1.4.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/emoji2/emoji2/1.4.0/emoji2-1.4.0.pom) |
| `androidx.graphics:graphics-path` | 1.0.1 | [POM](https://dl.google.com/dl/android/maven2/androidx/graphics/graphics-path/1.0.1/graphics-path-1.0.1.pom) |
| `androidx.interpolator:interpolator` | 1.0.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/interpolator/interpolator/1.0.0/interpolator-1.0.0.pom) |
| `androidx.lifecycle:lifecycle-common-jvm` | 2.9.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-common-jvm/2.9.4/lifecycle-common-jvm-2.9.4.pom) |
| `androidx.lifecycle:lifecycle-process` | 2.9.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-process/2.9.4/lifecycle-process-2.9.4.pom) |
| `androidx.lifecycle:lifecycle-runtime-android` | 2.9.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-android/2.9.4/lifecycle-runtime-android-2.9.4.pom) |
| `androidx.lifecycle:lifecycle-runtime-compose-android` | 2.9.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-compose-android/2.9.4/lifecycle-runtime-compose-android-2.9.4.pom) |
| `androidx.lifecycle:lifecycle-viewmodel-android` | 2.9.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-android/2.9.4/lifecycle-viewmodel-android-2.9.4.pom) |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate-android` | 2.9.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-savedstate-android/2.9.4/lifecycle-viewmodel-savedstate-android-2.9.4.pom) |
| `androidx.profileinstaller:profileinstaller` | 1.4.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/profileinstaller/profileinstaller/1.4.0/profileinstaller-1.4.0.pom) |
| `androidx.room:room-common-jvm` | 2.8.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/room/room-common-jvm/2.8.4/room-common-jvm-2.8.4.pom) |
| `androidx.room:room-runtime-android` | 2.8.4 | [POM](https://dl.google.com/dl/android/maven2/androidx/room/room-runtime-android/2.8.4/room-runtime-android-2.8.4.pom) |
| `androidx.savedstate:savedstate-android` | 1.3.2 | [POM](https://dl.google.com/dl/android/maven2/androidx/savedstate/savedstate-android/1.3.2/savedstate-android-1.3.2.pom) |
| `androidx.savedstate:savedstate-compose-android` | 1.3.2 | [POM](https://dl.google.com/dl/android/maven2/androidx/savedstate/savedstate-compose-android/1.3.2/savedstate-compose-android-1.3.2.pom) |
| `androidx.sqlite:sqlite-android` | 2.6.2 | [POM](https://dl.google.com/dl/android/maven2/androidx/sqlite/sqlite-android/2.6.2/sqlite-android-2.6.2.pom) |
| `androidx.sqlite:sqlite-framework-android` | 2.6.2 | [POM](https://dl.google.com/dl/android/maven2/androidx/sqlite/sqlite-framework-android/2.6.2/sqlite-framework-android-2.6.2.pom) |
| `androidx.startup:startup-runtime` | 1.1.1 | [POM](https://dl.google.com/dl/android/maven2/androidx/startup/startup-runtime/1.1.1/startup-runtime-1.1.1.pom) |
| `androidx.tracing:tracing` | 1.2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/tracing/tracing/1.2.0/tracing-1.2.0.pom) |
| `androidx.versionedparcelable:versionedparcelable` | 1.1.1 | [POM](https://dl.google.com/dl/android/maven2/androidx/versionedparcelable/versionedparcelable/1.1.1/versionedparcelable-1.1.1.pom) |
| `androidx.window:window` | 1.5.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/window/window/1.5.0/window-1.5.0.pom) |
| `com.google.guava:listenablefuture` | 1.0 | [Fonte ListenableFuture.java](https://repo.maven.apache.org/maven2/com/google/guava/listenablefuture/1.0/listenablefuture-1.0-sources.jar) |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.2.10 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/2.2.10/kotlin-stdlib-2.2.10.pom) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.9.0 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/1.9.0/kotlinx-coroutines-android-1.9.0.pom) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm` | 1.9.0 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.9.0/kotlinx-coroutines-core-jvm-1.9.0.pom) |
| `org.jetbrains:annotations` | 23.0.0 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/annotations/23.0.0/annotations-23.0.0.pom) |
| `org.jspecify:jspecify` | 1.0.0 | [POM](https://repo.maven.apache.org/maven2/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.pom) |

### Kotlin stdlib: materiais derivados

A [declaração de terceiros da Kotlin 2.2.10](https://github.com/JetBrains/kotlin/blob/v2.2.10/license/README.md) identifica materiais adicionais dentro da stdlib JVM:

- Coleções derivadas de GWT: Apache-2.0; Copyright (C) 2007-08 Google Inc.
- `UnsignedJVM.kt`, derivado de Guava: Apache-2.0; Copyright (C) 2011 The Guava Authors.
- `kotlin/time`, derivado de ThreeTen: BSD-3-Clause; preserve o copyright, as condições e a ressalva em [LICENSES/ThreeTen-BSD-3-Clause.txt](LICENSES/ThreeTen-BSD-3-Clause.txt).
- `MathJVM.kt`, com funções derivadas de Boost: BSL-1.0; Copyright Eric Ford & Hubert Holin 2001. Texto em [LICENSES/Boost-1.0.txt](LICENSES/Boost-1.0.txt). A licença dispensa a reprodução dos avisos em cópias exclusivamente em código objeto; preserve-os nos fontes correspondentes.

### Guava ListenableFuture 1.0

O arquivo `com/google/common/util/concurrent/ListenableFuture.java` do pacote de fontes contém explicitamente Apache-2.0 e o aviso:

> Copyright (C) 2007 The Guava Authors

O POM herda de `guava-parent:26.0-android`; a ausência de um bloco de licença no POM filho não significa ausência de licença. O JAR 1.0 contém a interface `ListenableFuture`, identificada no DEX do APK. Não é o artefato vazio `9999.0-empty-to-avoid-conflict-with-guava`.

### Artefatos de runtime sem código identificado no APK

As versões abaixo também declaram Apache-2.0 nos respectivos POMs. São módulos vazios/de compatibilidade, anotações ou código eliminado; marcadores, recursos e textos de licença podem permanecer. Não são contados novamente como bibliotecas com código incorporado:

- `androidx.activity` 1.8.2: `activity-ktx`.
- `androidx.annotation` 1.5.0: `annotation-experimental`.
- `androidx.autofill` 1.0.0: `autofill`.
- `androidx.collection` 1.5.0: `collection-ktx`.
- `androidx.compose.runtime` 1.10.4: `runtime-annotation-android`.
- `androidx.compose.ui` 1.10.4: `ui-tooling-preview-android`.
- `androidx.documentfile` 1.0.0: `documentfile`.
- `androidx.dynamicanimation` 1.0.0: `dynamicanimation`.
- `androidx.legacy` 1.0.0: `legacy-support-core-utils`.
- `androidx.lifecycle` 2.9.4: `lifecycle-common-java8`, `lifecycle-livedata-core-ktx`, `lifecycle-livedata-core`, `lifecycle-livedata`, `lifecycle-runtime-ktx-android`, `lifecycle-viewmodel-ktx`, `lifecycle-viewmodel`.
- `androidx.loader` 1.0.0: `loader`.
- `androidx.localbroadcastmanager` 1.0.0: `localbroadcastmanager`.
- `androidx.print` 1.0.0: `print`.
- `androidx.room` 2.8.4: `room-ktx`.
- `androidx.savedstate` 1.3.2: `savedstate-ktx`.
- `androidx.transition` 1.6.0: `transition`.
- `androidx.window` 1.5.0: `window-core-android`.
- `org.jetbrains.kotlinx` 1.7.3: `kotlinx-serialization-core-jvm`.

## Ferramentas e componentes do sistema

Gradle/AGP, compilador Kotlin e plugin Compose, KSP/Room Compiler, R8, lint, JDK e Android SDK são ferramentas de build; não são bibliotecas de runtime incorporadas por suas dependências de build. JUnit, Espresso, AndroidX Test e Compose tooling de debug/teste não integram o release.

As bibliotecas AndroidX SQLite da tabela são incorporadas. O SQLite do sistema, o framework Android e suas bibliotecas de sistema são fornecidos pelo dispositivo; não há `sqlite-bundled` no runtime nem um SQLite nativo empacotado neste APK. O APK contém `libandroidx.graphics.path.so` para quatro ABIs, proveniente de `graphics-path:1.0.1`.

## Código-fonte correspondente

Ao distribuir o APK pela GitHub Release, ofereça gratuitamente, na mesma página e com indicação clara, o código-fonte correspondente à revisão usada para gerar esse APK, conforme GPLv3 seção 6(d). Inclua código e recursos próprios, scripts/configuração de build e instalação, Wrapper, esquemas Room, licenças e avisos. Disponibilize também os fontes das partes de terceiros incorporadas, nas versões da tabela, preservando seus avisos e eventuais modificações; um ZIP automático apenas do repositório não inclui esses fontes obtidos pelo Gradle. Inclua os fontes nativos e os arquivos necessários à sua compilação. Não inclua senhas, keystore privada nem configurações locais de assinatura. O README informa os requisitos de build; outra pessoa deve poder compilar e assinar sua própria cópia.

## Biblioteca nativa graphics-path 1.0.1

Os fontes C++ e os cabeçalhos de `src/main/cpp/math` usados pelo CMake declaram Apache-2.0. Foram conferidos no [código oficial da versão 1.0.1](https://android.googlesource.com/platform/frameworks/support/+/8a05a22af450d589ef911d772a001a49dcb05b71/graphics/graphics-path/), revisão indicada pelas [notas de lançamento AndroidX](https://developer.android.com/jetpack/androidx/releases/graphics#graphics-path-1.0.1). Não há arquivo `NOTICE` adicional nesse módulo. Preserve os cabeçalhos de copyright ao distribuir os fontes; o texto Apache-2.0 acima acompanha a distribuição binária.

## Gradle Wrapper

Os arquivos `gradlew`, `gradlew.bat` e `gradle/wrapper/gradle-wrapper.jar` são componentes do Gradle Wrapper disponibilizados sob a Apache License 2.0. Os scripts identificam os titulares como seus autores originais e preservam seus avisos de copyright de 2015. O JAR contém uma cópia da licença em `META-INF/LICENSE`. O texto integral também está em [LICENSES/Apache-2.0.txt](LICENSES/Apache-2.0.txt).

Fontes: cabeçalhos dos scripts, conteúdo do JAR e [projeto Gradle](https://github.com/gradle/gradle).
