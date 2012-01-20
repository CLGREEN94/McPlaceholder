package ru.xPaw.McPlaceholder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ResponderThread extends Thread
{
	private String ip;
	private DataInputStream in;
	private DataOutputStream out;
	
	public ResponderThread( Socket s )
	{
		try
		{
			ip  = s.getInetAddress( ).getHostAddress( );
			in  = new DataInputStream( s.getInputStream() );
			out = new DataOutputStream( s.getOutputStream() );
			
			s.setSoTimeout( 2000 ); // 2s should be enough, right?
		}
		catch( Exception e )
		{
			Main.log.warning( "Exception: " + e.getMessage( ) );
		}
	}
	
	public void run( )
	{
		try
		{
			int i = in.read( );
			
			if( i == 254 ) // Query
			{
				String answer = Main.motd + "\u00A71\u00A71";
				
				out.writeByte( 255 );
				out.writeShort( answer.length( ) );
				out.writeChars( answer );
			}
			else if( i == 2 ) // Handshake
			{
				i = in.readShort( );
				
				if( i < 0 || i > 32 )
				{
					//throw new Exception( "Not valid length of client name (" + i + ")" );
					return;
				}
				
				StringBuilder name = new StringBuilder( );
				
				while( i-- > 0 )
				{
					name.append( in.readChar( ) );
				}
				
				Main.log.info( "Client nickname: " + name.toString( ) + " (" + ip + ")" );
				
				// Send disconnect packet
				out.writeByte( 255 );
				out.writeShort( Main.disconnectReason.length( ) );
				out.writeChars( Main.disconnectReason );
			}
			/*else
			{
				throw new Exception( "Unknown packet (" + i + ")" );
			}*/
		}
		catch( Exception e )
		{
			Main.log.warning( "Exception: " + e.getMessage( ) + " (" + ip + ")" );
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
			Main.log.severe( "Failed to close socket: " + e.getMessage() );
		}
		
		Thread.currentThread( ).interrupt( );
	}
}
