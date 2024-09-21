(ns core.entity.state.active-skill
  (:require [utils.core :refer [safe-merge]]
            [core.component :as component :refer [defcomponent]]
            [core.context :as ctx :refer [stopped? finished-ratio ->counter]]
            [core.entity :as entity]
            [core.effect :as effect]
            [core.g :as g]
            [core.state :as state]
            [core.tx :as tx]))

; SCHEMA effect-ctx
; * source = always available
; # npc:
;   * target = maybe
;   * direction = maybe
; # player
;  * target = maybe
;  * target-position  = always available (mouse world position)
;  * direction  = always available (from mouse world position)

(defcomponent :tx/effect
  (tx/do! [[_ effect-ctx effects] ctx]
    (-> ctx
        (merge effect-ctx)
        (ctx/do! (filter #(effect/applicable? % effect-ctx) effects))
        ; TODO
        ; context/source ?
        ; skill.context ?  ?
        ; generic context ?( projectile hit is not skill context)
        (dissoc :effect/source
                :effect/target
                :effect/direction
                :effect/target-position))))

; would have to do this only if effect even needs target ... ?
(defn- check-remove-target [{:keys [effect/source] :as ctx}]
  (update ctx :effect/target (fn [target]
                               (when (and target
                                          (not (:entity/destroyed? @target))
                                          (ctx/line-of-sight? ctx @source @target))
                                 target))))

(defn- applicable? [ctx effects]
  (let [ctx (check-remove-target ctx)]
    (some #(effect/applicable? % ctx) effects)))

(defn- mana-value [entity*]
  (if-let [mana (entity/stat entity* :stats/mana)]
    (mana 0)
    0))

(defn- not-enough-mana? [entity* {:keys [skill/cost]}]
  (> cost (mana-value entity*)))

(extend-type core.context.Context
  core.context/ActiveSkill
  (skill-usable-state [ctx
                       entity*
                       {:keys [skill/cooling-down? skill/effects] :as skill}]
    (cond
     cooling-down?
     :cooldown

     (not-enough-mana? entity* skill)
     :not-enough-mana

     (not (applicable? ctx effects))
     :invalid-params

     :else
     :usable)))

(defn- draw-skill-icon [g icon entity* [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions icon)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity*)) (float 0.15))
        center [x (+ y radius)]]
    (g/draw-filled-circle g center radius [1 1 1 0.125])
    (g/draw-sector g center radius
                   90 ; start-angle
                   (* (float action-counter-ratio) 360) ; degree
                   [1 1 1 0.5])
    (g/draw-image g icon [(- (float x) radius) y])))


(defn- apply-action-speed-modifier [entity* skill action-time]
  (/ action-time
     (or (entity/stat entity* (:skill/action-time-modifier-key skill))
         1)))

(defcomponent :active-skill
  {:let {:keys [eid skill effect-ctx counter]}}
  (component/create [[_ eid [skill effect-ctx]] ctx]
    {:eid eid
     :skill skill
     :effect-ctx effect-ctx
     :counter (->> skill
                   :skill/action-time
                   (apply-action-speed-modifier @eid skill)
                   (->counter ctx))})

  (state/player-enter [_]
    [[:tx/cursor :cursors/sandclock]])

  (state/pause-game? [_]
    false)

  (state/enter [_ ctx]
    [[:tx/sound (:skill/start-action-sound skill)]
     (when (:skill/cooldown skill)
       [:tx/assoc-in eid [:entity/skills (:property/id skill) :skill/cooling-down?] (->counter ctx (:skill/cooldown skill))])
     (when-not (zero? (:skill/cost skill))
       [:tx.entity.stats/pay-mana-cost eid (:skill/cost skill)])])

  (entity/tick [_ eid context]
    (cond
     (not (applicable? (safe-merge context effect-ctx) (:skill/effects skill)))
     [[:tx/event eid :action-done]
      ; TODO some sound ?
      ]

     (stopped? context counter)
     [[:tx/event eid :action-done]
      [:tx/effect effect-ctx (:skill/effects skill)]]))

  (entity/render-info [_ entity* g ctx]
    (let [{:keys [entity/image skill/effects]} skill]
      (draw-skill-icon g image entity* (:position entity*) (finished-ratio ctx counter))
      (run! #(effect/render % g (merge ctx effect-ctx)) effects))))