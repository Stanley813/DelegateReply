package cn.sh.ideal.model;


public class LoginRes {
	
	private String success;
    private String username;
    private String pwd;
    private String emptyAuthcode;
    private String _t;
	/**
	 * @return the success
	 */
	public String getSuccess() {
		return success;
	}
	/**
	 * @param success the success to set
	 */
	public void setSuccess(String success) {
		this.success = success;
	}
	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}
	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	/**
	 * @return the pwd
	 */
	public String getPwd() {
		return pwd;
	}
	/**
	 * @param pwd the pwd to set
	 */
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}
	/**
	 * @return the emptyAuthcode
	 */
	public String getEmptyAuthcode() {
		return emptyAuthcode;
	}
	/**
	 * @param emptyAuthcode the emptyAuthcode to set
	 */
	public void setEmptyAuthcode(String emptyAuthcode) {
		this.emptyAuthcode = emptyAuthcode;
	}
    
    
}
