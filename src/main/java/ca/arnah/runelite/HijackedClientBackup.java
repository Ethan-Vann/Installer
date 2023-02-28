package ca.arnah.runelite;

import javax.inject.Inject;
import javax.swing.*;

import com.google.common.io.ByteStreams;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Arnah
 * @since Nov 07, 2020
 */
public class HijackedClientBackup
{

	@Inject
	PluginManager pluginManager;
	@Inject
	EventBus eventBus;

	public void start()
	{
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

			try
			{
				SimpleClassLoader simpleLoader = new SimpleClassLoader(getClass().getClassLoader());
				List<Path> jarPaths = findJars();
				List<Class<?>> toLoad = new ArrayList<>();
				List<ClassByte> classes = new ArrayList<>();
				for (Path jarPath : jarPaths)
				{
					classes.addAll(listFilesInJar(jarPath));
				}
				for (ClassByte aClass : classes)
				{
					Class<?> loaded = simpleLoader.loadClass(aClass.name, aClass.bytes);
					if (loaded != null && loaded.getSuperclass() != null && loaded.getSuperclass().equals(Plugin.class))
					{
						toLoad.add(loaded);
					}
				}
				List<Plugin> loaded = pluginManager.loadPlugins(toLoad, null);
				loaded = loaded.stream().filter(Objects::nonNull).collect(Collectors.toList());
				List<Plugin> finalLoaded = loaded;
				SwingUtilities.invokeAndWait(() ->
				{
					try
					{
						for (Plugin plugin : finalLoaded)
						{
							pluginManager.loadDefaultPluginConfiguration(Collections.singleton(plugin));
							pluginManager.startPlugin(plugin);
						}
					}
					catch (PluginInstantiationException e)
					{
						e.printStackTrace();
					}
				});
				eventBus.post(new ExternalPluginsChanged(new ArrayList<>()));
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			Logger logger = LoggerFactory.getLogger(HijackedClientBackup.class);
			logger.info("finished loading plugins");
		}).start();
	}

	public List<Path> findJars()
	{
		try
		{
			List<Path> files = new ArrayList<>();
			try (Stream<Path> walkable = Files.walk(RuneLite.RUNELITE_DIR.toPath().resolve("externalplugins")))
			{

				walkable.filter(Files::isRegularFile)
						.filter(f -> f.toString().endsWith(".jar"))
						.forEach(files::add);
			}
			try (Stream<Path> walkable = Files.walk(RuneLite.RUNELITE_DIR.toPath().resolve("sideloaded-plugins")))
			{
				walkable.filter(Files::isRegularFile)
						.filter(f -> f.toString().endsWith(".jar"))
						.forEach(files::add);
			}
			return files;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return new ArrayList<>();
	}

	public List<ClassByte> listFilesInJar(Path jarPath)
	{
		List<ClassByte> classes = new ArrayList<>();
		try (JarFile jarFile1 = new JarFile(jarPath.toFile()))
		{
			jarFile1.stream().forEach(jarEntry ->
			{
				if (jarEntry == null || jarEntry.isDirectory() || !jarEntry.getName().contains(".class")) return;
				try (InputStream inputStream = jarFile1.getInputStream(jarEntry))
				{
					classes.add(new ClassByte(ByteStreams.toByteArray(inputStream),
							jarEntry.getName().replace('/', '.').substring(0,
									jarEntry.getName().length() - 6)));
				}
				catch (IOException ioException)
				{
					System.out.println("Could not obtain class entry for " + jarEntry.getName());
				}
			});
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return classes;
	}
}