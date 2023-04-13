package ca.arnah.runelite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.HashMap;

public class SimpleClassLoader extends ClassLoader
{
	public HashMap<String, InputStream> resources = new HashMap<>();
	public SimpleClassLoader(ClassLoader parent)
	{
		super(parent);
	}

	public Class<?> loadClass(String name, byte[] bytes)
	{
		try
		{
			return this.getParent().loadClass(name);
		}
		catch (ClassNotFoundException | NoClassDefFoundError e)
		{
			return lookupClass(name, bytes);
		}
	}
	@Override
	public InputStream getResourceAsStream(String name)
	{
		if (resources.containsKey(name)) {
			return resources.get(name);
		}
		return super.getResourceAsStream(name);
	}

	public Class lookupClass(String name, byte[] bytes)
	{
		Permissions perms = new Permissions();
		perms.add(new AllPermission());
		final ProtectionDomain protDomain =
				new ProtectionDomain(getClass().getProtectionDomain().getCodeSource(), perms,
						this,
						getClass().getProtectionDomain().getPrincipals());

		try
		{
			return defineClass(name, bytes, 0, bytes.length, protDomain);
		}
		catch (ClassFormatError | NoClassDefFoundError | VerifyError ex)
		{
			return null;
		}
		catch (LinkageError ex)
		{
			return null;
		}
	}
	public static byte[] getBytes(InputStream is) throws IOException
	{
		try (ByteArrayOutputStream os = new ByteArrayOutputStream();)
		{
			byte[] buffer = new byte[0xFFFF];
			for (int len; (len = is.read(buffer)) != -1;)
				os.write(buffer, 0, len);
			os.flush();
			return os.toByteArray();
		}
	}
}
