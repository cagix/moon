(ns cdq.tx.spawn-entity
  (:require [cdq.content-grid :as content-grid]
            [cdq.grid :as grid]
            [cdq.malli :as m]
            [qrecord.core :as q]))

(declare entity-components)

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
   {:keys [ctx/id-counter
           ctx/entity-ids
           ctx/content-grid
           ctx/grid]
    :as ctx}]
  (m/validate-humanize components-schema components)
  (assert (and (not (contains? components :entity/id))))
  (let [eid (atom (merge (map->Entity {})
                         (reduce (fn [m [k v]]
                                   (assoc m k (if-let [create (:create (k entity-components))]
                                                (create v ctx)
                                                v)))
                                 {}
                                 (assoc components :entity/id (swap! id-counter inc)))))]
    (let [id (:entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid))
    (content-grid/add-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid))
    (grid/add-entity! grid eid)
    (mapcat (fn [[k v]]
              (when-let [create! (:create! (k entity-components))]
                (create! v eid ctx)))
            @eid)))
