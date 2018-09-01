/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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

package org.springframework.security.ldap.userdetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.naming.Name;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.ldap.ppolicy.PasswordPolicyData;
import org.springframework.util.Assert;

/**
 * A UserDetails implementation which is used internally by the Ldap services. It also
 * contains the user's distinguished name and a set of attributes that have been retrieved
 * from the Ldap server.
 * <p>
 * An instance may be created as the result of a search, or when user information is
 * retrieved during authentication.
 * <p>
 * An instance of this class will be used by the <tt>LdapAuthenticationProvider</tt> to
 * construct the final user details object that it returns.
 * <p>
 * The {@code equals} and {@code hashcode} methods are implemented using the {@code Dn}
 * property and do not consider additional state, so it is not possible two store two
 * instances with the same DN in the same set, or use them as keys in a map.
 *
 * @author Luke Taylor
 */
public class LdapUserDetailsImpl implements LdapUserDetails, PasswordPolicyData {

	private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

	// ~ Instance fields
	// ================================================================================================

	private String dn;
	private String password;
	private String username;
	private Collection<GrantedAuthority> authorities = AuthorityUtils.NO_AUTHORITIES;
	private boolean accountNonExpired = true;
	private boolean accountNonLocked = true;
	private boolean credentialsNonExpired = true;
	private boolean enabled = true;
	// PPolicy data
	private int timeBeforeExpiration = Integer.MAX_VALUE;
	private int graceLoginsRemaining = Integer.MAX_VALUE;

	// ~ Constructors
	// ===================================================================================================

	protected LdapUserDetailsImpl() {
	}

	// ~ Methods
	// ========================================================================================================

	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getDn() {
		return dn;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return accountNonExpired;
	}

	@Override
	public boolean isAccountNonLocked() {
		return accountNonLocked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return credentialsNonExpired;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void eraseCredentials() {
		password = null;
	}

	@Override
	public int getTimeBeforeExpiration() {
		return timeBeforeExpiration;
	}

	@Override
	public int getGraceLoginsRemaining() {
		return graceLoginsRemaining;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LdapUserDetailsImpl) {
			return dn.equals(((LdapUserDetailsImpl) obj).dn);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return dn.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append(": ");
		sb.append("Dn: ").append(dn).append("; ");
		sb.append("Username: ").append(this.username).append("; ");
		sb.append("Password: [PROTECTED]; ");
		sb.append("Enabled: ").append(this.enabled).append("; ");
		sb.append("AccountNonExpired: ").append(this.accountNonExpired).append("; ");
		sb.append("CredentialsNonExpired: ").append(this.credentialsNonExpired)
				.append("; ");
		sb.append("AccountNonLocked: ").append(this.accountNonLocked).append("; ");

		if (this.getAuthorities() != null && !this.getAuthorities().isEmpty()) {
			sb.append("Granted Authorities: ");
			boolean first = true;

			for (Object authority : this.getAuthorities()) {
				if (first) {
					first = false;
				}
				else {
					sb.append(", ");
				}

				sb.append(authority.toString());
			}
		}
		else {
			sb.append("Not granted any authorities");
		}

		return sb.toString();
	}

	// ~ Inner Classes
	// ==================================================================================================

	/**
	 * Variation of essence pattern. Used to create mutable intermediate object
	 */
	public static class Essence {
		protected LdapUserDetailsImpl instance = createTarget();
		private List<GrantedAuthority> mutableAuthorities = new ArrayList<>();

		public Essence() {
		}

		public Essence(DirContextOperations ctx) {
			setDn(ctx.getDn());
		}

		public Essence(LdapUserDetails copyMe) {
			setDn(copyMe.getDn());
			setUsername(copyMe.getUsername());
			setPassword(copyMe.getPassword());
			setEnabled(copyMe.isEnabled());
			setAccountNonExpired(copyMe.isAccountNonExpired());
			setCredentialsNonExpired(copyMe.isCredentialsNonExpired());
			setAccountNonLocked(copyMe.isAccountNonLocked());
			setAuthorities(copyMe.getAuthorities());
		}

		protected LdapUserDetailsImpl createTarget() {
			return new LdapUserDetailsImpl();
		}

		/**
		 * Adds the authority to the list, unless it is already there, in which case it is
		 * ignored
		 */
		public void addAuthority(GrantedAuthority a) {
			if (!hasAuthority(a)) {
				mutableAuthorities.add(a);
			}
		}

		private boolean hasAuthority(GrantedAuthority a) {
			for (GrantedAuthority authority : mutableAuthorities) {
				if (authority.equals(a)) {
					return true;
				}
			}
			return false;
		}

		public LdapUserDetails createUserDetails() {
			Assert.notNull(instance,
					"Essence can only be used to create a single instance");
			Assert.notNull(instance.username, "username must not be null");
			Assert.notNull(instance.getDn(), "Distinguished name must not be null");

			instance.authorities = Collections.unmodifiableList(mutableAuthorities);

			LdapUserDetails newInstance = instance;

			instance = null;

			return newInstance;
		}

		public Collection<GrantedAuthority> getGrantedAuthorities() {
			return mutableAuthorities;
		}

		public void setAccountNonExpired(boolean accountNonExpired) {
			instance.accountNonExpired = accountNonExpired;
		}

		public void setAccountNonLocked(boolean accountNonLocked) {
			instance.accountNonLocked = accountNonLocked;
		}

		public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
			mutableAuthorities = new ArrayList<>();
			mutableAuthorities.addAll(authorities);
		}

		public void setCredentialsNonExpired(boolean credentialsNonExpired) {
			instance.credentialsNonExpired = credentialsNonExpired;
		}

		public void setDn(String dn) {
			instance.dn = dn;
		}

		public void setDn(Name dn) {
			instance.dn = dn.toString();
		}

		public void setEnabled(boolean enabled) {
			instance.enabled = enabled;
		}

		public void setPassword(String password) {
			instance.password = password;
		}

		public void setUsername(String username) {
			instance.username = username;
		}

		public void setTimeBeforeExpiration(int timeBeforeExpiration) {
			instance.timeBeforeExpiration = timeBeforeExpiration;
		}

		public void setGraceLoginsRemaining(int graceLoginsRemaining) {
			instance.graceLoginsRemaining = graceLoginsRemaining;
		}
	}
}
