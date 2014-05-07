package client;

public interface SessionReliableCallback {
	public void onSuccess();
	public void onFailure(String error);
}
