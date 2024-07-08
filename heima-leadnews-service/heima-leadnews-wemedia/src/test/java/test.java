import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class test {
    @Test
    public void test(){
        int[] nums = new int[3];
        System.out.println(nums[1]);
    }

    public class testClass {
        private int i;
        private Integer j;

        public  void print(){
            System.out.println(i);
            System.out.println(j);
        }
    }

    @Test
    public void test1(){
        List<stu> list = new ArrayList<>();

        List<Integer> integers = list.stream().map(stu::getAge).collect(Collectors.toList());
        System.out.println(integers);
    }


    public class stu{
        private Integer age;
        private String name;

        public Integer getAge() {
            return age;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    public void test3(){
        int[] arr = new int[]{};
        System.out.println(arr);
    }

    @Test
    public void test4(){
        BigDecimal a = new BigDecimal("1.0");
        BigDecimal b = new BigDecimal("0.9");
        BigDecimal c = new BigDecimal("0.8");
        BigDecimal x = a.subtract(b);
        BigDecimal y = b.subtract(c);
        System.out.println(x); /* 0.1 */
        System.out.println(y); /* 0.1 */
        System.out.println(x.equals(y));
        System.out.println(x.compareTo(y));

        BigDecimal m = new BigDecimal("1.0");
        BigDecimal n = new BigDecimal("1.00");
        System.out.println(m.equals(n));
    }

    @Test
    public void test5() throws InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();

        // 创建两个线程，同时向StringBuilder中追加字符串
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                stringBuilder.append("A");
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                stringBuilder.append("B");
            }
        });

        // 启动线程
        thread1.start();
        thread2.start();

        // 等待线程完成
        thread1.join();
        thread2.join();

        // 输出结果
        System.out.println("StringBuilder length: " + stringBuilder.length());
        System.out.println("StringBuilder content: " + stringBuilder.toString());
    }
}
