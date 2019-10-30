package agents.jaydee;

import core.competition.CompetitionParameters;
import tools.ElapsedCpuTimer;

public class AnyTime {
    private static long   MAX_TIME       = CompetitionParameters.ACTION_TIME * 1000 * 1000;
    private static long   MAX_INIT_TIME  = CompetitionParameters.INITIALIZATION_TIME * 1000 * 1000;
    private static double TIME_PESSIMISM = 1.5;
    private static long   SHUTDOWN_TIME  = 10 * 1000 * 1000;
    private static double PER_LOOP_GAIN  = 0.1;

    protected double maxTimePerLoop = 0;
    protected long   maxTime        = 0;
    protected long   lastTime       = 0;

    protected ElapsedCpuTimer elapsedTimer;

    public void beginInit(ElapsedCpuTimer elapsedTimer) {
        this.elapsedTimer = elapsedTimer;
        maxTime = MAX_INIT_TIME;
        lastTime = this.elapsedTimer.elapsed();
    }

    public void begin(ElapsedCpuTimer elapsedTimer) {
        this.elapsedTimer = elapsedTimer;
        maxTime = MAX_TIME;
        lastTime = this.elapsedTimer.elapsed();
    }

    public boolean isTimeOver() {
        return (maxTime - elapsedTimer.elapsed() < TIME_PESSIMISM * (maxTimePerLoop + SHUTDOWN_TIME));
    }

    public void updatePerLoop() {
        maxTimePerLoop = (1 - PER_LOOP_GAIN) * maxTimePerLoop + (PER_LOOP_GAIN) * (elapsedTimer.elapsed() - lastTime);
        lastTime = elapsedTimer.elapsed();
    }
}
