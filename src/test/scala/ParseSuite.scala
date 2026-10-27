package com.alecdorrington.cadmus

import com.alecdorrington.cadmus.Scaladoc.Tag
import munit.FunSuite

class ParseSuite extends FunSuite:

  test("a comment of one line is its description"):
    assertEquals(
      Scaladoc.parse("/** Warm. */"),
      Scaladoc(Some("Warm.")),
    )

  test("a description is kept as written, without its margin"):
    val comment = """/**
      * A review
      *   of writing.
      *
      * Kept short.
      */"""
    assertEquals(
      Scaladoc.parse(comment).description,
      Some("A review\n  of writing.\n\nKept short."),
    )

  test("a description begun on the comment's first line is kept whole"):
    val comment = """/** A review
      * of writing.
      */"""
    assertEquals(
      Scaladoc.parse(comment).description,
      Some("A review\nof writing."),
    )

  test("links and code examples are kept, the examples' indentation too"):
    val comment = """/**
      * Sent by [[Client.ask]].
      *
      * {{{
      * client
      *   .ask(prompt)
      * }}}
      */"""
    assertEquals(
      Scaladoc.parse(comment).description,
      Some("Sent by [[Client.ask]].\n\n{{{\nclient\n  .ask(prompt)\n}}}"),
    )

  test("parameters, type parameters and the result are read from their tags"):
    val comment = """/**
      * A person.
      *
      * @tparam A
      *   The type of their pet.
      *
      * @param name
      *   Their name,
      *   in full.
      *
      * @param age Their age.
      * @return
      *   A greeting.
      */"""
    assertEquals(
      Scaladoc.parse(comment),
      Scaladoc(
        Some("A person."),
        Map(
          "name" -> "Their name,\nin full.",
          "age"  -> "Their age.",
        ),
        Map("A" -> "The type of their pet."),
        Some("A greeting."),
      ),
    )

  test(
    "a tag's text keeps the indentation of its lines beyond what they share",
  ):
    val comment = """/**
      * @param query
      *   Written as:
      *   {{{
      *   select
      *     .all
      *   }}}
      */"""
    assertEquals(
      Scaladoc.parse(comment).params,
      Map("query" -> "Written as:\n{{{\nselect\n  .all\n}}}"),
    )

  test("every other tag is kept, in the order written"):
    val comment = """/**
      * Divides.
      *
      * @throws ArithmeticException
      *   If the divisor is zero.
      * @see [[Multiplier]]
      * @deprecated
      * @since 0.1.0
      */"""
    assertEquals(
      Scaladoc.parse(comment).tags,
      List(
        Tag(
          "throws",
          "ArithmeticException\nIf the divisor is zero.",
        ),
        Tag("see", "[[Multiplier]]"),
        Tag("deprecated", ""),
        Tag("since", "0.1.0"),
      ),
    )

  test("a tag repeating a part, or saying nothing of it, is kept with others"):
    val comment = """/**
      * @param name Their name.
      * @param name Their name again.
      * @param age
      * @return A greeting.
      * @return
      */"""
    assertEquals(
      Scaladoc.parse(comment),
      Scaladoc(
        params = Map("name" -> "Their name."),
        returns = Some("A greeting."),
        tags = List(
          Tag("param", "name Their name again."),
          Tag("param", "age"),
          Tag("return", ""),
        ),
      ),
    )

  test("a line within a code example opens no tag"):
    val comment = """/**
      * Run as:
      *
      * {{{
      * @main def run() = ()
      * }}}
      *
      * ```scala
      * @tailrec def loop() = loop()
      * ```
      *
      * @param args The arguments.
      */"""
    assertEquals(
      Scaladoc.parse(comment),
      Scaladoc(
        Some(
          "Run as:\n\n{{{\n@main def run() = ()\n}}}\n\n" +
            "```scala\n@tailrec def loop() = loop()\n```",
        ),
        Map("args" -> "The arguments."),
      ),
    )

  test("a tag is opened only by @ and a name beginning a line"):
    val comment = """/**
      * Mail me @ home.
      * @return.
      */"""
    assertEquals(
      Scaladoc.parse(comment),
      Scaladoc(Some("Mail me @ home.\n@return.")),
    )

  test("a comment of tags alone has no description"):
    assertEquals(
      Scaladoc.parse("/** @param name Their name. */").description,
      None,
    )

  test("a comment's text is read without delimiters or margin"):
    assertEquals(
      Scaladoc.parse("A person.\n\n@param name Their name."),
      Scaladoc(
        Some("A person."),
        Map("name" -> "Their name."),
      ),
    )

  test("an empty comment has no parts"):
    assertEquals(Scaladoc.parse("/** */"), Scaladoc())
