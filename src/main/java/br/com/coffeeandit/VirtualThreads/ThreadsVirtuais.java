package br.com.coffeeandit;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class ThreadsVirtuais {
    public static void main(String[] args) {

        // Para monitorar o número de Threads do Sistema usados pelo teste, escreva o seguinte código:
        var scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            var threadBean = ManagementFactory.getThreadMXBean();
            var threadInfo = threadBean.dumpAllThreads(false, false);
            System.out.println(threadInfo.length + " Threads de Sistema");
        }, 10, 10, TimeUnit.MILLISECONDS);

        long l = System.currentTimeMillis();

        //Código que usa threads virtuais é uma palavra a menos de usar tamanho fixo, substitua Executors.newFixedThreadPool(200)por Executors.newVirtualThreadPerTaskExecutor().
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 10000).forEach(i -> {
                executor.submit(() -> {
                    Thread.sleep(Duration.ofSeconds(1));
                    System.out.println(i);
                    return i;
                });
            });
        }

        System.out.printf("Tempo de execução: %dms\n", System.currentTimeMillis() - l);
    }
}