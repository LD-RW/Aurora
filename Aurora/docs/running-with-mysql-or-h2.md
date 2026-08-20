# Running Aurora with MySQL or H2

Aurora supports two datasources: an in-memory H2 database (the default, zero setup) and a
persistent MySQL database running in Docker (for testing against something closer to a real
deployment). This note covers running each one and switching between them.

## H2 (default, no setup needed)

Running the app with no extra flags uses H2:

```bash
./mvnw spring-boot:run
```

The schema is created fresh from the JPA entities on every startup and lives only in memory --
restarting the app wipes all data. This is also what the test suite and CI use, so nothing here
needs Docker or any external service.

Browse the data at `http://localhost:8080/h2-console` while the app is running. Connection
settings on that login page:

- **JDBC URL:** `jdbc:h2:mem:test`
- **User Name:** `sa`
- **Password:** *(leave blank)*

## MySQL (persistent, via Docker)

### One-time setup

1. Start the database container from the repository root (where `docker-compose.yml` lives):

   ```bash
   docker compose up -d
   ```

   This starts MySQL 8.4 and Adminer (a lightweight database web UI), creating a `aurora`
   database and an `aurora` user on first boot. Wait for it to finish initializing:

   ```bash
   docker compose logs -f mysql
   ```

   Watch for a line ending in `ready for connections`, then `Ctrl+C` to stop following the logs
   (the container keeps running).

### Running the app against it

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

**From IntelliJ:** Run → Edit Configurations → select the `AuroraApplication` configuration →
set **Active profiles** to `mysql` (Ultimate), or add the VM option `-Dspring.profiles.active=mysql`
/ environment variable `SPRING_PROFILES_ACTIVE=mysql` (Community). Once set, the regular Run
button always uses MySQL until you clear that field.

Unlike H2, MySQL data persists across app restarts -- stopping and restarting Aurora doesn't
wipe anything. The schema is still managed automatically (`ddl-auto=update`), so new columns or
tables from entity changes get added without you running any migration by hand.

### Browsing the data

Adminer is the closest equivalent to H2's console here: open `http://localhost:8081` and log in
with:

- **System:** MySQL
- **Server:** `mysql` -- the Docker Compose service name, not `localhost` (Adminer resolves
  other containers by service name over the internal Docker network)
- **Username:** `aurora`
- **Password:** `aurora`
- **Database:** `aurora`

If you have IntelliJ Ultimate, its built-in **Database** tool window (`View -> Tool Windows ->
Database`) works too, connecting directly with host `localhost`, port `3306`, and the same
credentials.

### Stopping and resetting

```bash
docker compose stop
```
Pauses the containers; data is untouched. `docker compose up -d` resumes where you left off.

```bash
docker compose down
```
Removes the containers, but the named volume (and therefore the data) survives.

```bash
docker compose down -v
```
Removes the containers **and** the volume -- this is the one that actually deletes all MySQL
data. The next `docker compose up -d` starts completely fresh.

## How the switch works

The two datasources are two separate property files, not one file you edit back and forth:

| Profile | File | Notes |
|---|---|---|
| default (H2) | `src/main/resources/application.properties` | Always loaded first, this is the baseline. |
| `mysql` | `src/main/resources/application-mysql.properties` | Only loaded when the `mysql` profile is active; its values override the defaults above. |

Passing `-Dspring-boot.run.profiles=mysql` (or setting `SPRING_PROFILES_ACTIVE=mysql`) is what
activates the second file. With no profile specified, only `application.properties` loads and
you get H2 -- exactly today's behavior, unchanged. This is also why CI is unaffected by any of
this: the pipeline never sets that profile, so it always runs against H2.

The credentials in `application-mysql.properties` and `docker-compose.yml` (`aurora`/`aurora`,
`rootpassword`) are local-development placeholders, the same spirit as the committed JWT secret
default in `application.properties` -- fine for a container that's only reachable on
`localhost`, not meant to be reused anywhere real.
