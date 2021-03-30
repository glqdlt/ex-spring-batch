package com.glqdlt.ex.springbatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author 전일웅
 */
@Configuration
@EnableBatchProcessing
public class TestLocalBatch {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    private static final Logger logger = LoggerFactory.getLogger(TestLocalBatch.class);

    public static class SimpleItemReader extends AbstractPagingItemReader<Map> {
        List<Map> items = IntStream.rangeClosed(1, 101).boxed()
                .map(x -> {
                    HashMap hashMap = new HashMap();
                    hashMap.put("number", x);
                    return hashMap;
                })
                .collect(Collectors.toList());

        @Override
        protected void doReadPage() {
            if (results == null) {
                results = new CopyOnWriteArrayList<>();
            } else {
                results.clear();
            }


            int page = getPage();
            int pageSize = getPageSize();
            List<Map> resultSet = new ArrayList<>();

            int current = ((page + 1) * pageSize);
            for (int i = page * pageSize; i < current; i++) {
                if (i > 100) {
                    break;
                }
                resultSet.add(items.get(i));
            }
            TestLocalBatch.logger.info("resultset size : {}", resultSet.size());
            results.addAll(resultSet);


        }

        @Override
        protected void doJumpToPage(int itemIndex) {

            TestLocalBatch.logger.info(" call index {}", itemIndex);
        }
    }

    @Bean
    public AbstractPagingItemReader<Map> reader() {

        return new SimpleItemReader();
    }

//
//    @Bean
//    public IteratorItemReader<Map> reader() {
//        List<Map> items = IntStream.rangeClosed(1, 100).boxed()
//                .map(x -> {
//                    HashMap hashMap = new HashMap();
//                    hashMap.put("number", x);
//                    return hashMap;
//                })
//                .collect(Collectors.toList());
//        logger.info("reader : {}", items.size());
//        return new IteratorItemReader<>(items);
//    }

//    read >> processor >> write

    @Bean
    public ItemProcessor<Map, String> mapToString() {
        return new ItemProcessor<Map, String>() {
            @Override
            public String process(Map item) throws Exception {
                String aaa = Optional.ofNullable(item.get("number")).map(Object::toString).orElse("");
                logger.info("processed {}", aaa);
                return aaa;
            }
        };
    }

    @Bean
    public ItemWriter<String> writer() {
        return new ItemWriter<String>() {
            @Override
            public void write(List<? extends String> items) throws Exception {

                items.stream()
                        .forEach(x -> {
                            logger.info("writer {}", x);
                        });

            }
        };
    }


    @Bean
    public Step step1() {
        return stepBuilderFactory.get("step1")
                .<Map, String>chunk(2)
                .reader(reader())
                .processor(mapToString())
                .writer(writer())
                .build();
    }

    @Bean
    public Step step2(){
        return stepBuilderFactory.get("step2")
                .<Map,String>chunk(10)
                .reader(reader())
                .processor(mapToString())
                .writer(writer())
                .build();
    }


    @Bean
    public Job job(Step step1) {
        return jobBuilderFactory.get("job1")
                .incrementer(new RunIdIncrementer())
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {

                        logger.info("before job!");

                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {

                        logger.info("after job!");

                    }
                })
                .flow(step1)
                .end()
                .build();
    }
}