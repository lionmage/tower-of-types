/*
 * The MIT License
 *
 * Copyright © 2019 Robert Poole <Tarquin.AZ@gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package tungsten.types.util;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A singleton class serving as a one-stop-shop for monitoring files for
 * changes.  For example, the backing file for a {@link BigMatrix} can be
 * watched here, and the {@code BigMatrix} can be notified if the backing
 * file has been changed by another process.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class FileMonitor {
    public static final String BASEDIR_PROP = FileMonitor.class.getName() + ".basedir";
    private static final FileMonitor instance = new FileMonitor();
    private ExecutorService exec;
    private Path basePath;
    
    protected FileMonitor() {
        String defaultDir = System.getProperty("user.dir"); // User working directory
        if (defaultDir == null || defaultDir.trim().isEmpty()) {
            defaultDir = System.getProperty("user.home");  // User home directory
        }
        File baseDir = new File(System.getProperty(BASEDIR_PROP, defaultDir));
        exec = Executors.newSingleThreadExecutor();
    }
    
    public static FileMonitor getInstance() {
        return instance;
    }
    
    public void monitorFile(File file, Runnable callback) {
        Path filepath = file.toPath();
    }
}
