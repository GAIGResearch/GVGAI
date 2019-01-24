package agents.ICELab.OpenLoopRLBiasMCTS;

import java.awt.Point;
import java.util.Comparator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import core.game.StateObservation;
import tools.Vector2d;
import agents.ICELab.GameInfo;

public class Utils {
    public static final Logger logger = Logger.getLogger("OpenLoopRLBiasMCTS");

    public static void initLogger(){
		Logger globalLogger = Logger.getLogger("global");
		Handler[] handlers = globalLogger.getHandlers();
		for(Handler handler : handlers) {
		    globalLogger.removeHandler(handler);
		}

		Formatter formatter = new Formatter() {
			@Override
			public String format(LogRecord record) {
				StringBuilder builder = new StringBuilder(1000);
		        builder.append(record.getMessage())
		        	   .append("\n");
		        return builder.toString();
			}
		};

		Filter filter = new Filter(){
			public boolean isLoggable(LogRecord record) {
				/*
				if (//record.getMessage().contains("MEMORY")  ||
					record.getMessage().contains("MEMORY")
					)//&& record.getLevel().intValue() >= Level.FINER.intValue())
					return true;
				//*/
				return false;
			}
		};

	    	ConsoleHandler ch = new ConsoleHandler();
	    	ch.setFormatter(formatter);
	    	ch.setLevel(Level.INFO);
	    	logger.addHandler(ch);
	    	logger.setFilter(filter);
	    	logger.setUseParentHandlers(false);
	    	logger.setLevel(Level.INFO);
    }


    public static double[][] copy2DArray(double[][] arr) {
        int n = arr.length;
        double[][] copy = new double[n][];
        for (int i = 0; i < n; i++) {
            copy[i] = new double[arr[i].length];
            System.arraycopy(arr[i], 0, copy[i], 0, arr[i].length);
        }
        return copy;
    }

    public static double[][] copyInto2DArray(double[][] dst, double[][] src) {
        int n = src.length;
        double[][] copy = new double[n][];
        for (int i = 0; i < n; i++) {
            System.arraycopy(src[i], 0, dst[i], 0, src[i].length);
        }
        return copy;
    }

    public static double min(double[][] arr) {
        double min = Double.POSITIVE_INFINITY;
        for (double[] anArr : arr) {
            for (double anAnArr : anArr) {
                if (anAnArr < min) {
                    min = anAnArr;
                }
            }
        }
        return min;
    }

    public static double max(double[][] arr) {
        double max = Double.NEGATIVE_INFINITY;
        for (double[] anArr : arr) {
            for (double anAnArr : anArr) {
                if (anAnArr > max) {
                    max = anAnArr;
                }
            }
        }
        return max;
    }

    public static Point toTileCoord(Vector2d coord) {
        return toTileCoord(coord.x, coord.y);
    }

    public static Point toTileCoord(double x, double y) {
        Point point = new Point();
        point.x = (int) Math.round(x / GameInfo.blocksize);
        point.y = (int) Math.round(y / GameInfo.blocksize);
        if (point.x < 0) point.x = 0;
        if (point.y < 0) point.y = 0;
        if (point.x > GameInfo.width - 1) point.x = GameInfo.width - 1;
        if (point.y > GameInfo.height - 1) point.y = GameInfo.height - 1;
        return point;
    }
    public static Comparator<Node> heuristicComparator = new Comparator<Node>() {
        @Override
        public int compare(Node o1, Node o2) {
            double f1 = o1.bestReward;
            double f2 = o2.bestReward;
            if (f1 < f2) {
                return -1;
            } else if (f1 > f2) {
                return 1;
            } else {
                return 0;
            }
        }
    };

	static public boolean isOnMap(StateObservation so, double x, double y){
		int intX = (int) so.getAvatarPosition().x / so.getBlockSize();
		int intY = (int) so.getAvatarPosition().y / so.getBlockSize();

		return isOnMap(so, intX, intY);
	}

	/*
	 * check is the coordinates are on map or not
	 */
	static public boolean isOnMap(StateObservation so, int x, int y) {
		if (x >= 0 && x < so.getObservationGrid().length &&
			y >= 0 && y < so.getObservationGrid()[0].length)
			return true;
		else
			return false;
	}
}

