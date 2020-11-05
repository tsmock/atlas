package org.openstreetmap.atlas.streaming;

import java.io.Closeable;
import java.io.Flushable;

import org.openstreetmap.atlas.exception.CoreException;

/**
 * Stream utility
 *
 * @author matthieun
 */
public final class Streams
{
    /** An error message for when the stream could not be closed */
    public static final String COULD_NOT_CLOSE_STREAM = "Could not close stream";

    /**
     * Safe close of a {@link Closeable} item.
     *
     * @param stream
     *            The stream to close.
     * @deprecated Use try-with-resources instead.
     */
    @Deprecated(since = "6.3.3", forRemoval = true)
    public static void close(final Closeable stream)
    {
        if (stream == null)
        {
            return;
        }
        try
        {
            stream.close();
        }
        catch (final Exception e)
        {
            throw new CoreException(COULD_NOT_CLOSE_STREAM, e);
        }
    }

    /**
     * Safe flush of a {@link Flushable} item.
     *
     * @param stream
     *            The stream to flush.
     */
    public static void flush(final Flushable stream)
    {
        if (stream == null)
        {
            return;
        }
        try
        {
            stream.flush();
        }
        catch (final Exception e)
        {
            throw new CoreException("Could not flush stream", e);
        }
    }

    private Streams()
    {
    }
}
