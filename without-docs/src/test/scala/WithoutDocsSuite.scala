// Compiled with `-Xdrop-docs`, outside Cadmus's package, as a user's code is.
package com.alecdorrington.example

import com.alecdorrington.cadmus.Scaladoc
import munit.FunSuite

/** A note, documented, though no comment is kept. */
final case class Note(text: String):

  /** Its text, shouted. */
  def shouted: String = text.toUpperCase

/** A colour. */
enum Colour:

  /** Warm. */
  case Red

class WithoutDocsSuite extends FunSuite:

  test("a type's or member's comment is none where Scala keeps none"):
    assertEquals(Scaladoc.of[Note], None)
    assertEquals(Scaladoc.rawOf[Colour], None)
    assertEquals(Scaladoc.of[Colour.Red.type], None)
    assertEquals(
      Scaladoc.ofMember[Note]("shouted"),
      None,
    )
