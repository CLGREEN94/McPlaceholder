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
				i = in.available( ) > 0 ? in.read( ) : 0;
				
				String answer = "";
				
				if( i == 1 )
				{
					answer = Main.COLOR_CHAR + "1" + Main.NULL_BYTE + "1" + Main.NULL_BYTE + Main.versionString + Main.NULL_BYTE + Main.motd + Main.NULL_BYTE + 1 + Main.NULL_BYTE + 1;
				}
				else
				{
					answer = Main.motdStripped + Main.COLOR_CHAR + 1 + Main.COLOR_CHAR + 1;
				}
				
				// Send server list response
				out.writeByte( 255 );
				out.writeShort( answer.length( ) );
				out.writeChars( answer );
			}
			else if( i == 2 ) // Handshake
			{
				Main.log.info( "Received connection from " + ip );
				
				// Send disconnect packet
				out.writeByte( 255 );
				out.writeShort( Main.disconnectReason.length( ) );
				out.writeChars( Main.disconnectReason );
			}
			else
			{
				throw new Exception( "Unknown packet (" + i + ")" );
			}
		}
		catch( Exception e )
		{
			Main.log.warning( "Exception: " + e.getMessage( ) + " (" + ip + ")" );
		}
		finally
		{
			closeSocket( );
		}
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
