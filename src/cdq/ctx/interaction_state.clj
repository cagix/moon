(ns cdq.ctx.interaction-state
  (:require [cdq.entity :as entity]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.windows.inventory :as inventory-window]
            [cdq.ctx :as ctx]
            [gdl.ui :as ui]
            [gdl.c :as c]))

(defn- mouseover-actor->cursor [actor player-entity-inventory]
  (let [inventory-slot (inventory-window/cell-with-item? actor)]
    (cond
     (and inventory-slot
         (get-in player-entity-inventory inventory-slot)) :cursors/hand-before-grab
     (ui/window-title-bar? actor) :cursors/move-window
     (ui/button? actor) :cursors/over-button
     :else :cursors/default)))

; Checks with mouse position (as input?)
; and either over an ui actor, over a clickable world entity
; or we try to use an action (see if selected, skill, then check usable state)
;[:interaction-state/mouseover-ui-actor mouseover-actor]
;[:interaction-state/clickable-mouseover-entity mouseover-eid]
;[:interaction-state/try-use-action ]
(defn interaction-state [{:keys [ctx/mouseover-eid
                                 ctx/stage]
                          :as ctx}
                         eid]
  (let [entity @eid
        mouseover-actor (c/mouseover-actor ctx)]
    (cond
     mouseover-actor
     [(mouseover-actor->cursor mouseover-actor (:entity/inventory entity))
      nil] ; handled by actors themself, they check player state

     (and mouseover-eid
          (:entity/clickable @mouseover-eid))
     (ctx/clickable-entity-interaction ctx entity mouseover-eid)

     :else
     (if-let [skill-id (action-bar/selected-skill (:action-bar stage))]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (ctx/player-effect-ctx ctx eid)
             state (entity/skill-usable-state entity skill effect-ctx)]
         (if (= state :usable)
           ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
           ; different color ?
           ; => e.g. meditation no TARGET .. etc.
           [:cursors/use-skill
            [[:tx/event eid :start-action [skill effect-ctx]]]]
           ; TODO cursor as of usable state
           ; cooldown -> sanduhr kleine
           ; not-enough-mana x mit kreis?
           ; invalid-params -> depends on params ...
           [:cursors/skill-not-usable
            [[:tx/sound "bfxr_denied"]
             [:tx/show-message (case state
                                 :cooldown "Skill is still on cooldown"
                                 :not-enough-mana "Not enough mana"
                                 :invalid-params "Cannot use this here")]]]))
       [:cursors/no-skill-selected
        [[:tx/sound "bfxr_denied"]
         [:tx/show-message "No selected skill"]]]))))

(defn ->cursor [player-eid ctx]
  (let [[cursor _on-click] (interaction-state ctx player-eid)]
    cursor))
