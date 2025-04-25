package util;

import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogEntity {
	
	private static final Logger jl = Logger.getLogger(LogEntity.class.getSimpleName());
	private static final ConsoleHandler jlCon = new ConsoleHandler();
	
	public static void initLogConfig() {
		Locale.setDefault(new Locale("en", "EN"));
		jl.setUseParentHandlers(false);
		jl.setLevel(Level.ALL);
		jlCon.setLevel(Level.ALL);
		jlCon.setFormatter(new LogFormatter());
		jl.addHandler(jlCon);
	}
}
