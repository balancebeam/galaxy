package io.anyway.galaxy.context;

/**
 * Created by yangzz on 16/7/21.
 */
public abstract class TXContextHolder {
    private static ThreadLocal<TXContext> holder= new ThreadLocal<TXContext>();

    public static TXContext getTXContext(){
        return holder.get();
    }

    public static void setTXContext(TXContext ctx){
        holder.set(ctx);
    }
}
