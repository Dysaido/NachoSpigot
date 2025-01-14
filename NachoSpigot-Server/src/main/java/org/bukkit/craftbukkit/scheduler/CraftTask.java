package org.bukkit.craftbukkit.scheduler;

import co.aikar.timings.NullTimingHandler;
import co.aikar.timings.SpigotTimings;
import co.aikar.timings.Timing;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;


public class CraftTask implements BukkitTask, Runnable { // Spigot

    private volatile CraftTask next = null;
    public static final int ERROR = 0;
    public static final int NO_REPEATING = -1;
    public static final int CANCEL = -2;
    public static final int PROCESS_FOR_FUTURE = -3;
    public static final int DONE_FOR_FUTURE = -4;
    /**
     * -1 means no repeating <br>
     * -2 means cancel <br>
     * -3 means processing for Future <br>
     * -4 means done for Future <br>
     * Never 0 <br>
     * >0 means number of ticks to wait between each execution
     */
    private volatile long period;
    private long nextRun;
    public final Runnable rTask; //Spigot
    public final Consumer<BukkitTask> cTask; // Paper
    public Timing timings; // Spigot
    private final Plugin plugin;
    private final int id;

    CraftTask() {
        this(null, null, CraftTask.NO_REPEATING, CraftTask.NO_REPEATING);
    }

    CraftTask(final Object rTask) {
        this(null, rTask, CraftTask.NO_REPEATING, CraftTask.NO_REPEATING);
    }

    // Paper start
    public String taskName = null;
    boolean internal = false;
    CraftTask(final Object rTask, int id, String taskName) {
        this.rTask = (Runnable) rTask;
        this.cTask = null;
        this.plugin = CraftScheduler.MINECRAFT;
        this.taskName = taskName;
        this.internal = true;
        this.id = id;
        this.period = CraftTask.NO_REPEATING;
        this.taskName = taskName;
        this.timings = SpigotTimings.getInternalTaskName(taskName);
    }
    // Paper end

    // Spigot start
    CraftTask(final Plugin plugin, final Object rTask, final int id, final long period) {
        this.plugin = plugin;
        if (rTask instanceof Runnable) {
            this.rTask = (Runnable) rTask;
            this.cTask = null;
        } else if (rTask instanceof Consumer) {
            this.cTask = (Consumer<BukkitTask>) rTask;
            this.rTask = null;
        } else if (rTask == null) {
            // Head or Future task
            this.rTask = null;
            this.cTask = null;
        } else {
            throw new AssertionError("Illegal task class " + rTask);
        }
        this.id = id;
        this.period = period;
        timings = rTask != null ? SpigotTimings.getPluginTaskTimings(this, period) : NullTimingHandler.NULL; // Spigot
    }

    public final int getTaskId() {
        return id;
    }

    public final Plugin getOwner() {
        return plugin;
    }

    public boolean isSync() {
        return true;
    }

    public void run() {
        try (Timing ignored = timings.startTiming()) { // Paper
            if (rTask != null) {
                rTask.run();
            } else {
                cTask.accept(this);
            }
        } // Paper
    }

    long getPeriod() {
        return period;
    }

    void setPeriod(long period) {
        this.period = period;
    }

    long getNextRun() {
        return nextRun;
    }

    void setNextRun(long nextRun) {
        this.nextRun = nextRun;
    }

    CraftTask getNext() {
        return next;
    }

    void setNext(CraftTask next) {
        this.next = next;
    }

    public Class<?> getTaskClass() {
        return (rTask != null) ? rTask.getClass() : ((cTask != null) ? cTask.getClass() : null);
    }

    @Override
    public boolean isCancelled() {
        return (period == CraftTask.CANCEL);
    }

    public void cancel() {
        Bukkit.getScheduler().cancelTask(id);
    }

    /**
     * This method properly sets the status to cancelled, synchronizing when required.
     *
     * @return false if it is a craft future task that has already begun execution, true otherwise
     */
    boolean cancel0() {
        setPeriod(CraftTask.CANCEL);
        return true;
    }

}
