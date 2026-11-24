# Software Factory za Java backende

> 🇬🇧 English: [README.md](README.md) · korak po korak: [VODIC.md](VODIC.md)

Software factory za [Claude Code](https://claude.com/claude-code). Ti otvoriš GitHub issue
(tiket); agenti odluče da li vrijedi praviti, napišu specifikaciju, napišu kod i unit testove,
pokrenu `mvn verify`, pregledaju izmjenu, **podignu servis lokalno nad Postgresom, Keycloakom i
LocalStackom i pozovu endpointe**, i mergaju pull request kad je GitHub Actions zelen. Ti
odlučuješ samo ono što agenti eskaliraju.

Pravi i **nove projekte** jednom naredbom:

```
/software-factory new identity java           Spring Boot servis sa svojom Postgres bazom
/software-factory new ui-service java proxy   Spring Boot proxy/BFF: bez baze, zove druge servise
/software-factory new development dev         lokalni stack: docker compose, Keycloak, LocalStack, Postgres, aliasi
```

Svaki novi projekat dobije `CLAUDE.md`, privatni GitHub repo pod računom tvog tokena i (java)
GitHub Actions pipeline koji na svaki push pokreće unit testove. Paket `com.indir.<ime>`.

## Instalacija (jednom po računaru)

Treba ti `git`, `gh`, `python3` ≥ 3.10, Java 17, Maven, Docker i Claude Code.

```bash
git clone https://github.com/indirkaric/software-factory.git ~/software-factory
```

```bash
~/software-factory/software-factory setup
```

Setup provjeri alate, zadrži GitHub token s kojim si klonirao (ili pita za novi), provjeri
njegove permisije ([GITHUB-PERMISSIONS.md](GITHUB-PERMISSIONS.md)), pita za folder projekata
(default `~/Desktop/projects`) i instalira `/software-factory`, `/software-factory-run` i
`sf-*` agente u Claude Code za svaki folder, plus `github` MCP server. Restartuj Claude Code.
Kasnije: `software-factory update` povuče novu verziju i reinstalira.

Ništa se ne instalira po servisu: prvi `/software-factory` u repou ga sam prikači (`factory/`
je van gita) i napiše `CLAUDE.md` ako ga repo nema.

## Korištenje

```
/software-factory new development dev   jednom: lokalni stack (pa: dev-up)
/software-factory new invoice java      servis, registrovan u development repou
/software-factory dodaj status fakturi: DRAFT, SENT, PAID   → otvori issue i napravi ga
/software-factory run                   svi otvoreni issue-i s labelom `factory`
/software-factory run 1                 issue #1 odmah (isto što i /software-factory-run 1)
/software-factory issues                otvoreni issue-i i njihovi jobovi
/software-factory context               (ponovo) napiši CLAUDE.md iz koda
```

Možeš otvoriti više Claude Code sesija u istom repou i u svakoj raditi drugi tiket: svaki job
ima svoj git worktree i granu, a sesija "zauzme" job prije nego što radi na njemu.

## Tok jednog tiketa

```
issue ─► intake ─► spec ─► build ─► gate ─► review ─► verify ─► approve ─► merge
                                    mvn     code + api/db/  podigne       čeka GitHub
                                    verify  integration/    granu na      Actions pa
                                            security        dev stacku    squash merge
```

Najviše 3 runde. Ono što padne u zadnjoj rundi, ili što approver neće sam mergati (security,
migracije koje nisu aditivne, novi plugini, CI izmjene, breaking API), čeka tebe kao
`factory:needs-human` sa tačnim pitanjem na PR-u. Odgovoriš sa `/software-factory za #12: ...`.

## Šta ima novi Java servis

Spring Boot 3.2, Java 17: slojevi (controller → response creator → operation service →
validator → repository), `Check` provjere, globalni exception handler, Keycloak JWT (realm role
postaju `SCOPE_` authorities), headeri između servisa `service-communication-id`,
`correlation-id`, `user-id`, `BaseClient` nad WebClientom, read/write rutiranje baze, audit
entiteti, Liquibase, checkstyle, JaCoCo ≥ 80 % (instrukcije i linije; gate odbija manje),
`Dockerfile`, `docker-compose.yml` + `docker-compose.override.yml`, `permissions.txt` i
`.github/workflows/ci.yml`
(`mvn verify`, plus Liquibase update → rollback → update test za servise s bazom). Pravila su
u `templates/java/CLAUDE.md` i `.claude/skills/*`, uključujući S3/Azure storage i
korisnike/grupe/permisije/personal access tokene.

## Development repo

`docker-compose.yml` (Postgres :5432, Keycloak :8080, LocalStack :4566; Azurite i Redis na
zahtjev), realm `factory` sa grupama Administrator/Manager/Employee i korisnicima `admin`,
`manager`, `user`, `basic` (lozinka = ime), bucket
`factory-local`, i `dev-*` aliasi (`dev-up`, `dev-run <servis>`, `dev-token`, `dev-psql`,
`dev-logs`, `dev-help`). Bez e2e testova. Detalji: `templates/dev/README.md`.

Ako ti već radi drugi stack na istim portovima, zaustavi ga ili promijeni `DB_PORT`,
`AUTH_PORT`, `AWS_PORT` u `development/.env`.

## Permisije

Servis u `permissions.txt` navodi Keycloak permisije koje provjerava (`READ-USER
Manager,Employee`); unit test pada ako kod provjerava permisiju koje tu nema. Kad npr. na
user servisu tražiš feature sa `READ-USER`, factory je sam doda u realm development repoa
(rola + client scope + grupa, kao u pravom realmu) za lokalnu provjeru, a nakon merge-a je
commita i pusha u development. Projekti su uvijek jedan pored drugog u istom folderu.

## Paralelne sesije i portovi

Svaki job ima svoj worktree, granu, port (51000-51999, kao `--server.port`, nikad u kodu) i
bazu `sfv_<servis>_<job>`; svi dijele jedan Postgres/Keycloak/LocalStack. Lease fajlovi u
`~/.cache/software-factory/run` sprečavaju da dvije sesije uzmu isti port. Po završetku
provjere (i na kraju svakog runa, `verify gc`) app se gasi, port oslobađa, baza briše; ako je
factory sam podigao infrastrukturu, zadnji job je i gasi. Tuđi proces na portu se nikad ne
ubija. `factory/bin/verify ps` pokaže šta radi.

## Licenca

[GPLv3](LICENSE). Bazirano na zazencodes software-factory-claude-code-v2.
