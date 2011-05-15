import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public class McPlaceholder
{
	private static final Logger log = Logger.getLogger( "McPlaceholder" );
	private static ServerSocket serve;
	private static String disconnectReason;
	
	public static void main( String[] args )
	{
		// Set log formatter
		ConsoleLogFormatter formatter = new ConsoleLogFormatter();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter( formatter );
		
		log.setUseParentHandlers( false );
		log.addHandler( handler );
		
		log.info( "Minecraft Placeholder v1.0 by xPaw" );
		
		// Parse launch parameters
		Integer port = 25565;
		
		LinkedList<String> arguments = new LinkedList<String>();
		Collections.addAll( arguments, args );
		
		while( !arguments.isEmpty() )
		{
			String arg = (String)arguments.pop();
			
			if( arg.equalsIgnoreCase( "-p" ) )
			{
				port = Integer.parseInt( (String)arguments.pop() );
			}
			else if( arg.equalsIgnoreCase( "-m" ) )
			{
				arg = (String)arguments.pop();
				
				if( arg.length() > 80 )
				{
					log.warning( "Message is too long. It cannot exceed 80 characters." );
					System.exit( -1 );
				}
				
				disconnectReason = arg;
			}
			else
			{
				log.warning( "Wrong parameter: " + arg );
			}
		}
		
		// See if its legit reason
		if( disconnectReason == null || disconnectReason.isEmpty() )
			disconnectReason = "&cServer is down for maintenance!";
		
		// Replace colors
		disconnectReason = disconnectReason.replaceAll( "(&([a-f0-9]))", "\u00A7$2" );
		
		// Catch program shutdown
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			public void run( )
			{
				closeServe();
			}
		} );
		
		startServe( port );
	}
	
	private static void startServe( int port )
	{
		try
		{
			log.info( "Starting fake server on port " + port + " ..." );
			serve = new ServerSocket( port );
			
			while( !serve.isClosed() )
			{
				try
				{
					new McPlaceholder.ResponderThread( serve.accept() ).start();
				}
				catch( IOException e )
				{
					log.info( "Failed to start thread: " + e.getMessage() );
				}
			}
			
			closeServe();
		}
		catch( BindException e )
		{
			log.severe( "Can't bind port " + port + ": " + e.getMessage() );
			
			System.exit( -1 );
		}
		catch( IOException e )
		{
			log.severe( e.getMessage() );
			
			System.exit( -1 );
		}
		finally
		{
			closeServe();
		}
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
			serve.close();
		}
		catch( IOException e )
		{
			log.severe( "Failed to stop serve: " + e.getMessage() );
		}
	}
	
	private static class ResponderThread extends Thread
	{
		private Socket socket;
		private DataInputStream in;
		private DataOutputStream out;
		
		public ResponderThread( Socket s )
		{
			socket = s;
			
			try
			{
				in  = new DataInputStream( s.getInputStream() );
				out = new DataOutputStream( s.getOutputStream() );
				
				s.setSoTimeout( 2000 ); // 2s should be enough, right?
			}
			catch( IOException e )
			{
				log.severe( "Failed to make stream: " + e.getMessage() );
			}
			
			log.info( "New connection: " + s.getInetAddress().getHostAddress() );
		}
		
		public void run( )
		{
			try
			{
				int i = in.read();
				
				if( i == -1 )
				{
					throw new IOException( "Nothing was sent" );
				}
				else if( i != 2 )
				{
					throw new IOException( "Not a handshake packet (" + i +")" );
				}
				
				i = in.readShort();
				
				if( i < 0 || i > 32 )
				{
					throw new IOException( "Not valid length of client name (" + i + ")" );
				}
				
				StringBuilder name = new StringBuilder();
				
				while( i-- > 0 )
				{
					name.append( in.readChar() );
				}
				
				log.info( "Client nickname: " + name.toString() );
				
				// Send disconnect packet
				out.writeByte( 255 );
				out.writeShort( McPlaceholder.disconnectReason.length() );
				out.writeChars( McPlaceholder.disconnectReason );
			}
			catch( EOFException e )
			{
				log.severe( "Exception: Reached end of stream" );
				e.printStackTrace();
			}
			catch( IOException e )
			{
				log.info( "Exception: " + e.getMessage() );
			}
			
			closeSocket();
		}
		
		private final void closeSocket( )
		{
			try
			{
				if( socket != null )
				{
					in.close();
					out.close();
					socket.close();
					socket = null;
				}
			}
			catch( IOException e )
			{
				log.severe( "Failed to close socket: " + e.getMessage() );
			}
			
			Thread.currentThread().interrupt();
		}
	}
}
