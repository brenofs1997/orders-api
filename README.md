# Orders API

API REST para cadastro de usuários, autenticação e gerenciamento de pedidos de delivery. A aplicação usa Spring Boot, Spring Security, JWT e persistência relacional com Spring Data JPA.

## Funcionalidades

- Cadastro de usuários com senha armazenada usando BCrypt.
- Login por e-mail e senha, com emissão de access token JWT e refresh token.
- Renovação e invalidação de refresh tokens.
- Criação, consulta e alteração de status de pedidos.
- Controle de acesso por token e escopos de usuário (`admin` e `basic`).
- Listagem de usuários restrita ao escopo administrativo.

## Tecnologias

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Security e OAuth2 Resource Server (JWT assinado com chaves RSA)
- Spring Data JPA / Hibernate
- MySQL
- Maven Wrapper
- Lombok

O `pom.xml` também declara dependências SQLite, mas a configuração ativa em `application.properties` utiliza MySQL.

## Pré-requisitos

- JDK 21
- Docker com Docker Compose (para iniciar o MySQL incluído) ou uma instância MySQL acessível

## Executar localmente

1. Inicie o banco MySQL fornecido pelo projeto:

   ```powershell
   docker compose -f docker/docker-compose.yml up -d mysql
   ```
   
   O serviço publica a porta `3306`, cria o banco `mydb` e configura o usuário de desenvolvimento `admin`. Para usar outro banco, atualize `spring.datasource.url` e as credenciais em `src/main/resources/application.properties`.

2. Configure as credenciais esperadas pela aplicação no PowerShell:

   ```powershell
   $env:DB_USER = "admin"
   $env:DB_PASSWORD = "123"
   ```

3. Compile e execute a aplicação:

   ```powershell
   .\mvnw.cmd clean install
   .\mvnw.cmd spring-boot:run
   ```

A API fica disponível em `http://localhost:8080`.

> As credenciais do Docker Compose e do administrador inicial são apenas para desenvolvimento local. Não as utilize em produção. Configure credenciais seguras e proteja/substitua as chaves RSA antes de publicar a aplicação.

## Autenticação

As rotas protegidas recebem um access token no cabeçalho:

```http
Authorization: Bearer <accessToken>
```

O access token expira em 300 segundos. O refresh token tem duração configurada por `jwt.refreshExpirationMs` (300.000 ms por padrão).

### Cadastrar usuário

`POST /api/users` — público. O novo usuário recebe o escopo `BASIC`.

```json
{
  "email": "cliente@example.com",
  "name": "Cliente Exemplo",
  "password": "senhaSegura123"
}
```

### Fazer login

`POST /api/auth/login` — público.

```json
{
  "email": "cliente@example.com",
  "password": "senhaSegura123"
}
```

A resposta contém `accessToken`, `refreshToken`, `expiresIn` e os dados básicos do usuário (`userDto`):

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<refresh-token>",
  "expiresIn": 300,
  "userDto": {
    "email": "cliente@example.com",
    "name": "Cliente Exemplo"
  }
}
```

### Renovar access token

`POST /api/auth/refresh` — público. Envie o refresh token recebido no login:

```json
{
  "refreshToken": "<refresh-token>"
}
```

Em caso de sucesso, a resposta contém um novo access token no campo `token`.

### Encerrar sessão

`POST /api/auth/logout` — público. Envie o refresh token no mesmo formato da renovação; o token será invalidado.

## Pedidos

As rotas de pedidos exigem autenticação. O identificador do pedido é numérico.

### Criar pedido

`POST /api/orders` — retorna `201 Created`.

```json
{
  "customerName": "Maria Silva",
  "deliveryAddress": "Rua das Flores, 123",
  "items": [
    {
      "name": "Hambúrguer Artesanal",
      "quantity": 2,
      "unitPrice": 35.00
    },
    {
      "name": "Refrigerante 350ml",
      "quantity": 2,
      "unitPrice": 6.00
    }
  ]
}
```

O pedido é criado com status `RECEBIDO` e registra a data/hora de criação.

### Listar pedidos

`GET /api/orders` — retorna todos os pedidos.

### Consultar pedido

`GET /api/orders/{id}` — consulta um pedido pelo ID.

### Atualizar status

`PATCH /api/orders/{id}/status`

```json
{
  "status": "EM_PREPARO"
}
```

Valores aceitos para `status`:

- `RECEBIDO`
- `EM_PREPARO`
- `SAIU_PARA_ENTREGA`
- `ENTREGUE`
- `CANCELADO`

## Usuários e acesso administrativo

`GET /api/users` lista usuários e exige um JWT com o escopo `ADMIN`. Os escopos são incluídos no token a partir dos papéis associados ao usuário.

Na inicialização, a aplicação cria o usuário administrador abaixo se ele ainda não existir:

- E-mail: `admin@gmail.com`
- Senha local padrão: `123456`

Essa conta é destinada somente ao ambiente local. Altere a estratégia de provisionamento e as credenciais antes de qualquer implantação real.

## Configuração

As principais propriedades ficam em `src/main/resources/application.properties`:

| Propriedade | Descrição |
| --- | --- |
| `spring.datasource.url` | URL JDBC do MySQL |
| `spring.datasource.username` | Variável de ambiente `DB_USER` |
| `spring.datasource.password` | Variável de ambiente `DB_PASSWORD` |
| `spring.jpa.hibernate.ddl-auto` | Estratégia de atualização do schema (`update`) |
| `jwt.public.key` | Chave pública RSA usada para validar JWTs |
| `jwt.private.key` | Chave privada RSA usada para assinar JWTs |
| `jwt.refreshExpirationMs` | Duração do refresh token em milissegundos |

As chaves RSA estão em `src/main/resources/app.pub` e `src/main/resources/app.key` para a configuração atual. Em produção, forneça essas chaves de forma segura e não versione material privado.

## Estrutura do projeto

```text
src/
├── main/
│   ├── java/com/tracker/orders_api/
│   │   ├── config/       # Segurança, JWT e inicialização do administrador
│   │   ├── controller/   # Endpoints REST e DTOs de entrada/saída
│   │   ├── entities/     # Usuários, papéis, pedidos, itens e refresh tokens
│   │   ├── repository/   # Repositórios Spring Data JPA
│   │   ├── service/      # Regras de pedidos e ciclo de refresh tokens
│   │   └── utils/        # Utilitários para geração de JWT
│   └── resources/        # Configuração, chaves RSA e inicialização SQL
└── test/                 # Testes automatizados
```

## Testes

Execute os testes com o Maven Wrapper:

```powershell
.\mvnw.cmd test
```
