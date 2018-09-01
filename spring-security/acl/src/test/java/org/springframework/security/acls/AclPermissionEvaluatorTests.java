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
package org.springframework.security.acls;

import static org.assertj.core.api.Assertions.*;

import static org.mockito.Mockito.*;

import java.util.Locale;

import org.junit.Test;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;

/**
 *
 * @author Luke Taylor
 * @since 3.0
 */
public class AclPermissionEvaluatorTests {

	@Test
	public void hasPermissionReturnsTrueIfAclGrantsPermission() throws Exception {
		AclService service = mock(AclService.class);
		AclPermissionEvaluator pe = new AclPermissionEvaluator(service);
		ObjectIdentity oid = mock(ObjectIdentity.class);
		ObjectIdentityRetrievalStrategy oidStrategy = mock(ObjectIdentityRetrievalStrategy.class);
		when(oidStrategy.getObjectIdentity(anyObject())).thenReturn(oid);
		pe.setObjectIdentityRetrievalStrategy(oidStrategy);
		pe.setSidRetrievalStrategy(mock(SidRetrievalStrategy.class));
		Acl acl = mock(Acl.class);

		when(service.readAclById(any(ObjectIdentity.class), anyListOf(Sid.class))).thenReturn(acl);
		when(acl.isGranted(anyListOf(Permission.class), anyListOf(Sid.class), eq(false))).thenReturn(true);

		assertThat(pe.hasPermission(mock(Authentication.class), new Object(), "READ")).isTrue();
	}

	@Test
	public void resolvePermissionNonEnglishLocale() {
		Locale systemLocale = Locale.getDefault();
		Locale.setDefault(new Locale("tr"));

		AclService service = mock(AclService.class);
		AclPermissionEvaluator pe = new AclPermissionEvaluator(service);
		ObjectIdentity oid = mock(ObjectIdentity.class);
		ObjectIdentityRetrievalStrategy oidStrategy = mock(ObjectIdentityRetrievalStrategy.class);
		when(oidStrategy.getObjectIdentity(anyObject())).thenReturn(oid);
		pe.setObjectIdentityRetrievalStrategy(oidStrategy);
		pe.setSidRetrievalStrategy(mock(SidRetrievalStrategy.class));
		Acl acl = mock(Acl.class);

		when(service.readAclById(any(ObjectIdentity.class), anyListOf(Sid.class))).thenReturn(acl);
		when(acl.isGranted(anyListOf(Permission.class), anyListOf(Sid.class), eq(false))).thenReturn(true);

		assertThat(pe.hasPermission(mock(Authentication.class), new Object(), "write")).isTrue();

		Locale.setDefault(systemLocale);
	}
}
