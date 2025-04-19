//1. Project Dependencies (Maven)
<dependencies>
    <!-- Spring Framework -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>5.3.30</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-orm</artifactId>
        <version>5.3.30</version>
    </dependency>

    <!-- Hibernate -->
    <dependency>
        <groupId>org.hibernate</groupId>
        <artifactId>hibernate-core</artifactId>
        <version>5.6.15.Final</version>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>

    <!-- JPA -->
    <dependency>
        <groupId>jakarta.persistence</groupId>
        <artifactId>jakarta.persistence-api</artifactId>
        <version>2.2.3</version>
    </dependency>

    <!-- Spring Tx -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-tx</artifactId>
        <version>5.3.30</version>
    </dependency>
</dependencies>

//2. hibernate.cfg.xml
<hibernate-configuration>
    <session-factory>
        <property name="hibernate.dialect">org.hibernate.dialect.MySQL8Dialect</property>
        <property name="hibernate.connection.driver_class">com.mysql.cj.jdbc.Driver</property>
        <property name="hibernate.connection.url">jdbc:mysql://localhost:3306/bankdb</property>
        <property name="hibernate.connection.username">root</property>
        <property name="hibernate.connection.password">your_password</property>
        <property name="hibernate.hbm2ddl.auto">update</property>
        <property name="show_sql">true</property>

        <mapping class="com.example.Account"/>
        <mapping class="com.example.BankTransaction"/>
    </session-factory>
</hibernate-configuration>

//3. Entity: Account.java
package com.example;

import javax.persistence.*;

@Entity
public class Account {
    @Id
    private int id;
    private String name;
    private double balance;

    public Account() {}
    public Account(int id, String name, double balance) {
        this.id = id; this.name = name; this.balance = balance;
    }

    // Getters and Setters
}

//4. Entity: BankTransaction.java
package com.example;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class BankTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int fromAccountId;
    private int toAccountId;
    private double amount;
    private LocalDateTime date;

    public BankTransaction() {}
    public BankTransaction(int from, int to, double amt) {
        this.fromAccountId = from;
        this.toAccountId = to;
        this.amount = amt;
        this.date = LocalDateTime.now();
    }

    // Getters and Setters
}

//5. BankService.java
package com.example;

import org.springframework.transaction.annotation.Transactional;
import org.hibernate.SessionFactory;
import org.hibernate.Session;

public class BankService {
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Transactional
    public void transferMoney(int fromId, int toId, double amount) {
        Session session = sessionFactory.getCurrentSession();

        Account from = session.get(Account.class, fromId);
        Account to = session.get(Account.class, toId);

        if (from.getBalance() < amount) {
            throw new RuntimeException("Insufficient funds");
        }

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        session.update(from);
        session.update(to);
        session.save(new BankTransaction(fromId, toId, amount));
    }
}

//6. Spring Configuration: AppConfig.java
package com.example;

import org.springframework.context.annotation.*;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Properties;

@Configuration
@EnableTransactionManagement
@ComponentScan("com.example")
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/bankdb");
        ds.setUsername("root");
        ds.setPassword("your_password");
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return ds;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean factory = new LocalSessionFactoryBean();
        factory.setDataSource(dataSource());
        factory.setAnnotatedClasses(Account.class, BankTransaction.class);

        Properties props = new Properties();
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        props.setProperty("hibernate.show_sql", "true");
        props.setProperty("hibernate.hbm2ddl.auto", "update");

        factory.setHibernateProperties(props);
        return factory;
    }

    @Bean
    public HibernateTransactionManager transactionManager() {
        HibernateTransactionManager txManager = new HibernateTransactionManager();
        txManager.setSessionFactory(sessionFactory().getObject());
        return txManager;
    }

    @Bean
    public BankService bankService() {
        BankService service = new BankService();
        service.setSessionFactory(sessionFactory().getObject());
        return service;
    }
}

//7. Test the Transactions: MainApp.java
package com.example;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MainApp {
    public static void main(String[] args) {
        var context = new AnnotationConfigApplicationContext(AppConfig.class);
        BankService bankService = context.getBean(BankService.class);

        // Add initial data (if not already present)
        var session = context.getBeanFactory().getBean("sessionFactory", org.hibernate.SessionFactory.class).openSession();
        session.beginTransaction();
        session.saveOrUpdate(new Account(1, "Alice", 1000));
        session.saveOrUpdate(new Account(2, "Bob", 500));
        session.getTransaction().commit();
        session.close();

        try {
            bankService.transferMoney(1, 2, 200); // ✅ Success
            bankService.transferMoney(2, 1, 1000); // ❌ Fail - Insufficient
        } catch (Exception e) {
            System.out.println("Transaction failed: " + e.getMessage());
        }

        context.close();
    }
}
