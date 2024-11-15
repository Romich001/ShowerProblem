import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


//состояния душа
enum SEX {
    MEN, WOMEN,

}

public class ShowerProblem {

    public static void main(String[] args) {
        var shower = new Shower();
        var random = new Random();
        var executor = Executors.newCachedThreadPool();
        for (int i = 0; i < 10; i++) {
            executor.submit(new Person(shower, random.nextInt(0, 2) == 0 ? SEX.MEN : SEX.WOMEN));

        }
        executor.shutdown();
    }
}

class Person implements Runnable {
    private final Random random = new Random();
    Shower shower;
    SEX sex;

    public Person(Shower shower, SEX state) {
        this.shower = shower;
        this.sex = state;
    }

    @Override
    public void run() {

        shower.enterToShower(sex);
        takeShower();
        shower.exitShower();
    }

    private void takeShower() {


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


    }
}

class Shower {

    //считаем находящихся в душе
    private final AtomicInteger inShower = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private boolean isFree = true;
    private SEX showerState;


    public void exitShower() {


        //    последний сообщает что в душе пусто
        if (inShower.decrementAndGet() == 0) {
            lock.lock();
            try {
                isFree = true;
                condition.signalAll();
            } finally {
                lock.unlock();
            }

        }
    }

    public void enterToShower(SEX sex) {

        lock.lock();
        try {
            //если душ свободен то изменинить стутус душа на пол указанный в первом зашедшем в метод треде
            if (isFree) {
                showerState = sex;
                isFree = false;
            } else {
                // если душ не свободен то проверить соответсвует ли пол входящего тому кто внутри
                if (showerState != sex) {
                    //если не соответвует то отправить тред ждать пока не измениться сотстояние душа на FREE
                    while (!isFree) {
                        condition.await();
                    }
                }

            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        inShower.incrementAndGet();
    }
}
