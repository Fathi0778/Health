package com.example.healthmate.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.healthmate.data.local.HealthMateDatabase;
import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.data.model.User;
import com.example.healthmate.data.repository.AuthRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/*
 * Login feature: creating accounts and logging in, on a test database.
 */
@RunWith(AndroidJUnit4.class)
public class AuthRepositoryTest {

    private HealthMateDatabase database;
    private ExecutorService executor;
    private AuthRepository auth;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, HealthMateDatabase.class).build();
        executor = Executors.newSingleThreadExecutor();
        auth = new AuthRepository(database.userDao(), database.healthRecordDao(), executor);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        database.close();
    }

    private AuthRepository.AuthResult register(String name, String password) throws InterruptedException {
        AtomicReference<AuthRepository.AuthResult> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        auth.register(name, password, r -> {
            result.set(r);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        return result.get();
    }

    private AuthRepository.AuthResult login(String name, String password) throws InterruptedException {
        AtomicReference<AuthRepository.AuthResult> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        auth.login(name, password, r -> {
            result.set(r);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        return result.get();
    }

    @Test
    public void register_thenLogin_works() throws Exception {
        AuthRepository.AuthResult created = register("Fathima", "secret123");
        assertEquals(AuthRepository.Status.SUCCESS, created.status);
        assertEquals("fathima", created.username); // saved in lower case

        AuthRepository.AuthResult loggedIn = login("FATHIMA", "secret123");
        assertEquals(AuthRepository.Status.SUCCESS, loggedIn.status);
        assertEquals(created.userId, loggedIn.userId);
    }

    @Test
    public void password_isNotSavedAsPlainText() throws Exception {
        register("fathima", "secret123");
        User user = database.userDao().findByUsername("fathima");
        assertNotEquals("secret123", user.getPasswordHash());
    }

    @Test
    public void sameUsernameTwice_isRejected() throws Exception {
        register("fathima", "secret123");
        assertEquals(AuthRepository.Status.USERNAME_TAKEN, register("Fathima", "other123").status);
        assertEquals(1, database.userDao().count());
    }

    @Test
    public void wrongPasswordOrUnknownUser_isRejected() throws Exception {
        register("fathima", "secret123");
        assertEquals(AuthRepository.Status.WRONG_CREDENTIALS, login("fathima", "wrong123").status);
        assertEquals(AuthRepository.Status.WRONG_CREDENTIALS, login("nobody", "secret123").status);
    }

    @Test
    public void firstAccount_getsTheDemoRecords() throws Exception {
        HealthRecord demo = new HealthRecord("2026-10-17", 1, 1, 1, 1); // user_id 0, like the demo data
        database.healthRecordDao().insert(demo);

        long first = register("first", "secret123").userId;
        long second = register("second", "secret123").userId;

        assertEquals(1, database.healthRecordDao().count(first));
        assertEquals(0, database.healthRecordDao().count(second));
    }
}