# Software Factory za Java backend

> 🇬🇧 English: [README.md](README.md)

Software factory za Claude Code koji radi kroz GitHub issue-e jednog Spring
Boot servisa. Označiš issue labelom `factory`, pokreneš `/factory run`, i agenti
odluče da li se isplati graditi, napišu spec, napišu kod i testove po pravilima
tvog `project-template`-a, puste `mvn verify`, reviewaju, i otvore PR koji
zatvara issue. Approver sam merge-a ono u šta je siguran, a ostalo ostavlja
tebi.

Baziran na [zazencodes software-factory-claude-code-v2](https://github.com/zazencodes/zazencodes-season-3/tree/main/src/software-factory-claude-code-v2)
(GPLv3), prilagođen Java/Spring Boot backendu i konvencijama iz
`project-template` (CLAUDE.md + skillovi po slojevima).

## Kako se uklapa s project-template

```
project-template  ──/new-microservice──▶  novi servis (../invoice)
                                            │  git init + GitHub repo
                                            ▼
software-factory  ──./install.sh──────▶  servis + factory
                                            │  issue s labelom `factory`
                                            ▼
                                        /factory run → PR → merge
```

- **project-template** pravi *novi servis* (skeleton, infrastruktura, prvi modul).
- **software-factory** gradi *feature-e u postojećem servisu*, issue po issue.

Factory agenti ne izmišljaju arhitekturu: builder i revieweri čitaju servisov
`CLAUDE.md` i skillove (`controller-conventions`, `service-layer-conventions`,
`entity-and-liquibase-conventions`, `testing-conventions`, …) i tvoje postojeće
`code-reviewer.md` / `integration-flow-auditor.md` checkliste. Kad promijeniš
pravilo u templateu, factory ga automatski poštuje.

## Pipeline

```
issue s labelom `factory`
        │
        ▼
intake (fable) ── NEEDS-INFO → pita autora na issue-u, čeka odgovor
        │         REJECT     → kaže zašto, skida label
        ▼ ACCEPT (možda suženo); ocijeni risk + surfaces (api, db, integration, security)
route (skripta)   bira reviewere i modele iz config.json
        ▼
spec-writer (opus) → spec.md: endpointi, tabele, klase po slojevima, acceptance kriteriji
        ▼
builder (opus) → kod + testovi, commit, push, draft PR "Closes #n"
        ▼            ▲
gate (skripta) ─FAIL─┤  factory/checks.sh = mvn -o clean verify (testovi, JaCoCo, checkstyle)
        ▼ PASS       │
revieweri ─CHANGES───┘  code uvijek; + api / db / integration / security po surface-u
        ▼ svi PASS
approver (opus) ── APPROVE  → sync s main, gate ponovo, gh pr merge
                └─ ESCALATE → PR spreman za tebe, label factory:needs-human
```

| Reviewer | Kad radi | Model | Gleda |
|---|---|---|---|
| `code` | uvijek | haiku (low) / sonnet | `Check`, `final`, slojevi, jedan interfejs po operaciji, testovi, Javadoc |
| `api` | surface `api` | sonnet | endpointi, status kodovi, `@Valid`, OpenAPI, kompatibilnost |
| `db` | surface `db` | sonnet | Liquibase (samo aditivno), entity↔šema, indeksi, N+1, `readOnly` |
| `integration` | surface `integration` | sonnet | `BaseClient`, `Sqa-*` headeri, `platform.md`, filteri, eventi |
| `security` | surface `security` | opus | `@PreAuthorize`, ownership, input → SQL, curenje podataka, tajne |

Approver **uvijek eskalira** tebi: promjene u `security/`, ne-aditivne Liquibase
migracije, nove dependency-je u `pom.xml`, CI/Docker/deploy, promjene koje
traže izmjenu u drugom servisu (`platform.md`), i breaking API promjene.

## Šta ti treba (jednom)

1. **GitHub CLI** — trenutno nije instaliran na ovoj mašini:

   ```bash
   sudo apt install gh
   ```

   ```bash
   gh auth login
   ```

2. **Claude Code** 2.1.257+ (intake radi na Fable modelu), `python3` 3.10+,
   Java 17 i Maven (već imaš oboje).
3. Servis mora biti **git repo s GitHub `origin`-om** i pushan `main` branch.
   Servisi iz templatea nastaju bez gita, pa:

   ```bash
   cd ~/Desktop/projects/invoice && git init -b main && git add -A && git commit -m "Initial service"
   ```

   ```bash
   gh repo create indirkaric/invoice --private --source . --push
   ```

## Instalacija u servis

Klonuj factory (jednom):

```bash
git clone https://github.com/indirkaric/software-factory.git ~/Desktop/projects/software-factory
```

Instaliraj ga u servis:

```bash
~/Desktop/projects/software-factory/install.sh ~/Desktop/projects/invoice --labels --verify
```

Skripta:

- kopira `kit/` u servis (`.claude/skills/factory`, 9 agenata, `factory/`);
- postavi `base_branch` iz `origin`-a;
- doda `/factory/jobs/`, `/.worktrees/` i `!/factory/bin/` u `.gitignore`
  (Java `.gitignore` inače ignoriše `bin`);
- doda sekciju "Software factory" u servisov `CLAUDE.md`;
- `--labels` napravi labele `factory`, `factory:needs-info`, `factory:needs-human`;
- `--verify` pusti `factory/checks.sh` — mora proći na `main`-u, inače factory
  ne može proći nijedan job.

Ne commita ništa. Ti onda:

```bash
cd ~/Desktop/projects/invoice && git add .claude factory .gitignore CLAUDE.md && git commit -m "Install software factory" && git push
```

Push je obavezan: svaki job pravi branch iz `origin/main`.

Alternativa: otvori Claude Code u servisu i reci mu
*"Instaliraj software factory iz ~/Desktop/projects/software-factory po INSTALL.md"*.

## Korištenje

Otvori Claude Code **u root-u servisa** (ne u factory repou).

**1. Napiši issue** na GitHubu s labelom `factory`, npr.:

> **Add status to invoices**
> Invoice treba imati status DRAFT / SENT / PAID. Novi endpoint
> `PATCH /invoice/{id}/status`. Samo DRAFT → SENT → PAID je dozvoljeno.

Ili ga napravi iz Claude Codea: `/factory dodaj status na invoice: DRAFT, SENT, PAID…`
— factory sam otvori issue i pokrene ga.

**2. Pokreni:**

```
/factory run          svi otvoreni `factory` issue-i, `concurrency` odjednom; nastavlja prekinute
/factory run 12       samo issue #12
/factory <bilo šta>   pitanja, feedback, novi feature
```

**3. Prati:** u terminalu dobiješ jednu liniju po koraku
(`001-add-status #12: gate PASS → review (code, api, db)`). Dashboard:

```bash
cd factory && python3 -m http.server
```

pa otvori <http://localhost:8000/dashboard.html> — kartica po jobu, gate i
review čipovi po rundi, cijena i vrijeme; klik otvara svaki fajl koji je job
napisao.

**4. Odluči.** Svaki job završi kao PR:

- **merged** — approver je bio siguran, PR je merge-an.
- **factory:needs-human** — PR čeka tebe, s komentarom šta tačno treba odlučiti.
  Odobriš ga **merge-om na GitHubu**. Ako hoćeš izmjene:
  `/factory za #12: status PAID ne smije se vraćati nazad, dodaj test` — to ide
  builderu kao `feedback-1.md` i krene nova runda.
- **factory:needs-info** — intake je pitao autora na issue-u. Odgovori u
  komentaru issue-a (ili kroz `/factory odgovor za #12: …`), pa `/factory run`.
- Da odbaciš job: `/factory odbaci #12, radimo to drugačije`.

**Šta factory neće:** praviti nove mikroservise (to je `/new-microservice` u
templateu), dirati druge repoe, pokretati `docker`, deployati, pushati išta osim
`factory/*` brancheva.

## Podešavanja — `factory/config.json`

| Ključ | Default | Značenje |
|---|---|---|
| `concurrency` | 2 | koliko jobova radi paralelno (počni s 1) |
| `max_rounds` | 3 | build→gate→review rundi prije nego pita tebe |
| `merge_method` | squash | mora biti dozvoljen u repo postavkama |
| `routing` | low→haiku, medium/high→sonnet | model code reviewera po riziku |
| `surface_reviewers` | api/db/integration→sonnet, security→opus | dodatni revieweri |
| `protected_paths` | factory fajlovi + checkstyle | gate pada ako ih branch izmijeni |

`checks.sh` radi Maven **offline** (`-o`), kao template. Ako job doda novu
dependency koja nije u `~/.m2`, pokreni sesiju s `FACTORY_MVN_ONLINE=1`.

## Struktura repoa

```
install.sh                       instalira kit u servis
INSTALL.md                       upute za agenta koji instalira
DESIGN.md                        kako radi i šta je promijenjeno u odnosu na original
kit/                             ono što ide u servis
  .claude/skills/factory/SKILL.md   orkestrator (/factory)
  .claude/agents/
    intake.md               fable    vrijedi li, koji scope, risk + surfaces
    spec-writer.md          opus     spec.md po slojevima
    builder.md              opus     kod, testovi, commit, PR, build.md
    reviewer-code.md        routed   konvencije, ispravnost, testovi
    reviewer-api.md         sonnet   REST ugovor
    reviewer-db.md          sonnet   Liquibase, JPA, transakcije
    reviewer-integration.md sonnet   drugi servisi, Sqa-* headeri
    reviewer-security.md    opus     autorizacija, input, tajne
    approver.md             opus     APPROVE ili ESCALATE
  factory/
    config.json  checks.sh  dashboard.html
    bin/job      jedini pisac job.json, odbija nedozvoljene prelaze, merge
    bin/gate     checks.sh u worktreeu → round-N/checks.md
    bin/route    risk + surfaces → revieweri
```

## Cijena i sigurnost

- Otvara i merge-a prave PR-ove na tvom Claude planu. Počni s `concurrency: 1`
  i čitaj svaki PR dok mu ne vjeruješ.
- Intake radi na Fable jednom po issue-u; zavisno od plana, Fable može ići na
  usage kredite.
- Cijena po jobu na dashboardu je API-ekvivalent iz transkripata; na pretplati
  pokazuje gdje ide potrošnja, ne račun.
- Samo issue-i s labelom `factory` se grade (label mogu dodati samo ljudi s
  triage/write pristupom). Tekst issue-a agenti vide kao citirane podatke, ne
  kao instrukcije.

## Licenca

[GPLv3](LICENSE), kao i original.
