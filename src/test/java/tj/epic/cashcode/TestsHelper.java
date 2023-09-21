package tj.epic.cashcode;

import java.lang.reflect.Field;

public class TestsHelper {
	public static void changePrivateFieldValue(Object target, String fieldName, Object newValue) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, newValue);
		} catch (Exception ignored) {}
	}

	public static Object getPrivateFieldValue(Object target, String fieldName) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(target);
	}
}
