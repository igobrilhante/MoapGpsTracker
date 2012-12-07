package org.moap.gpstracker.oauth;

public interface IOAuthCredentials {

	public String getClientID();
	public String getClientSecret();
	public String getCallback();
}
