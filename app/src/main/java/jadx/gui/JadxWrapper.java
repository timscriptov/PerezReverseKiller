package jadx.gui;

        import java.io.File;
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.Collections;
        import java.util.List;
        import java.util.concurrent.ThreadPoolExecutor;
        import java.util.stream.Collectors;

        import android.app.ProgressDialog;

        import org.jetbrains.annotations.Nullable;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        import jadx.api.JadxArgs;
        import jadx.api.JadxDecompiler;
        import jadx.api.JavaClass;
        import jadx.api.JavaPackage;
        import jadx.api.ResourceFile;

public class JadxWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(JadxWrapper.class);
    public static String g_excludedPackages = "";
    private final JadxArgs g_jadxArgs;
    private JadxDecompiler decompiler;
    private File openFile;

    public JadxWrapper(JadxArgs jadxArgs) {
        this.decompiler = new JadxDecompiler(jadxArgs);
        g_jadxArgs = jadxArgs;
    }

    public void openFile(File file) {
        this.openFile = file;
        try {
            g_jadxArgs.setInputFile(file);
            this.decompiler = new JadxDecompiler(g_jadxArgs);
            this.decompiler.load();
        } catch (Exception e) {
            LOG.error("Jadx init error", e);
        }
    }

    public void saveAll(File dir, final ProgressDialog progressMonitor) {
        Runnable save = () -> {
            try {
                decompiler.getArgs().setRootDir(dir);
                ThreadPoolExecutor ex = (ThreadPoolExecutor) decompiler.getSaveExecutor();
                ex.shutdown();
                while (ex.isTerminating()) {
                    long total = ex.getTaskCount();
                    long done = ex.getCompletedTaskCount();
                    progressMonitor.setProgress((int)(done * 100.0 / (double) total));
                    Thread.sleep(500);
                }
                progressMonitor.dismiss();
                LOG.info("decompilation complete, freeing memory ...");
                decompiler.getClasses().forEach(JavaClass::unload);
                LOG.info("done");
            } catch (InterruptedException e) {
                LOG.error("Save interrupted", e);
                Thread.currentThread().interrupt();
            }
        };
        new Thread(save).start();
    }

    /**
     * Get the complete list of classes
     */
    public List<JavaClass> getClasses() {
        return decompiler.getClasses();
    }

    /**
     * Get all classes that are not excluded by the excluded packages settings
     */
    public List<JavaClass> getIncludedClasses() {
        List<JavaClass> classList = decompiler.getClasses();
        List<String> excludedPackages = getExcludedPackages();
        if (excludedPackages.isEmpty()) {
            return classList;
        }

        return classList.stream().filter(cls -> {
            for (String exclude : excludedPackages) {
                if (cls.getFullName().equals(exclude)
                        || cls.getFullName().startsWith(exclude + '.')) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    // TODO: move to CLI and filter classes in JadxDecompiler
    public List<String> getExcludedPackages() {
        String excludedPackages = g_excludedPackages.trim();
        if (excludedPackages.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(excludedPackages.split("[ ]+"));
    }

    public void addExcludedPackage(String packageToExclude) {
        String newExclusion = g_excludedPackages + ' ' + packageToExclude;
        g_excludedPackages = (newExclusion.trim());
    }

    public void removeExcludedPackage(String packageToRemoveFromExclusion) {
        List<String> list = new ArrayList<>(getExcludedPackages());
        list.remove(packageToRemoveFromExclusion);
        g_excludedPackages = (String.join(" ", list));
    }

    public List<JavaPackage> getPackages() {
        return decompiler.getPackages();
    }

    public List<ResourceFile> getResources() {
        return decompiler.getResources();
    }

    public File getOpenFile() {
        return openFile;
    }

    public JadxDecompiler getDecompiler() {
        return decompiler;
    }

    public JadxArgs getArgs() {
        return decompiler.getArgs();
    }

    /**
     * @param fullName Full name of an outer class. Inner classes are not supported.
     */
    public @Nullable JavaClass searchJavaClassByClassName(String fullName) {
        return decompiler.getClasses().stream()
                .filter(cls -> cls.getFullName().equals(fullName))
                .findFirst()
                .orElse(null);
    }

    public @Nullable JavaClass searchJavaClassByOrigClassName(String fullName) {
        return decompiler.getClasses().stream()
                .filter(cls -> cls.getClassNode().getClassInfo().getFullName().equals(fullName))
                .findFirst()
                .orElse(null);
    }

    /**
     * @param rawName Full raw name of an outer class. Inner classes are not supported.
     */
    public @Nullable JavaClass searchJavaClassByRawName(String rawName) {
        return decompiler.getClasses().stream()
                .filter(cls -> cls.getRawName().equals(rawName))
                .findFirst()
                .orElse(null);
    }
}

