# Focus Quest — Android starter

Projeto Android nativo em Kotlin + Jetpack Compose, preparado para Android 16 (API 36).

## Requisitos
- Android Studio Meerkat (2024.3.1) ou mais recente
- JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 36.x
- Conexão com internet na primeira sincronização do Gradle

## Abrir e executar
1. Extraia o ZIP.
2. No Android Studio, escolha **Open** e selecione a pasta `FocusQuest`.
3. Aguarde o Gradle sincronizar e baixar as dependências.
4. No SDK Manager, confirme que o Android 16 / API 36 está instalado.
5. Conecte o Samsung por USB, habilite Depuração USB nas Opções do desenvolvedor e aceite a autorização no celular.
6. Selecione o dispositivo e pressione **Run**.

## Gerar APK
No Android Studio: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
O APK de debug ficará em `app/build/outputs/apk/debug/app-debug.apk`.

## O que já está implementado
- Dashboard escuro com progresso de nível e resumo de tarefas.
- Criação de tarefas com prioridade e XP sugerido.
- Conclusão de tarefas com concessão de XP.
- Persistência local com Room.
- Navegação inferior entre Início, Tarefas, Foco e Perfil.
- Jardim de foco inspirado na mecânica de crescimento do Forest, com timer de 15/25/45/60 minutos, pausa, retomada e reinício.
- Planta visual que evolui conforme o tempo de concentração avança.
- Recompensa de 2 XP por minuto ao concluir uma sessão; sessões concluídas ficam salvas no Room.
- Tela de permissões com atalhos para Acesso ao uso e Sobreposição.
- Manifesto com permissões preparatórias.

## Limitações desta primeira entrega
O bloqueio efetivo de outros aplicativos ainda NÃO está ativo. Esta versão apenas verifica/solicita os acessos especiais necessários e oferece a base de integração. A permissão de sobreposição, isoladamente, não impede o uso de outros apps. O monitoramento e a política de bloqueio precisam ser implementados e testados separadamente, com consentimento explícito, tratamento de exceções e compatibilidade com One UI / Android 16.

Não existe bloqueio inviolável em um app comum instalado pelo usuário: encerramento forçado, revogação de permissões e reinicialização podem interromper a proteção. Não use este protótipo como mecanismo de segurança.

## Identidade
- Fundo: #0D0F14
- Superfícies: #171A22
- Roxo: #9B7BFF
- Pacote: `com.focusquest.app`

## Estrutura
- `data/`: Room, entidade, DAO e repositório
- `ui/`: Compose e telas
- `viewmodel/`: estado e regras da aplicação
- `permissions/`: atalhos para permissões Android


## Jardim de foco
A aba **Foco** agora funciona como um timer de concentração com crescimento visual da planta:
- 15, 25, 45 ou 60 minutos;
- iniciar, pausar, continuar e reiniciar;
- estágios visuais de semente até árvore;
- conclusão da sessão concede 2 XP por minuto;
- o banco Room recebe a tabela `focus_sessions` por migração 1→2.

Nesta versão, o timer permanece ativo enquanto o processo do app estiver vivo. Se o Android encerrar o processo, a sessão pode ser perdida; execução confiável em segundo plano, notificações e restauração após reinicialização serão implementadas em uma etapa posterior.


## Atualização de interface — v3
- Navegação inferior reorganizada em cinco posições: Início, Tarefas, Criar, Jornada e Perfil.
- A ação Criar fica no centro e oferece atalhos para nova tarefa, sessão de foco e objetivo.
- A antiga experiência de jardim foi substituída por uma progressão temática de RPG, com personagem que muda conforme o nível.
- A Jornada reúne o timer e a recompensa de XP.
- O gesto de arrastar da borda esquerda para a direita retorna à tela inicial, e o botão Voltar do Android também é tratado.
- O Perfil passa a apresentar entradas para estatísticas, histórico, notificações e configurações (destinos ainda são estrutura visual para próximas implementações).
- O atalho de concluir tarefas na tela inicial agora executa a conclusão real.

Observação: esta versão é uma atualização do código-fonte. Ainda precisa ser aberta e compilada no Android Studio e validada em aparelho. O pull-to-refresh e a execução confiável do timer em segundo plano permanecem pendentes de implementação e teste.


## Identidade visual
- `assets/focus_quest_capa.png`: capa de apresentação do Focus Quest.
- Ícones de inicialização incluídos em `app/src/main/res/mipmap-*` e configurados no AndroidManifest.


## Focus Quest v5 — Bloqueio com foco em privacidade
- Removido o AccessibilityService e a declaração BIND_ACCESSIBILITY_SERVICE.
- Monitoramento de primeiro plano com UsageStatsManager durante uma sessão iniciada pelo usuário.
- Sobreposição de bloqueio com TYPE_APPLICATION_OVERLAY.
- Permissões solicitadas: Acesso ao uso, Exibir sobre outros apps e serviço em primeiro plano (notificação persistente durante a sessão).
- O monitor consulta eventos de primeiro plano localmente; não lê conteúdo das telas, mensagens, senhas ou notificações e não envia dados de uso para servidores.
- O Android/Samsung pode aplicar atrasos ou limitações ao histórico de uso; portanto, esse método é menos imediato e não garante bloqueio inviolável.


## Ícone e capa oficiais
- `assets/focus_quest_capa.png`: imagem original enviada para a capa do Focus Quest.
- `app/src/main/res/mipmap-*`: ícones de inicialização derivados do logotipo quadrado da imagem.
- O `AndroidManifest.xml` referencia `@mipmap/ic_launcher` e `@mipmap/ic_launcher_round`.


## Focus Quest 0.2.0 — Etapa 1: Organização e produtividade

Esta atualização amplia a base local do aplicativo com:
- Metas com descrição, horizonte (curto, médio ou longo prazo), prazo opcional e conclusão manual.
- Vínculo de tarefas a metas e indicador de progresso baseado nas tarefas vinculadas.
- Categorias de tarefas, prazos em formato AAAA-MM-DD, busca e filtros por status/atraso.
- Subtarefas com conclusão individual e barra de progresso por missão.
- Hábitos diários, em dias úteis ou semanais, com registro de conclusão e sequência.
- Notas rápidas com criação, edição e exclusão.
- Calendário/agenda agrupando as tarefas por prazo.
- Migração Room da versão 2 para 3, preservando os dados existentes.

A interface de planejamento é acessada pelo botão de calendário na tela Tarefas, pelo menu Criar ou pelo Perfil. A tela de calendário usa as datas informadas nas tarefas. Nesta entrega, os prazos são informados no formato ISO `AAAA-MM-DD`; a seleção visual por calendário poderá ser refinada numa próxima iteração.

**Validação:** o código-fonte foi atualizado e revisado estruturalmente, mas ainda precisa passar pelo build do Codemagic e por testes no dispositivo.
