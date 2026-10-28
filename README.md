<div align="center">

  <h1>📜 Cadmus</h1>
  <p>Reads Scaladoc comments at compile time.</p>
  
  <span>
    <a href="https://github.com/SgtSwagrid/cadmus/actions/workflows/build-integrity.yml"><img src="https://github.com/SgtSwagrid/cadmus/actions/workflows/build-integrity.yml/badge.svg" alt="Build status" /></a>
    <a href="https://search.maven.org/artifact/com.alecdorrington/cadmus_3"><img src="https://img.shields.io/maven-central/v/com.alecdorrington/cadmus_3.svg" alt="Maven Central" /></a>
    <a href="https://alecdorrington.com/cadmus"><img src="https://img.shields.io/badge/docs-latest-blue.svg" alt="Documentation" /></a>
  </span>
  
</div>

> [!WARNING]
> Cadmus is still a prototype. Anything could be broken or change between minor versions.

Macros reading the Scaladoc comment of a type, or of one of its members, into its parts: the description, what
each `@param`, `@tparam` and `@return` tag says, and every other tag. Each text is kept as written, markup and all,
and renders as plain text on request. A library can so describe a user's types from their own comments, as
[Iris](https://github.com/SgtSwagrid/iris) describes the JSON Schema it asks a model for.

Named for [Cadmus](https://en.wikipedia.org/wiki/Cadmus), who brought the letters of the alphabet to Greece.

## ⬇️ Installation

The following applies for [Scala 3](https://www.scala-lang.org/) projects built with [sbt 2](https://www.scala-sbt.org/).
Add the library to your `build.sbt` file, with the desired version number (e.g. [latest](../../releases/latest)):

```scala
libraryDependencies += "com.alecdorrington" %% "cadmus" % "{version}"
```

## 🚀 Usage

`Scaladoc.of[A]` reads the comment of a type, as a [`Scaladoc`](src/main/scala/Scaladoc.scala), or `None` where
there is none:

```scala
import com.alecdorrington.cadmus.Scaladoc

/**
  * A person, as [[Registry]] keeps them.
  *
  * @param name
  *   Their name,
  *   in full.
  * @see [[Pet]]
  */
case class Person(name: String, age: Int)

Scaladoc.of[Person]
// Some(Scaladoc(
//   description = Some("A person, as [[Registry]] keeps them."),
//   params      = Map("name" -> "Their name,\nin full."),
//   typeParams  = Map(),
//   returns     = None,
//   tags        = List(Tag("see", "[[Pet]]")),
// ))
```

A class, trait, object, type alias or opaque type is read from its own comment, and a singleton type, as a case
object's or an enum's case of no fields, from its term's, as `Scaladoc.of[Mood.Pleased.type]`. A generic type is
read alike whatever its arguments. `Scaladoc.ofMember[Person]("greet")` reads the comment of a field or method; its
name must be a literal, naming one field or a method that is not overloaded, or the call does not compile.

Each text is kept as written: only the comment's delimiters, each line's margin and the indentation a part's lines
share are taken away, so links, code examples and line breaks remain. `plain` renders it as plain text, leaving out
code examples, writing links as their labels (or else their targets) and each paragraph on a line of its own,
apart from the next by a blank line:

```scala
Scaladoc.of[Person].map(_.plain).flatMap(_.description) // Some("A person, as Registry keeps them.")
Scaladoc.plain("Their name,\nin [[Name full]].")         // "Their name, in full."
```

A tag begins a line, outside any code example, and runs to the next. The first `@param` or `@tparam` naming each
parameter, and the first `@return`, are read into a part of their own; every other tag is kept in `tags`, so nothing
is lost.

`Scaladoc.rawOf[A]` and `Scaladoc.rawOfMember[A](name)` give the comment as it is written, delimiters and all, and
`Scaladoc.parse` reads any comment, as a macro of your own may read a symbol's with
`symbol.docstring.map(Scaladoc.parse)`.

## 🔎 What Scala keeps

- Scala keeps the comment of a type it compiles from source in the same run as the call, with no compiler option.
  A call in the type's own file, as in a `derives` clause or a given in its companion, always sees it.
- A type compiled before, as one from a library or from another file that an incremental build did not recompile,
  has no comment, unless you compile with `-Xread-docs`, which reads it from the type's TASTy. Its library must have
  been compiled with Scala 3. A Java class has none.
- Compiled with `-Xdrop-docs`, Scala keeps no comments at all, and every type and member has none.
- Scala keeps no comment written on a class's parameter, so describe a field with its class's `@param` tag, which
  `params` gives by name.
- Scaladoc's own processing is not done: `@define` variables are not expanded, nor `@inheritdoc` inherited.

## 🤝 Contributing

Issues and pull requests are very welcome; for anything large, please open an issue first.

## 👁️ See also

- [Iris](https://github.com/SgtSwagrid/iris), a sibling, which describes the types a model answers in by their Scaladoc.
- This library was made using [Scala Library Template](https://github.com/SgtSwagrid/scala-library-template).
