(ns moon.entity.movement
  (:require [gdl.math.vector :as v]
            [malli.core :as m]
            [moon.component :as component]
            [moon.body :as body]
            [moon.entity :as entity]
            [moon.world.grid :as grid]
            [moon.world.time :as time]))

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta-time 0.04)

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ body/minimum-size max-delta-time)) ; need to make var because m/schema would fail later if divide / is inside the schema-form
(def speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [{:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (grid/rectangle->cells body))]
    (and (not-any? #(grid/blocked? % z-order) cells*)
         (->> cells*
              grid/cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (body/collides? other-entity body)))))))))

(defn- try-move [body movement]
  (let [new-body (move-body body movement)]
    (when (valid-position? new-body)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [body {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move body movement)
        (try-move body (assoc movement :direction [xdir 0]))
        (try-move body (assoc movement :direction [0 ydir])))))

(defc :entity/movement
  {:let {:keys [direction speed rotate-in-movement-direction?] :as movement}}
  (entity/tick [_ eid]
    (assert (m/validate speed-schema speed)
            (pr-str speed))
    (assert (or (zero? (v/length direction))
                (v/normalised? direction)))
    (when-not (or (zero? (v/length direction))
                  (nil? speed)
                  (zero? speed))
      (let [movement (assoc movement :delta-time time/delta)
            body @eid]
        (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                          (try-move-solid-body body movement)
                          (move-body body movement))]
          [[:e/assoc eid :position    (:position    body)]
           [:e/assoc eid :left-bottom (:left-bottom body)]
           (when rotate-in-movement-direction?
             [:e/assoc eid :rotation-angle (v/angle-from-vector direction)])
           [:tx/position-changed eid]])))))

(defc :tx/set-movement
  (component/handle [[_ eid movement]]
    (assert (or (nil? movement)
                (nil? (:direction movement))
                (and (:direction movement) ; continue schema of that ...
                     #_(:speed movement)))) ; princess no stats/movement-speed, then nil and here assertion-error
    [(if (or (nil? movement)
             (nil? (:direction movement)))
       [:e/dissoc eid :entity/movement]
       [:e/assoc eid :entity/movement movement])]))
