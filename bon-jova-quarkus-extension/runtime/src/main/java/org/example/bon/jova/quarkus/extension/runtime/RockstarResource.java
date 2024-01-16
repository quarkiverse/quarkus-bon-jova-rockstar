package org.example.bon.jova.quarkus.extension.runtime;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

@Path("/rockstar")
public class RockstarResource {
    @GET
    @Path("/{programName}")
    public Response runRockstarProgram(@PathParam("programName") String programName, @QueryParam("arg") List<String> args) {
        try {
            // We support both "hello-world" and "hello-world.rock" as 'programName'.
            String output = runClassCapturingSystemOut(FilenameUtils.removeExtension(programName), args.toArray(new String[0]));
            return Response.ok().entity(output).build();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            return Response.status(NOT_FOUND).entity(e).build();
        }
    }

    private String runClassCapturingSystemOut(String className, String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        var baos = new ByteArrayOutputStream();
        var ps = new PrintStream(baos);

        var systemOut = System.out;
        System.setOut(ps);

        Class<?> rockstarClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        Method main = rockstarClass.getMethod("main", String[].class);

        main.invoke(null, new Object[]{args});

        System.out.flush();
        System.setOut(systemOut);

        return baos.toString();
    }
}
