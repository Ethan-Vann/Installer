package ca.arnah.runelite;

import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;

public class SimpleClassLoader extends ClassLoader
{
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
		catch (ClassFormatError | NoClassDefFoundError|VerifyError ex)
		{
			return null;
		}
	}
}
