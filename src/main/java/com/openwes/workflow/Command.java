package com.openwes.workflow;

import com.openwes.core.utils.ClockService;

/**
 *
 * @author xuanloc0511@gmail.com
 */
class Command {

    private final long started = ClockService.nowNS();
    private long actionId;
    private String txId;
    private String actorType;
    private String processor;
    private Object data;
    private String actorId;
    private ActorProps props;
    private CommandWatcher watcher;
    private ActionEndHandler endHandler;

    public String getTxId() {
        return txId;
    }

    public Command setTxId(String txId) {
        this.txId = txId;
        return this;
    }

    public Command setActionId(long actionId) {
        this.actionId = actionId;
        return this;
    }

    public long getActionId() {
        return actionId;
    }

    public ActorProps getProps() {
        return props;
    }

    public Command setProps(ActorProps props) {
        this.props = props;
        return this;
    }

    public String getProcessor() {
        return processor;
    }

    public Command setProcessor(String processor) {
        this.processor = processor;
        return this;
    }

    public Object getData() {
        return data;
    }

    public Command setData(Object data) {
        this.data = data;
        return this;
    }

    public String getActorId() {
        return actorId;
    }

    public Command setActorId(String actorId) {
        this.actorId = actorId;
        return this;
    }

    public Command setActorType(String actorType) {
        this.actorType = actorType;
        return this;
    }

    public String getActorType() {
        return actorType;
    }

    public Command setWatcher(CommandWatcher watcher) {
        this.watcher = watcher;
        return this;
    }

    public ActionEndHandler getEndHandler() {
        return endHandler;
    }

    public Command setEndHandler(ActionEndHandler endHandler) {
        this.endHandler = endHandler;
        return this;
    }

    public long getStarted() {
        return started;
    }

    public void complete() {
        watcher.onComplete();
        if (endHandler != null) {
            endHandler.onCompleted(actorId, props, data);
        }
    }

    public void fail() {
        watcher.onFail();
        if (endHandler != null) {
            endHandler.onFailure(actorId, props, data);
        }
    }

    public void error(Throwable t) {
        fail();
        watcher.onError(t);
        if (endHandler != null) {
            endHandler.onError(t);
        }
    }

}
