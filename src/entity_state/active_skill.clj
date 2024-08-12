(ns entity-state.active-skill
  (:require [data.val-max :refer [apply-val]]
            [api.context :refer [stopped? finished-ratio ->counter]]
            [api.entity :as entity]
            [api.entity-state :as state]
            [api.graphics :as g]
            [effect-ctx.core :as effect-ctx]))

(defn skill-usable-state [effect-ctx
                          entity*
                          {:keys [skill/cost skill/cooling-down? skill/effect]}]
  (cond
   cooling-down?
   :cooldown

   (and cost (> cost ((entity/mana entity*) 0)))
   :not-enough-mana

   (not (effect-ctx/valid-params? effect-ctx effect))
   :invalid-params

   :else
   :usable))

(defn- draw-skill-icon [g icon entity* [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions icon)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height (:entity/body entity*))))
        center [x (+ y radius)]]
    (g/draw-filled-circle g center radius [1 1 1 0.125])
    (g/draw-sector g center radius
                   90 ; start-angle
                   (* (float action-counter-ratio) 360) ; degree
                   [1 1 1 0.5])
    (g/draw-image g icon [(- (float x) radius) y])))

(defn- set-skill-to-cooldown [entity* {:keys [property/id skill/cooldown] :as skill} ctx]
  (when cooldown
    [:tx.entity/assoc-in (:entity/id entity*) [:entity/skills id :skill/cooling-down?] (->counter ctx cooldown)]))

(defn- pay-skill-mana-cost [{:keys [entity/id] :as entity*} {:keys [skill/cost]}]
  (when cost
    [:tx.entity/assoc-in id [:entity/stats :stats/mana 0] (- ((entity/mana entity*) 0) cost)]))

(comment
 (require '[api.context :as ctx])

 (let [entity* (ctx/player-entity* @app.state/current-context)]
   (pay-skill-mana-cost entity* {:skill/cost 4})
   ; [46 50] and then next entity/mana returns 25 max .....
   ; apply-val & apply-max make private ?
   )
  @app.state/current-context

  ; mana 92/100 ...
  ; working with uncalculated mana ....
  ; o.o
  ; even with uncalculated hp ....

  ; base value [100 100]
  ; effective  [50 50]
  ; now pay 4 mana cost (on effective value)

 )

(defrecord ActiveSkill [skill effect-ctx counter]
  state/PlayerState
  (player-enter [_] [[:tx.context.cursor/set :cursors/sandclock]])
  (pause-game? [_] false)
  (manual-tick [_ entity* context])
  (clicked-inventory-cell [_ entity* cell])
  (clicked-skillmenu-skill [_ entity* skill])

  state/State
  (enter [_ entity* ctx]
    [[:tx/sound (:skill/start-action-sound skill)]
     (set-skill-to-cooldown entity* skill ctx)
     (pay-skill-mana-cost entity* skill)])

  (exit [_ entity* _ctx])

  (tick [_ {:keys [entity/id]} context]
    (cond
     (not (effect-ctx/valid-params? effect-ctx (:skill/effect skill)))
     [[:tx/event id :action-done]]

     (stopped? context counter)
     (cons [:tx/event id :action-done]
           (effect-ctx/txs effect-ctx (:skill/effect skill)))))

  (render-below [_ entity* g _ctx])
  (render-above [_ entity* g _ctx])
  (render-info [_ entity* g ctx]
    (let [{:keys [property/image skill/effect]} skill]
      (draw-skill-icon g image entity* (entity/position entity*) (finished-ratio ctx counter))
      (effect-ctx/render-info effect-ctx g (:skill/effect skill)))))

(defn- apply-action-speed-modifier [entity* skill action-time]
  (/ action-time
     (or (get (:entity/stats entity*)
              (:skill/action-time-modifier-key skill))
         1)))

(defn ->CreateWithCounter [context entity* [skill effect-ctx]]
  (->ActiveSkill skill
                 effect-ctx
                 (->> skill
                      :skill/action-time
                      (apply-action-speed-modifier entity* skill)
                      (->counter context))))
