package au.com.dius.pact.provider.spring.spring6

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import spock.lang.Specification

import java.nio.charset.StandardCharsets

import static org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction

@SuppressWarnings('ClosureAsLastMethodParameter')
class WebTestClientTargetSpec extends Specification {
  RouterFunction routerFunction = RouterFunctions.route(RequestPredicates.GET('/data'), { req ->
    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue('{"id":1234}'))
  })

  def 'should prepare get request'() {
    given:
    WebTestClientSpring6Target webTestClientTarget = new WebTestClientSpring6Target(
      bindToRouterFunction(routerFunction).build())
    def request = new Request('GET', '/data', [id: ['1234']])
    def interaction = new RequestResponseInteraction('some description', [], request)
    def pact = Mock(Pact)

    when:
    def requestAndClient = webTestClientTarget.prepareRequest(pact, interaction, [:])
    def requestBuilder = requestAndClient.first
    def builtRequest = requestBuilder.exchange().expectBody().returnResult()

    then:
    requestBuilder instanceof WebTestClient.RequestHeadersSpec
    builtRequest.url.path == '/data'
    builtRequest.method.toString() == 'GET'
    new String(builtRequest.responseBody) == '{"id":1234}'
  }

  def 'should prepare post request'() {
    given:
    RouterFunction postRouterFunction = RouterFunctions.route(RequestPredicates.POST('/data'), { req ->
      assert req.queryParams() == [id: ['1234']]
      def reqBody = req.bodyToMono(String).doOnNext({ s -> assert s == '{"foo":"bar"}' })
      ServerResponse.ok().build(reqBody)
    })
    WebTestClientSpring6Target webTestClientTarget = new WebTestClientSpring6Target(
      bindToRouterFunction(postRouterFunction).build())
    def request = new Request('POST', '/data', [id: ['1234']], [:],
            OptionalBody.body('{"foo":"bar"}'.getBytes(StandardCharsets.UTF_8)))
    def interaction = new RequestResponseInteraction('some description', [], request)
    def pact = Mock(Pact)

    when:
    def requestAndClient = webTestClientTarget.prepareRequest(pact, interaction, [:])
    def requestBuilder = requestAndClient.first

    then:
    requestBuilder instanceof WebTestClient.RequestHeadersSpec
    def builtRequest = requestBuilder.exchange().expectBody().returnResult()
    builtRequest.url.path == '/data'
    builtRequest.method.toString() == 'POST'
    builtRequest.rawStatusCode == 200
  }

  def 'should execute interaction'() {
    given:
    def request = new Request('GET', '/data', [id: ['1234']])
    def interaction = new RequestResponseInteraction('some description', [], request)
    def pact = Mock(Pact)
    WebTestClientSpring6Target webTestClientTarget = new WebTestClientSpring6Target(
      bindToRouterFunction(routerFunction).build())
    def requestAndClient = webTestClientTarget.prepareRequest(pact, interaction, [:])
    def requestBuilder = requestAndClient.first

    when:
    def response = webTestClientTarget.executeInteraction(requestAndClient.second, requestBuilder)

    then:
    response.statusCode == 200
    response.contentType.toString() == 'application/json'
    response.body.valueAsString() == '{"id":1234}'
  }
}
