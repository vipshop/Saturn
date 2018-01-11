package com.vip.saturn.job.console.domain.container;

/**
 * @author hebelala
 */
public class ContainerToken {

	private String userName;
	private String password;

	public ContainerToken() {
	}

	public ContainerToken(String userName, String password) {
		this.userName = userName;
		this.password = password;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ContainerToken that = (ContainerToken) o;

		if (userName != null ? !userName.equals(that.userName) : that.userName != null) {
			return false;
		}
		return password != null ? password.equals(that.password) : that.password == null;
	}

	@Override
	public int hashCode() {
		int result = userName != null ? userName.hashCode() : 0;
		result = 31 * result + (password != null ? password.hashCode() : 0);
		return result;
	}
}
