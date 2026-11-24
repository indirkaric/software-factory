# Software Factory — vodič korak po korak

Od praznog računara do prvog PR-a koji je factory napravio i mergao. Kod svakog koraka piše
**šta radiš** i **zašto**. (English: [GUIDE.md](GUIDE.md))

## 0. Šta je šta

| Dio | Gdje | Šta radi |
|---|---|---|
| Claude Code | komanda `claude` | pokreće agente |
| software-factory | `~/software-factory` (ovaj repo) | komanda `software-factory`, template-i, agenti |
| development | `~/Desktop/projects/development` | lokalni Postgres, Keycloak, LocalStack + `dev-*` aliasi |
| servis (npr. `invoice`) | `~/Desktop/projects/invoice` + svoj GitHub repo | tu factory radi tikete |
| GitHub | github.com | kod, issue-i (tiketi), PR-ovi, Actions (CI) |

## Faza A — jednom po računaru

### A1. Alati

```bash
sudo apt install git gh maven python3 docker.io docker-compose-v2
```

Java 17 (npr. `sdk install java 17-tem`) i Claude Code. **Zašto:** factory gradi Mavenom,
s GitHubom priča preko `gh`, a svaki feature lokalno provjerava u Dockeru.

### A2. GitHub token

Napravi fine-grained token po tabeli u [GITHUB-PERMISSIONS.md](GITHUB-PERMISSIONS.md)
(Administration, Contents, Pull requests, Issues, Workflows: read/write; Actions: read).
**Zašto:** isti token pravi repoe, pusha, otvara PR-ove i merga ih. Tvoj trenutni token nema
Administration, pa `new` ne bi mogao napraviti repo.

### A3. Kloniraj i pokreni setup

```bash
git clone https://github.com/indirkaric/software-factory.git ~/software-factory
```

```bash
~/software-factory/software-factory setup
```

Pitanja: "Use it? [Y/n]" (koristi token s kojim si klonirao; `n` = zalijepi novi), i folder
projekata (Enter = `~/Desktop/projects`). Setup instalira `/software-factory` i agente u
Claude Code i `github` MCP. **Restartuj Claude Code.**

Provjera:

```bash
software-factory doctor ~/software-factory
```

## Faza B — development repo (jednom)

### B1. Napravi ga

U Claude Codeu (bilo koji folder): `/software-factory new development dev`

Napravi `~/Desktop/projects/development` i privatni GitHub repo. **Zašto:** svaki servis i
lokalna provjera factoryja koriste iste kontejnere.

### B2. Aliasi i hostovi

```bash
cd ~/Desktop/projects/development && ./scripts/install-aliases.sh
```

Otvori novi terminal, pa:

```bash
dev-hosts
```

```bash
dev-up
```

`dev-hosts` mapira `*.factory.local` na 127.0.0.1 (traži sudo). `dev-up` diže Postgres (:5432),
Keycloak (:8080, admin/admin) i LocalStack (:4566). `dev-help` ispiše sve komande. Ako ti radi
stari stack na istim portovima, prvo ga zaustavi ili u `development/.env` promijeni
`DB_PORT`, `AUTH_PORT`, `AWS_PORT`.

## Faza C — novi servis

U Claude Codeu: `/software-factory new invoice java` (ili `... java proxy` za BFF bez baze;
`--ref <putanja>` daje svakom jobu lokalni projekat kao primjer).

Šta se desi, sve samo:

1. servis se generiše iz skeletona (`<bazni paket>.invoice`, port 50001+), sa `CLAUDE.md`;
2. `mvn verify` mora proći (unit testovi, JaCoCo, checkstyle);
3. privatni repo `<tvoj-račun>/invoice` + push `main`, auto-merge i squash uključeni;
4. servis se upiše u development repo (compose, baza `invoice`, alias, registar `platform.md`).

Lokalno pokretanje: `dev-run invoice`, ili iz IDE-a `mvn spring-boot:run`.

## Faza D — svakodnevni rad

### D1. Otvori Claude Code u servisu

```bash
cd ~/Desktop/projects/invoice && claude
```

### D2. Zadaj posao

Opisno: `/software-factory dodaj status fakturi: DRAFT, SENT, PAID; PATCH /invoice/v1/{id}/status`

Factory napiše issue, pokaže ti ga, i na tvoje "da" ga otvori sa labelom `factory` i krene.
Ili otvori issue na GitHubu sam pa: `/software-factory run 1` (ili `/software-factory-run 1`).

### D3. Šta factory radi

```
intake → spec → build (kod + testovi) → gate (mvn verify) → review → verify (lokalno) → approve → merge
```

U sesiji vidiš po jednu liniju za svaki korak. `verify` podigne tvoju granu nad development
stackom (prazna baza, Liquibase od nule), uzme token od Keycloaka i pozove endpointe.
Merge ide tek kad je i GitHub Actions zelen.

### D4. Kad te treba

- `factory:needs-info`: agent pita nešto na issue-u. Odgovori tamo ili
  `/software-factory odgovor za #3: ...`, pa `/software-factory run`.
- `factory:needs-human`: pitanje je na PR-u. Merge na GitHubu = odobreno; ili
  `/software-factory za #3: PAID se nikad ne vraća nazad, dodaj test` = nova runda.
- Odbaci: `/software-factory odbaci #3, radićemo drugačije`.

### D5. Paralelno

Otvori drugu Claude Code sesiju u istom servisu i pokreni drugi tiket. Svaki job ima svoj
worktree (`.worktrees/<job>`) i granu (`factory/<job>`); sesija zauzme job, druga ga preskoči.
Lokalna provjera svakom jobu da svoj port (51000+) i svoju bazu, a poslije sve ugasi
(`factory/bin/verify ps` pokaže šta radi, `verify gc` počisti).

### D5b. Nova permisija

Kažeš npr. "na user servisu treba permisija READ-USER za ...". Builder je doda u
`Constants.ApiScopes` i u `permissions.txt` (`READ-USER  Manager,Employee`); factory je ubaci u
realm development repoa (i u Keycloak koji radi), a nakon merge-a commita i pusha development.
Ručno: `dev-perm`, `dev-perm add READ-X --groups Manager`.

### D5c. Izmjena u drugom servisu

Ako feature traži izmjenu i u drugom servisu (npr. `portal` treba novi endpoint u `users`),
factory te pita: "treba promijeniti i `users`, na novoj grani?". Na "da" otvori issue i job u
`../users` (grana `factory/<id>`). Testira se cijeli task zajedno: svi servisi sa svojih
grana, povezani međusobno, plus servisi koji ih pozivaju (sa `main`-a), flow feature-a i smoke
svih GET endpointa. Ako nešto pukne, nazad ide samo servis koji je kriv. Merge: prvo pozvani
servis, pa onaj koji ga poziva. Na "ne" job čeka tebe (`needs-human`).

### D6. Dashboard

```bash
cd factory && python3 -m http.server
```

http://localhost:8000/dashboard.html: svaki job, runde, review-i, cijena, vrijeme.

## Postojeći projekat

Otvori Claude Code u njemu i pokreni `/software-factory run`. Ako nema `CLAUDE.md`, factory ga
prvo napiše iz koda (pita te prije commita). Repo mora imati GitHub `origin` i `pom.xml`.

## Ažuriranje

```bash
software-factory update
```

## Problemi

| Simptom | Rješenje |
|---|---|
| `could not create ... Administration: write` | novi token (A2), `software-factory setup`, `n` |
| `VERIFY: SKIP` | Docker ne radi ili nema development repoa: `dev-up` |
| gate pada "offline" | u sesiji pokreni sa `FACTORY_MVN_ONLINE=1 claude` |
| port zauzet | zaustavi drugi stack ili promijeni port u `docker-compose.override.yml` |
| token ističe | `software-factory doctor` upozori 14 dana prije |
