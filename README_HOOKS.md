# Git Hooks for this repository

This repository includes a pre-push hook under `scripts/git-hooks/pre-push` that runs `./gradlew build` locally and aborts the push if the build fails.

By default this is NOT installed in your `.git/hooks`; you can install it with:

```bash
./scripts/install-hooks.sh
```

Notes:

- You can bypass the hook if necessary using: `git push --no-verify`.
- CI also runs `./gradlew build`, so you should ensure your local build passes before pushing.
- You can customize the hook if you want it to run narrower checks (e.g., `./gradlew :sample:test`) to speed up local checks.
