# CLIuno Spring template

Spring Boot 3.4 (Java 17) + JPA (SQLite) REST API serving the CLIuno contract: JWT auth
(refresh, reset, email verification, OTP), users, todos, posts+comments, follows,
roles — under `/api/v1`.

## Commands

```bash
./mvnw spring-boot:run          # dev server (PORT env, default 3000)
./mvnw test                     # tests — keep green
./mvnw -DskipTests package      # target/cliuno-spring-template-2.0.1.jar
java -jar target/cliuno-spring-template-2.0.1.jar
```

SQLite `db.sqlite` auto-creates (`ddl-auto=update`; delete for a reset). JWT secrets:
`JWT_SECRET_KEY` / `REFRESH_JWT_SECRET_KEY` env (dev defaults exist).

## Structure

- `com.cliuno.api.controller` — one controller per resource; handlers call
  `auth.require(request)` (Bearer → User or 401 envelope via
  `GlobalExceptionHandler`) and `auth.requireAdmin(user)` where admin-gated.
- `com.cliuno.api.entity` — public-field JPA entities; secrets are `@JsonIgnore`;
  `Post.comments` is `FetchType.EAGER` deliberately (open-in-view is off — lazy
  collections would 500 during serialization).
- `com.cliuno.api.support` — `Api` (envelope builders: data keys are map literals so
  they bypass the snake_case naming policy), `JwtService` (jjwt), `TotpService`
  (dependency-free RFC 6238), `AuthSupport`.
- Jackson `SNAKE_CASE` globally: Java camelCase fields serialize as `first_name` etc.;
  `imageUrl` keeps its casing via `@JsonProperty`.

## Contract rules this codebase follows

- Responses: `{status, message, data}` with exact keys (`data.users/user/todos/todo/`
  `posts/post/followers/following/isFollowing`, login `data.token` + `data.refreshToken`).
- camelCase request keys; one-time tokens on the user row (`reset_token`,
  `verify_token` — explicit `@Column` names), lookup by token; the `user` role is
  created on first registration.

## Conventions

Conventional commits; `./mvnw` wrapper is committed — don't require a global maven.

## AI agents (Claude Code and friends)

- **Check the CLIuno skills first**: `npx cliuno@latest skills status` — if any is
  `missing`/`outdated`, run `npx cliuno@latest skills install` (adds `.claude/skills/cliuno-*`:
  the generator workflow, the scaffold guide, and the shared REST contract).
- **Generate resources, don't hand-write them**:
  `npx cliuno@latest make:resource Product --fields name:string,price:number,inStock:boolean`
  creates `src/main/java/com/cliuno/api/entity/Product.java`, `src/main/java/com/cliuno/api/repo/ProductRepository.java`, `src/main/java/com/cliuno/api/controller/ProductController.java`, `src/test/java/com/cliuno/api/ProductTests.java`, following this
  template's conventions and the `{status, message, data}` contract. `--dry-run` previews,
  re-running never duplicates registrations, `--force` overwrites.
- Single pieces: `make:model`, `make:controller`, `make:test`. `npx cliuno@latest make` lists them.
- Then edit fields/business logic only, and run the gates above.
