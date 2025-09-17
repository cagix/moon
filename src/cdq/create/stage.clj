(ns cdq.create.stage
  (:require [cdq.application :as application]
            cdq.ctx.stage
            [cdq.ui.message]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.windows.inventory :as inventory-window]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as group]
            [clojure.scene2d.stage :as stage]
            [clojure.gdx.scene2d.stage]))

(defn do! [ctx]
  (assoc ctx :ctx/stage (clojure.gdx.scene2d.stage/create
                         (:ctx/ui-viewport (:ctx/graphics ctx))
                         (:ctx/batch       (:ctx/graphics ctx))
                         application/state)))

(defn- stage-find [stage k]
  (-> stage
      stage/root
      (group/find-actor k)))

(extend-type com.badlogic.gdx.scenes.scene2d.Stage
  cdq.ctx.stage/Stage
  (viewport-width  [stage] (:viewport/width  (stage/viewport stage)))
  (viewport-height [stage] (:viewport/height (stage/viewport stage)))

  (inventory-window-visible? [stage]
    (-> stage
        (stage-find "cdq.ui.windows")
        (group/find-actor "cdq.ui.windows.inventory")
        actor/visible?))

  (toggle-inventory-visible! [stage]
    (-> stage
        (stage-find "cdq.ui.windows")
        (group/find-actor "cdq.ui.windows.inventory")
        actor/toggle-visible!))

  ; no window movable type cursor appears here like in player idle
  ; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
  ; => input events handling
  ; hmmm interesting ... can disable @ item in cursor  / moving / etc.
  (show-modal-window! [stage
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
                         [{:actor/type :actor.type/text-button
                           :text button-text
                           :on-clicked (fn [_actor _ctx]
                                         (actor/remove! (-> stage
                                                            stage/root
                                                            (group/find-actor "cdq.ui.modal-window")))
                                         (on-click))}]]
                  :actor/name "cdq.ui.modal-window"
                  :modal? true
                  :actor/center-position [(/ (:viewport/width  ui-viewport) 2)
                                          (* (:viewport/height ui-viewport) (/ 3 4))]
                  :pack? true})))

  (set-item!
    [stage cell item-properties]
    (-> stage
        (stage-find "cdq.ui.windows")
        (group/find-actor "cdq.ui.windows.inventory")
        (inventory-window/set-item! cell item-properties)))

  (remove-item!
    [stage inventory-cell]
    (-> stage
        (stage-find "cdq.ui.windows")
        (group/find-actor "cdq.ui.windows.inventory")
        (inventory-window/remove-item! inventory-cell)))

  (add-skill!
    [stage skill-properties]
    (-> stage
        stage/root
        (group/find-actor "cdq.ui.action-bar")
        (action-bar/add-skill! skill-properties)))

  (remove-skill!
    [stage skill-id]
    (-> stage
        stage/root
        (group/find-actor "cdq.ui.action-bar")
        (action-bar/remove-skill! skill-id)))

  (action-bar-selected-skill [stage]
    (-> stage
        stage/root
        (group/find-actor "cdq.ui.action-bar")
        action-bar/selected-skill))

  (show-text-message!
    [stage message]
    (-> stage
        stage/root
        (group/find-actor "player-message")
        (cdq.ui.message/show! message)))

  (toggle-entity-info-window! [stage]
    (-> stage
        (stage-find "cdq.ui.windows")
        (group/find-actor "cdq.ui.windows.entity-info")
        actor/toggle-visible!))

  (close-all-windows! [stage]
    (->> (stage-find stage "cdq.ui.windows")
         group/children
         (run! #(actor/set-visible! % false)))))
