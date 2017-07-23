/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.jupiter.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.engine.execution.injection.sample.DoubleParameterResolver;
import org.junit.jupiter.engine.execution.injection.sample.LongParameterResolver;
import org.junit.platform.engine.test.event.ExecutionEventRecorder;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

/**
 * Integration tests that verify support for selecting and executing default
 * methods from interfaces in conjunction with the {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
class DefaultMethodTests extends AbstractJupiterTestEngineTests {

	private static boolean beforeAllInvoked;
	private static boolean afterAllInvoked;
	private static boolean defaultMethodInvoked;
	private static boolean localMethodInvoked;

	@BeforeEach
	void resetFlags() {
		beforeAllInvoked = false;
		afterAllInvoked = false;
		defaultMethodInvoked = false;
		localMethodInvoked = false;
	}

	@Test
	void executeTestCaseWithDefaultMethodFromInterfaceSelectedByFullyQualifedMethodName() {
		String fqmn = TestCaseWithDefaultMethod.class.getName() + "#test";
		LauncherDiscoveryRequest request = request().selectors(selectMethod(fqmn)).build();
		ExecutionEventRecorder eventRecorder = executeTests(request);

		// @formatter:off
		assertAll(
				() -> assertTrue(beforeAllInvoked, "@BeforeAll static method invoked from interface"),
				() -> assertTrue(afterAllInvoked, "@AfterAll static method invoked from interface"),
				() -> assertTrue(defaultMethodInvoked, "default @Test method invoked from interface"),
				() -> assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started"),
				() -> assertEquals(1, eventRecorder.getTestSuccessfulCount(), "# tests succeeded"),
				() -> assertEquals(0, eventRecorder.getTestFailedCount(), "# tests failed")
		);
		// @formatter:on
	}

	// TODO [#969] Enable and complete @Disabled test.
	//
	// Note: we currently get an exception like the following:
	// org.junit.platform.commons.util.PreconditionViolationException:
	// Could not find method with name [test] and parameter types [java.lang.Long]
	// in class [org.junit.jupiter.engine.DefaultMethodTests$GenericTestCaseWithDefaultMethod].
	@Disabled("Disabled until #969 is resolved")
	@Test
	void executeTestCaseWithDefaultMethodFromGenericInterfaceSelectedByFullyQualifedMethodName() throws Exception {
		String fqmn = GenericTestCaseWithDefaultMethod.class.getName() + "#test(" + Long.class.getName() + ")";
		LauncherDiscoveryRequest request = request().selectors(selectMethod(fqmn)).build();
		ExecutionEventRecorder eventRecorder = executeTests(request);

		// @formatter:off
		assertAll(
				() -> assertTrue(beforeAllInvoked, "@BeforeAll default method invoked from interface"),
				() -> assertTrue(afterAllInvoked, "@AfterAll default method invoked from interface"),
				() -> assertTrue(defaultMethodInvoked, "default @Test method invoked from interface"),
				() -> assertFalse(localMethodInvoked, "local @Test method should not have been invoked from class"),
				() -> assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started"),
				() -> assertEquals(1, eventRecorder.getTestSuccessfulCount(), "# tests succeeded"),
				() -> assertEquals(0, eventRecorder.getTestFailedCount(), "# tests failed")
		);
		// @formatter:on
	}

	@Test
	void executeTestCaseWithOverloadedMethodNextToGenericDefaultMethodSelectedByFullyQualifedMethodName()
			throws Exception {

		String fqmn = GenericTestCaseWithDefaultMethod.class.getName() + "#test(" + Double.class.getName() + ")";
		LauncherDiscoveryRequest request = request().selectors(selectMethod(fqmn)).build();
		ExecutionEventRecorder eventRecorder = executeTests(request);

		// @formatter:off
		assertAll(
				() -> assertTrue(beforeAllInvoked, "@BeforeAll default method invoked from interface"),
				() -> assertTrue(afterAllInvoked, "@AfterAll default method invoked from interface"),
				() -> assertFalse(defaultMethodInvoked, "default @Test method should not have been invoked from interface"),
				() -> assertTrue(localMethodInvoked, "local @Test method invoked from class"),
				() -> assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started"),
				() -> assertEquals(1, eventRecorder.getTestSuccessfulCount(), "# tests succeeded"),
				() -> assertEquals(0, eventRecorder.getTestFailedCount(), "# tests failed")
		);
		// @formatter:on
	}

	// TODO [#976] Enable failing @Disabled test.
	@Disabled("Disabled until #976 is resolved")
	@Test
	void executeTestCaseWithOverloadedMethodNextToGenericDefaultMethodSelectedByClass() throws Exception {
		Class<?> clazz = GenericTestCaseWithDefaultMethod.class;
		LauncherDiscoveryRequest request = request().selectors(selectClass(clazz)).build();
		ExecutionEventRecorder eventRecorder = executeTests(request);

		// @formatter:off
		assertAll(
				() -> assertTrue(beforeAllInvoked, "@BeforeAll default method invoked from interface"),
				() -> assertTrue(afterAllInvoked, "@AfterAll default method invoked from interface"),
				() -> assertTrue(defaultMethodInvoked, "default @Test method invoked from interface"),
				() -> assertTrue(localMethodInvoked, "local @Test method invoked from class"),
				() -> assertEquals(2, eventRecorder.getTestStartedCount(), "# tests started"),
				() -> assertEquals(2, eventRecorder.getTestSuccessfulCount(), "# tests succeeded"),
				() -> assertEquals(0, eventRecorder.getTestFailedCount(), "# tests failed")
		);
		// @formatter:on
	}

	interface TestInterface {

		@BeforeAll
		static void beforeAll() {
			beforeAllInvoked = true;
		}

		@Test
		default void test() {
			defaultMethodInvoked = true;
		}

		@AfterAll
		static void afterAll() {
			afterAllInvoked = true;
		}

	}

	static class TestCaseWithDefaultMethod implements TestInterface {
	}

	@ExtendWith({ LongParameterResolver.class, DoubleParameterResolver.class })
	@TestInstance(Lifecycle.PER_CLASS)
	interface GenericTestInterface<N extends Number> {

		@BeforeAll
		default void beforeAll() {
			beforeAllInvoked = true;
		}

		@Test
		default void test(N number) {
			defaultMethodInvoked = true;
			assertThat(number.intValue()).isEqualTo(42);
		}

		@AfterAll
		default void afterAll() {
			afterAllInvoked = true;
		}

	}

	static class GenericTestCaseWithDefaultMethod implements GenericTestInterface<Long> {

		@Test
		void test(Double number) {
			localMethodInvoked = true;
			assertThat(number).isEqualTo(42.0);
		}

	}

}