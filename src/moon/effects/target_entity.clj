(ns ^:no-doc moon.effects.target-entity
  (:require [forge.db :as db]
            [forge.math.vector :as v]
            [forge.graphics :refer [draw-line]]
            [forge.effects :as effects]
            [moon.entity :as entity]
            [moon.world :as world]))

(defn- in-range? [entity target* maxrange] ; == circle-collides?
  (< (- (float (v/distance (:position entity)
                           (:position target*)))
        (float (:radius entity))
        (float (:radius target*)))
     (float maxrange)))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity target*]
  (v/add (:position entity)
         (v/scale (entity/direction entity target*)
                  (:radius entity))))

(defn- end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (entity/direction entity target*)
                  maxrange)))

(defn applicable? [{:keys [entity-effects]} {:keys [effect/target] :as ctx}]
  (and target
       (effects/applicable? ctx entity-effects)))

(defn useful? [{:keys [maxrange]} {:keys [effect/source effect/target]}]
  (in-range? @source @target maxrange))

(defn handle [{:keys [maxrange entity-effects]} {:keys [effect/source effect/target] :as ctx}]
  (let [source* @source
        target* @target]
    (if (in-range? source* target* maxrange)
      (do
       (world/line-render {:start (start-point source* target*)
                           :end (:position target*)
                           :duration 0.05
                           :color [1 0 0 0.75]
                           :thick? true})
       (effects/do! ctx entity-effects))
      (world/audiovisual (end-point source* target* maxrange) (db/get :audiovisuals/hit-ground)))))

(defn render [{:keys [maxrange]} {:keys [effect/source effect/target]}]
  (when target
    (let [source* @source
          target* @target]
      (draw-line (start-point source* target*)
                 (end-point source* target* maxrange)
                 (if (in-range? source* target* maxrange)
                   [1 0 0 0.5]
                   [1 1 0 0.5])))))
