#!/usr/bin/env python3
"""Compatibility entrypoint for VillAIgence 0.3.x release convergence validation."""
from release_convergence_runtime import *  # noqa: F401,F403
from release_convergence_runtime import main


if __name__ == "__main__":
    raise SystemExit(main())
