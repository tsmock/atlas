package org.openstreetmap.atlas.utilities.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.atlas.exception.CoreException;
import org.openstreetmap.atlas.streaming.SplittableInputStream;
import org.openstreetmap.atlas.streaming.Streams;
import org.openstreetmap.atlas.utilities.runtime.RunScriptMonitor.PrinterMonitor;
import org.openstreetmap.atlas.utilities.scalars.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author matthieun
 */
public final class RunScript
{
    private static final Logger logger = LoggerFactory.getLogger(RunScript.class);

    public static void run(final String command)
    {
        run(command.split("\\s+"));
    }

    public static void run(final String command, final List<RunScriptMonitor> monitors)
    {
        run(command.split("\\s+"), monitors);
    }

    public static void run(final String[] commandArray)
    {
        run(commandArray, new ArrayList<>());
    }

    public static void run(final String[] commandArray, final List<RunScriptMonitor> monitors)
    {
        int returnValue = 0;
        final String[] env = System.getenv().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.toList())
                .toArray(new String[0]);
        final PrinterMonitor printer = new PrinterMonitor(logger);
        final List<InputStream> otherStandardOuts = new ArrayList<>();
        final List<InputStream> otherStandardErrs = new ArrayList<>();
        try
        {
            final Process process = Runtime.getRuntime().exec(commandArray, env);
            try (SplittableInputStream standardOut = new SplittableInputStream(
                    process.getInputStream());
                    SplittableInputStream standardErr = new SplittableInputStream(
                            process.getErrorStream()))
            {
                if (monitors != null && !monitors.isEmpty())
                {
                    for (@SuppressWarnings("unused")
                    final RunScriptMonitor monitor : monitors)
                    {
                        otherStandardOuts.add(standardOut.split());
                        otherStandardErrs.add(standardErr.split());
                    }

                    // Launch the output monitors
                    printer.parse(standardOut, standardErr);
                    for (int index = 0; index < monitors.size(); index++)
                    {
                        final RunScriptMonitor monitor = monitors.get(index);
                        final InputStream otherStandardOut = otherStandardOuts.get(index);
                        final InputStream otherStandardErr = otherStandardErrs.get(index);
                        monitor.parse(otherStandardOut, otherStandardErr);
                    }
                }
                else
                {
                    printer.parse(process.getInputStream(), process.getErrorStream());
                }

                returnValue = process.waitFor();

                // Wait for the monitors
                printer.waitForCompletion(Duration.ONE_SECOND);

                if (monitors != null && !monitors.isEmpty())
                {
                    for (final RunScriptMonitor monitor : monitors)
                    {
                        monitor.waitForCompletion(Duration.ONE_SECOND);
                    }
                }
            }
        }
        catch (final Exception e)
        {
            throw new CoreException("Could not launch script \"{}\"", commandArray, e);
        }
        finally
        {
            for (final List<InputStream> inputStreamList : Arrays.asList(otherStandardOuts,
                    otherStandardErrs))
            {
                for (final InputStream inputStream : inputStreamList)
                {
                    try
                    {
                        inputStream.close();
                    }
                    catch (final IOException e)
                    {
                        logger.error(Streams.COULD_NOT_CLOSE_STREAM, e);
                    }
                }
            }
        }
        if (returnValue != 0)
        {
            throw new CoreException("Non-Zero return value {} when running script \"{}\".",
                    returnValue, commandArray);
        }
    }

    private RunScript()
    {
    }
}
