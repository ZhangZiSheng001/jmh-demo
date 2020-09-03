package cn.zzs.jmh;

public class CountServiceImpl implements CountService {
    private volatile int count = 0;  
    
    public int count() {  
        return count ++;  
    }  
}
