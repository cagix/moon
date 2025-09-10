(ns cdq.render.tick-entities
  (:require [cdq.ctx :as ctx]
            [cdq.entity-tick]
            [cdq.stacktrace :as stacktrace]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn- tick-component! [k v eid ctx]
  (when-let [f (cdq.entity-tick/entity->tick k)]
    (f v eid ctx)))

(defn- tick-entity! [ctx eid]
  (doseq [k (keys @eid)]
    (try (when-let [v (k @eid)]
           (ctx/handle-txs! ctx (tick-component! k v eid ctx)))
         (catch Throwable t
           (throw (ex-info "entity-tick"
                           {:k k
                            :entity/id (:entity/id @eid)}
                           t))))))

(defn- tick-entities!
  [{:keys [ctx/active-entities]
    :as ctx}]
  (doseq [eid active-entities]
    (tick-entity! ctx eid)))

(defn- do!*
  [{:keys [ctx/stage]
    :as ctx}]
  (try
   (tick-entities! ctx)
   (catch Throwable t
     (stacktrace/pretty-print t)
     (stage/add! stage (actor/build {:actor/type :actor.type/error-window
                                     :throwable t}))
     #_(bind-root ::error t)))
  ctx)

(defn do!
  [ctx]
  (if (:ctx/paused? ctx)
    ctx
    (do!* ctx)))
