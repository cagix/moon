(ns cdq.stage
  (:require [cdq.ui.message]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [clojure.graphics.viewport :as viewport]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.group :as group]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [com.badlogic.gdx.scenes.scene2d.ui.button :as button]
            com.badlogic.gdx.scenes.scene2d.ui.horizontal-group
            com.badlogic.gdx.scenes.scene2d.ui.stack
            [clojure.scene2d.vis-ui.window :as window]))

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
  (assert (not (-> stage
                   stage/root
                   (group/find-actor "cdq.ui.modal-window"))))
  (stage/add! stage
              (scene2d/build
               {:actor/type :actor.type/window
                :title title
                :rows [[{:actor {:actor/type :actor.type/label
                                 :label/text text}}]
                       [{:actor {:actor/type :actor.type/text-button
                                 :text button-text
                                 :on-clicked (fn [_actor _ctx]
                                               (actor/remove! (-> stage
                                                                  stage/root
                                                                  (group/find-actor "cdq.ui.modal-window")))
                                               (on-click))}}]]
                :actor/name "cdq.ui.modal-window"
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
      stage/root
      (group/find-actor "cdq.ui.action-bar")
      (action-bar/add-skill! skill-properties)))

(defn remove-skill!
  [stage skill-id]
  (-> stage
      stage/root
      (group/find-actor "cdq.ui.action-bar")
      (action-bar/remove-skill! skill-id)))

(defn action-bar-selected-skill [stage]
  (-> stage
      stage/root
      (group/find-actor "cdq.ui.action-bar")
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
      (group/find-actor "cdq.ui.windows.entity-info")
      actor/toggle-visible!))

(defn close-all-windows! [stage]
  (->> (stage-find stage "cdq.ui.windows")
       group/children
       (run! #(actor/set-visible! % false))))

(defn actor-information [stage actor]
  (let [inventory-slot (inventory-window/cell-with-item? (-> stage
                                                             (stage-find "cdq.ui.windows")
                                                             (group/find-actor "cdq.ui.windows.inventory"))
                                                         actor)]
    (cond
     inventory-slot            [:mouseover-actor/inventory-cell inventory-slot]
     (window/title-bar? actor) [:mouseover-actor/window-title-bar]
     (button/is?        actor) [:mouseover-actor/button]
     :else                     [:mouseover-actor/unspecified])))

(defn mouseover-actor [stage mouse-position]
  (stage/hit stage
             (viewport/unproject (stage/viewport stage) mouse-position)))
