package example;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.observability.DefaultSignalListener;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class DemoController {

    private WebClient webClient;

    public DemoController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @GetMapping("/notWork")
    Mono<String> notWork() {
        log.info("1) this has trace id");
        return webClient.head().uri("https://httpbin.org/status/200").retrieve().toBodilessEntity()
                .tap(contextView -> new DefaultSignalListener<ResponseEntity<Void>>() {
                    @Override
                    public void doOnNext(ResponseEntity<Void> value) throws Throwable {
                        Observation currentObservation = contextView.get(ObservationThreadLocalAccessor.KEY);
                        log.info("current observation {}", currentObservation.getContext().toString());
                    }
                })
                .map(r -> {
                    log.info("2) this does not have a trace id {}", r.getStatusCode());
                    return r;
                }).thenReturn("OK");
    }

    @GetMapping("/works")
    Mono<String> works() {
        log.info("3) this has trace id");
        return webClient.head().uri("https://httpbin.org/status/200").retrieve().toEntity(String.class)
                .tap(contextView -> new DefaultSignalListener<ResponseEntity<String>>() {
                    @Override
                    public void doOnNext(ResponseEntity<String> value) throws Throwable {
                        Observation currentObservation = contextView.get(ObservationThreadLocalAccessor.KEY);
                        log.info("current observation {}", currentObservation.getContext().toString());
                    }
                })
                .map(r -> {
                    log.info("4) this has trace id {}", r.getStatusCode());
                    return r;
                }).thenReturn("OK");
    }

}
