package com.alecdorrington.cadmus

import scala.quoted.*
import scala.util.control.NonFatal
import scala.util.matching.Regex

/**
  * A Scaladoc comment, read into its parts: the description, the tags naming a
  * parameter, type parameter or result, and every other tag. Each part's text
  * is kept as written, markup and all, with only the comment's delimiters, the
  * margin of each line and the indentation its lines share taken away. Every
  * text is non-blank, but for that of a tag in [[tags]]. [[plain]] renders them
  * as plain text.
  *
  * [[Scaladoc.of]] reads the comment of a type, and [[Scaladoc.ofMember]] that
  * of one of its members, at compile time:
  *
  * {{{
  * Scaladoc.of[Person].flatMap(_.params.get("name"))
  * }}}
  *
  * @param description
  *   The description, before the first tag, if there is one.
  *
  * @param params
  *   What each `@param` tag says of a parameter, by the parameter's name.
  *
  * @param typeParams
  *   What each `@tparam` tag says of a type parameter, by its name.
  *
  * @param returns
  *   What the `@return` tag says of the result, if there is one.
  *
  * @param tags
  *   Every other tag, in the order written, including any repeating a
  *   parameter's or the result's, and any saying nothing of one.
  */
final case class Scaladoc
  (
    description: Option[String] = None,
    params: Map[String, String] = Map.empty,
    typeParams: Map[String, String] = Map.empty,
    returns: Option[String] = None,
    tags: List[Scaladoc.Tag] = Nil,
  ):

  /**
    * Renders each part of this comment as plain text, by [[Scaladoc.plain]],
    * leaving out any but a tag of which nothing is left.
    *
    * @return
    *   A comment whose texts are plain.
    */
  def plain: Scaladoc = Scaladoc(
    description.flatMap(Scaladoc.rendered),
    Scaladoc.rendered(params),
    Scaladoc.rendered(typeParams),
    returns.flatMap(Scaladoc.rendered),
    tags.map(tag => tag.copy(text = Scaladoc.plain(tag.text))),
  )

object Scaladoc:

  /**
    * A tag of a comment, as `@see` or `@throws`.
    *
    * @param name
    *   The tag's name, without its `@`, as `throws`.
    *
    * @param text
    *   The text after the name, which may be empty.
    */
  final case class Tag(name: String, text: String)

  /**
    * Reads the comment of a type: a class, trait, object, type alias or opaque
    * type by its own, and a singleton type, as a case object's or an enum's
    * case of no fields, by that of its term.
    *
    * Scala keeps the comment of a type it compiles from source in the same run
    * as the call, as it always does where the call is in the type's own file
    * (in a `derives` clause, or a given in its companion). That of a type
    * compiled before, as one from a library or from a file not recompiled
    * since, it reads from the type's TASTy only with the compiler option
    * `-Xread-docs`. A Java class has none, and nothing has one under
    * `-Xdrop-docs`.
    *
    * @tparam A
    *   The type.
    *
    * @return
    *   The type's comment, if Scala keeps one.
    */
  inline def of[A]: Option[Scaladoc] = rawOf[A].map(parse)

  /**
    * Reads the comment of a field or method of a type, as [[of]] reads the
    * type's. Scala keeps no comment written on a class's parameter, so a field
    * of one has none; its class's `@param` tag describes it instead.
    *
    * @tparam A
    *   The type.
    *
    * @param name
    *   The member's name, which must be a literal, and name one field or a
    *   method that is not overloaded.
    *
    * @return
    *   The member's comment, if Scala keeps one.
    */
  inline def ofMember[A](inline name: String): Option[Scaladoc] =
    rawOfMember[A](name).map(parse)

  /**
    * Reads the comment of a type as it is written, delimiters and all, where
    * [[of]] would read it.
    *
    * @tparam A
    *   The type.
    *
    * @return
    *   The type's comment as written, if Scala keeps one.
    */
  inline def rawOf[A]: Option[String] = ${ commentOf[A] }

  /**
    * Reads the comment of a field or method of a type as it is written,
    * delimiters and all, where [[ofMember]] would read it.
    *
    * @tparam A
    *   The type.
    *
    * @param name
    *   The member's name, which must be a literal, and name one field or a
    *   method that is not overloaded.
    *
    * @return
    *   The member's comment as written, if Scala keeps one.
    */
  inline def rawOfMember[A](inline name: String): Option[String] =
    ${ commentOfMember[A]('name) }

  /**
    * Reads a comment into its parts. A tag starts a line outside any code
    * example, and runs to the next. A macro of one's own may read a symbol's
    * comment this way, as `symbol.docstring.map(Scaladoc.parse)`, though
    * `docstring` throws rather than give none under `-Xdrop-docs`.
    *
    * @param comment
    *   The comment, with or without its delimiters and margin.
    *
    * @return
    *   The comment read into its parts.
    */
  def parse(comment: String): Scaladoc =
    val (body, tags) = split(unmargined(comment))
    tags.foldLeft(Scaladoc(described(body)))(adding)

  /**
    * Renders Scaladoc markup as plain text: code examples left out, links
    * written as their labels, or else their targets, and each paragraph on a
    * line of its own, apart from the next by a blank line.
    *
    * @param markup
    *   The text, as a part of a comment is written.
    *
    * @return
    *   The text in plain words, which is empty where none are left.
    */
  def plain(markup: String): String =
    paragraphs(linked(example.replaceAllIn(markup, "")))

  /** The kind of code example a line opens, which only one of a kind closes. */
  private enum Fence:
    case Wiki, Markdown

  private val margin: Regex   = """^\s*\* ?""".r
  private val trailing: Regex = """\s+$""".r
  private val tag: Regex      = """@([A-Za-z]\w*)(?:\s+(.*))?""".r
  private val example: Regex  = """(?s)\{\{\{.*?}}}|```.*?```""".r
  private val link: Regex     = """\[\[([^\]\s]+)(?:\s+([^\]]+))?]]""".r

  private def commentOf[A : Type](using Quotes): Expr[Option[String]] =
    Expr(commented(subject[A]))

  /** The symbol a type is documented on: a singleton's term, else its own. */
  private def subject[A : Type](using Quotes): quotes.reflect.Symbol =
    import quotes.reflect.*
    TypeRepr.of[A] match
      case singleton: TermRef => singleton.termSymbol
      case other              => other.typeSymbol

  private def commentOfMember[A : Type]
    (name: Expr[String])
    (using Quotes)
    : Expr[Option[String]] = Expr(commented(member[A](name.valueOrAbort)))

  /** An object's comment may be on its term or on its class. */
  private def commented
    (using Quotes)
    (symbol: quotes.reflect.Symbol)
    : Option[String] = docstring(symbol).orElse(docstring(symbol.moduleClass))

  /**
    * A symbol's comment. Under `-Xdrop-docs`, Scala keeps no comments, and
    * asking for one throws rather than giving none.
    */
  private def docstring
    (using Quotes)
    (symbol: quotes.reflect.Symbol)
    : Option[String] =
    try symbol.docstring
    catch case NonFatal(_) => None

  private def member[A : Type]
    (using Quotes)
    (name: String)
    : quotes.reflect.Symbol =
    import quotes.reflect.*
    val owner = TypeRepr.of[A].typeSymbol
    (owner.fieldMember(name) :: owner.methodMember(name))
      .filter(_.exists)
      .distinct match
        case List(found) => found
        case Nil         => report.errorAndAbort(
            s"${ Type.show[A] } has no field or method $name.",
          )
        case _ => report.errorAndAbort(
            s"${ Type.show[A] }'s method $name is overloaded, so which of its" +
              " comments is meant is unclear.",
          )

  /** The comment's lines, without its delimiters or each line's margin. */
  private def unmargined(comment: String): List[String] =
    comment.trim.stripPrefix("/**").stripSuffix("*/").linesIterator.toList match
      case first :: rest => first.trim :: rest.map(line =>
          trailing.replaceFirstIn(margin.replaceFirstIn(line, ""), ""),
        )
      case Nil => Nil

  /** The lines before the first tag, and each tag of the lines after. */
  private def split(lines: List[String]): (List[String], List[Tag]) = lines
    .zip(lines.scanLeft(Option.empty[Fence])(fenced))
    .foldRight((List.empty[String], List.empty[Tag])):
      case ((line, fence), (after, tags)) =>
        opening(line, fence).fold((line :: after, tags))((name, said) =>
          (Nil, Tag(name, joined(said :: dedented(after))) :: tags),
        )

  /** The text of the lines before the first tag, if they say anything. */
  private def described(lines: List[String]): Option[String] =
    Some(joined(dedented(lines))).filter(_.nonEmpty)

  /** The name and first words of the tag a line opens, if it opens one. */
  private def opening
    (line: String, fence: Option[Fence])
    : Option[(String, String)] = Option
    .when(fence.isEmpty)(line.trim)
    .collect:
      case tag(name, said) => (name, Option(said).getOrElse(""))

  /** The code example open after a line, given the one open before it. */
  private def fenced(fence: Option[Fence], line: String): Option[Fence] =
    fence match
      case None if line.trim.startsWith("```") => Some(Fence.Markdown)
      case None if line.lastIndexOf("{{{") > line.lastIndexOf("}}}") =>
        Some(Fence.Wiki)
      case Some(Fence.Markdown) if line.trim.startsWith("```") => None
      case Some(Fence.Wiki) if line.contains("}}}")            => None
      case other                                               => other

  /** A comment with a tag added: to its own part, else to the other tags. */
  private def adding(doc: Scaladoc, tag: Tag): Scaladoc = tag match
    case Tag("param", Named(name, said)) if !doc.params.contains(name) =>
      doc.copy(params = doc.params.updated(name, said))
    case Tag("tparam", Named(name, said)) if !doc.typeParams.contains(name) =>
      doc.copy(typeParams = doc.typeParams.updated(name, said))
    case Tag("return", said) if said.nonEmpty && doc.returns.isEmpty =>
      doc.copy(returns = Some(said))
    case _ => doc.copy(tags = doc.tags :+ tag)

  /** A tag's text as the name it begins with, and what it says of it. */
  private object Named:

    def unapply(text: String): Option[(String, String)] =
      val name = text.takeWhile(!_.isWhitespace)
      val said = text.drop(name.length).dropWhile(_.isWhitespace)
      Option.when(name.nonEmpty && said.nonEmpty)((name, said))

  /** The lines without the indentation of the least indented, if not blank. */
  private def dedented(lines: List[String]): List[String] =
    val indent = lines
      .filterNot(blank)
      .map(_.takeWhile(_.isWhitespace).length)
      .minOption
      .getOrElse(0)
    lines.map(_.drop(indent))

  /** The lines as one text, without the blank lines around them. */
  private def joined(lines: List[String]): String = lines
    .dropWhile(blank)
    .reverse
    .dropWhile(blank)
    .reverse
    .mkString("\n")

  private def blank(line: String): Boolean = line.forall(_.isWhitespace)

  /** The text with each `[[link]]` written as its label, or else its target. */
  private def linked(text: String): String = link.replaceAllIn(
    text,
    found =>
      Regex.quoteReplacement(Option(found.group(2)).getOrElse(found.group(1))),
  )

  /** Each paragraph on a line of its own, apart from the next by a blank one. */
  private def paragraphs(text: String): String = text
    .split("\\n\\s*\\n")
    .map(_.split("\\s+").filter(_.nonEmpty).mkString(" "))
    .filter(_.nonEmpty)
    .mkString("\n\n")

  private def rendered(text: String): Option[String] =
    Some(plain(text)).filter(_.nonEmpty)

  private def rendered(texts: Map[String, String]): Map[String, String] = texts
    .flatMap((name, text) => rendered(text).map(name -> _))
