(ns cdq.world
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.gdx.math.vector2 :as v]
            [cdq.entity-tick]
            [cdq.grid :as grid]
            [cdq.malli :as m]
            [cdq.potential-fields.update :as potential-fields.update]
            [cdq.raycaster :as raycaster]
            [qrecord.core :as q]))

(q/defrecord Entity [entity/body])

(defn spawn-entity!
  [{:keys [ctx/id-counter
           ctx/entity-ids
           ctx/entity-components
           ctx/spawn-entity-schema
           ctx/content-grid
           ctx/grid]
    :as ctx}
   entity]
  (m/validate-humanize spawn-entity-schema entity)
  (let [build-component (fn [[k v]]
                          (if-let [create (:create (k entity-components))]
                            (create v ctx)
                            v))
        entity (reduce (fn [m [k v]]
                         (assoc m k (build-component [k v])))
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
    (grid/add-entity! grid eid)
    (mapcat (fn [[k v]]
              (when-let [create! (:create! (k entity-components))]
                (create! v eid ctx)))
            @eid)))

(defn move-entity!
  [{:keys [ctx/content-grid
           ctx/grid]}
   [_ eid body direction rotate-in-movement-direction?]]
  (content-grid/position-changed! content-grid eid)
  (grid/position-changed! grid eid)
  (swap! eid assoc-in [:entity/body :body/position] (:body/position body))
  (when rotate-in-movement-direction?
    (swap! eid assoc-in [:entity/body :body/rotation-angle] (v/angle-from-vector direction)))
  nil)

(def destroy-components
  {:entity/destroy-audiovisual
   {:destroy! (fn [audiovisuals-id eid _ctx]
                [[:tx/audiovisual
                  (:body/position (:entity/body @eid))
                  audiovisuals-id]])}})

(defn remove-destroyed-entities!
  [{:keys [ctx/entity-ids
           ctx/grid]
    :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (let [id (:entity/id @eid)]
      (assert (contains? @entity-ids id))
      (swap! entity-ids dissoc id))
    (content-grid/remove-entity! eid)
    (grid/remove-entity! grid eid)
    (ctx/handle-txs! ctx
                     (mapcat (fn [[k v]]
                               (when-let [destroy! (:destroy! (k destroy-components))]
                                 (destroy! v eid ctx)))
                             @eid))))

(defn creatures-in-los-of
  [{:keys [ctx/active-entities
           ctx/raycaster]}
   entity]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(raycaster/line-of-sight? raycaster entity @%))
       (remove #(:entity/player? @%))))

(defn assoc-active-entities
  [{:keys [ctx/content-grid
           ctx/player-eid]
    :as ctx}]
  (assoc ctx :ctx/active-entities (content-grid/active-entities content-grid @player-eid)))

(defn update-potential-fields!
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

(defn tick-entities!
  [{:keys [ctx/active-entities]
    :as ctx}]
  (doseq [eid active-entities]
    (tick-entity! ctx eid)))
