(ns moon.entity
  (:require [gdl.math.shape :as shape]
            [gdl.math.vector :as v]
            [gdl.utils :refer [define-order ->tile dissoc-in]]
            [malli.core :as m]
            [moon.component :refer [defsystem defc] :as component]))

(defsystem ->v "Create component value. Default returns v.")
(defmethod ->v :default [[_ v]] v)

(defsystem create [_ eid])
(defmethod create :default [_ eid])

(defsystem destroy [_ eid])
(defmethod destroy :default [_ eid])

(defsystem tick [_ eid])
(defmethod tick :default [_ eid])

(defsystem render-below [_ entity])
(defmethod render-below :default [_ entity])

(defsystem render [_ entity])
(defmethod render :default [_ entity])

(defsystem render-above [_ entity])
(defmethod render-above :default [_ entity])

(defsystem render-info [_ entity])
(defmethod render-info :default [_ entity])

(def render-systems [render-below render render-above render-info])

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta-time 0.04)

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def ^:private min-solid-body-size 0.39) ; == spider smallest creature size.

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ min-solid-body-size max-delta-time)) ; need to make var because m/schema would fail later if divide / is inside the schema-form
(def movement-speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

(def ^:private z-orders [:z-order/on-ground
                         :z-order/ground
                         :z-order/flying
                         :z-order/effect])

(def render-order (define-order z-orders))

(defrecord Entity [position
                   left-bottom
                   width
                   height
                   half-width
                   half-height
                   radius
                   collides?
                   z-order
                   rotation-angle])

(defn ->Body [{[x y] :position
               :keys [position
                      width
                      height
                      collides?
                      z-order
                      rotation-angle]}]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? min-solid-body-size 0)))
  (assert (>= height (if collides? min-solid-body-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set z-orders) z-order))
  (assert (or (nil? rotation-angle)
              (<= 0 rotation-angle 360)))
  (map->Entity
   {:position (mapv float position)
    :left-bottom [(float (- x (/ width  2)))
                  (float (- y (/ height 2)))]
    :width  (float width)
    :height (float height)
    :half-width  (float (/ width  2))
    :half-height (float (/ height 2))
    :radius (float (max (/ width  2)
                        (/ height 2)))
    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))

(defn tile [entity]
  (->tile (:position entity)))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

(defn collides? [entity other-entity]
  (shape/overlaps? entity other-entity))

(defn enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))

(defn friend [{:keys [entity/faction]}]
  faction)

(defc :entity/faction
  {:schema [:enum :good :evil]
   :let faction}
  (component/info [_]
    (str "[SLATE]Faction: " (name faction) "[]")))

(defc :e/destroy
  (component/handle [[_ eid]]
    [[:e/assoc eid :entity/destroyed? true]]))

(defc :e/assoc
  (component/handle [[_ eid k v]]
    (assert (keyword? k))
    (swap! eid assoc k v)
    nil))

(defc :e/assoc-in
  (component/handle [[_ eid ks v]]
    (swap! eid assoc-in ks v)
    nil))

(defc :e/dissoc
  (component/handle [[_ eid k]]
    (assert (keyword? k))
    (swap! eid dissoc k)
    nil))

(defc :e/dissoc-in
  (component/handle [[_ eid ks]]
    (swap! eid dissoc-in ks)
    nil))

(defc :e/update
  (component/handle [[_ eid k f]]
    (swap! eid update k f)
    nil))

(declare selected-skill
         can-pickup-item?
         has-skill?
         ai-direction-vector
         stat
         modified-value
         skill-usable-state)

(defn state-k [entity]
  (-> entity :entity/fsm :state))

(defn state-obj [entity]
  (let [k (state-k entity)]
    [k (k entity)]))

(defsystem enter)
(defmethod enter :default [_])

(defsystem exit)
(defmethod exit :default [_])

(defsystem player-enter)
(defmethod player-enter :default [_])

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defsystem manual-tick)
(defmethod manual-tick :default [_])

(defsystem clicked-inventory-cell [_ cell])
(defmethod clicked-inventory-cell :default [_ cell])

(defsystem clicked-skillmenu-skill [_ skill])
(defmethod clicked-skillmenu-skill :default [_ skill])
