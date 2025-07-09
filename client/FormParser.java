class FormField {
	String name;
	byte content[];
	String taskType;
}

class FileFormField extends FormField {
	String contentType;
	String filename;
}

class FormData {
	FormField fields[];
}

public abstract class FormParser {
	abstract FormData getFormData(String contentType, byte payload[]);
}