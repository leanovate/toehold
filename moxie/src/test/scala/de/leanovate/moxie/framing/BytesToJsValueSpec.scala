/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.moxie.framing

import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import de.leanovate.moxie.testutil.CollectingPMConsumer
import play.api.libs.json._
import akka.util.ByteString
import de.leanovate.akka.tcp.PMConsumer.{Data, Subscription, EOF, NoSubscription}
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber
import org.specs2.mock.Mockito

class BytesToJsValueSpec extends Specification with ShouldMatchers with Mockito {
  "Framing.bytesToJsValue" should {
    "parse all chunks to JsValues" in {
      val out = new CollectingPMConsumer[JsValue]
      val pipe = Framing.bytesToJsValue |> out

      pipe.push(ByteString( """{"hello":"world"}"""), ByteString("42"), ByteString("true"))
      pipe.onNext(EOF)

      out.eof should beTrue
      out.result() shouldEqual Seq(JsObject(Seq("hello" -> JsString("world"))), JsNumber(42L), JsBoolean(true))
    }

    "abort on parse error" in {
      val out = new CollectingPMConsumer[JsValue]
      val pipe = Framing.bytesToJsValue |> out
      val subscription = mock[Subscription]

      pipe.onSubscribe(subscription)
      pipe.onNext(Data(ByteString( """{nojson}""")))

      there was one(subscription).abort(startWith("Unexpected character ('n' (code 110))"))
    }
  }
}
