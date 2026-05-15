# Spec — Programação do Evento

## Visão Geral
A **Programação** é uma nova aba no detalhe do evento, ao lado de "Equipe" e "Repertório".
Ela representa a ordem do culto — o que vai acontecer durante o evento e em qual sequência.

---

## Tipos de Item

### MUSIC
- Representa uma música que já está no repertório do evento
- Não é criado diretamente na programação

### TEXT
- Representa um momento do culto (ex: "Início do Culto", "Oração", "Avisos")
- Criado e gerenciado diretamente na programação
- Campo `title` obrigatório
- Campo `description` opcional

---

## Regras de Negócio

### Músicas
- Ao **adicionar** uma música ao repertório → ela é automaticamente inserida no **final** da programação
- Ao **remover** uma música do repertório → ela é automaticamente removida da programação
- Músicas **não podem** ser adicionadas ou removidas diretamente pela programação

### Itens de Texto
- São criados diretamente na programação
- Entram no **final** da programação ao serem criados
- Podem ser editados e removidos diretamente na programação

### Ordenação
- Todos os itens possuem uma `position` que define a ordem de exibição
- A ordem é reordenável via drag & drop
- A reordenação é feita enviando a lista completa de IDs na nova ordem (batch update)

---

## Endpoints

```
GET    /api/events/{eventId}/program              Lista a programação ordenada por position
POST   /api/events/{eventId}/program/text         Adiciona item de texto
PUT    /api/events/{eventId}/program/{itemId}     Edita item de texto
DELETE /api/events/{eventId}/program/{itemId}     Remove item de texto
PATCH  /api/events/{eventId}/program/reorder      Reordena todos os itens
```

---

## Contratos

### GET /program
**Response**
```json
[
  {
    "id": "uuid",
    "type": "MUSIC",
    "position": 1000,
    "music": {
      "id": "uuid",
      "title": "Superman",
      "artist": "Fruto Sagrado"
    }
  },
  {
    "id": "uuid",
    "type": "TEXT",
    "position": 2000,
    "title": "Oração",
    "description": "Momento de oração coletiva"
  }
]
```

### POST /program/text
**Request**
```json
{
  "title": "Início do Culto",
  "description": "Abertura com boas-vindas"
}
```

### PUT /program/{itemId}
**Request**
```json
{
  "title": "Título atualizado",
  "description": "Descrição atualizada"
}
```

### PATCH /program/reorder
**Request**
```json
{
  "orderedIds": ["uuid1", "uuid2", "uuid3"]
}
```

---

## Observações para Implementação
- O serviço de repertório deve chamar o serviço de program ao adicionar/remover músicas
- A posição inicial dos itens deve usar gaps de 1000 (ex: 1000, 2000, 3000) para facilitar reordenação futura
- Quando os gaps ficarem pequenos (< 10), redistribuir as posições em background
