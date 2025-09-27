(ns cdq.tx.spawn-entity
  (:require [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [cdq.malli :as m]
            [qrecord.core :as q]))

(def ^:private create-fns
  (update-vals '{:entity/animation             cdq.entity.animation/create
                 :entity/body                  cdq.entity.body/create
                 :entity/delete-after-duration cdq.entity.delete-after-duration/create
                 :entity/projectile-collision  cdq.entity.projectile-collision/create
                 :entity/stats               cdq.entity.stats/create}
               (fn [sym]
                 (let [avar (requiring-resolve sym)]
                   (assert avar sym)
                   avar))))

(defn- create-component [[k v] world]
  (if-let [f (create-fns k)]
    (f v world)
    v))

(def ^:private create!-fns
  (update-vals '{:entity/fsm                             cdq.entity.fsm/create!
                 :entity/inventory                       cdq.entity.inventory/create!
                 :entity/skills                          cdq.entity.skills/create!}
               (fn [sym]
                 (let [avar (requiring-resolve sym)]
                   (assert avar sym)
                   avar))))

(defn- after-create-component [[k v] eid world]
  (when-let [f (create!-fns k)]
    (f v eid world)))

(q/defrecord Entity [entity/body])

(defn do! [{:keys [ctx/world]} entity]
  (let [{:keys [world/content-grid
                world/entity-ids
                world/grid
                world/id-counter
                world/spawn-entity-schema]} world
        _ (m/validate-humanize spawn-entity-schema entity)
        entity (reduce (fn [m [k v]]
                         (assoc m k (create-component [k v] world)))
                       {}
                       entity)
        _ (assert (and (not (contains? entity :entity/id))))
        entity (assoc entity :entity/id (swap! id-counter inc))
        entity (merge (map->Entity {}) entity)
        eid (atom entity)]
    (let [id (:entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid))
    (content-grid/add-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid))
    (grid/set-touched-cells! grid eid)
    (when (:body/collides? (:entity/body @eid))
      (grid/set-occupied-cells! grid eid))
    (mapcat #(after-create-component % eid world) @eid)))
