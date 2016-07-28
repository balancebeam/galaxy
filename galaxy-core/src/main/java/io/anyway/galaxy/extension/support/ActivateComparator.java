package io.anyway.galaxy.extension.support;



import io.anyway.galaxy.extension.Activate;
import io.anyway.galaxy.extension.ExtensionLoader;
import io.anyway.galaxy.extension.SPI;

import java.util.Comparator;

/**
 * OrderComparetor
 * 
 * @author william.liangf
 */
public class ActivateComparator implements Comparator<Object> {
    
    public static final Comparator<Object> COMPARATOR = new ActivateComparator();

    public int compare(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        if (o1.equals(o2)) {
            return 0;
        }
        Activate a1 = o1.getClass().getAnnotation(Activate.class);
        Activate a2 = o2.getClass().getAnnotation(Activate.class);

        int n1 = a1 == null ? 0 : a1.order();
        int n2 = a2 == null ? 0 : a2.order();
        return n1 > n2 ? 1 : -1; // 就算n1 == n2也不能返回0，否则在HashSet等集合中，会被认为是同一值而覆盖
    }

}
