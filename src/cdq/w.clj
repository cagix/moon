(ns cdq.w
  (:require [cdq.cell :as cell]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.raycaster :as raycaster]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [gdl.math.vector2 :as v]))

(defn line-of-sight? [{:keys [world/raycaster]}
                      source
                      target]
  (assert raycaster)
  (not (raycaster/blocked? raycaster
                           (entity/position source)
                           (entity/position target))))

(defn nearest-enemy-distance [{:keys [world/grid]} entity]
  (cell/nearest-entity-distance @(grid/cell grid (mapv int (entity/position entity)))
                                (entity/enemy entity)))

(defn nearest-enemy [{:keys [world/grid]} entity]
  (cell/nearest-entity @(grid/cell grid (mapv int (entity/position entity)))
                       (entity/enemy entity)))

(defn potential-field-find-direction [{:keys [world/grid]} eid]
  (potential-fields.movement/find-direction grid eid))

(defn creatures-in-los-of-player
  [{:keys [world/active-entities
           world/player-eid]
    :as world}]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(line-of-sight? world @player-eid @%))
       (remove #(:entity/player? @%))))

(defn npc-effect-ctx [world eid]
  (let [entity @eid
        target (nearest-enemy world entity)
        target (when (and target
                          (line-of-sight? world entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (v/direction (entity/position entity)
                                             (entity/position @target)))}))

(defn path-blocked? [{:keys [world/raycaster]} start end width]
  (raycaster/path-blocked? raycaster start end width))
