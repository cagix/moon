(ns components.effect.target-entity
  (:require [math.vector :as v]
            [core.component :refer [defcomponent]]
            [core.data :as data]
            [core.graphics :as g]
            [core.context :as ctx]
            [core.effect :as effect]
            [core.entity :as entity]))

(defn- in-range? [entity* target* maxrange] ; == circle-collides?
  (< (- (float (v/distance (:position entity*)
                           (:position target*)))
        (float (:radius entity*))
        (float (:radius target*)))
     (float maxrange)))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity* target*]
  (v/add (:position entity*)
         (v/scale (entity/direction entity* target*)
                  (:radius entity*))))

(defn- end-point [entity* target* maxrange]
  (v/add (start-point entity* target*)
         (v/scale (entity/direction entity* target*)
                  maxrange)))

(defcomponent :maxrange data/pos-attr)
; TODO how should this work ???
; can not contain the other effects properly o.o
(defcomponent :hit-effects (data/components-attribute :effect))

(defcomponent :effect/target-entity
  {:widget :nested-map ; TODO circular depdenency components-attribute  - cannot use map-attribute..
   :schema [:map {:closed true}
            [:hit-effects [:map]]
            [:maxrange pos?]]
   :default-value {:hit-effects {}
                   :max-range 2.0}
   :doc "Applies hit-effects to a target if they are inside max-range & in line of sight.
        Cancels if line of sight is lost. Draws a red/yellow line wheter the target is inside the max range. If the effect is to be done and target out of range -> draws a hit-ground-effect on the max location."}
  {:keys [maxrange hit-effects]}
  (effect/text [_ ctx]
    (str "Range " maxrange " meters\n"
         (ctx/effect-text ctx hit-effects)))

  (effect/applicable? [_ {:keys [effect/target] :as ctx}]
    (and target
         (ctx/effect-applicable? ctx hit-effects)))

  (effect/useful? [_ {:keys [effect/source effect/target]}]
    (assert source)
    (assert target)
    (in-range? @source @target maxrange))

  (effect/do! [_ {:keys [effect/source effect/target]}]
    (let [source* @source
          target* @target]
      (if (in-range? source* target* maxrange)
        (cons
         [:tx.entity/line-render {:start (start-point source* target*)
                                  :end (:position target*)
                                  :duration 0.05
                                  :color [1 0 0 0.75]
                                  :thick? true}]
         ; TODO => make new context with end-point ... and check on point entity
         ; friendly fire ?!
         ; player maybe just direction possible ?!
         hit-effects)
        [; TODO
         ; * clicking on far away monster
         ; * hitting ground in front of you ( there is another monster )
         ; * -> it doesn't get hit ! hmmm
         ; * either use 'MISS' or get enemy entities at end-point
         [:tx.entity/audiovisual (end-point source* target* maxrange) :audiovisuals/hit-ground]])))

  (effect/render-info [_ g {:keys [effect/source effect/target]}]
    (let [source* @source
          target* @target]
      (g/draw-line g
                   (start-point source* target*)
                   (end-point   source* target* maxrange)
                   (if (in-range? source* target* maxrange)
                     [1 0 0 0.5]
                     [1 1 0 0.5])))))