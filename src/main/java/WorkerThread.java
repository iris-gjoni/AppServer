import ClientRequests.ClientRequestHandler;
import org.apache.log4j.Logger;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by irisg on 14/04/2020.
 */
public class WorkerThread implements Runnable {

    public static final String LOGIN = "RQ1";
    public static final String CREATE_USER_ACCOUNT = "CNU";
    public static final String CHECK_USER_VALID = "CUV";
    public static final String UPDATE_ACCOUNT = "UAC";
    public static final String LOAD_AVAILABLE_SLOTS = "LSL";
    public static final String BOOK_SLOT = "BSL";
    public static final String PAYMENT_COMPLETE = "PAY";

    public boolean running = true;
    private final ArrayBlockingQueue<ClientRequest> queue;
    private final ClientRequestHandler clientRequestHandler;
    private static final Logger logger = Logger.getLogger(WorkerThread.class);

    private static final long STATS_TIME_MILLIS = 60000;
    private long lastTime;
    private long requestsProcessed;


    WorkerThread(final ArrayBlockingQueue<ClientRequest> queue, final int port) {
        this.queue = queue;
        clientRequestHandler = new ClientRequestHandler(port);
        logger.info("<< Worker Thread initialised >> ");
    }

    @Override
    public void run() {
        lastTime = System.currentTimeMillis();
        while (running) { // busy spin
            logStats();
            if (queue.peek() != null) {
                /* we have data to work on */
                logger.info("Worker Thread: working on new Client Request");
                requestsProcessed++;
                process(queue.poll());
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void process(final ClientRequest context) {

        final String message = context.getMessage();
        final Socket socket = context.getSocket();
        final long startTime = context.getStartTime();

        String requestType = message.substring(0, 3);

        switch (requestType) {
            case LOGIN:
                clientRequestHandler.handleLoginRequest(socket, message, startTime);
                break;
            case CREATE_USER_ACCOUNT:
                clientRequestHandler.handleNewUserAccountRequest(socket, message, startTime);
                break;
            case UPDATE_ACCOUNT:
                clientRequestHandler.handleUpdateAccountRequest(socket, message, startTime);
                break;
            case LOAD_AVAILABLE_SLOTS:
                clientRequestHandler.handleLoadAvaibleSlotsRequest(socket, message, startTime);
                break;
            case BOOK_SLOT:
                clientRequestHandler.handleBookSlotRequest(socket, message, startTime);
                break;
            case PAYMENT_COMPLETE:
                clientRequestHandler.handlePaymentComplete(socket, message, startTime);
                break;
            default:
                break;
        }
    }

    private void logStats() {
        long timeNow = System.currentTimeMillis();
        if ((lastTime + STATS_TIME_MILLIS) - timeNow < 0) {
            lastTime = timeNow;
            logger.info("requests processed: " + requestsProcessed +
                    " - in " + STATS_TIME_MILLIS / 1000 + "s");
            requestsProcessed = 0;
        }
    }

}
