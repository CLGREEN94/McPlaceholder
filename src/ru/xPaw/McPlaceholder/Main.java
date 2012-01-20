package ru.xPaw.McPlaceholder;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public class Main
{
	protected static final Logger log = Logger.getLogger( "McPlaceholder" );
	protected static String disconnectReason;
	protected static String motd;
	private static ServerSocket server;
	
	public static void main( String[ ] args )
	{
		// Set log formatter
		ConsoleLogFormatter formatter = new ConsoleLogFormatter( );
		ConsoleHandler handler = new ConsoleHandler( );
		handler.setFormatter( formatter );
		
		log.setUseParentHandlers( false );
		log.addHandler( handler );
		
		log.info( "Minecraft Placeholder v1.1 by xPaw" );
		
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
			motd = "Server is down for maintenance!";
		}
		
		if( disconnectReason == null || disconnectReason.isEmpty( ) )
		{
			disconnectReason = "&cServer is down for maintenance!";
		}
		
		// Replace colors
		disconnectReason = disconnectReason.replaceAll( "(&([a-f0-9]))", "\u00A7$2" );
		motd = motd.replaceAll( "\u00A7", "" );
		
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
		
		log.info( "Stopping fake server ..." );
		
		try
		{
			server.close( );
		}
		catch( Exception e )
		{
			log.severe( "Failed to stop server: " + e.getMessage( ) );
		}
	}
}
