package ru.xPaw.McPlaceholder;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Main
{
	protected static final Logger log = Logger.getLogger( "McPlaceholder" );
	protected static String disconnectReason;
	protected static String versionString;
	protected static String motd;
	protected static String motdStripped;
	private static ServerSocket server;
	
	protected static final char COLOR_CHAR = '\u00A7';
	protected static final char NULL_BYTE  = '\u0000';
	
	public static void main( String[ ] args )
	{
		// Set log formatter
		ConsoleLogFormatter formatter = new ConsoleLogFormatter( );
		ConsoleHandler handler = new ConsoleHandler( );
		handler.setFormatter( formatter );
		
		log.setUseParentHandlers( false );
		log.addHandler( handler );
		
		log.info( "Minecraft Placeholder v1.2 by xPaw" );
		log.info( "View on GitHub: https://github.com/xPaw/McPlaceholder" );
		
		// Parse launch parameters
		Integer port = 25565;
		String ip = null;
		
		LinkedList<String> arguments = new LinkedList<String>( );
		Collections.addAll( arguments, args );
		
		while( !arguments.isEmpty( ) )
		{
			String arg = (String)arguments.pop( );
			
			if( arg.equalsIgnoreCase( "-ip" ) )
			{
				ip = (String)arguments.pop( );
			}
			else if( arg.equalsIgnoreCase( "-port" ) )
			{
				port = Integer.parseInt( (String)arguments.pop( ) );
			}
			else if( arg.equalsIgnoreCase( "-motd" ) )
			{
				arg = (String)arguments.pop( );
				
				if( arg.length( ) > 60 )
				{
					log.warning( "MOTD is too long. It cannot exceed 60 characters." );
					
					arg = arg.substring( 0, 60 );
				}
				
				motd = arg;
			}
			else if( arg.equalsIgnoreCase( "-version" ) )
			{
				arg = (String)arguments.pop( );
				
				if( arg.length( ) > 30 )
				{
					log.warning( "Version is too long. It cannot exceed 30 characters." );
					
					arg = arg.substring( 0, 30 );
				}
				
				versionString = arg;
			}
			else if( arg.equalsIgnoreCase( "-message" ) )
			{
				arg = (String)arguments.pop( );
				
				if( arg.length( ) > 80 )
				{
					log.warning( "Message is too long. It cannot exceed 80 characters." );
					
					arg = arg.substring( 0, 80 );
				}
				
				disconnectReason = arg;
			}
			else
			{
				log.warning( "Unknown parameter: " + arg );
			}
		}
		
		if( motd == null || motd.isEmpty( ) )
		{
			motd = "&4Server is down for maintenance!";
		}
		
		if( versionString == null || motd.isEmpty( ) )
		{
			versionString = "McPlaceholder";
		}
		
		if( disconnectReason == null || disconnectReason.isEmpty( ) )
		{
			disconnectReason = "&cServer is down for maintenance!";
		}
		
		// Replace colors
		disconnectReason = translateAlternateColorCodes( disconnectReason );
		versionString    = translateAlternateColorCodes( versionString );
		motd             = translateAlternateColorCodes( motd );
		
		// Make stripped version of motd
		motdStripped = Pattern.compile( "(?i)" + String.valueOf(COLOR_CHAR) + "[0-9A-FK-OR]" ).matcher( motd ).replaceAll( "" );
		
		// Catch program shutdown
		Runtime.getRuntime( ).addShutdownHook( new Thread( )
		{
			public void run( )
			{
				closeServer( );
			}
		} );
		
		try
		{
			startServer( port, ip == null ? InetAddress.getLocalHost() : InetAddress.getByName( ip ) );
		}
		catch( Exception e )
		{
			log.severe( "Exception: " + e.getMessage( ) );
			System.exit( 0 );
		}
	}
	
	private static void startServer( int port, InetAddress addr )
	{
		log.info( "Starting fake server on " + addr.getHostAddress( ) + ":" + port );
		
		try
		{
			server = new ServerSocket( port, 10, addr );
			
			while( !server.isClosed( ) )
			{
				new ResponderThread( server.accept( ) ).start( );
			}
		}
		catch( Exception e )
		{
			log.severe( "Exception: (Port " + port + ") " + e.getMessage( ) );
			
			System.exit( 0 );
		}
		
		closeServer( );
	}
	
	private static void closeServer( )
	{
		if( server == null )
		{
			return;
		}
		
		log.info( "Stopping fake server..." );
		
		try
		{
			server.close( );
		}
		catch( Exception e )
		{
			log.severe( "Failed to stop server: " + e.getMessage( ) );
		}
	}
	
	public static String translateAlternateColorCodes( String textToTranslate )
	{
		char[] b = textToTranslate.toCharArray( );
		
		for( int i = 0; i < b.length - 1; i++ )
		{
			if( b[ i ] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf( b[ i + 1 ] ) > -1 )
			{
				b[ i ] = COLOR_CHAR;
				b[ i + 1 ] = Character.toLowerCase( b[ i + 1 ] );
		    }
		}
		
		return new String( b );
	}
}
