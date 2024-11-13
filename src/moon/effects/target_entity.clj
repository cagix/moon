(ns moon.effects.target-entity
  (:require [gdl.db :as db]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.math.vector :as v]
            [moon.body :as body]
            [moon.effects :as effects]
            [moon.world.entities :as entities]))

(defn- in-range? [entity target* maxrange] ; == circle-collides?
  (< (- (float (v/distance (:position entity)
                           (:position target*)))
        (float (:radius entity))
        (float (:radius target*)))
     (float maxrange)))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity target*]
  (v/add (:position entity)
         (v/scale (body/direction entity target*)
                  (:radius entity))))

(defn- end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (body/direction entity target*)
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
       (entities/line-render {:start (start-point source* target*)
                              :end (:position target*)
                              :duration 0.05
                              :color [1 0 0 0.75]
                              :thick? true})
       (effects/do! ctx entity-effects))
      (entities/audiovisual (end-point source* target* maxrange) (db/get :audiovisuals/hit-ground)))))

(defn render [{:keys [maxrange]} {:keys [effect/source effect/target]}]
  (when target
    (let [source* @source
          target* @target]
      (sd/line (start-point source* target*)
               (end-point source* target* maxrange)
               (if (in-range? source* target* maxrange)
                 [1 0 0 0.5]
                 [1 1 0 0.5])))))
