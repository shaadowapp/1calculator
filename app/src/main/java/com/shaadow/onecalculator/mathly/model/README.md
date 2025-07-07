# Mathly Model (Rule-Based)

This folder contains the rule-based model logic for Mathly's voice assistant.

- All normalization, command detection, and math parsing is done with hand-written rules (see RuleBasedMathlyModel.kt).
- No ML/AI model is used; everything is offline and deterministic.
- Supports basic arithmetic, advanced operators (factorial, root, power, modulo), and in-app commands (open history/calculator/settings, clear). 