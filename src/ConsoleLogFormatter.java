import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class ConsoleLogFormatter extends Formatter
{
	private SimpleDateFormat a = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
	
	public String format( LogRecord logRecord )
	{
		StringBuilder buffer = new StringBuilder();
		
		buffer.append( a.format( Long.valueOf( logRecord.getMillis() ) ) );
		
		buffer.append( " [" + logRecord.getLevel() + "] " );
		buffer.append( logRecord.getMessage() + '\n' );
		
		Throwable localThrowable = logRecord.getThrown();
		
		if( localThrowable != null )
		{
			StringWriter localStringWriter = new StringWriter();
			localThrowable.printStackTrace( new PrintWriter( localStringWriter ) );
			buffer.append( localStringWriter.toString() );
		}
		
		return buffer.toString();
	}
}
