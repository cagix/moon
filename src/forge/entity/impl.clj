(ns forge.entity.impl
  (:require [forge.entity :as entity]
            [forge.entity.components :refer [hitpoints enemy add-skill collides? remove-mods event]]
            [forge.entity.inventory :as inventory]
            [forge.graphics :refer [draw-filled-circle draw-text draw-filled-rectangle pixels->world-units draw-rotated-centered draw-line with-line-width draw-ellipse draw-centered]]
            [forge.item :as item]
            [forge.val-max :as val-max]
            [forge.world :as world :refer [audiovisual timer stopped? player-eid]]
            [malli.core :as m]
            [reduce-fsm :as fsm]))

(comment
 (def ^:private entity
   {:optional [#'entity/->v
               #'entity/create
               #'entity/destroy
               #'entity/tick
               #'entity/render-below
               #'entity/render
               #'entity/render-above
               #'entity/render-info]}))

(def ^:private hpbar-colors
  {:green     [0 0.8 0]
   :darkgreen [0 0.5 0]
   :yellow    [0.5 0.5 0]
   :red       [0.5 0 0]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn- draw-hpbar [{:keys [position width half-width half-height]}
                   ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (pixels->world-units 5)
          border (pixels->world-units borders-px)]
      (draw-filled-rectangle x y width height black)
      (draw-filled-rectangle (+ x border)
                             (+ y border)
                             (- (* width ratio) (* 2 border))
                             (- height (* 2 border))
                             (hpbar-color ratio)))))

(defmethods :entity/hp
  (entity/->v [[_ v]]
    [v v])

  (entity/render-info [_ entity]
    (let [ratio (val-max/ratio (hitpoints entity))]
      (when (or (< ratio 1) (:entity/mouseover? entity))
        (draw-hpbar entity ratio)))))

(defmethod entity/->v :entity/mana [[_ v]] [v v])

(defmethods :entity/temp-modifier
  (entity/tick [[k {:keys [modifiers counter]}] eid]
    (when (stopped? counter)
      (swap! eid dissoc k)
      (swap! eid remove-mods modifiers)))

  ; TODO draw opacity as of counter ratio?
  (entity/render-above [_ entity]
    (draw-filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4])))

(defmethods :entity/string-effect
  (entity/tick  [[k {:keys [counter]}] eid]
    (when (stopped? counter)
      (swap! eid dissoc k)))

  (entity/render-above [[_ {:keys [text]}] entity]
    (let [[x y] (:position entity)]
      (draw-text {:text text
                  :x x
                  :y (+ y (:half-height entity) (pixels->world-units 5))
                  :scale 2
                  :up? true}))))

(defmethod entity/destroy :entity/destroy-audiovisual [[_ audiovisuals-id] eid]
  (audiovisual (:position @eid) (build audiovisuals-id)))

(def ^:private shout-radius 4)

(defn- friendlies-in-radius [position faction]
  (->> {:position position
        :radius shout-radius}
       world/circle->entities
       (filter #(= (:entity/faction @%) faction))))

(defmethod entity/tick :entity/alert-friendlies-after-duration
  [[_ {:keys [counter faction]}] eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius (:position @eid) faction)]
      (event friendly-eid :alert))))

(defmethods :entity/delete-after-duration
  (entity/->v [duration]
    (timer duration))

  (entity/tick [counter eid]
    (when (stopped? counter)
      (swap! eid assoc :entity/destroyed? true))))

(def ^:private npc-fsm
  (fsm/fsm-inc
   [[:npc-sleeping
     :kill -> :npc-dead
     :stun -> :stunned
     :alert -> :npc-idle]
    [:npc-idle
     :kill -> :npc-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :movement-direction -> :npc-moving]
    [:npc-moving
     :kill -> :npc-dead
     :stun -> :stunned
     :timer-finished -> :npc-idle]
    [:active-skill
     :kill -> :npc-dead
     :stun -> :stunned
     :action-done -> :npc-idle]
    [:stunned
     :kill -> :npc-dead
     :effect-wears-off -> :npc-idle]
    [:npc-dead]]))

(def ^:private player-fsm
  (fsm/fsm-inc
   [[:player-idle
     :kill -> :player-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :pickup-item -> :player-item-on-cursor
     :movement-input -> :player-moving]
    [:player-moving
     :kill -> :player-dead
     :stun -> :stunned
     :no-movement-input -> :player-idle]
    [:active-skill
     :kill -> :player-dead
     :stun -> :stunned
     :action-done -> :player-idle]
    [:stunned
     :kill -> :player-dead
     :effect-wears-off -> :player-idle]
    [:player-item-on-cursor
     :kill -> :player-dead
     :stun -> :stunned
     :drop-item -> :player-idle
     :dropped-item -> :player-idle]
    [:player-dead]]))

; fsm throws when initial-state is not part of states, so no need to assert initial-state
; initial state is nil, so associng it. make bug report at reduce-fsm?
(defn- ->init-fsm [fsm initial-state]
  (assoc (fsm initial-state nil) :state initial-state))

(defmethod entity/create :entity/fsm [[k {:keys [fsm initial-state]}] eid]
  (swap! eid assoc
         k (->init-fsm (case fsm
                         :fsms/player player-fsm
                         :fsms/npc npc-fsm)
                       initial-state)
         initial-state (entity/->v [initial-state eid])))

(defmethods :entity/projectile-collision
  (entity/->v [[_ v]]
    (assoc v :already-hit-bodies #{}))

  (entity/tick [[k {:keys [entity-effects already-hit-bodies piercing?]}] eid]
    ; TODO this could be called from body on collision
    ; for non-solid
    ; means non colliding with other entities
    ; but still collding with other stuff here ? o.o
    (let [entity @eid
          cells* (map deref (world/rectangle->cells entity)) ; just use cached-touched -cells
          hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                       (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                             (:entity/faction @%))
                                       (:collides? @%)
                                       (collides? entity @%))
                                 (world/cells->entities cells*))
          destroy? (or (and hit-entity (not piercing?))
                       (some #(world/blocked? % (:z-order entity)) cells*))]
      (when destroy?
        (swap! eid assoc :entity/destroyed? true))
      (when hit-entity
        (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
      (when hit-entity
        (effects-do! {:effect/source eid :effect/target hit-entity} entity-effects)))))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [{:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (world/rectangle->cells body))]
    (and (not-any? #(world/blocked? % z-order) cells*)
         (->> cells*
              world/cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (collides? other-entity body)))))))))

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

(defmethod entity/tick :entity/movement
  [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
   eid]
  (assert (m/validate world/speed-schema speed)
          (pr-str speed))
  (assert (or (zero? (v-length direction))
              (v-normalised? direction))
          (str "cannot understand direction: " (pr-str direction)))
  (when-not (or (zero? (v-length direction))
                (nil? speed)
                (zero? speed))
    (let [movement (assoc movement :delta-time world/delta)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body body movement)
                        (move-body body movement))]
        (world/position-changed eid)
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v-angle-from-vector direction)))))))

(defmethods :entity/skills
  (entity/create [[k skills] eid]
    (swap! eid assoc k nil)
    (doseq [skill skills]
      (swap! eid add-skill skill)))

  (entity/tick [[k skills] eid]
    (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
            :when (and cooling-down?
                       (stopped? cooling-down?))]
      (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false))))

(defmethod entity/create :entity/inventory [[k items] eid]
  (swap! eid assoc k item/empty-inventory)
  (doseq [item items]
    (inventory/pickup-item eid item)))

(defmethod entity/render :entity/clickable [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity}]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (draw-text {:text text
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))

(defmethod entity/render :entity/line-render [[_ {:keys [thick? end color]}] entity]
  (let [position (:position entity)]
    (if thick?
      (with-line-width 4
        #(draw-line position end color))
      (draw-line position end color))))

(defmethod entity/render :entity/image [[_ image] entity]
  (draw-rotated-centered image
                         (or (:rotation-angle entity) 0)
                         (:position entity)))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethod entity/render-below :entity/mouseover? [_ {:keys [entity/faction] :as entity}]
  (let [player @player-eid]
    (with-line-width 3
      #(draw-ellipse (:position entity)
                     (:half-width entity)
                     (:half-height entity)
                     (cond (= faction (enemy player))
                           enemy-color
                           (= faction (:entity/faction player))
                           friendly-color
                           :else
                           neutral-color)))))
