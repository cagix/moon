(ns cdq.application.create.stage
  (:require [cdq.graphics :as graphics]
            [cdq.stage]
            [cdq.ui.message]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [clojure.gdx :as gdx]
            [clojure.graphics.viewport :as viewport]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.ctx]
            [gdl.scene2d.group :as group]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui.button :as button]
            gdl.scene2d.ui.horizontal-group
            gdl.scene2d.ui.stack
            [gdl.scene2d.vis-ui.window :as window]))

(defn do!
  [{:keys [ctx/graphics]
    :as ctx}]
  (extend-type (class ctx)
    gdl.scene2d.ctx/Graphics
    (draw! [{:keys [ctx/graphics]} draws]
      (graphics/handle-draws! graphics draws)))
  (assoc ctx :ctx/stage (gdx/stage (:graphics/ui-viewport graphics)
                                   (:graphics/batch       graphics))))

(defn- stage-find [stage k]
  (-> stage
      stage/root
      (group/find-actor k)))

(extend-type gdl.scene2d.Stage
  cdq.stage/Stage
  (rebuild-actors! [stage db graphics]
    (stage/clear! stage)
    (let [actors [((requiring-resolve 'cdq.application.create.stage.dev-menu/create) db graphics)
                  ((requiring-resolve 'cdq.application.create.stage.action-bar/create))
                  ((requiring-resolve 'cdq.application.create.stage.hp-mana-bar/create) stage graphics)
                  ((requiring-resolve 'cdq.application.create.stage.windows/create) stage graphics)
                  ((requiring-resolve 'cdq.application.create.stage.player-state-draw/create))
                  ((requiring-resolve 'cdq.application.create.stage.message/create))]]
      (doseq [actor actors]
        (stage/add! stage (scene2d/build actor)))))

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
         (run! #(actor/set-visible! % false))))

  (actor-information [stage actor]
    (let [inventory-slot (inventory-window/cell-with-item? (-> stage
                                                               (stage-find "cdq.ui.windows")
                                                               (group/find-actor "cdq.ui.windows.inventory"))
                                                           actor)]
      (cond
       inventory-slot            [:mouseover-actor/inventory-cell inventory-slot]
       (window/title-bar? actor) [:mouseover-actor/window-title-bar]
       (button/is?        actor) [:mouseover-actor/button]
       :else                     [:mouseover-actor/unspecified])))

  (mouseover-actor [stage mouse-position]
    (stage/hit stage
               (viewport/unproject (stage/viewport stage) mouse-position))))
