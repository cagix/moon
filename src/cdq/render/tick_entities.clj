(ns cdq.render.tick-entities
  (:require [cdq.entity :as entity]
            [cdq.ui.error-window :as error-window]
            [cdq.utils :as utils]
            [cdq.world :as world]
            [gdl.ui.stage :as stage]))

(defn- tick-entities! [ctx entities entity->tick]
  (doseq [eid entities]
    (try
     (doseq [k (keys @eid)]
       (try (when-let [v (k @eid)] ; component might have been removed
              (world/handle-txs! ctx (when-let [f (entity->tick k)]
                                       (f [k v] eid ctx))))
            (catch Throwable t
              (throw (ex-info "entity-tick" {:k k} t)))))
     (catch Throwable t
       (throw (ex-info (str "entity/id: " (entity/id @eid)) {} t))))))

(defn do!
  [{:keys [ctx/active-entities
           ctx/stage]
    :as ctx}
   entity->tick]
  (try
   (tick-entities! ctx active-entities entity->tick)
   (catch Throwable t
     (utils/pretty-pst t)
     (stage/add! stage (error-window/create t))
     #_(bind-root ::error t)))
  ctx)
