package org.example.bon.jova.quarkus.extension.runtime;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

@Path("/rockstar")
public class RockstarResource {
    @GET
    @Path("/{programName}")
    public Response runRockstarProgram(@PathParam("programName") String programName) {
        try {
            // We support both "hello-world" and "hello-world.rock".
            String output = runClassCapturingSystemOut(FilenameUtils.removeExtension(programName));
            return Response.ok().entity(output).build();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            return Response.status(NOT_FOUND).entity(e).build();
        }
    }

    private String runClassCapturingSystemOut(String className) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        var baos = new ByteArrayOutputStream();
        var ps = new PrintStream(baos);

        var systemOut = System.out;
        System.setOut(ps);

        Class<?> rockstarClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        Method main = rockstarClass.getMethod("main", String[].class);

        // TODO: support passing actual arguments via the REST endpoint.
        final Object[] args = new Object[1];
        args[0] = new String[] { "1", "2"};

        main.invoke(null, args);

        System.out.flush();
        System.setOut(systemOut);

        return baos.toString();
    }
}
