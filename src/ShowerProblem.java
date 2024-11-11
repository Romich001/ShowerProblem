import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


//состояния душа
enum SHOWER_STATE {
    MEN,
    WOMEN,
    FREE
}

public class ShowerProblem {

    public static void main(String[] args) {
        Shower shower = new Shower();
        var random = new Random();

        for (int i = 0; i < 10; i++) {
            new Thread(
                    //случайный пол у Person
                    new Person(shower,
                            random.nextInt(0, 2) == 0 ? SHOWER_STATE.MEN : SHOWER_STATE.WOMEN),
                    String.valueOf(i)
            ).start();
        }
    }
}

class Person implements Runnable {
    Shower shower;
    SHOWER_STATE sex;

    public Person(Shower shower, SHOWER_STATE state) {
        this.shower = shower;
        this.sex = state;
    }

    @Override
    public void run() {

        shower.takeShower(sex);
    }
}

class Shower {

    //считаем находящихся в душе
    private final AtomicInteger inShower = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();
    private final Random random = new Random();
    private final Condition condition = lock.newCondition();
    private SHOWER_STATE showerState = SHOWER_STATE.FREE;

    public void takeShower(SHOWER_STATE sex) {
//      вход в душ по одному
        enterToShower(sex);
        inShower.incrementAndGet();

//        душ
        System.out.println(sex.name() + " start taking a shower! in thread " + Thread.currentThread().getName());
//      симулируем помывку
        try {
            // помывка занимает случайное время от 2 до 6 секунд
            TimeUnit.SECONDS.sleep(random.nextInt(2, 6));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(sex.name() + " finish taking a shower! in thread " + Thread.currentThread().getName());

//        выход из душа
        exitShower();


    }

    private void exitShower() {
        //    последний сообщает что в душе пусто
        if (inShower.decrementAndGet() == 0) {
            lock.lock();
            try {
                showerState = SHOWER_STATE.FREE;
                condition.signalAll();
            } finally {
                lock.unlock();
            }

        }
    }

    private void enterToShower(SHOWER_STATE sex) {
        lock.lock();
        try {
            //если душ свободен то изменинить стутус душа на пол указанный в первом зашедшем в метод треде
            if (showerState == SHOWER_STATE.FREE) {
                showerState = sex;
            } else {
                // если душ не свободен то проверить соответсвует ли пол входящего тому кто внутри
                if (showerState != sex) {
                    //если не соответвует то отправить тред ждать пока не измениться сотстояние душа на FREE
                    while (showerState != SHOWER_STATE.FREE) {
                        condition.await();

                    }
                }

            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
