package haven.error;

import haven.HashDirCache;
import haven.ResCache;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * File logger that writes errors and stack traces to rotating log files.
 * Logs are stored in the same directory as nurgling config files.
 */
public class FileLogger {
    private static final String LOG_PREFIX = "nurgling";
    private static final long MAX_LOG_SIZE = 10 * 1024 * 1024; // 10MB
    
    private static FileLogger instance;
    private final Path logDir;
    private final LinkedBlockingQueue<LogEntry> queue;
    private final Thread writerThread;
    private volatile boolean running = true;
    private PrintWriter currentWriter;
    private Path currentLogFile;
    private long currentLogSize = 0;
    
    private static class LogEntry {
        final String message;
        final Throwable throwable;
        final long timestamp;
        
        LogEntry(String message, Throwable throwable) {
            this.message = message;
            this.throwable = throwable;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    private FileLogger() throws IOException {
        // Use same directory as nurgling config
        String basePath;
        if (ResCache.global instanceof HashDirCache) {
            basePath = ((HashDirCache) ResCache.global).base.toString();
        } else {
            basePath = System.getProperty("user.dir");
        }
        
        this.logDir = Paths.get(basePath).getParent();
        
        // Create log directory if it doesn't exist
        if (logDir != null && !Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }
        
        // Delete old logs (not from today) on startup
        deleteOldLogs();
        
        this.queue = new LinkedBlockingQueue<>(1000);
        this.writerThread = new Thread(this::processQueue, "FileLogger-Writer");
        this.writerThread.setDaemon(true);
        
        // Initialize first log file
        rotateLogFile();
        
        // Start writer thread
        this.writerThread.start();
        
        // Add shutdown hook to flush remaining logs
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
    
    public static synchronized FileLogger getInstance() {
        if (instance == null) {
            try {
                instance = new FileLogger();
            } catch (IOException e) {
                System.err.println("Failed to initialize FileLogger: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return instance;
    }
    
    /**
     * Log a message
     */
    public static void log(String message) {
        FileLogger logger = getInstance();
        if (logger != null) {
            logger.enqueue(new LogEntry(message, null));
        }
    }
    
    /**
     * Log an error with stack trace
     */
    public static void logError(String message, Throwable throwable) {
        FileLogger logger = getInstance();
        if (logger != null) {
            logger.enqueue(new LogEntry(message, throwable));
        }
    }
    
    /**
     * Log just a throwable
     */
    public static void logError(Throwable throwable) {
        logError("Exception caught", throwable);
    }
    
    /**
     * Redirect System.err to file logger while preserving original output
     */
    public static void redirectSystemErr() {
        final PrintStream originalErr = System.err;
        
        PrintStream loggingErr = new PrintStream(new OutputStream() {
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            
            @Override
            public void write(int b) throws IOException {
                originalErr.write(b);
                buffer.write(b);
                
                // Check if we have a complete line
                if (b == '\n') {
                    String line = buffer.toString("UTF-8");
                    buffer.reset();
                    
                    // Log to file
                    FileLogger logger = getInstance();
                    if (logger != null && logger.currentWriter != null) {
                        try {
                            synchronized (logger) {
                                logger.currentWriter.write(line);
                                logger.currentLogSize += line.length();
                                
                                if (logger.currentLogSize > MAX_LOG_SIZE) {
                                    logger.rotateLogFile();
                                }
                            }
                        } catch (Exception e) {
                            // Can't log the error without creating infinite loop
                        }
                    }
                }
            }
            
            @Override
            public void flush() throws IOException {
                originalErr.flush();
                if (buffer.size() > 0) {
                    String remaining = buffer.toString("UTF-8");
                    buffer.reset();
                    
                    FileLogger logger = getInstance();
                    if (logger != null && logger.currentWriter != null) {
                        try {
                            synchronized (logger) {
                                logger.currentWriter.write(remaining);
                                logger.currentWriter.flush();
                                logger.currentLogSize += remaining.length();
                            }
                        } catch (Exception e) {
                            // Can't log the error
                        }
                    }
                }
            }
        }, true);
        
        System.setErr(loggingErr);
    }
    
    private void enqueue(LogEntry entry) {
        if (!queue.offer(entry)) {
            // Queue is full, log to stderr as fallback
            System.err.println("FileLogger queue full, dropping log entry: " + entry.message);
        }
    }
    
    private void processQueue() {
        while (running || !queue.isEmpty()) {
            try {
                LogEntry entry = queue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (entry != null) {
                    writeEntry(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void writeEntry(LogEntry entry) {
        synchronized (this) {
            try {
                // Check if we need to rotate
                if (currentLogSize > MAX_LOG_SIZE) {
                    rotateLogFile();
                }
                
                if (currentWriter == null) {
                    return;
                }
                
                // Format timestamp
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                String timestamp = sdf.format(new Date(entry.timestamp));
                
                // Write entry
                String logLine = String.format("[%s] %s%n", timestamp, entry.message);
                currentWriter.write(logLine);
                currentLogSize += logLine.length();
                
                // Write stack trace if present
                if (entry.throwable != null) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    entry.throwable.printStackTrace(pw);
                    String stackTrace = sw.toString();
                    currentWriter.write(stackTrace);
                    currentLogSize += stackTrace.length();
                }
                
                currentWriter.flush();
                
                // Also write to stderr for immediate visibility
                System.err.print(logLine);
                if (entry.throwable != null) {
                    entry.throwable.printStackTrace(System.err);
                }
                
            } catch (IOException e) {
                System.err.println("Error writing to log file: " + e.getMessage());
            }
        }
    }
    
    private void rotateLogFile() throws IOException {
        // Close current writer
        if (currentWriter != null) {
            currentWriter.close();
            currentWriter = null;
        }
        
        // Create new log file
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String filename = String.format("%s-%s.log", LOG_PREFIX, sdf.format(new Date()));
        currentLogFile = logDir.resolve(filename);
        
        currentWriter = new PrintWriter(
            new BufferedWriter(
                new OutputStreamWriter(
                    Files.newOutputStream(currentLogFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                    "UTF-8"
                )
            )
        );
        
        currentLogSize = Files.size(currentLogFile);
        
        // Write header
        String header = String.format("=== Log started at %s ===%n", new Date());
        currentWriter.write(header);
        currentWriter.flush();
    }
    
    private void deleteOldLogs() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String today = sdf.format(new Date());
            
            Files.list(logDir)
                .filter(p -> p.getFileName().toString().startsWith(LOG_PREFIX))
                .filter(p -> p.getFileName().toString().endsWith(".log"))
                .filter(p -> !p.getFileName().toString().contains(today))
                .forEach(p -> {
                    try {
                        if (Files.exists(p)) {
                            Files.delete(p);
                            System.err.println("Deleted old log file: " + p.getFileName());
                        }
                    } catch (IOException e) {
                        // Ignore - file might be already deleted by another thread
                    }
                });
        } catch (IOException e) {
            System.err.println("Error deleting old logs: " + e.getMessage());
        }
    }
    
    private void shutdown() {
        running = false;
        try {
            writerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (currentWriter != null) {
            currentWriter.close();
        }
    }
    
    public Path getLogDirectory() {
        return logDir;
    }
    
    public Path getCurrentLogFile() {
        return currentLogFile;
    }
}
