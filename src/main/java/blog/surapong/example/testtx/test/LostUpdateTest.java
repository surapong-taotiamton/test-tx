package blog.surapong.example.testtx.test;


import blog.surapong.example.testtx.entity.Account;
import blog.surapong.example.testtx.repository.AccountRepository;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Service
public class LostUpdateTest {

    private static final String OUTPUT_FILE = "test-lost-update.csv";
    
    private static final Logger logger = LoggerFactory.getLogger(LostUpdateTest.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void startTestTx() {
        lostUpdateTest("A", 1, 5, 2, 5);
        lostUpdateTest("B", 10, 5, 2, 5);
    }




    public void lostUpdateTest(String prefix, int startBeforeSleepBeforeWrite, int startBeforeSleepBeforeCommit,
                               int startAfterSleepBeforeWrite, int startAfterSleepBeforeCommit ) {

        List<Integer> isolationStartBeforeList = Arrays.asList(TransactionDefinition.ISOLATION_READ_UNCOMMITTED, TransactionDefinition.ISOLATION_READ_COMMITTED, TransactionDefinition.ISOLATION_REPEATABLE_READ, TransactionDefinition.ISOLATION_SERIALIZABLE);
        List<Integer> isolationStartAfterList = Arrays.asList(TransactionDefinition.ISOLATION_READ_UNCOMMITTED, TransactionDefinition.ISOLATION_READ_COMMITTED, TransactionDefinition.ISOLATION_REPEATABLE_READ, TransactionDefinition.ISOLATION_SERIALIZABLE);

        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(2);
        threadPoolTaskExecutor.setMaxPoolSize(2);
        threadPoolTaskExecutor.initialize();

        for (int i = 0; i < isolationStartBeforeList.size(); i++) {
            for (int j = 0; j < isolationStartAfterList.size(); j++) {


                int startBeforeIsolationLevel = isolationStartBeforeList.get(i);
                int startAfterIsolationLevel = isolationStartAfterList.get(j);

                String accountId = prefix +  startBeforeIsolationLevel + "" + startAfterIsolationLevel;
                Integer startBeforeValue = 20;
                Integer startAfterValue = 30;

                Callable<Account> startBeforeCallable = createCallable(startBeforeSleepBeforeWrite, startBeforeSleepBeforeCommit, startBeforeIsolationLevel, accountId, startBeforeValue);
                Callable<Account> startAfterCallable = createCallable(startAfterSleepBeforeWrite, startAfterSleepBeforeCommit, startAfterIsolationLevel, accountId, startAfterValue);

                Future<Account> startBeforeFuture = threadPoolTaskExecutor.submit(startBeforeCallable);
                Future<Account> startAfterFuture= threadPoolTaskExecutor.submit(startAfterCallable);

                while( !startBeforeFuture.isDone() || !startAfterFuture.isDone() ) {
                    logger.info("Waiting task for accountId {} complete.", accountId);
                    try {
                        Thread.sleep(10 * 1000L );
                    } catch (InterruptedException ex) {
                        logger.info("ignore");
                    }
                }

                if (startBeforeFuture.isDone() && startAfterFuture.isDone()) {
                    try {
                        Account startBeforeAccount= startBeforeFuture.get();
                        Account startAfterAccount = startAfterFuture.get();

                        Integer startBeforeBalance = null;
                        Integer startAfterBalance = null;

                        if (startBeforeAccount != null) {
                            startBeforeBalance = startBeforeAccount.getBalance();
                        }

                        if (startAfterAccount != null) {
                            startAfterBalance = startAfterAccount.getBalance();
                        }

                        Account finalResult = accountRepository.findById(accountId).orElseThrow();

                        logger.info("Test for accountId : {}  startBeforeBalance : {} startAfterBalance : {} finalBalance : {}, endBy {}",
                                accountId, startBeforeBalance, startAfterBalance, finalResult.getBalance(), finalResult.getEndBy());

                        String outputString = String.format("%s,%d,%d,%d,%d,%n", accountId, startBeforeBalance, startAfterBalance, finalResult.getBalance(), finalResult.getEndBy());
                        FileUtils.writeStringToFile(new File(this.OUTPUT_FILE), outputString, StandardCharsets.UTF_8, true);

                    } catch (Exception ex) {
                        logger.error("{}", accountId, ex);
                    }
                }
            }
        }
    }

    private Callable<Account> createCallable(int sleepBeforeWrite, int sleepBeforeCommit, int isolationLevel, String accountId, Integer value) {

        return new Callable<Account>() {
            @Override
            public Account call() throws Exception {
                final String LOG_MSG_ID = String.format("accountId : %s, isolationLevel : %d, value : %d", accountId, isolationLevel, value);
                TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
                transactionTemplate.setIsolationLevel(isolationLevel);
                transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transactionTemplate.afterPropertiesSet();
                try {
                    return transactionTemplate.execute(new TransactionCallback<Account>() {
                        @Override
                        public Account doInTransaction(TransactionStatus status) {
                            try {
                                logger.info("{} : Begin read data ", LOG_MSG_ID);
                                Account accountForUpdate = accountRepository.findById(accountId).orElseThrow();
                                logger.info("{} : End read data ", LOG_MSG_ID);

                                logger.info("{} : Begin sleep before write", LOG_MSG_ID);
                                Thread.sleep( sleepBeforeWrite* 1000L);
                                logger.info("{} : End sleep before write", LOG_MSG_ID);

                                logger.info("{} : Begin call save", LOG_MSG_ID);
                                accountForUpdate
                                        .setEndBy(isolationLevel)
                                        .setBalance( value + accountForUpdate.getBalance() );

                                Account returnAccount = accountRepository.saveAndFlush(accountForUpdate);
                                logger.info("{} : End call save : Account : {}", LOG_MSG_ID, returnAccount);

                                logger.info("{} : Begin sleep before commit ", LOG_MSG_ID);
                                Thread.sleep(sleepBeforeCommit* 1000L);
                                logger.info("{} : End sleep before commit ", LOG_MSG_ID);
                                return returnAccount;
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

                } catch (Exception ex) {
                    logger.error("{} : Found problem : {}", LOG_MSG_ID, ex.getMessage());
                    return null;
                }
            }
        };
    }
}
