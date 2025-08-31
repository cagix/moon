(ns cdq.stage
  (:require [cdq.ui.action-bar :as action-bar]
            [cdq.ui.windows.inventory :as inventory-window]
            [cdq.ui.message]
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.stage :as stage]
            [cdq.gdx.ui :as ui]))

(defn create! [graphics config]
  (ui/load! config)
  (ui/stage (:ui-viewport graphics)
            (:batch       graphics)))

(defn dispose! []
  (ui/dispose!))

(defn action-bar-selected-skill [stage]
  (-> stage
      :action-bar
      action-bar/selected-skill))

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
                          :center-position [(/ (:viewport/width  ui-viewport) 2)
                                            (* (:viewport/height ui-viewport) (/ 3 4))]
                          :pack? true})))

(defn toggle-inventory-visible! [stage]
  (-> stage :windows :inventory-window actor/toggle-visible!))

(defn toggle-entity-info-window! [stage]
  (-> stage :windows :entity-info-window actor/toggle-visible!))

(defn close-all-windows! [stage]
  (run! #(actor/set-visible! % false) (group/children (:windows stage))))
