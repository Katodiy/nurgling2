package nurgling;

import haven.Audio;
import haven.Resource;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class NAlarmManager {
    static long startTick = -1000;
    static final long duration = 1000;
    static private final ReentrantLock mutex = new ReentrantLock();

    private static String last = null;

    public static void play (
            String resName
    ) {
        mutex.lock();
        if (startTick > NUtils.getTickId())
            startTick = 0;
        if (last!=null && !last.equals(resName) || NUtils.getTickId() - startTick > duration) {
            Audio.play(Audio.fromres(Resource.local().loadwait(resName)));
            last = resName;
            startTick = NUtils.getTickId();
        }
        mutex.unlock();
    }
}