package io.vanillabp.cockpit.commons.rest.adapter;

public class Authentication {

	private boolean basic = false;
	
    private OauthAuthentication oauth;

	private String username;
	
	private String password;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public boolean isBasic() {
		return basic;
	}
	
	public void setBasic(boolean basic) {
		this.basic = basic;
	}

    public OauthAuthentication getOauth() {
        return oauth;
    }

    public void setOauth(OauthAuthentication oauth) {
        this.oauth = oauth;
    }

}
