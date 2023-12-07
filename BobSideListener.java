
public class BobSideListener implements Runnable {
	private Bob bob;

	public BobSideListener(Bob bob) {
		this.bob = bob;
	}

	@Override
	public void run() {
		while (true) {
			bob.receiveData();
		}
	}
}
