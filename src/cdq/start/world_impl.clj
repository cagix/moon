(ns cdq.start.world-impl
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.entity-tick]
            [cdq.raycaster :as raycaster]
            [cdq.potential-fields.update :as potential-fields.update]
            [cdq.world]))

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

(defn do! [ctx]
  (extend-type (class ctx)
    cdq.world/World
    (creatures-in-los-of
      [{:keys [ctx/active-entities
               ctx/raycaster]}
       entity]
      (->> active-entities
           (filter #(:entity/species @%))
           (filter #(raycaster/line-of-sight? raycaster entity @%))
           (remove #(:entity/player? @%))))

    (assoc-active-entities
      [{:keys [ctx/content-grid
               ctx/player-eid]
        :as ctx}]
      (assoc ctx :ctx/active-entities (content-grid/active-entities content-grid @player-eid)))

    (update-potential-fields!
      [{:keys [ctx/factions-iterations
               ctx/potential-field-cache
               ctx/grid
               ctx/active-entities]}]
      (doseq [[faction max-iterations] factions-iterations]
        (potential-fields.update/tick! potential-field-cache
                                       grid
                                       faction
                                       active-entities
                                       max-iterations)))

    (tick-entities!
      [{:keys [ctx/active-entities]
        :as ctx}]
      (doseq [eid active-entities]
        (tick-entity! ctx eid))))
  ctx)
