# GitHub token permissions · Permisije GitHub tokena

The factory uses one token: the one `gh` is logged in with (`software-factory setup` keeps it
or takes a new one). New repos are created under that token's account, and every pull,
checkout, push and merge runs with it.

Factory koristi jedan token: onaj s kojim je `gh` prijavljen (`software-factory setup` ga
zadrži ili uzme novi). Novi repoi se prave pod računom tog tokena, i svaki pull, checkout,
push i merge ide s njim.

## Fine-grained token (recommended · preporučeno)

https://github.com/settings/personal-access-tokens → **Generate new token**

- **Resource owner**: your account · tvoj račun
- **Expiration**: 90 days or more · 90 dana ili više (the factory warns 14 days before)
- **Repository access**: **All repositories** (needed so the token can create repos and work
  in them · potrebno da token može praviti nove repoe i raditi u njima)

**Repository permissions** · **Permisije za repozitorije**:

| Permission | Access | Why · Zašto |
|---|---|---|
| Administration | Read and write | create repos, turn on auto-merge, squash, delete branch on merge · pravljenje repoa i podešavanje merge opcija |
| Contents | Read and write | clone, pull, checkout, push branches, merge · kloniranje, push, merge |
| Pull requests | Read and write | open draft PRs, mark ready, comment, merge · PR-ovi |
| Issues | Read and write | read issues, labels `factory*`, comments · tiketi, labele, komentari |
| Workflows | Read and write | push `.github/workflows/ci.yml` in new services · push CI fajla |
| Actions | Read | read CI runs before merging · čitanje statusa CI-a prije merge-a |
| Commit statuses | Read | CI status on the PR · status CI-a na PR-u |
| Metadata | Read | mandatory · obavezno |

No account permissions are needed · Account permisije nisu potrebne.

## Classic token (alternative · alternativa)

https://github.com/settings/tokens → scopes **`repo`** and **`workflow`**
(`delete_repo` is not needed · nije potreban).

## Check · Provjera

```bash
software-factory doctor ~/Desktop/projects/<service>
```

It prints the token's user, kind and expiry, and probes issues, PRs, Actions and
Administration on that repo without changing anything. Ispiše korisnika, vrstu i istek
tokena i provjeri permisije na tom repou bez ikakvih izmjena.

## Branch protection

Private repos on a free account have no branch protection, so GitHub's own auto-merge has
nothing to wait for. The factory therefore waits for the `ci` workflow itself (`job merge`,
up to `ci_wait_minutes`) and merges only when it is green. With GitHub Pro you may also
protect `main` and require the `build` check.
