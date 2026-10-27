package com.alecdorrington.cadmus

import com.alecdorrington.cadmus.Scaladoc.Tag
import munit.FunSuite

class PlainSuite extends FunSuite:

  test("a paragraph's lines are joined, and paragraphs kept apart"):
    assertEquals(
      Scaladoc.plain("A review\n  of writing.\n\n\nKept short."),
      "A review of writing.\n\nKept short.",
    )

  test("links are written as their labels, or else their targets"):
    assertEquals(
      Scaladoc.plain(
        "Sent by [[Client.ask]] and [[Part.json the json\npart]], as " +
          "[[https://json-schema.org]] says.",
      ),
      "Sent by Client.ask and the json part, as https://json-schema.org says.",
    )

  test("code examples are left out"):
    assertEquals(
      Scaladoc.plain(
        "Ask.\n\n{{{\nclient.ask(prompt)\n}}}\n\n" +
          "```scala\nclient.send(chat)\n```\n\nThen wait.",
      ),
      "Ask.\n\nThen wait.",
    )

  test("other markup is kept"):
    assertEquals(
      Scaladoc.plain("Never `null`, and '''never''' empty."),
      "Never `null`, and '''never''' empty.",
    )

  test("a text of nothing but code examples is empty"):
    assertEquals(Scaladoc.plain("{{{\nrun()\n}}}"), "")

  test("a comment is rendered part by part, leaving out what is left empty"):
    val comment = Scaladoc(
      Some("{{{\nrun()\n}}}"),
      Map(
        "name" -> "Their [[Name name]].",
        "age"  -> "{{{ 42 }}}",
      ),
      Map("A" -> "The type\nof pet."),
      Some("A [[Greeting]]."),
      List(
        Tag("see", "[[Other]]"),
        Tag("example", "{{{ run() }}}"),
      ),
    )
    assertEquals(
      comment.plain,
      Scaladoc(
        None,
        Map("name" -> "Their name."),
        Map("A"    -> "The type of pet."),
        Some("A Greeting."),
        List(Tag("see", "Other"), Tag("example", "")),
      ),
    )
