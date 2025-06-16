(ns cdq.render.tick-entities
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.ui.error-window :as error-window]
            [cdq.utils :as utils]
            [gdl.ui.stage :as stage]))

(defn- tick-entity! [{:keys [ctx/world] :as ctx} eid entity->tick]
  (doseq [k (keys @eid)]
    (try (when-let [v (k @eid)]
           (ctx/handle-txs! ctx (when-let [f (entity->tick k)]
                                  (f v eid world))))
         (catch Throwable t
           (throw (ex-info "entity-tick"
                           {:k k
                            :entity/id (entity/id @eid)}
                           t))))))

(defn do!
  [{:keys [ctx/world
           ctx/stage]
    :as ctx}
   entity->tick]
  (try
   (doseq [eid (:world/active-entities world)]
     (tick-entity! ctx eid entity->tick))
   (catch Throwable t
     (utils/pretty-pst t)
     (stage/add! stage (error-window/create t))
     #_(bind-root ::error t)))
  ctx)
