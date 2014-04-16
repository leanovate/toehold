/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.play.fastcgi

import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers

import play.api.test._
import play.api.test.Helpers._
import java.io.{PrintWriter, File}
import scala.util.Try

class FileAssetsControllerSpec extends Specification with ShouldMatchers {
  val documentRoot = new File("./target/tmp")

  documentRoot.mkdirs()

  "FileAssetsController.file" should {
    "be forbidden for everything without an extension" in {
      running(FakeApplication()) {
        val result = FileAssetsController.file("noextension", Some(documentRoot.getAbsolutePath))(FakeRequest())

        status(result) shouldEqual FORBIDDEN
      }
    }

    "be forbidden for everything not on the whitelist" in {
      running(FakeApplication()) {
        val result = FileAssetsController.file("notallowed.php", Some(documentRoot.getAbsolutePath))(FakeRequest())

        status(result) shouldEqual FORBIDDEN
      }
    }

    "be not found for nonexisting files" in {
      running(FakeApplication()) {
        val result = FileAssetsController.file("unknonw.css", Some(documentRoot.getAbsolutePath))(FakeRequest())

        status(result) shouldEqual NOT_FOUND
      }
    }

    "be not found for directories" in {
      running(FakeApplication()) {
        new File(documentRoot, "illegal.gif").mkdirs()

        val result = FileAssetsController.file("illegal.gif", Some(documentRoot.getAbsolutePath))(FakeRequest())

        status(result) shouldEqual NOT_FOUND
      }
    }

    "server a regular file" in {
      running(FakeApplication()) {

        printToFile(new File(documentRoot, "allowed.css")) {
          out =>
            out.print("This is a test")
        }

        val result = FileAssetsController.file("allowed.css", Some(documentRoot.getAbsolutePath))(FakeRequest())

        status(result) shouldEqual OK
        contentType(result) should beSome("text/css")
        contentAsString(result) shouldEqual """This is a test"""
      }
    }

    "answer not modified if etag matches" in {
      running(FakeApplication()) {

        printToFile(new File(documentRoot, "allowed2.css")) {
          out =>
            out.print("This is a test")
        }

        val result = FileAssetsController.file("allowed2.css", Some(documentRoot.getAbsolutePath))(FakeRequest())

        status(result) shouldEqual OK
        val etag = header(ETAG, result)
        etag should beSome

        val result2 = FileAssetsController.file("allowed2.css", Some(documentRoot.getAbsolutePath))(FakeRequest()
          .withHeaders(IF_NONE_MATCH -> etag.get))

        status(result2) shouldEqual NOT_MODIFIED

        printToFile(new File(documentRoot, "allowed2.css")) {
          out =>
            out.print("This is a test 2")
        }

        val result3 = FileAssetsController.file("allowed2.css", Some(documentRoot.getAbsolutePath))(FakeRequest()
          .withHeaders(IF_NONE_MATCH -> etag.get))

        status(result3) shouldEqual OK
      }
    }

    "answer not modified if last modfied has not changed" in {
      running(FakeApplication()) {
        printToFile(new File(documentRoot, "allowed3.css")) {
          out =>
            out.print("This is a test")
        }

        val result = FileAssetsController.file("allowed3.css", Some(documentRoot.getAbsolutePath))(FakeRequest())

        status(result) shouldEqual OK
        val date = header(DATE, result)
        date should beSome

        val result2 = FileAssetsController.file("allowed3.css", Some(documentRoot.getAbsolutePath))(FakeRequest()
          .withHeaders(IF_MODIFIED_SINCE -> date.get))

        status(result2) shouldEqual NOT_MODIFIED

        Thread.sleep(2000)

        printToFile(new File(documentRoot, "allowed3.css")) {
          out =>
            out.print("This is a test")
        }

        val result3 = FileAssetsController.file("allowed3.css", Some(documentRoot.getAbsolutePath))(FakeRequest()
          .withHeaders(IF_MODIFIED_SINCE -> date.get))

        status(result3) shouldEqual OK
      }
    }
  }

  def printToFile(f: File)(op: PrintWriter => Unit) {

    val p = new PrintWriter(f)
    try {
      op(p)
    } finally {
      p.close()
    }
  }
}

