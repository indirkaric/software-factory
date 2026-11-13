# Software Factory — vodič korak po korak

Ovaj vodič te vodi od praznog računara do prvog PR-a koji je napravio factory.
Kod svakog koraka piše **šta radiš** i **zašto**.

---

## 0. Pregled — šta je šta

| Dio | Gdje je | Šta radi |
|---|---|---|
| **Claude Code** | na tvom računaru (komanda `claude`) | program koji pokreće agente |
| **project-template** | `~/Desktop/projects/project-template` (+ GitHub) | pravi **nove servise**: skeleton, pravila, prvi modul |
| **software-factory** | GitHub `indirkaric/software-factory` | **izvor** factoryja; sam po sebi ne radi ništa |
| **servis** (npr. `invoice`) | `~/Desktop/projects/<ime>` + svoj GitHub repo | ovdje se factory instalira i ovdje radi |
| **GitHub** | github.com | čuva kod, issue-e (zadatke) i PR-ove |

Tok rada:

```
project-template ──/new-microservice──▶ novi servis
                                          │ git + GitHub repo
                                          ▼
software-factory ──install.sh────────▶ servis + factory
                                          │ /factory <zadatak>
                                          ▼
                                   issue → kod + testovi → PR → merge
```

Tri faze:

- **Faza A** — jednom po računaru
- **Faza B** — jednom po projektu (servisu)
- **Faza C** — svakodnevni rad

---

## Faza A — priprema računara (samo jednom)

### A1. Instaliraj GitHub CLI

```bash
sudo apt install gh
```

**Zašto:** factory s GitHubom razgovara preko komande `gh`: otvara issue-e,
PR-ove, labele i merge-a. Bez nje factory ne može raditi.

### A2. Prijavi se na GitHub

```bash
gh auth login
```

Pokreni je u običnom terminalu, u bilo kojem folderu. Odgovori ovako
(strelice za izbor, Enter za potvrdu):

| Pitanje | Odgovor |
|---|---|
| What account do you want to log into? | **GitHub.com** |
| Preferred protocol for Git operations? | **HTTPS** |
| Authenticate Git with your GitHub credentials? | **Yes** |
| How would you like to authenticate? | **Login with a web browser** |

Terminal ispiše kod (npr. `ABCD-1234`). Pritisni Enter, u browseru unesi kod
i klikni **Authorize**.

**Zašto:** prijava se spremi za tvog korisnika (`~/.config/gh/`), pa i `gh`
i `git push` rade bez lozinke. Radiš je samo jednom.

Provjera:

```bash
gh auth status
```

Mora pisati `Logged in to github.com account indirkaric`.

### A3. Provjeri alate

```bash
java -version && mvn -version && python3 --version && claude --version
```

**Zašto:** servisi su Java 17 + Maven, factory skripte su Python (3.10+),
a intake agent traži Claude Code 2.1.257 ili noviji.

### A4. Skini factory

```bash
git clone https://github.com/indirkaric/software-factory.git ~/Desktop/projects/software-factory
```

**Zašto:** ovo je samo izvor iz kojeg `install.sh` kopira factory u servise.
U ovom folderu ne radiš ništa drugo. Kad kasnije ažuriraš factory, ovdje
pokreneš `git pull`.

> Ako već imaš lokalnu kopiju (npr. `~/Desktop/practice/software-facotry`),
> možeš koristiti nju. U komandama ispod samo zamijeni putanju.

---

## Faza B — novi projekat (jednom po servisu)

Primjer je servis `invoice`. Za svoj projekat zamijeni ime.

### B1. Provjeri registar servisa

Otvori:

```
~/Desktop/projects/project-template/.claude/skills/inter-service-propagation/platform.md
```

**Zašto:** to je tabela svih servisa (ime, port, adresa). Template iz nje:

- odbija ime ili port koji je već zauzet;
- uzima adrese servisa koje novi servis poziva.

Template **nikad** ne čita druge repoe, zna samo ono što piše u tabeli.

Provjeri dvije stvari:

- **Ime nije zauzeto.** Ako red s tim imenom već postoji, izaberi drugo ime
  ili obriši taj red, ako taj servis zapravo ne postoji.
- **Servisi koje ćeš zvati su upisani.** Ako servisa nema, dodaj mu red:

  ```
  | billing | billing.nsight.local | 50040 | http://billing.nsight.local:50040/billing/ | nothing | |
  ```

  Port i URI nađeš u `application.yaml` tog servisa ili u docker-compose.

### B2. Napravi servis iz templatea

```bash
cd ~/Desktop/projects/project-template && claude
```

U Claude Codeu:

```
/new-microservice invoice:persistence
```

Format je `<ime>:<uloga>[:<koga poziva>]`:

| Dio | Vrijednosti | Značenje |
|---|---|---|
| ime | `invoice` | ime servisa (kebab-case) |
| uloga | `persistence` | servis ima svoju bazu (entity, Liquibase, repository) |
| | `proxy` | nema bazu, samo kombinuje podatke iz drugih servisa |
| koga poziva | `identity` ili `identity,artifact` | servisi koje novi servis zove preko HTTP-a (moraju biti u `platform.md`) |

Primjeri:

```
/new-microservice invoice:persistence
/new-microservice invoice:persistence:identity
/new-microservice report:proxy:identity,artifact
```

**Zašto:** template deterministički kopira provjereni skeleton (security,
klijenti, exception handling, checkstyle, JaCoCo), popuni `CLAUDE.md` i
pravila, i napravi prvi modul.

**Treći dio nije "po uzoru na".** Stil koda uvijek dolazi iz skeletona, koji
je već napravljen po identity i artifact servisima. `:identity` znači da će
`invoice` **zvati** identity.

Kad završi, izađi s `/exit`. Servis je u `~/Desktop/projects/invoice`.

### B3. Napravi git repo

```bash
cd ~/Desktop/projects/invoice && git init -b main && git add -A && git commit -m "Initial invoice service"
```

**Zašto:** template pravi folder bez gita, a factory svaki zadatak radi na
posebnom git branchu i u posebnom worktreeu. `target/` (Maven build) je u
`.gitignore`, pa ne ulazi u commit.

### B4. Napravi GitHub repo i pushaj

```bash
gh repo create indirkaric/invoice --private --source . --push
```

**Zašto:** factory radi preko GitHub issue-a i PR-ova, pa servis mora imati
GitHub repo. Ova komanda sama napravi privatni repo, poveže ga kao `origin` i
pushne `main`. Na github.com ne moraš ništa ručno praviti.

### B5. Instaliraj factory u servis

```bash
~/Desktop/projects/software-factory/install.sh ~/Desktop/projects/invoice --labels --verify
```

**Šta radi:**

| Korak | Zašto |
|---|---|
| kopira `.claude/skills/factory`, 9 agenata i `factory/` | to je sam factory: orkestrator, agenti i skripte |
| postavi `base_branch` iz `origin`-a | factory zna iz kojeg brancha pravi jobove |
| doda `/factory/jobs/`, `/.worktrees/`, `!/factory/bin/` u `.gitignore` | stanje jobova i worktreeovi su lokalni; `factory/bin` mora u git, a Java `.gitignore` inače ignoriše `bin` |
| doda sekciju "Software factory" u `CLAUDE.md` | agenti znaju da factory postoji i šta smiju |
| `--labels` napravi labele `factory`, `factory:needs-info`, `factory:needs-human` | po njima factory zna koje issue-e gradi i koji čekaju tebe |
| `--verify` pokrene `mvn -o clean verify` | build mora proći na `main`, inače factory ne može proći nijedan zadatak |

Na kraju mora pisati **`checks.sh: PASS`**.

Skripta **ne commita ništa**, to radiš ti u sljedećem koraku.

### B6. Commitaj i pushaj instalaciju

```bash
git add .claude factory .gitignore CLAUDE.md && git commit -m "Install software factory" && git push
```

**Zašto:** svaki job pravi branch iz `origin/main`. Ako instalacija nije
pushana, jobovi je ne vide.

### B7. Podesi za početak

U `~/Desktop/projects/invoice/factory/config.json` stavi:

```json
"concurrency": 1,
```

Commitaj i pushaj:

```bash
git commit -am "Factory: one job at a time" && git push
```

**Zašto:** factory tada radi jedan po jedan zadatak, pa stigneš pročitati
svaki PR dok ne vidiš kako radi.

### B8. Pogledaj `PLATFORM_CHANGES.md`

```
~/Desktop/projects/invoice/PLATFORM_CHANGES.md
```

**Zašto:** tu piše šta treba promijeniti **izvan** servisa: bazu, docker-compose,
hostove i allowlist kod servisa koje zoveš. Ni factory ni template to ne rade
umjesto tebe, jer diraju druge repoe.

Projekat je sada spreman.

---

## Faza C — svakodnevni rad

### C1. Otvori Claude Code u servisu

```bash
cd ~/Desktop/projects/invoice && claude
```

**Zašto:** komanda `/factory` postoji samo u folderu gdje je factory
instaliran. U drugom folderu je Claude ne zna.

### C2. Zadaj zadatak

Opiši šta želiš:

```
/factory dodaj CRUD modul za Invoice: number (string, unique), amount (decimal), status (DRAFT/SENT/PAID)
```

Factory sam otvori issue na GitHubu i pokrene ga.

Drugi način je da issue napišeš na GitHubu, staviš mu label `factory` i
pokreneš:

```
/factory run
```

**Savjet:** budi konkretan. Navedi polja i tipove, endpoint (metoda i
putanja), pravila i ko smije zvati. Ako servis treba podatke iz drugog
servisa, napiši tačan endpoint, npr. "zovi `GET /identity/v1/users/{id}`".

### C3. Šta factory radi

```
intake       odluči da li se gradi, suzi opseg, ocijeni rizik
spec-writer  napiše spec.md: endpointi, tabele, klase po slojevima, kriteriji
builder      napiše kod i testove, commit, push, draft PR
gate         mvn verify (testovi, JaCoCo, checkstyle); ako padne, nazad builderu
revieweri    code uvijek; api / db / integration / security po potrebi
approver     merge-a ako je siguran, inače PR ostavi tebi
```

U Claudeu dobiješ jednu liniju po koraku, npr.
`001-invoice-crud #1: gate PASS → review (code, api, db)`.

### C4. Dashboard (opcionalno)

U drugom terminalu:

```bash
cd ~/Desktop/projects/invoice/factory && python3 -m http.server
```

Otvori <http://localhost:8000/dashboard.html>.

**Zašto:** vidiš svaki job kao karticu s rezultatima po rundi, cijenom i
vremenom. Klik na karticu otvara svaki fajl koji je job napisao.

### C5. Odluči šta s PR-om

| Šta vidiš | Šta znači | Šta radiš |
|---|---|---|
| PR **merged** | approver je bio siguran | ništa, gotovo je |
| label **factory:needs-human** | approver želi tvoju odluku (security, migracija, nova dependency…) | pročitaj komentar na PR-u; ako je dobro, **merge-aj na GitHubu**; ako nije: `/factory za #1: <šta da promijeni>` |
| label **factory:needs-info** | intake je nešto pitao na issue-u | odgovori u komentaru issue-a, pa `/factory run` |
| želiš odustati | | `/factory odbaci #1, razlog…` |

**Zašto:** merge PR-a je tvoje odobrenje. Feedback ide builderu kao nova
runda.

### C6. Korisne komande

```
/factory run              svi otvoreni factory issue-i; nastavlja i prekinute
/factory run 5            samo issue #5
/factory šta čeka na mene?
/factory koliko je koštao #1?
/factory zašto je #1 eskaliran?
```

Ako se sesija prekine, samo ponovo pokreni `/factory run`. Stanje je u
fajlovima, pa factory nastavlja gdje je stao.

### Šta factory NEĆE raditi

- praviti nove mikroservise (to radi template, faza B);
- dirati druge repoe;
- pokretati `docker` ili deployati;
- pushati išta osim `factory/*` brancheva.

---

## Podešavanja — `factory/config.json`

| Ključ | Default | Značenje |
|---|---|---|
| `concurrency` | 2 | koliko zadataka radi paralelno (počni s 1) |
| `max_rounds` | 3 | koliko puta builder smije popravljati prije nego pita tebe |
| `merge_method` | `squash` | način merge-a; mora biti dozvoljen u postavkama repoa |
| `routing` | low → haiku, medium/high → sonnet | koji model radi code review po riziku |
| `surface_reviewers` | api/db/integration → sonnet, security → opus | dodatni revieweri |
| `protected_paths` | factory fajlovi + checkstyle | gate pada ako ih branch izmijeni |

---

## Česti problemi

| Problem | Uzrok | Rješenje |
|---|---|---|
| `/factory` ne postoji | Claude je otvoren u pogrešnom folderu | `cd` u folder servisa, pa `claude` |
| `Name already taken` | ime je već u `platform.md` | drugo ime ili obriši red u tabeli |
| `gh: command not found` / not logged in | faza A nije završena | A1 i A2 |
| `install.sh` javlja da fajlovi već postoje | factory je već instaliran | `--force` za prepisivanje (ažuriranje) |
| `checks.sh` pada s "offline" | nova dependency nije u `~/.m2` | pokreni Claude s `FACTORY_MVN_ONLINE=1 claude` |
| job ne vidi instalaciju | instalacija nije pushana | B6 |
| PR čeka a ne merge-a se | approver je eskalirao | pročitaj komentar na PR-u (C5) |

---

## Ažuriranje factoryja u postojećem servisu

```bash
cd ~/Desktop/projects/software-factory && git pull
```

```bash
~/Desktop/projects/software-factory/install.sh ~/Desktop/projects/invoice --force --verify
```

```bash
cd ~/Desktop/projects/invoice && git add .claude factory && git commit -m "Update software factory" && git push
```

`--force` prepisuje i `factory/config.json`, pa svoje izmjene (npr.
`concurrency`) ponovo podesi prije commita.

---

## Checklista za novi projekat

- [ ] A — `gh auth status` pokazuje da si prijavljen
- [ ] B1 — ime slobodno u `platform.md`
- [ ] B2 — `/new-microservice <ime>:<uloga>[:<poziva>]`
- [ ] B3 — `git init` i prvi commit
- [ ] B4 — `gh repo create … --private --source . --push`
- [ ] B5 — `install.sh … --labels --verify` → `checks.sh: PASS`
- [ ] B6 — commit i push instalacije
- [ ] B7 — `concurrency: 1`
- [ ] B8 — pročitan `PLATFORM_CHANGES.md`
- [ ] C — `claude` u servisu → `/factory <prvi zadatak>`
