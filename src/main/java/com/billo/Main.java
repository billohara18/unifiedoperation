package com.billo;

import com.billo.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.billo.PrivateConfig.platformSessionDataArray;

public class Main {
    private static ArrayList<PlatformSession> sessionArray = new ArrayList<>();

    public static void main(String[] args) {

        feedSessionArray();

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getActiveCount());
        for (PlatformSession platformSession : sessionArray) {
            Runnable worker = () -> {
                if (platformSession instanceof BySession) {
                    BySession bySession = (BySession) platformSession;
                    bySession.displayBalance();
                    bySession.refreshCurrentLeverage_By();
                } else if (platformSession instanceof BnSession) {
                    BnSession bnSession = (BnSession) platformSession;
                    bnSession.displayBalance();
                    bnSession.refreshCurrentLeverage();
                }
            };
            executor.execute(worker);
        }
        try {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (PlatformSession platformSession : sessionArray) {
            if (platformSession instanceof BySession) {
                new Thread(() -> {
                    ((BySession) platformSession).pollServer_By();
                }).start();
            } else if (platformSession instanceof BnSession) {
                new Thread(() -> {
                    ((BnSession) platformSession).pollServer();
                }).start();
            }
        }

        while (true) {
            try {
                System.out.println("");

                Scanner sc = new Scanner(System.in);  //System.in is a standard input stream
                System.out.println("Enter a String: ");
                String command = sc.nextLine().toLowerCase();   //reads string before the space

                if (command.startsWith("exit")) {
                    for (PlatformSession platformSession : sessionArray) {
                        platformSession.exit();
                    }
                    break;
                }

                if (command.startsWith("getSize".toLowerCase())) {
                    executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getActiveCount());
                    for (PlatformSession platformSession : sessionArray) {
                        if (!platformSession.isTurnedOn) continue;
                        Runnable worker = () -> {
                            if (platformSession instanceof BySession) {
                                ((BySession) platformSession).displayBalance();
                            } else if (platformSession instanceof BnSession) {
                                ((BnSession) platformSession).displayBalance();
                            }
                        };
                        executor.execute(worker);
                    }
                    try {
                        executor.shutdown();
                        executor.awaitTermination(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }

                if (command.startsWith("setSize".toLowerCase())) {

                    String[] elements = command.split(" ");
                    if (elements != null && elements.length > 1) {
                        double balance = Double.valueOf(elements[1]);
                        for (PlatformSession platformSession : sessionArray) {
                            platformSession.setSize(balance);
                        }
                    }
                    continue;
                }

                if (command.startsWith("setLeverage".toLowerCase())) {
                    String[] elements = command.split(" ");
                    if (elements != null && elements.length > 1) {
                        int leverage = Integer.valueOf(elements[1]);

                        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getActiveCount());
                        for (PlatformSession platformSession : sessionArray) {
                            if (!platformSession.isTurnedOn) continue;
                            Runnable worker = () -> {
                                if (platformSession instanceof BySession) {
                                    ((BySession) platformSession).changeLeverage_By(leverage);
                                } else if (platformSession instanceof BnSession) {
                                    ((BnSession) platformSession).changeLeverage(leverage);
                                }
                            };
                            executor.execute(worker);
                        }
                        try {
                            executor.shutdown();
                            executor.awaitTermination(30, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    continue;
                }

                if (command.startsWith("getLeverage".toLowerCase())) {
                    executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getActiveCount());
                    for (PlatformSession platformSession : sessionArray) {
                        if (!platformSession.isTurnedOn) continue;
                        Runnable worker = () -> {
                            if (platformSession instanceof BySession) {
                                ((BySession) platformSession).refreshCurrentLeverage_By();
                            } else if (platformSession instanceof BnSession) {
                                ((BnSession) platformSession).refreshCurrentLeverage();
                            }
                        };
                        executor.execute(worker);
                    }
                    try {
                        executor.shutdown();
                        executor.awaitTermination(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }

                if (command.startsWith("setTP".toLowerCase())) {
                    String[] elements = command.split(" ");
                    if (elements != null && elements.length > 1) {
                        double tp = Integer.valueOf(elements[1]);
                        for (PlatformSession platformSession : sessionArray) {
                            platformSession.setTP(tp);
                        }
                    }
                    continue;
                }

                if (command.startsWith("getTP".toLowerCase())) {
                    for (PlatformSession platformSession : sessionArray) {
                        platformSession.showTP();
                    }
                    continue;
                }

                if (command.startsWith("setDiff".toLowerCase())) {
                    String[] elements = command.split(" ");
                    if (elements != null && elements.length > 1) {
                        double diff = Double.valueOf(elements[1]);
                        for (PlatformSession platformSession : sessionArray) {
                            if (platformSession instanceof BnSession) {
                                ((BnSession) platformSession).setDiff(diff);
                            }
                        }
                    }
                    continue;
                }

                if (command.startsWith("getDiff".toLowerCase())) {
                    for (PlatformSession platformSession : sessionArray) {
                        if (platformSession instanceof BnSession) {
                            ((BnSession) platformSession).getDiff();
                        }
                    }
                    continue;
                }

                if (command.startsWith("setSL".toLowerCase())) {
                    String[] elements = command.split(" ");
                    if (elements != null && elements.length > 1) {
                        double sl = Integer.valueOf(elements[1]);
                        for (PlatformSession platformSession : sessionArray) {
                            platformSession.setSL(sl);
                        }
                    }
                    continue;
                }

                if (command.startsWith("getSL".toLowerCase())) {
                    for (PlatformSession platformSession : sessionArray) {
                        platformSession.showSL();
                    }
                    continue;
                }

                if (command.startsWith("openOrders".toLowerCase())) {
                    executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getActiveCount());
                    for (PlatformSession platformSession : sessionArray) {
                        if (!platformSession.isTurnedOn) continue;
                        Runnable worker = () -> {
                            if (platformSession instanceof BySession) {
                                ((BySession) platformSession).displayOpenOrders();
                            } else if (platformSession instanceof BnSession) {
                                ((BnSession) platformSession).displayOpenOrders();
                            }
                        };
                        executor.execute(worker);
                    }
                    try {
                        executor.shutdown();
                        executor.awaitTermination(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }

                if (command.startsWith("position".toLowerCase())) {
                    executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getActiveCount());
                    for (PlatformSession platformSession : sessionArray) {
                        if (!platformSession.isTurnedOn) continue;
                        Runnable worker = () -> {
                            if (platformSession instanceof BySession) {
                                ((BySession) platformSession).displayCurrentPosition();
                            } else if (platformSession instanceof BnSession) {
                                ((BnSession) platformSession).displayCurrentPosition();
                            }
                        };
                        executor.execute(worker);
                    }
                    try {
                        executor.shutdown();
                        executor.awaitTermination(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }

                if (command.startsWith("hits")) {
                    executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getActiveCount());
                    for (PlatformSession platformSession : sessionArray) {
                        if (!platformSession.isTurnedOn) continue;
                        Runnable worker = () -> {
                            if (platformSession instanceof BySession) {
                                ((BySession) platformSession).getLastPositionHistory_By();
                            } else if (platformSession instanceof BnSession) {
                                ((BnSession) platformSession).getLastPositionHistory();
                            }
                        };
                        executor.execute(worker);
                    }
                    try {
                        executor.shutdown();
                        executor.awaitTermination(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }

                if (command.startsWith("run") || command.startsWith("escape")) {
                    executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getActiveCount());
                    for (PlatformSession platformSession : sessionArray) {
                        if (!platformSession.isTurnedOn) continue;
                        Runnable worker = () -> {
                            if (platformSession instanceof BySession) {
                                ((BySession) platformSession).escapeNow_By();
                            } else if (platformSession instanceof BnSession) {
                                ((BnSession) platformSession).escapeNow();
                            }
                        };
                        executor.execute(worker);
                    }
                    try {
                        executor.shutdown();
                        executor.awaitTermination(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }

                if (command.startsWith("forceSl".toLowerCase())) {
                    for (PlatformSession platformSession : sessionArray) {
                        if (!platformSession.isTurnedOn) continue;
                        if (platformSession instanceof BySession) {
                            ((BySession) platformSession).placeSLForce();
                        } else if (platformSession instanceof BnSession) {
                            ((BnSession) platformSession).placeSLForce();
                        }
                    }
                    continue;
                }

                if (command.startsWith("placeOrder".toLowerCase())) {
                    String[] elements = command.split(" ");
                    if (elements != null && elements.length > 2) {
                        boolean isLong = elements[1].equalsIgnoreCase("Long");
                        double entryPrice = Double.valueOf(elements[2]);

                        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getActiveCount());
                        for (PlatformSession platformSession : sessionArray) {
                            if (!platformSession.isTurnedOn) continue;
                            Runnable worker = () -> {
                                if (platformSession instanceof BySession) {
                                    ((BySession) platformSession).placeOrder_By(isLong, entryPrice, false);
                                } else if (platformSession instanceof BnSession) {
                                    ((BnSession) platformSession).placeOrderCasual(isLong, entryPrice, false);
                                }
                            };
                            executor.execute(worker);
                        }
                        try {
                            executor.shutdown();
                            executor.awaitTermination(30, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    continue;
                }

                if (command.startsWith("forceTp".toLowerCase())) {
                    for (PlatformSession platformSession : sessionArray) {
                        if (!platformSession.isTurnedOn) continue;
                        if (platformSession instanceof BySession) {
                            ((BySession) platformSession).placeTPForce();
                        } else if (platformSession instanceof BnSession) {
                            ((BnSession) platformSession).placeTPForce();
                        }
                    }
                    continue;
                }

                if (command.startsWith("cancelTP".toLowerCase())) {
                    for (PlatformSession platformSession : sessionArray) {
                        platformSession.cancelTP();
                    }
                    continue;
                }

                if (command.startsWith("cancelSL".toLowerCase())) {
                    for (PlatformSession platformSession : sessionArray) {
                        platformSession.cancelSL();
                    }
                    continue;
                }

                if (command.startsWith("cancel".toLowerCase())) {
                    executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getActiveCount());
                    for (PlatformSession platformSession : sessionArray) {
                        if (!platformSession.isTurnedOn) continue;
                        Runnable worker = () -> {
                            if (platformSession instanceof BySession) {
                                ((BySession) platformSession).cancelAllOrders_By();
                            } else if (platformSession instanceof BnSession) {
                                ((BnSession) platformSession).cancelAllOrders();
                            }
                        };
                        executor.execute(worker);
                    }
                    try {
                        executor.shutdown();
                        executor.awaitTermination(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }

                if (command.startsWith("modifyOrder".toLowerCase())) {
                    String[] elements = command.split(" ");
                    if (elements != null && elements.length > 2) {
                        boolean isTP = elements[1].equalsIgnoreCase("tp");
                        double stopPrice = Double.valueOf(elements[2]);
                        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getActiveCount());
                        for (PlatformSession platformSession : sessionArray) {
                            if (!platformSession.isTurnedOn) continue;
                            Runnable worker = () -> {
                                if (platformSession instanceof BySession) {
                                    ((BySession) platformSession).modifyOrder_By(isTP, stopPrice);
                                } else if (platformSession instanceof BnSession) {
                                    ((BnSession) platformSession).modifyOrder(isTP, stopPrice);
                                }
                            };
                            executor.execute(worker);
                        }
                        try {
                            executor.shutdown();
                            executor.awaitTermination(30, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    continue;
                }

                if (command.startsWith("filterVersion".toLowerCase())) {
                    String[] elements = command.split(" ");
                    if (elements != null && elements.length > 1) {
                        String filterVersion = elements[1];
                        if (Utils.isInteger(filterVersion)) {
                            int filterIndex = Integer.parseInt(filterVersion);
                            filterSessionArrayById(filterIndex);
                        } else {
                            filterSessionArray(PrivateConfig.PlatformVersion.getByString(filterVersion));
                        }
                    }
                    continue;
                }

                if (command.startsWith("displayVersion".toLowerCase())) {
                    for (PlatformSession session : sessionArray) {
                        if (session.isTurnedOn)
                            System.out.print(session.version.getVersionId() + " ");
                    }
                    System.out.println();
                    continue;
                }

                if (command.startsWith("filterLog".toLowerCase())) {
                    String[] elements = command.split(" ");
                    if (elements != null && elements.length > 1) {
                        String filterVersion = elements[1];
                        if (Utils.isInteger(filterVersion)) {
                            int filterIndex = Integer.parseInt(filterVersion);
                            filterLogById(filterIndex);
                        } else {
                            filterLog(PrivateConfig.PlatformVersion.getByString(filterVersion));
                        }
                    }
                    continue;
                }

                if (command.startsWith("displayLog".toLowerCase())) {
                    for (PlatformSession session : sessionArray) {
                        System.out.print((session.isPrintable ? "1" : "0") + " ");
                    }
                    System.out.println();
                    continue;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            System.out.println("no action");
        }
    }

    public static void feedSessionArray() {
        for (PlatformSession session: sessionArray) {
            session.exit();
        }
        sessionArray.clear();

        int idx = 0;
        for (Map<Object, Object> platformSessionData: platformSessionDataArray) {
            PlatformSession session = null;
            if (platformSessionData.get(PrivateConfig.SessionDataField.API_VERSION) == PrivateConfig.PlatformVersion.By) {
                session = new BySession((String)platformSessionData.get(PrivateConfig.SessionDataField.API_KEY), (String)platformSessionData.get(PrivateConfig.SessionDataField.SECRET_KEY));
            } else if (platformSessionData.get(PrivateConfig.SessionDataField.API_VERSION) == PrivateConfig.PlatformVersion.Bn) {
                session = new BnSession((String)platformSessionData.get(PrivateConfig.SessionDataField.API_KEY), (String)platformSessionData.get(PrivateConfig.SessionDataField.SECRET_KEY));
            }

            if (session != null) {
                session.sessionIndex = idx;
                sessionArray.add(session);
                idx ++;
            }
        }
    }

    public static void filterSessionArray(PrivateConfig.PlatformVersion filterVersion) {
        for (PlatformSession platformSession: sessionArray) {
            if (filterVersion != platformSession.version && filterVersion != PrivateConfig.PlatformVersion.None) {
                platformSession.exit();
                platformSession.isTurnedOn = false;
            } else {
                if (!platformSession.isTurnedOn) {
                    if (platformSession instanceof BySession) {
                        new Thread(() -> {
                            ((BySession) platformSession).pollServer_By();
                        }).start();
                    } else if (platformSession instanceof BnSession) {
                        new Thread(() -> {
                            ((BnSession) platformSession).pollServer();
                        }).start();
                    }
                    platformSession.isTurnedOn = true;
                }
            }
        }
    }

    public static void filterSessionArrayById(int index) {
        if (index < 0 || index > sessionArray.size() - 1) return;

        for (PlatformSession platformSession: sessionArray) {
            if (index != platformSession.sessionIndex) {
                platformSession.exit();
                platformSession.isTurnedOn = false;
            } else {
                if (!platformSession.isTurnedOn) {
                    if (platformSession instanceof BySession) {
                        new Thread(() -> {
                            ((BySession) platformSession).pollServer_By();
                        }).start();
                    } else if (platformSession instanceof BnSession) {
                        new Thread(() -> {
                            ((BnSession) platformSession).pollServer();
                        }).start();
                    }
                    platformSession.isTurnedOn = true;
                }
            }
        }
    }

    public static void filterLog(PrivateConfig.PlatformVersion filterVersion) {
        for (PlatformSession session: sessionArray) {
            session.isPrintable = filterVersion == PrivateConfig.PlatformVersion.None || filterVersion == session.version;
        }
    }

    public static void filterLogById(int index) {
        if (index < 0 || index > sessionArray.size() - 1) return;
        for (PlatformSession session: sessionArray) {
            session.isPrintable = index == session.sessionIndex;
        }
    }

    public static int getActiveCount() {
        int activeCount = 0;
        for (PlatformSession session: sessionArray) {
            if (session.isTurnedOn)
                activeCount ++;
        }
        return activeCount;
    }
}
