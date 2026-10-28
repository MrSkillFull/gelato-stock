# GelatoStock

Aplicativo Android para vendedores de sacolé gerenciarem produção, vendas e estoque. Os fluxos principais funcionam offline, sem conta ou servidor; os dados e as configurações ficam em Room/SQLite. Não há sincronização entre dispositivos.

## Funcionalidades

- Cadastro e gerenciamento de sabores.
- Registro de produção e vendas, com proteção contra estoque negativo.
- Consulta de estoque e histórico, perdas, ajustes e estornos.
- Alerta configurável de estoque baixo.
- Exportação e restauração de backup JSON. O arquivo não é criptografado pelo aplicativo; guarde-o em local seguro.

## Compilar e executar

Requisitos: Android Studio com suporte a AGP 9.4.1, Android SDK Platform 37 e JDK 25 para o daemon Gradle. O projeto inclui Gradle Wrapper 9.6.0. Para executar, use dispositivo ou emulador com Android 7.0 (API 24) ou superior.

Abra a raiz no Android Studio, configure o SDK e sincronize o Gradle. Selecione o módulo `app` e use **Run** para executar no dispositivo ou emulador.

Na raiz, em PowerShell, para compilar e verificar:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Em Linux/macOS, use `sh ./gradlew` com as mesmas tarefas. O APK debug fica em `app/build/outputs/apk/debug/`. O build debug não exige credenciais de assinatura release. A configuração inicial e o download das dependências exigem internet.

## Autor e licença

Autor e mantenedor: **Fernando Lima**.
Copyright (C) 2026 Fernando Lima — código próprio.

O código e os recursos próprios do GelatoStock são disponibilizados sob **GPL-3.0-only** (GNU GPL versão 3 exclusivamente), na medida dos direitos que o responsável possa licenciar. Veja [LICENSE](LICENSE). Materiais de terceiros mantêm suas licenças e avisos, descritos em [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Para contribuir, leia [CONTRIBUTING.md](CONTRIBUTING.md).
