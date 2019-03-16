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
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A singleton class serving as a one-stop-shop for monitoring files for
 * changes.  For example, the backing file for a {@link BigMatrix} can be
 * watched here, and the {@code BigMatrix} can be notified if the backing
 * file has been changed by another process.
 * 
 * During initialization, this class will look for a system variable identified
 * by the key found in {@link #BASEDIR_PROP} (public for the convenience of clients);
 * if defined, FileMonitor will monitor files in that directory by default
 * for deletion or modification.
 * 
 * If {@code tungsten.types.util.FileMonitor.basedir} is undefined, FileMonitor
 * will then look in the user's working directory, or, failing that, it will
 * default to the user's home directory.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class FileMonitor {
    public static final String BASEDIR_PROP = FileMonitor.class.getName() + ".basedir";
    private static final FileMonitor instance = new FileMonitor();
    private final ConcurrentHashMap<Path, List<File>> dirsToFiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<File, Runnable> callbackMap = new ConcurrentHashMap<>();
    private ExecutorService exec;
    private Path basePath;
    private WatchService watcher;
    
    protected FileMonitor() {
        String defaultDir = System.getProperty("user.dir"); // User working directory
        if (defaultDir == null || defaultDir.trim().isEmpty()) {
            defaultDir = System.getProperty("user.home");  // User home directory
        }
        File baseDir = new File(System.getProperty(BASEDIR_PROP, defaultDir));
        basePath = baseDir.toPath();
        try {
            watcher = FileSystems.getDefault().newWatchService();
            basePath.register(watcher, ENTRY_MODIFY, ENTRY_DELETE);
            dirsToFiles.put(basePath, new ArrayList<>());
        } catch (IOException ex) {
            Logger.getLogger(FileMonitor.class.getName()).log(Level.SEVERE, "Unable to initialize FileMonitor singleton.", ex);
            throw new IllegalStateException(ex);
        }
        exec = Executors.newSingleThreadExecutor();
        exec.submit(new WatchTask());
    }
    
    public static FileMonitor getInstance() {
        return instance;
    }
    
    public void monitorFile(File file, Runnable callback) {
        if (!file.isFile()) throw new IllegalArgumentException("File " + file.getName() + " is not normal.");
        
        final Path fileDir = file.getParentFile().toPath();
        if (!fileDir.startsWith(basePath)) {
            // I'm not sure if this is worthy of a warning, but probably at least a "good to know."
            Logger.getLogger(FileMonitor.class.getName()).log(Level.INFO, "File {} is outside base path {}",
                    new Object[] { file, basePath });
        }
        
        if (dirsToFiles.containsKey(fileDir)) {
            dirsToFiles.get(fileDir).add(file);
            callbackMap.put(file, callback);
        } else {
            try {
                fileDir.register(watcher, ENTRY_MODIFY, ENTRY_DELETE);
                ArrayList<File> files = new ArrayList<>();
                files.add(file);
                dirsToFiles.put(fileDir, files);
                callbackMap.put(file, callback);
            } catch (IOException ioe) {
                Logger.getLogger(FileMonitor.class.getName()).log(Level.SEVERE, "Unable to monitor directory " + fileDir, ioe);
                throw new IllegalStateException(ioe);
            }
        }
    }
    
    private class WatchTask implements Runnable {

        @Override
        public void run() {
            WatchKey key;
            boolean lastFileDeleted;  // set true if the last file in a watched directory has been deleted
            do {
                lastFileDeleted = false;
                try {
                    key = watcher.take();
                } catch (InterruptedException ex) {
                    Logger.getLogger(FileMonitor.class.getName()).log(Level.WARNING, "WatchTask interrupted; shutting down.", ex);
                    return;
                }
                
                for (WatchEvent<?> event: key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == OVERFLOW) continue;
                    
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    Path dirPath = findRelevantDirpath(filename);
                    if (dirPath == null) {
                        Logger.getLogger(FileMonitor.class.getName()).log(Level.WARNING,
                                "Could not find monitored directory for file {}", filename);
                        continue;
                    }
                    File resolved = dirPath.resolve(filename).toFile();
                    Runnable callback = callbackMap.get(resolved);
                    if (kind == ENTRY_DELETE) {
                        dirsToFiles.get(dirPath).remove(resolved);
                        if (dirsToFiles.get(dirPath).isEmpty() && !dirPath.equals(basePath)) {
                            dirsToFiles.remove(dirPath);
                            key.cancel();  // cancel this registration
                            lastFileDeleted = true;
                        }
                        callbackMap.remove(resolved);
                    }
                    if (callback != null) {
                        callback.run();
                    }
                }
            } while (key.isValid() || lastFileDeleted);  // canceling a specific key will invalidate it
            Logger.getLogger(FileMonitor.class.getName()).log(Level.WARNING,
                    "WatchKey {} is no longer valid; shutting down.", key);
        }
        
        private Path findRelevantDirpath(Path leafNode) {
            for (Path candidate : dirsToFiles.keySet()) {
                Path resolved = candidate.resolve(leafNode);
                if (dirsToFiles.get(candidate).contains(resolved.toFile())) {
                    return candidate;
                }
            }
            return null;
        }
        
    }
}
