(ns cdq.ui.stage
  (:require [cdq.ui.action-bar :as action-bar]
            [cdq.ui.windows.inventory :as inventory-window]
            [cdq.ui.message]
            [gdl.ui.actor :as actor]
            [gdl.ui.stage :as stage]
            [gdx.ui :as ui]))

(defn add-action-bar-skill! [stage item-opts]
  (-> stage
      :action-bar
      (action-bar/add-skill! item-opts)))

(defn remove-action-bar-skill! [stage id]
  (-> stage
      :action-bar
      (action-bar/remove-skill! id)))

(defn set-inventory-item! [stage inventory-cell item-opts]
  (-> stage
      :windows
      :inventory-window
      (inventory-window/set-item! inventory-cell item-opts)))

(defn remove-inventory-item! [stage inventory-cell]
  (-> stage
      :windows
      :inventory-window
      (inventory-window/remove-item! inventory-cell)))

(defn show-player-ui-msg! [stage message]
  (-> stage
      (stage/find-actor "player-message")
      (cdq.ui.message/show! message)))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal-window! [stage
                          ui-viewport
                          {:keys [title text button-text on-click]}]
  (assert (not (::modal stage)))
  (stage/add! stage
              (ui/window {:title title
                          :rows [[{:actor {:actor/type :actor.type/label
                                           :label/text text}}]
                                 [(ui/text-button button-text
                                                  (fn [_actor _ctx]
                                                    (actor/remove! (::modal stage))
                                                    (on-click)))]]
                          :id ::modal
                          :modal? true
                          :center-position [(/ (:width  ui-viewport) 2)
                                            (* (:height ui-viewport) (/ 3 4))]
                          :pack? true})))

(defn toggle-inventory-visible! [stage]
  (-> stage :windows :inventory-window actor/toggle-visible!))
