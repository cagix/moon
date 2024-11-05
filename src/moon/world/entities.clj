(ns moon.world.entities
  (:require [clj-commons.pretty.repl :refer [pretty-pst]]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.graphics.world-view :as world-view]
            [gdl.utils :refer [sort-by-order]]
            [moon.component :as component]
            [moon.body :as body]
            [moon.entity :as entity]
            [moon.player :as player]
            [moon.world.content-grid :as content-grid]
            [moon.world.grid :as grid]
            [moon.world.line-of-sight :refer [line-of-sight?]]))

(declare ^:private ids->eids)

(defn all [] (vals ids->eids))
(defn get-entity [id] (get ids->eids id))

(declare ^:private content-grid)

(defn active []
  (content-grid/active-entities content-grid @player/eid))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (sd/rectangle x y (:width entity) (:height entity) color)))

(defn- render-entity! [system entity]
  (try
   (when show-body-bounds
     (draw-body-rect entity (if (:collides? entity) :white :gray)))
   (run! #(system % entity) entity)
   (catch Throwable t
     (draw-body-rect entity :red)
     (pretty-pst t 12))))

(defn render
  "Draws entities in the correct z-order and in the order of render-systems for each z-order."
  [entities]
  (let [player @player/eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              body/render-order)
            system entity/render-systems
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (render-entity! system entity))))

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (component/->handle (entity/tick [k v] eid)))
          (catch Throwable t
            (throw (ex-info "entity/tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn tick [entities]
  (run! tick-entity entities))

(defn- add-to-world [eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (alter-var-root #'ids->eids assoc id eid))
  (content-grid/update-entity! content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (grid/add-entity eid))

(defn- remove-from-world [eid]
  (let [id (:entity/id @eid)]
    (assert (contains? ids->eids id))
    (alter-var-root #'ids->eids dissoc id))
  (content-grid/remove-entity! eid)
  (grid/remove-entity eid))

(defn- position-changed [eid]
  (content-grid/update-entity! content-grid eid)
  (grid/entity-position-changed eid))

(defmethod component/handle :world/entity [[_ op eid]]
  (case op
    :add              (add-to-world      eid)
    :remove           (remove-from-world eid)
    :position-changed (position-changed  eid))
  nil)
