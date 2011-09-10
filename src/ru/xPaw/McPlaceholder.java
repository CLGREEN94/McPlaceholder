package ru.xPaw;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class McPlaceholder
{
	private static final Logger log = Logger.getLogger( "McPlaceholder" );
	private static ServerSocket serve;
	private static String disconnectReason;
	private static String motd;
	
	public static void main( String[ ] args )
	{
		// Set log formatter
		ConsoleLogFormatter formatter = new ConsoleLogFormatter( );
		ConsoleHandler handler = new ConsoleHandler( );
		handler.setFormatter( formatter );
		
		log.setUseParentHandlers( false );
		log.addHandler( handler );
		
		log.info( "Minecraft Placeholder v1.0 by xPaw" );
		
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
					System.exit( 0 );
				}
				
				motd = arg;
			}
			else if( arg.equalsIgnoreCase( "-message" ) )
			{
				arg = (String)arguments.pop( );
				
				if( arg.length( ) > 80 )
				{
					log.warning( "Message is too long. It cannot exceed 80 characters." );
					System.exit( 0 );
				}
				
				disconnectReason = arg;
			}
			else
			{
				log.warning( "Wrong parameter: " + arg );
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
		Runtime.getRuntime().addShutdownHook( new Thread( )
		{
			public void run( )
			{
				closeServe( );
			}
		} );
		
		try
		{
			startServe( port, ip == null ? InetAddress.getLocalHost() : InetAddress.getByName( ip ) );
		}
		catch( Exception e )
		{
			log.severe( "Exception: " + e.getMessage( ) );
			System.exit( 0 );
		}
	}
	
	private static void startServe( int port, InetAddress addr )
	{
		try
		{
			log.info( "Starting fake server on " + addr.getHostAddress( ) + ":" + port + " ..." );
			serve = new ServerSocket( port, 10, addr );
			
			while( !serve.isClosed( ) )
			{
				new McPlaceholder.ResponderThread( serve.accept( ) ).start( );
			}
		}
		catch( BindException e )
		{
			log.severe( "Can't bind port " + port + ": " + e.getMessage( ) );
			
			System.exit( 0 );
		}
		catch( Exception e )
		{
			log.severe( "Exception: " + e.getMessage( ) );
			
			System.exit( 0 );
		}
		
		closeServe( );
	}
	
	private static void closeServe( )
	{
		if( serve == null )
		{
			return;
		}
		
		log.info( "Stopping fake server ..." );
		
		try
		{
			serve.close( );
		}
		catch( Exception e )
		{
			log.severe( "Failed to stop serve: " + e.getMessage( ) );
		}
	}
	
	private static class ResponderThread extends Thread
	{
		private String ip;
		private DataInputStream in;
		private DataOutputStream out;
		
		public ResponderThread( Socket s )
		{
			try
			{
				ip  = s.getInetAddress().getHostAddress();
				in  = new DataInputStream( s.getInputStream() );
				out = new DataOutputStream( s.getOutputStream() );
				
				s.setSoTimeout( 2000 ); // 2s should be enough, right?
			}
			catch( Exception e )
			{
				log.warning( "Exception: " + e.getMessage( ) );
			}
		}
		
		public void run( )
		{
			try
			{
				int i = in.read( );
				
				if( i == 254 ) // Query
				{
					String answer = motd + "\u00A71\u00A71";
					
					out.writeByte( 255 );
					out.writeShort( answer.length( ) );
					out.writeChars( answer );
				}
				else if( i == 2 ) // Handshake
				{
					i = in.readShort( );
					
					if( i < 0 || i > 32 )
					{
						throw new Exception( "Not valid length of client name (" + i + ")" );
					}
					
					StringBuilder name = new StringBuilder( );
					
					while( i-- > 0 )
					{
						name.append( in.readChar( ) );
					}
					
					log.info( "Client nickname: " + name.toString( ) + " (" + ip + ")" );
					
					// Send disconnect packet
					out.writeByte( 255 );
					out.writeShort( McPlaceholder.disconnectReason.length( ) );
					out.writeChars( McPlaceholder.disconnectReason );
				}
				else
				{
					throw new Exception( "Unknown packet (" + i + ")" );
				}
			}
			catch( Exception e )
			{
				log.warning( "Exception: " + e.getMessage( ) + " (" + ip + ")" );
			}
			
			closeSocket( );
		}
		
		private final void closeSocket( )
		{
			try
			{
				in.close( );
				out.close( );
			}
			catch( Exception e )
			{
				log.severe( "Failed to close socket: " + e.getMessage() );
			}
			
			Thread.currentThread( ).interrupt( );
		}
	}
	
	private static class ConsoleLogFormatter extends Formatter
	{
		private SimpleDateFormat a = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		
		public String format( LogRecord logRecord )
		{
			StringBuilder buffer = new StringBuilder( );
			
			buffer.append( a.format( Long.valueOf( logRecord.getMillis( ) ) ) );
			
			buffer.append( " [" + logRecord.getLevel( ) + "] " );
			buffer.append( logRecord.getMessage( ) + '\n' );
			
			return buffer.toString( );
		}
	}
}
