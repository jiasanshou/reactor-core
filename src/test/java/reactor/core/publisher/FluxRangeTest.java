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

import org.junit.Test;
import reactor.test.subscriber.AssertSubscriber;

public class FluxRangeTest {

	@Test
	public void normal() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		Flux.range(1, 10).subscribe(ts);

		ts.assertNoError()
		  .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
		  .assertComplete();
	}

	@Test
	public void normalBackpressured() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create(0);

		Flux.range(1, 10).subscribe(ts);

		ts.assertNoError()
		  .assertNoValues()
		  .assertNotComplete();

		ts.request(5);

		ts.assertNoError()
		  .assertValues(1, 2, 3, 4, 5)
		  .assertNotComplete();

		ts.request(10);

		ts.assertNoError()
		  .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
		  .assertComplete();
	}

	@Test
	public void normalBackpressuredExact() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create(10);

		Flux.range(1, 10).subscribe(ts);

		ts.assertNoError()
		  .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
		  .assertComplete();

		ts.request(10);

		ts.assertNoError()
		  .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
		  .assertComplete();
	}

	@Test(expected = IllegalArgumentException.class)
	public void countIsNegative() {
		Flux.range(1, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rangeOverflow() {
		Flux.range(2, Integer.MAX_VALUE);
	}

	@Test
	public void normalNearMaxValue1() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		Flux.range(Integer.MAX_VALUE, 1).subscribe(ts);

		ts.assertNoError()
		  .assertValues(Integer.MAX_VALUE)
		  .assertComplete();
	}

	@Test
	public void normalNearMaxValue2() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		Flux.range(Integer.MAX_VALUE - 1, 2).subscribe(ts);

		ts.assertNoError()
		  .assertValues(Integer.MAX_VALUE - 1, Integer.MAX_VALUE)
		  .assertComplete();
	}

	@Test
	public void normalNegativeStart() {
		AssertSubscriber<Integer> ts = AssertSubscriber.create();

		Flux.range(-10, 2).subscribe(ts);

		ts.assertNoError()
		  .assertValues(-10, -9)
		  .assertComplete();

	}
}
