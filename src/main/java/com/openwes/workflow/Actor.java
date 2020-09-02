package com.openwes.workflow;

import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author xuanloc0511@gmail.com
 * @since Sep 1, 2020
 * @version 1.0.0
 *
 */
public class Actor {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(Actor.class);
    private final AtomicBoolean inProcess = new AtomicBoolean(false);
    private final PriorityQueue<Action> actions = new PriorityQueue<>((left, right) -> {
        return (int) (left.getId() - right.getId());
    });
    private final Object mutex = new Object();
    private final ActorProps props = new ActorProps();
    private String id;
    private String actorType;
    private String currentState;
    private TransitionLookup lookup;
    
    void setActorType(String actorType) {
        this.actorType = actorType;
    }
    
    public ActorProps getProps() {
        return props;
    }
    
    TransitionLookup getLookup() {
        return lookup;
    }
    
    Actor setLookup(TransitionLookup lookup) {
        this.lookup = lookup;
        return this;
    }
    
    public final <T extends Actor> T setCurrentState(String currentState) {
        this.currentState = currentState;
        return (T) this;
    }
    
    public final <T extends Actor> T setId(String id) {
        this.id = id;
        return (T) this;
    }
    
    public final String getId() {
        return id;
    }
    
    public final String getCurrentState() {
        return currentState;
    }
    
    void enqueueAction(Action action) {
        actions.add(action);
        nextAction();
    }
    
    void nextAction() {
        try {
            synchronized (mutex) {
                if (inProcess.compareAndSet(true, true)) {
                    //it is being processed
                    return;
                }
                Action action = dequeueAction();
                if (action == null) {
                    inProcess.set(false);
                    return;
                }
                Transition transition = lookup.lookup(currentState, action.getName());
                if (transition == null) {
                    LOGGER.error("Invalid action {}. Actor {} is in state {}", action.getName(), id, currentState);
                    inProcess.set(false);
                    nextAction();
                    return;
                }
                inProcess.set(true);
                Command cmd = new Command()
                        .setActionId(action.getId())
                        .setActorType(actorType)
                        .setActorId(getId())
                        .setProps(props)
                        .setData(action.getData())
                        .setProcessor(transition.getProcessor())
                        .setWatcher(new CommandWatcher() {
                            @Override
                            public void onComplete() {
                                LOGGER.info("Actor {} change state from {} to {}", id, currentState, transition.getTo());
                                setCurrentState(transition.getTo());
                                inProcess.set(false);
                                nextAction();
                            }
                            
                            @Override
                            public void onFail() {
                                LOGGER.info("Process action {} fail. Actor {} keep state {}", action.getName(), id, currentState);
                                inProcess.set(false);
                                nextAction();
                            }
                            
                            @Override
                            public void onError(Throwable t) {
                                LOGGER.error("Actor {} get error with action {}", id, action.getName(), t);
                            }
                        });
                WorkFlowManager.instance()
                        .getExecutor()
                        .submit(cmd);
            }
        } catch (InterruptedException e) {
            LOGGER.error("handle next action get exception ", e);
        }
    }
    
    Action dequeueAction() {
        return actions.poll();
    }
    
    void clearAction() {
        actions.clear();
    }
    
    public int remainingAction() {
        return actions.size();
    }
}
