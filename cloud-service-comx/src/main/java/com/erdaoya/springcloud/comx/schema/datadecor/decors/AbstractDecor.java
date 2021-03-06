package com.erdaoya.springcloud.comx.schema.datadecor.decors;

import com.erdaoya.springcloud.comx.schema.datadecor.DecorException;
import com.erdaoya.springcloud.comx.schema.datadecor.DecorFactory;
import com.erdaoya.springcloud.comx.schema.onError.Strategy;
import com.erdaoya.springcloud.comx.utils.config.ConfigException;
import com.erdaoya.springcloud.comx.context.Context;
import com.erdaoya.springcloud.comx.schema.ConfBaseNode;
import com.erdaoya.springcloud.comx.source.SourceException;
import com.erdaoya.springcloud.comx.utils.config.Config;

import java.util.Set;

/**
 * Created by xue on 12/19/16.
 */
public abstract class AbstractDecor extends ConfBaseNode{
    public static final String FIELD_DECORS                = "decors";
    public static final String FIELD_CACHE                 = "cache";
    public static final String FIELD_PRECONDITION          = "precondition";
    public static final String FIELD_LOCAL_CACHE_ENABLED   = "localCacheEnabled";
    // localCache 未启用

    public static final String FIELD_TYPE          = "type";
    public static final String[] ACCEPTED_TYPES = {
            AbstractDecor.TYPE_ROOT,
            AbstractDecor.TYPE_SCRIPT,
            AbstractDecor.TYPE_COMPOSITION,
            AbstractDecor.TYPE_EACH,
            AbstractDecor.TYPE_BATCH,
            AbstractDecor.TYPE_FIXED,
    };
    public static final String TYPE_COMPOSITION    = "Composition";
    public static final String TYPE_FIXED          = "Fixed";
    public static final String TYPE_ROOT           = "Root";
    public static final String TYPE_BATCH          = "Batch";
    public static final String TYPE_EACH           = "Each";
    public static final String TYPE_SCRIPT         = "Script";


    public AbstractDecor(Config conf){
        super(conf);
    }

    abstract public void doDecorate(Object data, Context context) throws ConfigException, SourceException, DecorException;
    abstract public String getType();



    /**
     * 抛出3类错误 1, ConfigException 2, SourceException 3, DecorException
     * localcache default: disabled
     * @param data
     * @param context
     * @throws ConfigException
     * @throws SourceException
     * @throws DecorException
     */
    public void decorate(Object data, Context context) throws ConfigException, SourceException, DecorException {
        context.setLocalCacheEnabled(conf.bool(FIELD_LOCAL_CACHE_ENABLED, false));
        context.getLogger().debug("Execute Decor:" + this.getType() +  " " + getComxId());
        try {
            // TODO before decorate : precondition node
            // DecorCache decorCache = DecorCache.fromConf(conf.sub(AbstractDecor.FIELD_CACHE), context, data);
            // TODO 处理 decorCache withChildren & withOutChildren

            if (!executePrecondition(data, context)) return;
            this.doDecorate(data, context);
            // TODO decorCache set before children;
            this.executeChildDecors(data, context);
            // TODO decorCache set after children;
        } catch(Exception ex) {
            context.getLogger().error(ex);
            context.getLogger().error("Decorate error:" + ex.getMessage() + "; class:" + ex.getClass());
            Strategy.fromConf(conf.sub("onError")).handleDecorException(ex, context, data);
        }
    }


    /**
     * precondition 默认返回是true; true 时执行
     * @param data
     * @param context
     * @return
     */
    public boolean executePrecondition(Object data, Context context){
        String precondition = conf.str(FIELD_PRECONDITION, "");
        if (precondition.isEmpty()) return true;
        return Precondition.execute(precondition, data, context);
    }








    // TODO 写法变更
    public void executeChildDecors(Object data, Context context) throws ConfigException, SourceException, DecorException{
        this.sequentialExecuteChildDecors(data, context);
    }



    public void sequentialExecuteChildDecors(Object data, Context context) throws ConfigException, SourceException, DecorException{
        Config children = conf.sub(AbstractDecor.FIELD_DECORS);
        Set<String> keys = children.keys();
        for(String key: keys){
            Config conf = children.sub(key);
            AbstractDecor decor = DecorFactory.create(conf);
            decor.decorate(data, context);
        }
    }
}






