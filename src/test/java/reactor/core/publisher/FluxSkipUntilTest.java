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
import reactor.test.StepVerifier;

public class FluxSkipUntilTest {

	@Test
	public void normalHidden() {
		StepVerifier.create(Flux.range(1, 10)
		                        .hide()
		                        .skipUntil(v -> v > 4))
		            .expectNext(5, 6, 7, 8, 9, 10)
		            .verifyComplete();
	}

	@Test
	public void normal() {
		StepVerifier.create(Flux.range(1, 10)
		                        .skipUntil(v -> v > 4))
		            .expectNext(5, 6, 7, 8, 9, 10)
		            .verifyComplete();
	}
}