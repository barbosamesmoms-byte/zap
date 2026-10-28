# Alarme Zap — correção de serviço e parada

Correções nesta versão:
- registra o `AlarmService` no AndroidManifest;
- corrige as permissões e o tipo de serviço em primeiro plano para reprodução de áudio;
- ao deslizar para parar, cancela player, vibração, notificação e serviço.

## Publicação
Envie o conteúdo desta pasta para o repositório GitHub, preservando a estrutura de pastas. Depois confira a aba Actions: o workflow `Build APK` precisa terminar em verde. Baixe o APK em Artifacts da execução mais recente e instale essa versão.

## Configuração no Android
Mantenha o acesso às notificações habilitado para o Alarme Zap e confirme que a regra está ligada e que o horário/dias incluem o momento do teste. Se o aparelho bloquear atividade em tela cheia, o som/vibração ainda deve iniciar, mas a tela do alarme pode não abrir automaticamente.
