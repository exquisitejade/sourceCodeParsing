/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.access.intercept.method;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

@SuppressWarnings("unchecked")
public class MockMethodInvocation implements MethodInvocation {
	private Method method;
	private Object targetObject;

	public MockMethodInvocation(Object targetObject, Class clazz, String methodName,
			Class... parameterTypes) throws NoSuchMethodException {
		this.method = clazz.getMethod(methodName, parameterTypes);
		this.targetObject = targetObject;
	}

	public Object[] getArguments() {
		return null;
	}

	public Method getMethod() {
		return method;
	}

	public AccessibleObject getStaticPart() {
		return null;
	}

	public Object getThis() {
		return targetObject;
	}

	public Object proceed() throws Throwable {
		return null;
	}
}
