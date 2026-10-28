# Alarme Zap — correção do alarme em segundo plano

Correções nesta versão:
- registra o `AlarmService` no AndroidManifest;
- registra quando o Android aceita ou recusa o serviço de áudio em primeiro plano;
- deixa de descartar notificações contínuas ou classificadas como serviço, que podem conter mensagens válidas;
- mostra horário do último diagnóstico e o resultado da tentativa de iniciar o alarme;
- oferece um atalho para solicitar ao Android a exceção de otimização de bateria;
- ao deslizar para parar, cancela player, vibração, notificação e serviço.

## Publicação
Envie o conteúdo desta pasta para o repositório GitHub, preservando a estrutura de pastas. Depois confira a aba Actions: o workflow `Build APK` precisa terminar em verde. Baixe o APK em Artifacts da execução mais recente e instale essa versão.

## Configuração no Android
Mantenha o acesso às notificações habilitado para o Alarme Zap e confirme que a regra está ligada e que o horário/dias incluem o momento do teste. Use o botão 2 para permitir a execução sem otimização de bateria.

Em Xiaomi/Redmi, também abra **Configurações > Apps > Gerenciar apps > Alarme Zap** e, conforme os nomes da sua versão do sistema, habilite **Inicialização automática**, escolha **Sem restrições** em Economia de bateria e fixe o app na tela de apps recentes. Os nomes e caminhos variam entre MIUI e HyperOS.

Para diagnóstico, abra o Alarme Zap depois que uma notificação chegar. A tela mostra o horário e o motivo da última notificação tratada, além da última tentativa de iniciar o serviço. Se aparecer “Android recusou iniciar serviço”, confira a exceção de bateria e as opções Xiaomi/Redmi. Se não atualizar o horário, o Android desconectou ou suspendeu o acesso às notificações. Se disser que não há regra ativa correspondente, confira app, dias e horário da regra.

O projeto foi inspecionado, mas não foi compilado nem testado em um aparelho nesta sessão. A tela em segundo plano ainda depende das regras do Android para atividades em tela cheia; o alarme sonoro e a vibração são iniciados pelo serviço.
