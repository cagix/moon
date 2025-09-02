(ns cdq.tx.spawn-entity
  (:require [cdq.ctx.world :as world]
            [cdq.malli :as m]
            [qrecord.core :as q]))

(def ^:private components-schema
  (m/schema [:map {:closed true}
             [:entity/body :some]
             [:entity/image {:optional true} :some]
             [:entity/animation {:optional true} :some]
             [:entity/delete-after-animation-stopped? {:optional true} :some]
             [:entity/alert-friendlies-after-duration {:optional true} :some]
             [:entity/line-render {:optional true} :some]
             [:entity/delete-after-duration {:optional true} :some]
             [:entity/destroy-audiovisual {:optional true} :some]
             [:entity/fsm {:optional true} :some]
             [:entity/player? {:optional true} :some]
             [:entity/free-skill-points {:optional true} :some]
             [:entity/click-distance-tiles {:optional true} :some]
             [:entity/clickable {:optional true} :some]
             [:property/id {:optional true} :some]
             [:property/pretty-name {:optional true} :some]
             [:creature/level {:optional true} :some]
             [:entity/faction {:optional true} :some]
             [:entity/species {:optional true} :some]
             [:entity/movement {:optional true} :some]
             [:entity/skills {:optional true} :some]
             [:creature/stats {:optional true} :some]
             [:entity/inventory    {:optional true} :some]
             [:entity/item {:optional true} :some]
             [:entity/projectile-collision {:optional true} :some]]))

(q/defrecord Entity [entity/body])

(defn do!
  [[_ components]
   {:keys [ctx/world]}]
  (m/validate-humanize components-schema components)
  (assert (and (not (contains? components :entity/id))))
  (let [{:keys [world/id-counter]} world]
    (let [eid (atom (merge (map->Entity {})
                           (reduce (fn [m [k v]]
                                     (assoc m k (if-let [create (:create (k world/entity-components))]
                                                  (create v world)
                                                  v)))
                                   {}
                                   (assoc components :entity/id (swap! id-counter inc)))))]
      (world/context-entity-add! world eid)
      (mapcat (fn [[k v]]
                (when-let [create! (:create! (k world/entity-components))]
                  (create! v eid world)))
              @eid))))
