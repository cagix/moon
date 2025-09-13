(ns cdq.gdx.stage
  (:require [cdq.ui.message]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.windows.inventory :as inventory-window]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as group]
            [clojure.scene2d.stage :as stage]
            [clojure.vis-ui.widget :as widget]))

(defn- stage-find [stage k]
  (-> stage
      stage/root
      (group/find-actor k)))

(defn viewport-width  [stage] (:viewport/width  (stage/viewport stage)))
(defn viewport-height [stage] (:viewport/height (stage/viewport stage)))

(defn inventory-window-visible? [stage]
  (-> stage
      (stage-find "cdq.ui.windows")
      (group/find-actor "cdq.ui.windows.inventory")
      actor/visible?))

(defn toggle-inventory-visible! [stage]
  (-> stage
      (stage-find "cdq.ui.windows")
      (group/find-actor "cdq.ui.windows.inventory")
      actor/toggle-visible!))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal-window! [stage
                          ui-viewport
                          {:keys [title text button-text on-click]}]
  (assert (not (::modal stage)))
  (stage/add! stage
              (widget/window {:title title
                              :rows [[{:actor {:actor/type :actor.type/label
                                               :label/text text}}]
                                     [(widget/text-button button-text
                                                          (fn [_actor _ctx]
                                                            (actor/remove! (::modal stage))
                                                            (on-click)))]]
                              :actor/user-object ::modal
                              :modal? true
                              :actor/center-position [(/ (:viewport/width  ui-viewport) 2)
                                                      (* (:viewport/height ui-viewport) (/ 3 4))]
                              :pack? true})))

(defn set-item!
  [stage cell item-properties]
  (-> stage
      (stage-find "cdq.ui.windows")
      (group/find-actor "cdq.ui.windows.inventory")
      (inventory-window/set-item! cell item-properties)))

(defn remove-item!
  [stage inventory-cell]
  (-> stage
      (stage-find "cdq.ui.windows")
      (group/find-actor "cdq.ui.windows.inventory")
      (inventory-window/remove-item! inventory-cell)))

(defn add-skill!
  [stage skill-properties]
  (-> stage
      :action-bar
      (action-bar/add-skill! skill-properties)))

(defn remove-skill!
  [stage skill-id]
  (-> stage
      :action-bar
      (action-bar/remove-skill! skill-id)))

(defn action-bar-selected-skill [stage]
  (-> stage
      :action-bar
      action-bar/selected-skill))

(defn show-text-message!
  [stage message]
  (-> stage
      stage/root
      (group/find-actor "player-message")
      (cdq.ui.message/show! message)))

(defn toggle-entity-info-window! [stage]
  (-> stage
      (stage-find "cdq.ui.windows")
      :entity-info-window
      actor/toggle-visible!))

(defn close-all-windows! [stage]
  (->> (stage-find stage "cdq.ui.windows")
       group/children
       (run! #(actor/set-visible! % false))))
