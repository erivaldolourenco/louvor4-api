# Louvor4 API 🎶

Louvor4 API é o backend da plataforma **Louvor4**, criada para ajudar músicos que atuam em **vários projetos ao mesmo tempo** (ex.: 2–3 bandas, gigs, eventos e trabalhos diferentes) a manterem **agenda organizada** e um **histórico confiável** do que foi executado em cada ocasião.

A API fornece autenticação, regras de negócio e persistência para músicas, eventos, repertórios e histórico musical.

## 🚀 O problema
Para um músico que toca com várias bandas e projetos, a rotina vira um quebra-cabeça:

- Conflitos de agenda (datas e horários se chocando)
- Dificuldade para lembrar **o repertório** de cada evento
- Falta de histórico: **quais músicas foram tocadas**, em qual tom/versão, e em quais eventos
- Informações espalhadas em mensagens, planilhas, anotações e arquivos soltos

Com o tempo, isso vira retrabalho e aumenta a chance de erro no dia do evento.

## 💡 A solução
O Louvor4 centraliza a vida profissional do músico em um único lugar, com foco em:

### ✅ Agenda multi-projetos
- Visualização organizada dos compromissos por projeto/banda
- Eventos com data, local, participantes e observações
- Ajuda a reduzir conflitos e melhorar planejamento

### ✅ Histórico de eventos e repertórios
- Registro do que foi executado em cada evento (repertório)
- Consulta rápida: “o que tocamos naquele show/culto/apresentação?”
- Linha do tempo da vida musical do usuário

### ✅ Histórico de músicas
- Cadastro de músicas com tom, BPM e referências (ex.: YouTube)
- Possibilidade de manter versões/observações
- Consulta do uso: em quais repertórios e eventos a música apareceu

A API foi pensada para evoluir com o produto, suportando novas funcionalidades e integrações.

## 🧑‍🤝‍🧑 Público-alvo
- Músicos independentes
- Músicos que atuam em múltiplas bandas/projetos
- Bandas e grupos musicais
- Produtores e equipes que organizam eventos

## 🛠️ Tecnologias
- **Linguagem:** Java
- **Framework:** Spring Boot
- **Segurança:** Spring Security + JWT
- **Persistência:** JPA / Hibernate
- **Banco de dados:** MySQL
- **Arquitetura:** REST API
- **Configuração:** application.yml (profiles dev/prd)

## 🔐 Segurança
- Autenticação baseada em JWT
- API stateless
- CORS configurável por ambiente
- Dados vinculados ao usuário autenticado (ex.: músicas criadas pelo próprio usuário)

## 📌 Funcionalidades (em desenvolvimento)
- Autenticação e gerenciamento de usuários
- Cadastro e consulta de músicas
- Criação de eventos/agenda
- Associação de músicas a eventos (repertório)
- Histórico de eventos e repertórios
- Histórico de execução de músicas (uso por evento)

## 🧪 Status do projeto
🚧 **MVP em desenvolvimento ativo**

## ▶️ Como rodar o projeto localmente

### Pré-requisitos
- Java 17+ (ou 21)
- Maven
- MySQL

### Configuração
As configurações podem ser feitas via `application.yml` e variáveis de ambiente.

Exemplo:
```env
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:mysql://localhost:3306/louvor4db
DB_USERNAME=root
DB_PASSWORD=******
JWT_SECRET=******
