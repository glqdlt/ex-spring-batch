package com.glqdlt.ex.springbatch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author glqdlt
 * @see <a href='https://kingbbode.tistory.com/38'>https://kingbbode.tistory.com/38</a>
 */
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

//    @Bean
//    public FlatFileItemReader<Person> flatFileItemReader() {
//        return new FlatFileItemReaderBuilder<Person>()
//                .name("personItemReader")
//                .resource(new ClassPathResource("sample-data.csv"))
//                .delimited()
//                .names(new String[]{"firstName", "lastName"})
//                .fieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{setTargetType(Person.class);}})
//                .build();
//    }

    @Bean
    public ItemReader<Person> arrayReader() {
        return new ItemReader<Person>() {
            private final List<Person> p = IntStream.rangeClosed(1, 1000).boxed()
                    .map(x -> {
                        Person person
                                = new Person();
                        person.setFirstName("first" + x);
                        person.setLastName("last" + x);
                        return person;
                    }).collect(Collectors.toList());
            private final AtomicInteger count = new AtomicInteger(0);

            @Override
            public Person read() {
                try {
                    return p.get(count.getAndAdd(1));
                } catch (IndexOutOfBoundsException e) {
                    return null;
                }
            }
        };
    }

    @Bean
    public PersonItemProcessor processor() {
        return new PersonItemProcessor();
    }

    @Bean
    public ItemWriter<Person> jdbcBatchItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Person>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO people (first_name, last_name) VALUES (:firstName, :lastName)")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Job importUserJob(JobCompletionNotificationListener listener, Step step1) {
        return jobBuilderFactory.get("importUserJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(step1)
                .end()
                .build();
    }

    @Bean
    public Step step1(ItemWriter<Person> writer) {
        return stepBuilderFactory.get("step1")
                .<Person, Person>chunk(10)
                .reader(arrayReader())
                .chunk(3)
                .processor(processor())
                .writer(writer)
                .chunk(2)
                .faultTolerant()
                .retryLimit(10000)
                .retry(Throwable.class)
                .build();
    }

}
