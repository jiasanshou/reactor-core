/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.core.publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import reactor.test.subscriber.AssertSubscriber;
import reactor.util.concurrent.QueueSupplier;

import static org.assertj.core.api.Assertions.assertThat;

public class FluxFlatMapTest {

	/*@Test
	public void constructors() {
		ConstructorTestBuilder ctb = new ConstructorTestBuilder(FluxFlatMap.class);
		
		ctb.addRef("source", Flux.never());
		ctb.addRef("mapper", (Function<Object, Publisher<Object>>)v -> Flux.never());
		ctb.addInt("prefetch", 1, Integer.MAX_VALUE);
		ctb.addInt("maxConcurrency", 1, Integer.MAX_VALUE);
		ctb.addRef("mainQueueSupplier", (Supplier<Queue<Object>>)() -> new ConcurrentLinkedQueue<>());
		ctb.addRef("innerQueueSupplier", (Supplier<Queue<Object>>)() -> new ConcurrentLinkedQueue<>());
		
		ctb.test();
	}*/

	@Test
	public void normal() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();
		
		Flux.range(1, 1000).flatMap(v -> Flux.range(v, 2)).subscribe(ts);
		
		ts.assertValueCount(2000)
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void normalBackpressured() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create(0);
		
		Flux.range(1, 1000).flatMap(v -> Flux.range(v, 2)).subscribe(ts);
		
		ts.assertNoValues()
		.assertNoError()
		.assertNotComplete();
		
		ts.request(1000);

		ts.assertValueCount(1000)
		.assertNoError()
		.assertNotComplete();

		ts.request(1000);

		ts.assertValueCount(2000)
		.assertNoError()
		.assertComplete();
	}
	
	@Test
	public void mainError() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create(0);

		Flux.<Integer>error(new RuntimeException("forced failure"))
		.flatMap(v -> Flux.just(v)).subscribe(ts);
		
		ts.assertNoValues()
		.assertError(RuntimeException.class)
		  .assertErrorWith( e -> Assert.assertTrue(e.getMessage().contains("forced failure")))
		.assertNotComplete();
	}

	@Test
	public void innerError() {
		AssertSubscriber<Object> ts = AssertSubscriber.create(0);

		Flux.just(1).flatMap(v -> Flux.error(new RuntimeException("forced failure"))).subscribe(ts);
		
		ts.assertNoValues()
		.assertError(RuntimeException.class)
		  .assertErrorWith( e -> Assert.assertTrue(e.getMessage().contains("forced failure")))
		.assertNotComplete();
	}

	@Test
	public void normalQueueOpt() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();
		
		Flux.range(1, 1000).flatMap(v -> Flux.just(v, v + 1)).subscribe(ts);
		
		ts.assertValueCount(2000)
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void normalQueueOptBackpressured() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create(0);
		
		Flux.range(1, 1000).flatMap(v -> Flux.just(v, v + 1)).subscribe(ts);
		
		ts.assertNoValues()
		.assertNoError()
		.assertNotComplete();
		
		ts.request(1000);

		ts.assertValueCount(1000)
		.assertNoError()
		.assertNotComplete();

		ts.request(1000);

		ts.assertValueCount(2000)
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void nullValue() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();
		
		Flux.range(1, 1000).flatMap(v -> Flux.just((Integer)null)).subscribe(ts);
		
		ts.assertNoValues()
		.assertError(NullPointerException.class)
		.assertNotComplete();
	}

	@Test
	public void mainEmpty() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create(0);

		Flux.<Integer>empty().flatMap(v -> Flux.just(v)).subscribe(ts);
		
		ts.assertNoValues()
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void innerEmpty() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create(0);

		Flux.range(1, 1000).flatMap(v -> Flux.<Integer>empty()).subscribe(ts);
		
		ts.assertNoValues()
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void flatMapOfJust() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();
		
		Flux.range(1, 1000).flatMap(Flux::just).subscribe(ts);
		
		ts.assertValueCount(1000)
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void flatMapOfMixed() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();
		
		Flux.range(1, 1000).flatMap(
				v -> v % 2 == 0 ? Flux.just(v) : Flux.fromIterable(Arrays.asList(v)))
		.subscribe(ts);
		
		ts.assertValueCount(1000)
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void flatMapOfMixedBackpressured() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create(0);
		
		Flux.range(1, 1000).flatMap(v -> v % 2 == 0 ? Flux.just(v) : Flux.fromIterable(Arrays.asList(v))).subscribe(ts);
		
		ts.assertNoValues()
		.assertNoError()
		.assertNotComplete();
		
		ts.request(500);

		ts.assertValueCount(500)
		.assertNoError()
		.assertNotComplete();

		ts.request(500);

		ts.assertValueCount(1000)
		.assertNoError()
		.assertComplete();
	}
	
	@Test
	public void flatMapOfMixedBackpressured1() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create(0);
		
		Flux.range(1, 1000).flatMap(v -> v % 2 == 0 ? Flux.just(v) : Flux.fromIterable(Arrays.asList(v))).subscribe(ts);
		
		ts.assertNoValues()
		.assertNoError()
		.assertNotComplete();
		
		ts.request(500);

		ts.assertValueCount(500)
		.assertNoError()
		.assertNotComplete();

		ts.request(501);

		ts.assertValueCount(1000)
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void flatMapOfJustBackpressured() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create(0);
		
		Flux.range(1, 1000).flatMap(Flux::just).subscribe(ts);
		
		ts.assertNoValues()
		.assertNoError()
		.assertNotComplete();
		
		ts.request(500);

		ts.assertValueCount(500)
		.assertNoError()
		.assertNotComplete();

		ts.request(500);

		ts.assertValueCount(1000)
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void flatMapOfJustBackpressured1() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create(0);
		
		Flux.range(1, 1000).flatMap(Flux::just).subscribe(ts);
		
		ts.assertNoValues()
		.assertNoError()
		.assertNotComplete();
		
		ts.request(500);

		ts.assertValueCount(500)
		.assertNoError()
		.assertNotComplete();

		ts.request(501);

		ts.assertValueCount(1000)
		.assertNoError()
		.assertComplete();
	}


	@Test
	public void testMaxConcurrency1() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		Flux.range(1, 1_000_000).flatMap(Flux::just, 1, 32).subscribe(ts);

		ts.assertValueCount(1_000_000)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void testMaxConcurrency2() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		Flux.range(1, 1_000_000).flatMap(Flux::just, 64).subscribe(ts);

		ts.assertValueCount(1_000_000)
		  .assertNoError()
		  .assertComplete();
	}

	@Test
	public void singleSubscriberOnly() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();
		
		AtomicInteger emission = new AtomicInteger();
		
		Flux<Integer> source = Flux.range(1, 2).doOnNext(v -> emission.getAndIncrement());
		
		EmitterProcessor<Integer> source1 = EmitterProcessor.create();
		EmitterProcessor<Integer> source2 = EmitterProcessor.create();

		source.flatMap(v -> v == 1 ? source1 : source2, 1, 32).subscribe(ts);

		source1.connect();
		source2.connect();
		
		Assert.assertEquals(1, emission.get());
		
		ts.assertNoValues()
		.assertNoError()
		.assertNotComplete();
		
		Assert.assertTrue("source1 no subscribers?", source1.downstreamCount() != 0);
		Assert.assertFalse("source2 has subscribers?", source2.downstreamCount() != 0);
		
		source1.onNext(1);
		source2.onNext(10);
		
		source1.onComplete();
		
		source2.onNext(2);
		source2.onComplete();
		
		ts.assertValues(1, 2)
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void flatMapUnbounded() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();
		
		AtomicInteger emission = new AtomicInteger();
		
		Flux<Integer> source = Flux.range(1, 1000).doOnNext(v -> emission.getAndIncrement());
		
		EmitterProcessor<Integer> source1 = EmitterProcessor.create();
		EmitterProcessor<Integer> source2 = EmitterProcessor.create();
		
		source.flatMap(v -> v == 1 ? source1 : source2, false, Integer.MAX_VALUE, 32).subscribe(ts);

		source1.connect();
		source2.connect();

		Assert.assertEquals(1000, emission.get());
		
		ts.assertNoValues()
		.assertNoError()
		.assertNotComplete();
		
		Assert.assertTrue("source1 no subscribers?", source1.downstreamCount() != 0);
		Assert.assertTrue("source2 no  subscribers?", source2.downstreamCount() != 0);
		
		source1.onNext(1);
		source1.onComplete();
		
		source2.onNext(2);
		source2.onComplete();
		
		ts.assertValueCount(1000)
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void syncFusionIterable() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();
		
		List<Integer> list = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			list.add(i);
		}
		
		Flux.range(1, 1000).flatMap(v -> Flux.fromIterable(list)).subscribe(ts);
		
		ts.assertValueCount(1_000_000)
		.assertNoError()
		.assertComplete();
	}
	
	@Test
	public void syncFusionRange() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();
		
		Flux.range(1, 1000).flatMap(v -> Flux.range(v, 1000)).subscribe(ts);
		
		ts.assertValueCount(1_000_000)
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void syncFusionArray() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();
		
		Integer[] array = new Integer[1000];
		Arrays.fill(array, 777);
		
		Flux.range(1, 1000).flatMap(v -> Flux.fromArray(array)).subscribe(ts);
		
		ts.assertValueCount(1_000_000)
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void innerMapSyncFusion() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		Flux.range(1, 1000).flatMap(v -> Flux.range(1, 1000).map(w -> w + 1)).subscribe(ts);

		ts.assertValueCount(1_000_000)
		.assertNoError()
		.assertComplete();
	}

	@Test
	public void defaultPrefetch() {
		assertThat(Flux.just(1, 2, 3)
		               .flatMap(Flux::just)
		               .getPrefetch()).isEqualTo(QueueSupplier.XS_BUFFER_SIZE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void failMaxConcurrency() {
		Flux.just(1, 2, 3)
		    .flatMap(Flux::just, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void failPrefetch() {
		Flux.just(1, 2, 3)
		    .flatMap(Flux::just, 128, -1);
	}

	@Test
	public void failCallable() {
		StepVerifier.create(Flux.just(1, 2, 3)
		                        .flatMap(d -> Mono.fromCallable(() -> {
			                        throw new Exception("test");
		                        })))
		            .verifyErrorMessage("test");
	}

	@Test
	public void failMap() {
		StepVerifier.create(Flux.just(1, 2, 3)
		                        .flatMap(d -> {
			                        throw new RuntimeException("test");
		                        }))
		            .verifyErrorMessage("test");
	}

	@Test
	public void failNull() {
		StepVerifier.create(Flux.just(1, 2, 3)
		                        .flatMap(d -> null))
		            .verifyError(NullPointerException.class);
	}

	@Test
	public void failScalarCallable() {
		StepVerifier.create(Mono.fromCallable(() -> {
			throw new Exception("test");
		})
		                        .flatMap(Flux::just))
		            .verifyErrorMessage("test");
	}

	@Test
	public void failScalarMap() {
		StepVerifier.create(Mono.just(1)
		                        .flatMap(f -> {
			                        throw new RuntimeException("test");
		                        }))
		            .verifyErrorMessage("test");
	}

	@Test
	public void failScalarMapNull() {
		StepVerifier.create(Mono.just(1)
		                        .flatMap(f -> null))
		            .verifyError(NullPointerException.class);
	}

	@Test
	public void failScalarMapCallableError() {
		StepVerifier.create(Mono.just(1)
		                        .flatMap(f -> Mono.fromCallable(() -> {
			                        throw new Exception("test");
		                        })))
		            .verifyErrorMessage("test");
	}

	@Test
	public void prematureScalarMapCallableNullComplete() {
		StepVerifier.create(Mono.just(1)
		                        .flatMap(f -> Mono.fromCallable(() -> null)))
		            .verifyComplete();
	}

	@Test
	public void prematureScalarMapCallableJust() {
		StepVerifier.create(Mono.just(1)
		                        .flatMap(f -> Mono.fromCallable(() -> 2)))
		            .expectNext(2)
		            .verifyComplete();
	}

	@Test
	public void prematureCancel() {
		StepVerifier.create(Flux.just(1, 2, 3)
		                        .flatMap(Flux::just))
		            .expectNext(1, 2, 3)
		            .thenCancel()
		            .verify();
	}

	@Test
	public void prematureCancel2() {
		StepVerifier.create(Flux.just(1, 2, 3)
		                        .flatMap(Flux::just))
		            .thenCancel()
		            .verify();
	}

	@Test //FIXME use Violation.NO_CLEANUP_ON_TERMINATE
	public void failNextOnTerminated() {
		UnicastProcessor<Integer> up = UnicastProcessor.create();

		Hooks.onNextDropped(c -> {
			assertThat(c).isEqualTo(2);
		});
		StepVerifier.create(up.flatMap(Flux::just))
		            .then(() -> {
			            up.onNext(1);
			            Subscriber<? super Integer> a = up.actual;
			            up.onComplete();
			            a.onNext(2);
		            })
		            .expectNext(1)
					.verifyComplete();
		Hooks.resetOnNextDropped();
	}

	@Test //FIXME use Violation.NO_CLEANUP_ON_TERMINATE
	public void ignoreDoubleComplete() {
		StepVerifier.create(Flux.from(s -> {
			s.onSubscribe(Operators.emptySubscription());
			s.onComplete();
			s.onComplete();
		}).flatMap(Flux::just))
		            .verifyComplete();
	}

	@Test //FIXME use Violation.NO_CLEANUP_ON_TERMINATE
	public void failDoubleError() {
		try {
			StepVerifier.create(Flux.from(s -> {
				s.onSubscribe(Operators.emptySubscription());
				s.onError(new Exception("test"));
				s.onError(new Exception("test2"));
			}).flatMap(Flux::just))
			            .verifyErrorMessage("test");
			Assert.fail();
		}
		catch (Exception e) {
			assertThat(Exceptions.unwrap(e)).hasMessage("test2");
		}
	}


	@Test //FIXME use Violation.NO_CLEANUP_ON_TERMINATE
	public void failDoubleError2() {
		try {
			StepVerifier.create(Flux.from(s -> {
				s.onSubscribe(Operators.emptySubscription());
				((FluxFlatMap.FlatMapMain)s).error = Exceptions.TERMINATED;
				s.onError(new Exception("test"));
			}).flatMap(Flux::just))
			            .verifyErrorMessage("test");
			Assert.fail();
		}
		catch (Exception e) {
			assertThat(Exceptions.unwrap(e)).hasMessage("test");
		}
	}

	@Test
	public void failOverflowScalar() {
		TestPublisher<Integer> ts =
				TestPublisher.createNoncompliant(TestPublisher.Violation.REQUEST_OVERFLOW);

		StepVerifier.create(ts.flux()
		                      .flatMap(Flux::just, 1), 0)
		            .then(() -> ts.emit(1, 2))
		            .verifyErrorMatches(Exceptions::isOverflow);
	}

	@Test
	public void failOverflowScalarThenError() {
		AtomicBoolean set = new AtomicBoolean();
		Hooks.onErrorDropped(e -> {
			assertThat(Exceptions.isOverflow(e)).isTrue();
			set.set(true);
		});
		StepVerifier.create(Flux.from(s -> {
			s.onSubscribe(Operators.emptySubscription());
			s.onNext(1);
			((FluxFlatMap.FlatMapMain)s).error = Exceptions.TERMINATED;
			s.onNext(2);
			((FluxFlatMap.FlatMapMain)s).error = null;
			s.onError(new Exception("test"));
		})
		                      .flatMap(Flux::just, 1), 0)
		            .verifyErrorMessage("test");
		Hooks.resetOnErrorDropped();
		assertThat(set.get()).isTrue();
	}

	@Test
	public void failOverflowWhileActiveScalar() {
		StepVerifier.create(Flux.from(s -> {
			s.onSubscribe(Operators.emptySubscription());
			s.onNext(1);
			((FluxFlatMap.FlatMapMain)s).wip = 1; //simulate concurrent active
			s.onNext(2);
			((FluxFlatMap.FlatMapMain)s).drainLoop();
		})
		                      .flatMap(Flux::just, 1), 0)
		            .verifyErrorMatches(Exceptions::isOverflow);
	}

	@Test
	public void failOverflowWhileActiveScalarThenError() {
		AtomicBoolean set = new AtomicBoolean();
		Hooks.onErrorDropped(e -> {
			assertThat(Exceptions.isOverflow(e)).isTrue();
			set.set(true);
		});
		StepVerifier.create(Flux.from(s -> {
			s.onSubscribe(Operators.emptySubscription());
			s.onNext(1);
			((FluxFlatMap.FlatMapMain)s).error = Exceptions.TERMINATED;
			((FluxFlatMap.FlatMapMain)s).wip = 1; //simulate concurrent active
			s.onNext(2);
			s.onNext(3);
			((FluxFlatMap.FlatMapMain)s).error = null;
			((FluxFlatMap.FlatMapMain)s).drainLoop();
			s.onError(new Exception("test"));
		})
		                        .flatMap(Flux::just, 1), 1)
		            .expectNext(1)
		            .verifyErrorMessage("test");
		Hooks.resetOnErrorDropped();
		assertThat(set.get()).isTrue();
	}

	@Test //FIXME use Violation.NO_CLEANUP_ON_TERMINATE
	public void failDoubleErrorSilent() {
		Hooks.onErrorDropped(e -> {
			assertThat(e).hasMessage("test2");
		});
		StepVerifier.create(Flux.from(s -> {
			s.onSubscribe(Operators.emptySubscription());
			s.onError(new Exception("test"));
			s.onError(new Exception("test2"));
		}).flatMap(Flux::just))
		            .verifyErrorMessage("test");
		Hooks.resetOnErrorDropped();
	}

	@Test
	public void suppressFusionIfExpected() {
		StepVerifier.create(Mono.just(1)
		                        .then(d -> Mono.just(d)
		                                       .hide()))
		            .consumeSubscriptionWith(s -> {
			            assertThat(s).isInstanceOf(FluxHide.SuppressFuseableSubscriber.class);
		            })
		            .expectNext(1)
		            .verifyComplete();
	}

	@Test
	public void ignoreRequestZeroThenRequestOneByOne() {
		StepVerifier.create(Flux.just(1, 2, 3)
		                        .flatMap(f -> Flux.just(f * 2)), 0)
		            .consumeSubscriptionWith(s -> s.request(0))
		            .thenRequest(1)
		            .expectNext(2)
		            .thenRequest(1)
		            .expectNext(4)
		            .thenRequest(1)
		            .expectNext(6)
		            .verifyComplete();
	}

	@Test
	public void suppressFusionIfExpectedError() {
		StepVerifier.create(Mono.just(1)
		                        .then(d -> Mono.error(new Exception("test"))
		                                       .hide()))
		            .consumeSubscriptionWith(s -> {
			            assertThat(s).isInstanceOf(FluxHide.SuppressFuseableSubscriber.class);
		            })
		            .verifyErrorMessage("test");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void cancelled() {
		AtomicReference<FluxZip.ZipSingleCoordinator> ref = new AtomicReference<>();

		StepVerifier.create(Flux.zip(obj -> (int) obj[0] + (int) obj[1] + (int) obj[2],
				1,
				Flux.just(1, 2),
				Flux.defer(() -> {
					ref.get()
					   .cancel();
					return Flux.just(3);
				}),
				Flux.just(3))
		                        .doOnSubscribe(s -> {
			                        assertThat(s instanceof FluxZip.ZipSingleCoordinator).isTrue();
			                        ref.set((FluxZip.ZipSingleCoordinator) s);
			                        assertInnerSubscriberBefore(ref.get());
		                        }), 0)
		            .then(() -> assertThat(ref.get()
		                                      .getCapacity()).isEqualTo(3))
		            .then(() -> assertThat(ref.get()
		                                      .getPending()).isEqualTo(1))
		            .then(() -> assertThat(ref.get()
		                                      .upstreams()).hasSize(3))
		            .thenCancel()
		            .verify();

		assertInnerSubscriber(ref.get());
	}

	@SuppressWarnings("unchecked")
	void assertInnerSubscriberBefore(FluxZip.ZipSingleCoordinator c) {
		FluxZip.ZipSingleSubscriber s = (FluxZip.ZipSingleSubscriber) c.upstreams()
		                                                               .next();

		assertThat(s.isStarted()).isTrue();
		assertThat(s.isTerminated()).isFalse();
		assertThat(s.upstream()).isNull();
		assertThat(s.getCapacity()).isEqualTo(1L);
		assertThat(s.getPending()).isEqualTo(1L);
		assertThat(s.isCancelled()).isFalse();
	}

	@SuppressWarnings("unchecked")
	void assertInnerSubscriber(FluxZip.ZipSingleCoordinator c) {
		FluxZip.ZipSingleSubscriber s = (FluxZip.ZipSingleSubscriber) c.upstreams()
		                                                               .next();

		assertThat(s.isStarted()).isFalse();
		assertThat(s.isTerminated()).isTrue();
		assertThat(s.upstream()).isNotNull();
		assertThat(s.getCapacity()).isEqualTo(1);
		assertThat(s.getPending()).isEqualTo(-1L);
		assertThat(s.isCancelled()).isTrue();

		Hooks.onNextDropped(v -> {
		});
		s.onNext(0);
		Hooks.resetOnNextDropped();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void cancelledHide() {
		AtomicReference<FluxZip.ZipCoordinator> ref = new AtomicReference<>();

		StepVerifier.create(Flux.zip(obj -> (int) obj[0] + (int) obj[1] + (int) obj[2],
				123,
				Flux.just(1, 2)
				    .hide(),
				Flux.defer(() -> {
					ref.get()
					   .cancel();
					return Flux.just(3);
				}),
				Flux.just(3)
				    .hide())
		                        .doOnSubscribe(s -> {
			                        assertThat(s instanceof FluxZip.ZipCoordinator).isTrue();
			                        ref.set((FluxZip.ZipCoordinator) s);
			                        assertInnerSubscriberBefore(ref.get());
		                        }), 0)
		            .then(() -> assertThat(ref.get()
		                                      .getCapacity()).isEqualTo(3))
		            .then(() -> assertThat(ref.get()
		                                      .getPending()).isEqualTo(1))
		            .then(() -> assertThat(ref.get()
		                                      .upstreams()).hasSize(3))
		            .then(() -> assertThat(ref.get()
		                                      .getError()).isNull())
		            .then(() -> assertThat(ref.get()
		                                      .requestedFromDownstream()).isEqualTo(0))
		            .thenCancel()
		            .verify();

		assertInnerSubscriber(ref.get());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void delayedCancelledHide() {
		AtomicReference<FluxZip.ZipCoordinator> ref = new AtomicReference<>();

		StepVerifier.create(Flux.zip(obj -> (int) obj[0] + (int) obj[1] + (int) obj[2],
				123,
				Flux.just(1, 2)
				    .hide(),
				Flux.defer(() -> {
					ref.get()
					   .cancel();
					assertThat(ref.get()
					              .getPending()).isEqualTo(1);
					assertThat(ref.get()
					              .isCancelled()).isTrue();
					assertThat(ref.get()
					              .isTerminated()).isFalse();
					return Flux.just(3);
				}),
				Flux.just(3)
				    .hide())
		                        .doOnSubscribe(s -> {
			                        assertThat(s instanceof FluxZip.ZipCoordinator).isTrue();
			                        ref.set((FluxZip.ZipCoordinator) s);
			                        assertInnerSubscriberBefore(ref.get());
		                        }), 0)
		            .then(() -> assertThat(ref.get()
		                                      .getCapacity()).isEqualTo(3))
		            .then(() -> assertThat(ref.get()
		                                      .getPending()).isEqualTo(1))
		            .then(() -> assertThat(ref.get()
		                                      .upstreams()).hasSize(3))
		            .then(() -> assertThat(ref.get()
		                                      .getError()).isNull())
		            .then(() -> assertThat(ref.get()
		                                      .requestedFromDownstream()).isEqualTo(0))
		            .thenCancel()
		            .verify();

		assertInnerSubscriber(ref.get());
	}

	@SuppressWarnings("unchecked")
	void assertInnerSubscriberBefore(FluxZip.ZipCoordinator c) {
		FluxZip.ZipInner s = (FluxZip.ZipInner) c.upstreams()
		                                         .next();

		assertThat(s.isStarted()).isTrue();
		assertThat(s.isTerminated()).isFalse();
		assertThat(s.upstream()).isNull();
		assertThat(s.getCapacity()).isEqualTo(123);
		assertThat(s.getPending()).isEqualTo(-1L);
		assertThat(s.limit()).isEqualTo(93);
		assertThat(s.expectedFromUpstream()).isEqualTo(0);
		assertThat(s.downstream()).isNull();
		assertThat(s.isCancelled()).isFalse();
	}

	@SuppressWarnings("unchecked")
	void assertInnerSubscriber(FluxZip.ZipCoordinator c) {
		FluxZip.ZipInner s = (FluxZip.ZipInner) c.upstreams()
		                                         .next();

		assertThat(s.isStarted()).isFalse();
		assertThat(s.isTerminated()).isFalse();
		assertThat(s.upstream()).isNotNull();
		assertThat(s.getCapacity()).isEqualTo(123);
		assertThat(s.getPending()).isEqualTo(1);
		assertThat(s.limit()).isEqualTo(93);
		assertThat(s.expectedFromUpstream()).isEqualTo(0);
		assertThat(s.downstream()).isNull();
		assertThat(s.isCancelled()).isFalse();
	}

}
