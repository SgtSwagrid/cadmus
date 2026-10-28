// Outside Cadmus's package, so that the macros expand as in a user's code.
package com.alecdorrington.example

import com.alecdorrington.cadmus.Scaladoc
import munit.FunSuite

/**
  * A person.
  *
  * @param name
  *   Their name.
  */
final case class Person
  (
    /** Never kept. */
    name: String,
    age: Int,
  ):

  /** Greets them. */
  def greet: String = s"Hello, $name."

  /** Their age next year. */
  val older: Int = age + 1

/**
  * A box.
  *
  * @tparam A
  *   The type of what it holds.
  */
final case class Box[A](value: A)

/** How a reader may feel. */
enum Mood:

  /** Glad to have read it. */
  case Pleased

  case Indifferent

  /** Angry, out of ten. */
  case Angry(strength: Int)

/** What should become of a piece. */
sealed trait Verdict

object Verdict:

  /** Published as it is. */
  case object Accept extends Verdict

  /** Sent back for changes. */
  final case class Revise(changes: List[String]) extends Verdict

/** Settings shared by everyone. */
object Settings:

  /** How long to wait, in seconds. */
  val timeout: Int = 30

  /** Greets. */
  def hello(): String = "Hello."

  /** Greets someone. */
  def hello(name: String): String = s"Hello, $name."

object Units:

  /** A distance in metres. */
  opaque type Metres = Double

  /** A person's name. */
  type Name = String

final case class Undocumented(value: Int)

class MacroSuite extends FunSuite:

  test("a type is read from its comment"):
    assertEquals(
      Scaladoc.of[Person],
      Some(Scaladoc(
        Some("A person."),
        Map("name" -> "Their name."),
      )),
    )

  test("a type's or member's comment is given as written"):
    val raw = Scaladoc.rawOf[Person]
    assert(raw.exists(_.startsWith("/**")), raw)
    assert(raw.exists(_.endsWith("*/")), raw)
    assertEquals(
      raw.map(Scaladoc.parse),
      Scaladoc.of[Person],
    )
    assertEquals(
      Scaladoc.rawOfMember[Person]("greet"),
      Some("/** Greets them. */"),
    )

  test("a generic type is read from its comment, whatever its arguments"):
    assertEquals(
      Scaladoc.of[Box[Int]],
      Some(Scaladoc(
        Some("A box."),
        typeParams = Map("A" -> "The type of what it holds."),
      )),
    )

  test("an enum and each of its cases are read from their own comments"):
    assertEquals(
      List(
        Scaladoc.of[Mood],
        Scaladoc.of[Mood.Pleased.type],
        Scaladoc.of[Mood.Indifferent.type],
        Scaladoc.of[Mood.Angry],
      ).map(_.flatMap(_.description)),
      List(
        Some("How a reader may feel."),
        Some("Glad to have read it."),
        None,
        Some("Angry, out of ten."),
      ),
    )

  test("a sealed trait and each of its cases are read from their own comments"):
    assertEquals(
      List(
        Scaladoc.of[Verdict],
        Scaladoc.of[Verdict.Accept.type],
        Scaladoc.of[Verdict.Revise],
      ).map(_.flatMap(_.description)),
      List(
        Some("What should become of a piece."),
        Some("Published as it is."),
        Some("Sent back for changes."),
      ),
    )

  test("an object, a type alias and an opaque type are read from comments"):
    assertEquals(
      List(
        Scaladoc.of[Settings.type],
        Scaladoc.of[Units.Name],
        Scaladoc.of[Units.Metres],
      ).map(_.flatMap(_.description)),
      List(
        Some("Settings shared by everyone."),
        Some("A person's name."),
        Some("A distance in metres."),
      ),
    )

  test("a type with no comment, or compiled before, has none"):
    assertEquals(
      List(
        Scaladoc.of[Undocumented],
        Scaladoc.of[String],
        Scaladoc.of[Scaladoc],
      ),
      List(None, None, None),
    )

  test(
    "a field or method is read from its comment, a class's parameter from none",
  ):
    assertEquals(
      List(
        Scaladoc.ofMember[Person]("greet"),
        Scaladoc.ofMember[Person]("older"),
        Scaladoc.ofMember[Settings.type]("timeout"),
        Scaladoc.ofMember[Person]("name"),
      ).map(_.flatMap(_.description)),
      List(
        Some("Greets them."),
        Some("Their age next year."),
        Some("How long to wait, in seconds."),
        None,
      ),
    )

  test("a member that is missing, overloaded or named at run time is refused"):
    assert(compileErrors("""Scaladoc.ofMember[Person]("height")""").contains(
      "has no field or method height",
    ))
    assert(
      compileErrors("""Scaladoc.ofMember[Settings.type]("hello")""").contains(
        "is overloaded",
      ),
    )
    assert(
      compileErrors("""Scaladoc.ofMember[Person]("greet".trim)""").nonEmpty,
    )
