package client;

public interface SessionReliableCallback {
	public void onExpire(long timeDelayed, long TTL);
	public void onAck(long timeDelayed, long TTL);
	public void onRetry(long timeDelayed, long TTL);
}
