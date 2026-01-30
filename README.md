# Genetic Image Evolution

A Kotlin implementation of evolutionary image generation using genetic algorithms<sup>1</sup> to evolve geometric shapes
that approximate a target image.

## Overview

This project uses genetic programming to evolve simple geometric shapes (circles, rectangles, polygons, curves) 
that gradually approximate a target image through natural selection. Each "individual" consists of a genome of 
coloured shapes, and evolution occurs through mutation and fitness-based selection.

## Inspiration

This implementation is inspired by:
- [Roger Johansson's "Genetic Programming: Evolution of Mona Lisa"](
  https://rogerjohansson.blog/2008/12/07/genetic-programming-evolution-of-mona-lisa/)
- [NP Contemplation's "Clojure genetic Mona Lisa problem"](
  https://npcontemplation.blogspot.com/2009/01/clojure-genetic-mona-lisa-problem-in.html)

## How It Works

1. **Initialization**: Start with a random collection of zero or more geometric shapes
2. **Mutation**: Randomly modify shape properties (position, colour, size) or add new shapes
3. **Fitness Evaluation**: Compare the rendered image against the target using pixel-by-pixel RGB distance
4. **Selection**: Keep mutations that improve fitness (lower pixel distance)
5. **Iteration**: Repeat the process continuously to evolve better approximations

## Configuration

Evolution parameters can be configured via `evolver-settings.json`:
- `maxGenomeSize` - Maximum number of shapes per individual
- `newShapeProbabilityFactor` - Likelihood of adding new shapes
- `avgShapesToMutate` - Average number of shapes to mutate per generation

## Architecture

The system shows a GUI if a DISPLAY exists but not if run headless. Shapes can be automatically uploaded to the
(appallingly named) dudestore web service.

---

## Footnotes

<sup>1</sup> Note that I was later informed that this isn't a true genetic algorithm and is, in fact, a stochastic hill
climbing algorithm. I confess I'm no expert on either and continue to call this project "genetic", even if it's
inaccurate.