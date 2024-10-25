(ns moon.world.entities
  (:require [clj-commons.pretty.repl :refer [pretty-pst]]
            [gdl.utils :refer [sort-by-order]]
            [moon.component :as component]
            [moon.entity :as entity]
            [moon.graphics :as g]
            [moon.stage :as stage]
            [moon.world :as world]))

(defn- calculate-mouseover-eid []
  (let [player @world/player
        hits (remove #(= (:z-order @%) :z-order/effect) ; or: only items/creatures/projectiles.
                     (world/point->entities
                      (g/world-mouse-position)))]
    (->> entity/render-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(world/line-of-sight? player @%))
         first)))

(defn update-mouseover []
  (let [eid (if (stage/mouse-on-actor?)
              nil
              (calculate-mouseover-eid))]
    [(when world/mouseover-eid
       [:e/dissoc world/mouseover-eid :entity/mouseover?])
     (when eid
       [:e/assoc eid :entity/mouseover? true])
     (fn []
       (.bindRoot #'world/mouseover-eid eid)
       nil)]))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (g/draw-rectangle x y (:width entity) (:height entity) color)))

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
  (let [player @world/player]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              entity/render-order)
            system entity/render-systems
            entity entities
            :when (or (= z-order :z-order/effect)
                      (world/line-of-sight? player entity))]
      (render-entity! system entity))))

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (when-let [v (k @eid)]
       (component/->handle
        (try (entity/tick [k v] eid)
             (catch Throwable t
               (throw (ex-info "entity/tick" {:k k} t)))))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn tick [entities]
  (run! tick-entity entities))
