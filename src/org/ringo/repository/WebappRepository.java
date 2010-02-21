package org.ringo.repository;

import javax.servlet.ServletContext;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

public class WebappRepository extends AbstractRepository {

    ServletContext context;

    long timestamp;
    private int exists = -1;

    public WebappRepository(ServletContext context, String path) {
        this.context = context;
        this.parent = null;
        if (path == null) {
            path = "/";
        } else if (!path.endsWith("/")) {
            path = path + "/";
        }
        this.path = path;
        this.name = path;
        this.timestamp = System.currentTimeMillis();
    }

    protected WebappRepository(ServletContext context, WebappRepository parent, String name) {
        this.context = context;
        this.parent = parent;
        this.name = name;
        this.path = parent.path + name + "/";
        this.timestamp = parent.timestamp;
    }

    public long getChecksum() {
        return timestamp;
    }

    public long lastModified() {
        return timestamp;
    }

    public boolean exists() {
        if (exists < 0) {
            if ("/".equals(path)) {
                exists = 1;
            } else {
                Set paths = context.getResourcePaths(path);
                exists = (paths != null && !paths.isEmpty()) ? 1 : 0;
            }
        }
        return exists == 1;
    }

    public Repository getChildRepository(String name) {
        if (".".equals(name)) {
            return this;
        } else if ("..".equals(name)) {
            return getParentRepository();
        }
        AbstractRepository repo = repositories.get(name);
        if (repo == null) {
            repo = new WebappRepository(context, this, name);
            repositories.put(name, repo);
        }
        return repo;
    }

    public URL getUrl() throws MalformedURLException {
        return context.getResource(path);
    }

    @Override
    protected Resource lookupResource(String name) {
        AbstractResource res = resources.get(name);
        if (res == null) {
            res = new WebappResource(context, this, name);
            resources.put(name, res);
        }
        return res;
    }

    protected void getResources(List<Resource> list, boolean recursive)
            throws IOException {
        Set paths = context.getResourcePaths(path);

        if (paths != null) {
            for (Object obj: paths) {
                String path = (String) obj;
                if (!path.endsWith("/")) {
                    int n = path.lastIndexOf('/', path.length() - 1);
                    String name = path.substring(n + 1);
                    list.add(lookupResource(name));
                } else if (recursive) {
                    int n = path.lastIndexOf('/', path.length() - 2);
                    String name = path.substring(n + 1, path.length() - 1);
                    AbstractRepository repo = (AbstractRepository) getChildRepository(name);
                    repo.getResources(list, true);
                }
            }
        }
    }

    public Repository[] getRepositories() throws IOException {
        Set paths = context.getResourcePaths(path);
        List<Repository> list = new ArrayList<Repository>();

        if (paths != null) {
            for (Object obj: paths) {
                String path = (String) obj;
                if (path.endsWith("/")) {
                    int n = path.lastIndexOf('/', path.length() - 2);
                    String name = path.substring(n + 1, path.length() - 1);
                    list.add(getChildRepository(name));
                }
            }
        }
        return list.toArray(new Repository[list.size()]);
    }

    @Override
    public String toString() {
        return "WebappRepository[" + path + "]";
    }


    @Override
    public int hashCode() {
        return 5 + path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WebappRepository && path.equals(((WebappRepository)obj).path);
    }

}