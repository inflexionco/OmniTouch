Create a git commit for the current staged/unstaged changes in the OmniTouch project.

Rules:
- Run `git status` and `git diff` first to understand what changed
- Write an imperative-mood subject line (e.g. "fix: ...", "feat: ...", "refactor: ...")
- Body should explain the *why*, not just the *what*
- Do NOT include `🤖 Generated with [Claude Code]` or `Co-Authored-By: Claude <noreply@anthropic.com>`
- ALWAYS end the commit message with:

Co-Authored-By: Indrajeetsinh Chauhan <indrajeetsinhchauhan@outlook.com>

Use a HEREDOC to pass the message to `git commit -m` so formatting is preserved.
