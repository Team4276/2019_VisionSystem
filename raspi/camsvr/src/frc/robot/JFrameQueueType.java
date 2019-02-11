package frc.robot;

public enum JFrameQueueType {
	UNKNOWN, WAIT_FOR_BLOB_DETECT, WAIT_FOR_TEXT_CLIENT, WAIT_FOR_BROWSER_CLIENT, FREE;

	private final int value;

	private JFrameQueueType() {
		this.value = 0;
	}

	JFrameQueueType(int value) {
		this.value = value;
	}

	public static JFrameQueueType fromInt(int val) {
		return JFrameQueueType.values()[val];
	}

	public int toInt() {
		return value;
	}

	String queueTypeToText(JFrameQueueType val) {
		switch (val) {
		case UNKNOWN:
			return "FRAME_QUEUE_TYPE.UNKNOWN";

		case WAIT_FOR_BLOB_DETECT:
			return "FRAME_QUEUE_TYPE.WAIT_FOR_BLOB_DETECT";

		case WAIT_FOR_TEXT_CLIENT:
			return "FRAME_QUEUE_TYPE.WAIT_FOR_TEXT_CLIENT";

		case WAIT_FOR_BROWSER_CLIENT:
			return "FRAME_QUEUE_TYPE.WAIT_FOR_BROWSER_CLIENT";

		case FREE:
			return "FRAME_QUEUE_TYPE.FREE";

		default:
			break;
		}
		return "***";
	}
}
