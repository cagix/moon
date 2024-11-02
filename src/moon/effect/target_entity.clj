(ns moon.effect.target-entity
  (:require [gdl.math.vector :as v]
            [moon.component :as component]
            [moon.body :as body]
            [moon.effect :as effect :refer [source target]]
            [moon.graphics.shape-drawer :as sd]))

(defmethods :maxrange
  (component/info [[_ maxrange]]
    (str "[LIGHT_GRAY]Range " maxrange " meters[]")))

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

(defmethods :effect/target-entity
  {:let {:keys [maxrange entity-effects]}}
  (component/applicable? [_]
    (and target
         (effect/applicable? entity-effects)))

  (component/useful? [_]
    (assert (bound? #'source))
    (assert (bound? #'target))
    (in-range? @source @target maxrange))

  (component/handle [_]
    (let [source* @source
          target* @target]
      (if (in-range? source* target* maxrange)
        (cons
         [:tx/line-render {:start (start-point source* target*)
                           :end (:position target*)
                           :duration 0.05
                           :color [1 0 0 0.75]
                           :thick? true}]
         ; TODO => make new context with end-point ... and check on point entity
         ; friendly fire ?!
         ; player maybe just direction possible ?!

         ; TODO FIXME
         ; have to use tx/effect now ?!
         ; still same context ...
         ; filter applicable ?! - omg
         entity-effects

         )
        [; TODO
         ; * clicking on far away monster
         ; * hitting ground in front of you ( there is another monster )
         ; * -> it doesn't get hit ! hmmm
         ; * either use 'MISS' or get enemy entities at end-point
         [:tx/audiovisual (end-point source* target* maxrange) :audiovisuals/hit-ground]])))

  (component/render [_]
    (when target
      (let [source* @source
            target* @target]
        (sd/line (start-point source* target*)
                 (end-point source* target* maxrange)
                 (if (in-range? source* target* maxrange)
                   [1 0 0 0.5]
                   [1 1 0 0.5]))))))
