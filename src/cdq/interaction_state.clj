(ns cdq.interaction-state
  (:require [cdq.world.entity :as entity]
            [cdq.ctx.stage :as stage]
            [cdq.ui.windows.inventory :as inventory-window]
            [cdq.c :as c]
            [cdq.gdx.math.vector2 :as v]
            [cdq.ui.actor :as actor]))

(defn distance [a b]
  (v/distance (entity/position a)
              (entity/position b)))

(defn in-click-range? [player-entity clicked-entity]
  (< (distance player-entity clicked-entity)
     (:entity/click-distance-tiles player-entity)))

(defn- player-effect-ctx [{:keys [ctx/world]
                           :as ctx}
                          eid]
  (let [mouseover-eid (:world/mouseover-eid world)
        target-position (or (and mouseover-eid
                                 (entity/position @mouseover-eid))
                            (c/world-mouse-position ctx))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (entity/position @eid) target-position)}))

(defn interaction-state
  [{:keys [ctx/stage
           ctx/world] :as ctx}
   player-eid]
  (let [mouseover-eid (:world/mouseover-eid world)
        mouseover-actor (c/mouseover-actor ctx)]
    (cond
     mouseover-actor
     [:interaction-state/mouseover-actor mouseover-actor]

     (and mouseover-eid
          (:entity/clickable @mouseover-eid))
     [:interaction-state/clickable-mouseover-eid
      {:clicked-eid mouseover-eid
       :in-click-range? (in-click-range? @player-eid @mouseover-eid)}]

     :else
     (if-let [skill-id (stage/action-bar-selected-skill stage)]
       (let [entity @player-eid
             skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx ctx player-eid)
             state (entity/skill-usable-state entity skill effect-ctx)]
         (if (= state :usable)
           [:interaction-state.skill/usable [skill effect-ctx]]
           [:interaction-state.skill/not-usable state]))
       [:interaction-state/no-skill-selected]))))

(defn ->cursor [player-eid ctx]
  (let [[k params] (interaction-state ctx player-eid)]
    (case k
      :interaction-state/mouseover-actor
      (let [actor params]
        (let [inventory-slot (inventory-window/cell-with-item? actor)]
          (cond
           (and inventory-slot
                (get-in (:entity/inventory @player-eid) inventory-slot)) :cursors/hand-before-grab
           (actor/window-title-bar? actor) :cursors/move-window
           (actor/button?           actor) :cursors/over-button
           :else :cursors/default)))

      :interaction-state/clickable-mouseover-eid
      (let [{:keys [clicked-eid
                    in-click-range?]} params]
        (case (:type (:entity/clickable @clicked-eid))
          :clickable/item (if in-click-range?
                            :cursors/hand-before-grab
                            :cursors/hand-before-grab-gray)
          :clickable/player :cursors/bag))

      :interaction-state.skill/usable
      :cursors/use-skill

      :interaction-state.skill/not-usable
      :cursors/skill-not-usable

      :interaction-state/no-skill-selected
      :cursors/no-skill-selected)))
