package ca.arnah.runelite;

import javax.inject.Inject;

import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ExternalPluginsChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.SplashScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Arnah
 * @since Nov 07, 2020
 */
public class HijackedClient
{

	@Inject
	private PluginManager pluginManager;
	private static final Logger logger = LoggerFactory.getLogger(HijackedClient.class);
	@Inject
	EventBus eventBus;

	public void start()
	{
		System.out.println("Start");
		new Thread(() ->
		{
			while (SplashScreen.isOpen())
			{
				try
				{
					Thread.sleep(100);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
			System.out.println("Splash Screen done");
				SimpleClassLoader classLoader = new SimpleClassLoader(getClass().getClassLoader());
				ArrayList<Path> result = new ArrayList<Path>();
				try (Stream<Path> walk = Files.walk(RuneLite.RUNELITE_DIR.toPath().resolve("externalPlugins")))
				{
					result.addAll(walk.filter(x -> x.getFileName().toString().endsWith(".jar"))
							.collect(Collectors.toList()));
				}catch (Exception ex)
				{
					ex.printStackTrace();
				}
				try (Stream<Path> walk = Files.walk(RuneLite.RUNELITE_DIR.toPath().resolve("sideloaded-plugins")))
				{
					result.addAll(walk.filter(x -> x.getFileName().toString().endsWith(".jar"))
							.collect(Collectors.toList()));
				}catch (Exception ex)
				{
					ex.printStackTrace();
				}
				List<Class<?>> loadedPlugins = new ArrayList<>();
				for (Path path : result)
				{
					JarFile jarFile;
					try
					{
						jarFile = new JarFile(path.toFile());
					}
					catch (IOException e)
					{
						throw new RuntimeException(e);
					}
					for (Enumeration<? extends JarEntry> list = jarFile.entries(); list.hasMoreElements(); )
					{
						JarEntry entry = list.nextElement();
						if (entry.isDirectory() || !entry.getName().endsWith(".class") || entry.getName().contains("$"))
						{
							continue;
						}
						String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
						String loadableClassName = className.replace('/', '.');
						InputStream is;
						try
						{
							is = jarFile.getInputStream(entry);
						}
						catch (IOException e)
						{
							throw new RuntimeException(e);
						}
						byte[] byteArr;
						try
						{
							byteArr = is.readAllBytes();
						}
						catch (IOException e)
						{
							throw new RuntimeException(e);
						}
						Class<?> loadedClass = null;
						try
						{
							 loadedClass= classLoader.loadClass(loadableClassName, byteArr);
						}catch (Exception ex)
						{
							//todomaybe
						}
						if (loadedClass == null || loadedClass.getSuperclass() == null) continue;
						if (loadedClass.getSuperclass().getSimpleName().equals("Plugin"))
						{
							logger.info("loading plugin: " + loadableClassName);
							loadedPlugins.add(loadedClass);
						}
					}
				}
			List<Plugin> pluginList;
			try
			{
				pluginList = pluginManager.loadPlugins(loadedPlugins, null);
			}
			catch (PluginInstantiationException e)
			{
				throw new RuntimeException(e);
			}
			pluginManager.loadDefaultPluginConfiguration(pluginList);
				ExternalPluginsChanged event = new ExternalPluginsChanged(new ArrayList<>());
				eventBus.post(event);
		}).start();
	}
}