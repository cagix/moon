(ns cdq.render.remove-destroyed-entities
  (:require [cdq.ctx :as ctx]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]))

(def destroy-components
  {:entity/destroy-audiovisual
   {:destroy! (fn [audiovisuals-id eid _ctx]
                [[:tx/audiovisual
                  (:body/position (:entity/body @eid))
                  audiovisuals-id]])}})

(defn do!
  [{:keys [ctx/entity-ids
           ctx/world]
    :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (let [id (:entity/id @eid)]
      (assert (contains? @entity-ids id))
      (swap! entity-ids dissoc id))
    (content-grid/remove-entity! eid)
    (grid/remove-from-touched-cells! (:world/grid world) eid)
    (when (:body/collides? (:entity/body @eid))
      (grid/remove-from-occupied-cells! (:world/grid world) eid))
    (ctx/handle-txs! ctx
                     (mapcat (fn [[k v]]
                               (when-let [destroy! (:destroy! (k destroy-components))]
                                 (destroy! v eid ctx)))
                             @eid))))
